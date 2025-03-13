package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.StaticLtmDirectedPath;
import org.goplanit.od.path.OdMultiPaths;
import org.goplanit.utils.functionalinterface.TriConsumer;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.Movement;
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
public abstract class PathFlowUpdateConsumer<T extends NetworkFlowUpdateData>
        implements TriConsumer<OdZone, OdZone, Double> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PathFlowUpdateConsumer.class.getCanonicalName());

  /** data and configuration used for a flow update by derived classes */
  protected T dataConfig;

  /**
   * Od Paths to use
   */
  private final OdMultiPaths<StaticLtmDirectedPath, ? extends List<StaticLtmDirectedPath>> odMultiPaths;

  /** be able to convert entry/exit segment to their corresponding movement */
  protected final MultiKeyMap<Object,Movement> segmentPair2MovementMap;

  /**
   * Apply the flow to the turn (and update link sending flow if required)
   * 
   * @param movement         the movement
   * @param turnSendingFlowPcuH sending flow rate of turn
   * @param turnUnconstrainedFlowPcuH unconstrained flow rate of turn
   * @return accepted flow rate of turn after applying link acceptance factor
   */
  protected abstract double applySingleFlowUpdate(
          final Movement movement, final double turnSendingFlowPcuH, final double turnUnconstrainedFlowPcuH);

  /**
   * Apply the flow to a final path segment (and update link sending flow if required) which has no outgoing edge segment on the turn
   * 
   * @param lastEdgeSegment      of path
   * @param acceptedPathFlowRate sending flow rate on last edge segment
   * @param unconstrainedFlowPcuH unconstrained total demand on last edge segment
   */
  protected abstract void applyPathFinalSegmentFlowUpdate(
          final EdgeSegment lastEdgeSegment, double acceptedPathFlowRate, final double unconstrainedFlowPcuH);

  /**
   * Constructor
   * 
   * @param dataConfig to use
   * @param odMultiPaths    to use
   * @param segmentPair2MovementMap to use
   */
  public PathFlowUpdateConsumer(
          final T dataConfig,
          final OdMultiPaths<StaticLtmDirectedPath, ? extends List<StaticLtmDirectedPath>> odMultiPaths,
          final MultiKeyMap<Object,Movement> segmentPair2MovementMap) {
    this.dataConfig = dataConfig;
    this.odMultiPaths = odMultiPaths;
    this.segmentPair2MovementMap = segmentPair2MovementMap;
  }

  /**
   * Update the turn flows for the path of the given origin,destination,demand combination
   */
  @Override
  public void accept(OdZone origin, OdZone destination, Double odDemand) {
    /* path */
    var odPaths = odMultiPaths.getValue(origin, destination);
    if(odPaths == null){
      return; // no paths for some reason, skip
    }
    for (StaticLtmDirectedPath odPath : odPaths) {
      double pathDemand = odDemand * odPath.getPathChoiceProbability();
      if(Double.compare(Double.NaN, pathDemand)==0) {
        System.out.println(String.format("path demand: %.1f", pathDemand));
      }

      /* movement iterator */
      double acceptedPathFlowRate = pathDemand;
      var movementArray = odPath.getMovements();
      for(var movement : movementArray){
        acceptedPathFlowRate = applySingleFlowUpdate(movement, acceptedPathFlowRate, pathDemand);
      }

      applyPathFinalSegmentFlowUpdate(
              movementArray[movementArray.length-1].getSegmentTo(), acceptedPathFlowRate, pathDemand);

    }
  }
}
