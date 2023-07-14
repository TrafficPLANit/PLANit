package org.goplanit.algorithms.nodemodel;

import java.util.ArrayList;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.ojalgo.array.Array1D;
import org.ojalgo.function.PrimitiveFunction;

/**
 * Inner class that holds the mapping of the inputs to/from the underlying physical network (if any). Currently we support the PLANit network format for this mapping, or one
 * provides the fixed input separate from a particular network format
 * 
 * By default we:
 * <ul>
 * <li>map incoming link segments in order of appearance to index a=0,...,|A^in|-1</li>
 * <li>map outgoing link segments in order of appearance to index b=0,...,|B^out|-1</li>
 * <li>extract incoming link capacities C_a</li>
 * <li>extract receiving flow capacities such that R_b=C_b</li>
 * </ul>
 * 
 * Note that in case the receiving flows are not fixed at the outgoing link's capacity this can be omitted and provided as a separate input via the TampereNodeModelInput
 * 
 */
public class TampereNodeModelFixedInput {

  /**
   * Map link segments to local data mapping structure in order of appearance, i.e., first incoming link segment is placed in index 0, second in index 1 etc.
   * 
   * @param linkSegments, to map to
   * @param edgeSegments  to map from
   * @throws PlanItException
   */
  private void mapLinkSegments(ArrayList<MacroscopicLinkSegment> linkSegments, Iterable<? extends EdgeSegment> edgeSegments) throws PlanItException {
    PlanItException.throwIf(edgeSegments == null, "edge segments to map are null");

    for (var incomingLinkSegment : edgeSegments) {
      PlanItException.throwIf(!(incomingLinkSegment instanceof MacroscopicLinkSegment), "Edges of node are not of type MacroScopicLinkSegment when mapping in Tampere node model");

      linkSegments.add((MacroscopicLinkSegment) incomingLinkSegment);
    }
  }

  /**
   * initialise array with link segment capacities. It is assumed the incoming link segments are available
   * 
   * @param arrayToInitialise the array to initialise
   * @param linkSegments      to extract capacities from
   * @throws PlanItException thrown if link segments are null
   */
  private void initialiseWithCapacity(Array1D<Double> arrayToInitialise, ArrayList<MacroscopicLinkSegment> linkSegments) throws PlanItException {
    PlanItException.throwIf(linkSegments == null, "link segments to extract capacity from are null");

    arrayToInitialise = Array1D.PRIMITIVE64.makeZero(linkSegments.size());
    for (var linkSegment : linkSegments) {
      arrayToInitialise.add(Math.min(getMaxInLinkSegmentCapacity(), linkSegment.getCapacityOrDefaultPcuH()));
    }
  }

  /**
   * Map incoming link segments to node model compatible index in order of appearance, i.e., first incoming link segment is placed in index 0, second in index 1 etc.
   * 
   * @param incomingEdgeSegments to map
   * @throws PlanItException
   */
  private void mapIncomingLinkSegments(Iterable<? extends EdgeSegment> incomingEdgeSegments, int numSegments) throws PlanItException {
    this.incomingLinkSegments = new ArrayList<MacroscopicLinkSegment>(numSegments);
    mapLinkSegments(incomingLinkSegments, incomingEdgeSegments);
  }

  /**
   * Map outgoingEdgeSegments link segments to node model compatible index in order of appearance, i.e., first incoming link segment is placed in index 0, second in index 1 etc.
   * 
   * @param outgoingEdgeSegments to map
   * @throws PlanItException
   */
  private void mapOutgoingLinkSegments(Iterable<? extends EdgeSegment> outgoingEdgeSegments, int numSegments) throws PlanItException {
    this.outgoingLinkSegments = new ArrayList<MacroscopicLinkSegment>(numSegments);
    mapLinkSegments(outgoingLinkSegments, outgoingEdgeSegments);
  }

  /**
   * Extract the available incoming link capacities. It is assumed the incoming link segments are available
   * 
   * @throws PlanItException thrown if error
   */
  private void initialiseIncomingLinkSegmentCapacities() throws PlanItException {
    initialiseWithCapacity(incomingLinkSegmentCapacities, incomingLinkSegments);
  }

  /**
   * Extract the available outgoing link receiving flows by setting them to capacity. It is assumed the incoming link segments are available. Note that assignment methods that
   * support storage constraints, i.e., spillback do not always have receiving flows equal to capacity. In that case receiving flows are not fixed and should not be initialised
   * here but instead by provided on-the-fly for each tampere node model update
   * 
   * @param initialiseReceivingFlowsAtCapacity
   * @throws PlanItException thrown if error
   */
  private void initialiseOutoingLinkSegmentReceivingFlows(boolean initialiseReceivingFlowsAtCapacity) throws PlanItException {
    if (initialiseReceivingFlowsAtCapacity) {
      initialiseWithCapacity(outgoingLinkSegmentReceivingFlows, outgoingLinkSegments);
    } else {
      outgoingLinkSegmentReceivingFlows = null;
    }
  }

  /** in case in link shave no capacity set, or an physically infeasible capacity, it is capped to this capacity */
  protected double maxInLinkSegmentCapacity = DEFAULT_MAX_IN_CAPACITY;

  /** mapping of incoming link index to link segment (if any), i.e., a=1,...|A^in|-1 */
  protected ArrayList<MacroscopicLinkSegment> incomingLinkSegments;
  /** mapping of outgoing link index to link segment (if any), i.e.e, b=1,...|B^out|-1 */
  protected ArrayList<MacroscopicLinkSegment> outgoingLinkSegments;

  /** store the capacities of each incoming link segment, i.e., C_a */
  protected Array1D<Double> incomingLinkSegmentCapacities;
  /** store the receiving flows of each outgoing link segment at capacity, i.e., R_b=C_b */
  protected Array1D<Double> outgoingLinkSegmentReceivingFlows;

  /** default max in capacity */
  public static double DEFAULT_MAX_IN_CAPACITY = 10_000.0;

  /**
   * Constructor. The TampereNodeModelFixedInput class is meant to be created once for each node where the node model is applied more than once. All fixed inputs conditioned on the
   * network infrastructure are stored here and therefore can be reused with every update of the node model throughout a simulation.
   * 
   * 
   * @param node                               to use for extracting static inputs
   * @param initialiseReceivingFlowsAtCapacity indicate to initialise receiving flows at capacity (true), or not initialise them at all
   * @throws PlanItException thrown when error occurs
   */
  public TampereNodeModelFixedInput(Node node, boolean initialiseReceivingFlowsAtCapacity) throws PlanItException {
    // Set A^in
    mapIncomingLinkSegments(node.getEntryEdgeSegments(), node.getNumberOfEntryEdgeSegments());
    // Set A^out
    mapOutgoingLinkSegments(node.getExitEdgeSegments(), node.getNumberOfExitEdgeSegments());
    // Set C_a
    initialiseIncomingLinkSegmentCapacities();
    // Set R_b
    initialiseOutoingLinkSegmentReceivingFlows(initialiseReceivingFlowsAtCapacity);
  }

  /**
   * Constructor. Using this constructor does not require any dependency on PLANit network infrastructure.
   * 
   * @param incomingLinkSegmentCapacities     to use
   * @param outgoingLinkSegmentReceivingFlows to use
   */
  public TampereNodeModelFixedInput(Array1D<Double> incomingLinkSegmentCapacities, Array1D<Double> outgoingLinkSegmentReceivingFlows) {
    this.incomingLinkSegmentCapacities = incomingLinkSegmentCapacities.copy();
    this.outgoingLinkSegmentReceivingFlows = outgoingLinkSegmentReceivingFlows.copy();
  }

  /**
   * Constructor. Using this constructor does not require any dependency on PLANit network infrastructure
   * 
   * @param incomingLinkSegmentCapacities to use
   */
  public TampereNodeModelFixedInput(Array1D<Double> incomingLinkSegmentCapacities) {
    this.incomingLinkSegmentCapacities = incomingLinkSegmentCapacities.copy();
    this.outgoingLinkSegmentReceivingFlows = null;
  }

  /**
   * Based on the set maximum in link capacity, check and update current in link capacities if they exceed this maximum
   */
  public void capInLinkCapacitiesToMaximum() {
    PrimitiveFunction.Unary capToMaxCapacity = v -> Math.max(getMaxInLinkSegmentCapacity(), v);
    incomingLinkSegmentCapacities.modifyAll(capToMaxCapacity);
  }

  /**
   * Collect number of incoming link segments
   * 
   * @return number of incoming link segments
   */
  public int getNumberOfIncomingLinkSegments() {
    return incomingLinkSegmentCapacities.size();
  }

  /**
   * Collect number of outgoing link segments
   * 
   * @return number of outgoing link segments
   */
  public int getNumberOfOutgoingLinkSegments() {
    return outgoingLinkSegmentReceivingFlows.size();
  }

  /**
   * Collect current maximum in link capacity that is being used
   * 
   * @return max in capacity
   */
  public double getMaxInLinkSegmentCapacity() {
    return maxInLinkSegmentCapacity;
  }

  /**
   * Collect current maximum in link capacity that is being used
   *
   * @param maxInLinkSegmentCapacity set the capacity
   */
  public void setMaxInLinkSegmentCapacity(double maxInLinkSegmentCapacity) {
    this.maxInLinkSegmentCapacity = maxInLinkSegmentCapacity;
  }

}