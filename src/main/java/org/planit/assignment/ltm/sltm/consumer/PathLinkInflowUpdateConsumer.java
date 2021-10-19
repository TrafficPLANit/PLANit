package org.planit.assignment.ltm.sltm.consumer;

import java.util.logging.Logger;

import org.planit.od.path.OdPaths;
import org.planit.utils.functionalinterface.TriConsumer;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.zoning.OdZone;

/**
 * Consumer to apply during path based loading to update the inflows on the provided raw array by their link segment ids for each combination of origin, destination, and demand
 * 
 * @author markr
 *
 */
public class PathLinkInflowUpdateConsumer implements TriConsumer<OdZone, OdZone, Double> {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PathLinkInflowUpdateConsumer.class.getCanonicalName());

  /**
   * Od Paths to use
   */
  private final OdPaths odPaths;

  /**
   * flow acceptance factors (alpha) to use
   */
  final double[] linkSegmentFlowAcceptanceFactors;

  /**
   * Link segment inflows to populate
   */
  private final double[] linkSegmentSendingFlows;

  /**
   * constructor
   * 
   * @param odPaths                          to use
   * @param linkSegmentFlowAcceptanceFactors flow acceptance factors to apply to path flows while traversing the path
   * @param linkSegmentSendingFlows          to populate
   */
  public PathLinkInflowUpdateConsumer(final OdPaths odPaths, final double[] linkSegmentFlowAcceptanceFactors, final double[] linkSegmentSendingFlows) {
    this.odPaths = odPaths;
    this.linkSegmentSendingFlows = linkSegmentSendingFlows;
    this.linkSegmentFlowAcceptanceFactors = linkSegmentFlowAcceptanceFactors;
  }

  /**
   * Update the turn flow for the path of the given origin,destination,demand combination
   */
  @Override
  public void accept(OdZone origin, OdZone destination, Double odDemand) {
    /* path */
    DirectedPath odPath = odPaths.getValue(origin, destination);
    double acceptedPathFlowRate = odDemand;
    int segmentId;
    for (EdgeSegment edgeSegment : odPath) {
      /* link segment */
      segmentId = (int) edgeSegment.getId();
      /* u_a: update inflow for link segment */
      linkSegmentSendingFlows[segmentId] += acceptedPathFlowRate;
      acceptedPathFlowRate *= linkSegmentFlowAcceptanceFactors[segmentId];
    }
  }
}
