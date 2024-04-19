package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.List;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.StaticLtmDirectedPath;
import org.goplanit.od.path.OdMultiPaths;
import org.goplanit.utils.functionalinterface.TriConsumer;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.zoning.OdZone;

/**
 * Base Consumer to apply during path based flow update for each combination of origin, destination, and demand
 * <p>
 * Derived implementation can apply different changes to each of the (turn/link) flows on the known paths by providing different single flow update implementations that are applied
 * to each turn on each path with non-zero demand.
 * 
 * @author markr
 *
 */
public abstract class PathFlowUpdateConsumer<T extends NetworkFlowUpdateData> implements TriConsumer<OdZone, OdZone, Double> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PathFlowUpdateConsumer.class.getCanonicalName());

  /** data and configuration used for a flow update by derived classes */
  protected T dataConfig;

  /**
   * Od Paths to use
   */
  private final OdMultiPaths<StaticLtmDirectedPath, List<StaticLtmDirectedPath>> odMultiPaths;

  /**
   * Apply the flow to the turn (and update link sending flow if required)
   * 
   * @param prevSegment         of turn
   * @param currentSegment      of turn
   * @param turnSendingFlowPcuH sending flow rate of turn
   * @return accepted flow rate of turn after applying link acceptance factor
   */
  protected abstract double applySingleFlowUpdate(final EdgeSegment prevSegment, final EdgeSegment currentSegment, final double turnSendingFlowPcuH);

  /**
   * Apply the flow to a final path segment (and update link sending flow if required) which has no outgoing edge segment on the turn
   * 
   * @param lastEdgeSegment      of path
   * @param acceptedPathFlowRate sending flow rate on last edge segment
   */
  protected abstract void applyPathFinalSegmentFlowUpdate(final EdgeSegment lastEdgeSegment, double acceptedPathFlowRate);

  /**
   * Constructor
   * 
   * @param dataConfig to use
   * @param odMultiPaths    to use
   */
  public PathFlowUpdateConsumer(final T dataConfig, final OdMultiPaths<StaticLtmDirectedPath, List<StaticLtmDirectedPath>> odMultiPaths) {
    this.dataConfig = dataConfig;
    this.odMultiPaths = odMultiPaths;
  }

  /**
   * Update the turn flows for the path of the given origin,destination,demand combination
   */
  @Override
  public void accept(OdZone origin, OdZone destination, Double odDemand) {
    /* path */
    var odPaths = odMultiPaths.getValue(origin, destination);
    for (StaticLtmDirectedPath odPath : odPaths) {
      double acceptedPathFlowRate = odDemand * odPath.getPathChoiceProbability();
      if (odPath == null || odPath.isEmpty()) {
        LOGGER.warning(String.format("IGNORE: encountered empty path %s", odPath == null ? "" : odPath.getXmlId()));
        return;
      }

      /* turn */
      var edgeSegmentIter = odPath.iterator();
      var previousEdgeSegment = edgeSegmentIter.next();
      EdgeSegment currEdgeSegment = null;
      while (edgeSegmentIter.hasNext()) {
        currEdgeSegment = edgeSegmentIter.next();
        acceptedPathFlowRate = applySingleFlowUpdate(previousEdgeSegment, currEdgeSegment, acceptedPathFlowRate);
        previousEdgeSegment = currEdgeSegment;
      }

      applyPathFinalSegmentFlowUpdate(currEdgeSegment, acceptedPathFlowRate);
    }
  }
}
