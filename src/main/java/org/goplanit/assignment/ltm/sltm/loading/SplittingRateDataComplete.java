package org.goplanit.assignment.ltm.sltm.loading;

import java.util.TreeSet;
import java.util.logging.Logger;

import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.ojalgo.array.Array1D;

/**
 * Store the splitting rates used during sLTM loading updates (Step 1 and 5). In this implementation we track all splitting rates of turns that are used by a path and assumed they
 * are potentially blocking. This implementation explicitly requires registering tracked nodes (not all nodes might be used in loading) but the way the information is stored is
 * different to reduce the memory footprint. All nodes of used paths are tracked providing a complete picture of the network. This requires more memory compared to the partial
 * implementation. This way of tracking is compatible with the PhysicalQueue solution methods as well as the Advanced PointQueue solution method.
 * 
 * @author markr
 *
 */
public class SplittingRateDataComplete implements SplittingRateData {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(SplittingRateDataComplete.class.getCanonicalName());

  /** the activated nodes for which we are tracking splitting rates for their entry link segments */
  private final TreeSet<DirectedVertex> activatedNodes;

  /**
   * Splitting rates per link segment (as different lengths Array1D), only activated link segments will have an actual instantiation of the splitting rate array to minimise memory
   * use. Also, we cannot have a typed array because Array1D has no public default constructor
   */
  private Object[] splittingRates;

  /**
   * Register splitting rates for entry segment
   * 
   * @param entrySegment             to use
   * @param numberOfExitLinkSegments to create splitting rates for
   */
  private void initialiseSplittingRates(EdgeSegment entrySegment, int numberOfExitLinkSegments) {
    splittingRates[(int) entrySegment.getId()] = Array1D.PRIMITIVE64.makeZero(numberOfExitLinkSegments);
  }

  /**
   * Constructor
   * 
   * @param numberOfLinkSegments in the network
   */
  public SplittingRateDataComplete(long numberOfLinkSegments) {
    super();
    this.splittingRates = new Object[(int) numberOfLinkSegments];
    this.activatedNodes = new TreeSet<DirectedVertex>();
  }

  /**
   * Activate a node so we are able to track its splitting rates (and turn flows) during loading. It is regarded as both tracked and potentially blocking
   * 
   * @param trackedNode to start tracking turn flows and splitting rates for
   */
  public void activateNode(DirectedVertex trackedNode) {
    int numberOfExitLinkSegments = trackedNode.getExitEdgeSegments().size();
    activatedNodes.add(trackedNode);
    for (var entrySegment : trackedNode.getEntryEdgeSegments()) {
      initialiseSplittingRates(entrySegment, numberOfExitLinkSegments);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPotentiallyBlocking(DirectedVertex nodeToVerify) {
    return activatedNodes.contains(nodeToVerify); // when tracked assumed potentially blocking
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isTracked(DirectedVertex nodeToVerify) {
    return activatedNodes.contains(nodeToVerify);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TreeSet<DirectedVertex> getTrackedNodes() {
    return this.activatedNodes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public Array1D<Double> getSplittingRates(final EdgeSegment entrySegment) {
    return (Array1D<Double>) splittingRates[(int) entrySegment.getId()];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetTrackedNodes() {
    this.activatedNodes.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetPotentiallyBlockingNodes() {
    resetTrackedNodes();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetSplittingRates() {
    this.splittingRates = new Object[splittingRates.length];
  }

}
