package org.planit.algorithms.nodemodel;

import java.util.ArrayList;
import java.util.Set;

import org.ojalgo.array.Array1D;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;

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
  private void mapLinkSegments(ArrayList<MacroscopicLinkSegment> linkSegments, Set<EdgeSegment> edgeSegments) throws PlanItException {
    PlanItException.throwIf(edgeSegments == null, "edge segments to map are null");

    for (EdgeSegment incomingLinkSegment : edgeSegments) {
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
    for (MacroscopicLinkSegment linkSegment : linkSegments) {
      arrayToInitialise.add(linkSegment.computeCapacity());
    }
  }

  /**
   * Map incoming link segments to node model compatible index in order of appearance, i.e., first incoming link segment is placed in index 0, second in index 1 etc.
   * 
   * @param incomingEdgeSegments to map
   * @throws PlanItException
   */
  private void mapIncomingLinkSegments(Set<EdgeSegment> incomingEdgeSegments) throws PlanItException {
    this.incomingLinkSegments = new ArrayList<MacroscopicLinkSegment>(incomingEdgeSegments.size());
    mapLinkSegments(incomingLinkSegments, incomingEdgeSegments);
  }

  /**
   * Map outgoingEdgeSegments link segments to node model compatible index in order of appearance, i.e., first incoming link segment is placed in index 0, second in index 1 etc.
   * 
   * @param outgoingEdgeSegments to map
   * @throws PlanItException
   */
  private void mapOutgoingLinkSegments(Set<EdgeSegment> outgoingEdgeSegments) throws PlanItException {
    this.outgoingLinkSegments = new ArrayList<MacroscopicLinkSegment>(outgoingEdgeSegments.size());
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

  /** mapping of incoming link index to link segment (if any), i.e., a=1,...|A^in|-1 */
  protected ArrayList<MacroscopicLinkSegment> incomingLinkSegments;
  /** mapping of outgoing link index to link segment (if any), i.e.e, b=1,...|B^out|-1 */
  protected ArrayList<MacroscopicLinkSegment> outgoingLinkSegments;

  /** store the capacities of each incoming link segment, i.e., C_a */
  protected Array1D<Double> incomingLinkSegmentCapacities;
  /** store the receiving flows of each outgoing link segment at capacity, i.e., R_b=C_b */
  protected Array1D<Double> outgoingLinkSegmentReceivingFlows;

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
    mapIncomingLinkSegments(node.getEntryEdgeSegments());
    // Set A^out
    mapOutgoingLinkSegments(node.getEntryEdgeSegments());
    // Set C_a
    initialiseIncomingLinkSegmentCapacities();
    // Set R_b
    initialiseOutoingLinkSegmentReceivingFlows(initialiseReceivingFlowsAtCapacity);
  }

  /**
   * Constructor. Using this constructor does not require any dependency on PLANit network infrastructure
   * 
   * @param incomingLinkSegmentCapacities     to use
   * @param outgoingLinkSegmentReceivingFlows to use
   * @throws PlanItException thrown when error occurs
   */
  public TampereNodeModelFixedInput(Array1D<Double> incomingLinkSegmentCapacities, Array1D<Double> outgoingLinkSegmentReceivingFlows) throws PlanItException {
    this.incomingLinkSegmentCapacities = incomingLinkSegmentCapacities.copy();
    this.outgoingLinkSegmentReceivingFlows = incomingLinkSegmentCapacities.copy();
  }

  /**
   * Constructor. Using this constructor does not require any dependency on PLANit network infrastructure
   * 
   * @param incomingLinkSegmentCapacities to use
   * @throws PlanItException thrown when error occurs
   */
  public TampereNodeModelFixedInput(Array1D<Double> incomingLinkSegmentCapacities) throws PlanItException {
    this.incomingLinkSegmentCapacities = incomingLinkSegmentCapacities.copy();
    this.outgoingLinkSegmentReceivingFlows = null;
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

}