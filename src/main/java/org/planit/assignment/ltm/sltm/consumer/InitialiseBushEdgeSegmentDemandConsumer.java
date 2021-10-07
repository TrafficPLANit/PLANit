package org.planit.assignment.ltm.sltm.consumer;

import java.util.function.Consumer;

import org.planit.assignment.ltm.sltm.Bush;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.directed.DirectedVertex;

/**
 * Initialise the bush for a given origin/destination with the shortest path being offered from the destination to the origin via the provided edge segments in the callback.
 * <p>
 * Add the edge segments to the bush and update the turn sending flow accordingly.
 * <p>
 * Consumer can be reused for multiple destinations by updating the destination and demand that goes with it.
 * 
 * @author markr
 *
 */
public class InitialiseBushEdgeSegmentDemandConsumer implements Consumer<EdgeSegment> {

  /** the bush to initialise */
  private final Bush originBush;

  /** current destination at hand */
  @SuppressWarnings("unused")
  private DirectedVertex currentDestination;

  /** od demand to apply to all edge segments */
  Double originDestinationDemandPcuH;

  private EdgeSegment succeedingEdgeSegment;

  /**
   * Reset to prep for next destination (if any)
   */
  private void reset() {
    this.succeedingEdgeSegment = null;
    this.originDestinationDemandPcuH = null;
    this.currentDestination = null;
  }

  /**
   * Constructor
   * 
   * @param originBush to use
   */
  public InitialiseBushEdgeSegmentDemandConsumer(final Bush originBush) {
    this.originBush = originBush;
    reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(EdgeSegment edgeSegment) {
    if (succeedingEdgeSegment != null) {
      originBush.addTurnSendingFlow(edgeSegment, succeedingEdgeSegment, originDestinationDemandPcuH);
    }
    succeedingEdgeSegment = edgeSegment;
  }

  /**
   * Update to next destination with accompanying demand. Demand is applied to all edge segments on the provided edge segments via the consumer callback
   * 
   * @param destination                 to set
   * @param originDestinationDemandPcuH total travel demand of the od combination
   */
  public void setDestination(DirectedVertex destination, double originDestinationDemandPcuH) {
    reset();
    this.currentDestination = destination;
    this.originDestinationDemandPcuH = originDestinationDemandPcuH;
  }

}
