package org.goplanit.assignment.ltm.sltm.loading;

import java.util.TreeSet;
import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.ojalgo.array.Array1D;

/**
 * Store the splitting rates used during sLTM loading updates (Step 1 and 5). This implementation tracks all
 * splitting rates of turns used by a path, assuming they are potentially blocking. Tracked nodes must be registered
 * explicitly (not all nodes might be used in loading), but the information is stored differently to reduce the memory
 * footprint. All nodes of used paths are tracked, giving a complete picture of the network, which requires more memory
 * than the partial implementation. This way of tracking is compatible with the PhysicalQueue solution methods and the
 * Advanced PointQueue solution method.
 * 
 * @author markr
 *
 */
public class NetworkLoadingSplittingRateDataComplete extends NetworkLoadingSplittingRateDataBase
    implements NetworkLoadingSplittingRateData {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(
      NetworkLoadingSplittingRateDataComplete.class.getCanonicalName());

  /** the activated nodes for which we are tracking splitting rates for their entry link segments */
  private final TreeSet<DirectedVertex> activatedNodes;

  /**
   * Splitting rates per link segment (as Array1D of different lengths). Only activated link segments have a splitting
   * rate array, to minimise memory use. The array cannot be typed, as Array1D has no public default constructor
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
   * @param numberOfVertices in the network
   * @param numberOfLinkSegments in the network
   */
  public NetworkLoadingSplittingRateDataComplete(int numberOfVertices, long numberOfLinkSegments) {
    super(numberOfVertices);
    this.splittingRates = new Object[(int) numberOfLinkSegments];
    this.activatedNodes = new TreeSet<>();
  }

  /**
   * Activate a node so we are able to track its splitting rates (and turn flows) during loading.
   * It is regarded as both tracked and potentially blocking
   * 
   * @param trackedNode to start tracking turn flows and splitting rates for
   */
  @Override
  public void registerTrackedNode(DirectedVertex trackedNode) {
    int numberOfExitLinkSegments = trackedNode.getNumberOfExitEdgeSegments();
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

  @Override
  public void registerPotentiallyBlockingNode(final DirectedVertex potentiallyBlockingNode) {
    if (!isPotentiallyBlocking(potentiallyBlockingNode)) {
      registerTrackedNode(potentiallyBlockingNode); // in case not already done so
    }
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
