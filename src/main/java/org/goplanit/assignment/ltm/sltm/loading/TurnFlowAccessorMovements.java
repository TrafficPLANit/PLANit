package org.goplanit.assignment.ltm.sltm.loading;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.Movement;

/**
 * Access turn flows via underlying movement information
 *
 * @author markr
 */
public class TurnFlowAccessorMovements implements TurnFlowAccessor {

  /** be able to convert entry/exit segment to their corresponding movement */
  private final MultiKeyMap<Object, Movement> segmentPair2MovementMap;

  private final double[] turnFlowsIndexedByMovementIds;

  private TurnFlowAccessorMovements(
          final MultiKeyMap<Object, Movement> segmentPair2MovementMap,
          double[] turnFlowsIndexedByMovementIds){
    this.segmentPair2MovementMap = segmentPair2MovementMap;
    this.turnFlowsIndexedByMovementIds = turnFlowsIndexedByMovementIds;
  }

  /**
   * Factory method
   *
   * @param segmentPair2MovementMap to be able to complete conversion
   * @param turnFlowsIndexedByMovementIds access to turn flow data by movement id
   * @return instance
   */
  public static TurnFlowAccessorMovements of(final MultiKeyMap<Object, Movement> segmentPair2MovementMap,
                   double[] turnFlowsIndexedByMovementIds){
    return new TurnFlowAccessorMovements(segmentPair2MovementMap, turnFlowsIndexedByMovementIds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTurnFlow(EdgeSegment from, EdgeSegment to) {
    var movement = segmentPair2MovementMap.get(from, to);
    return movement != null ? this.turnFlowsIndexedByMovementIds[(int)movement.getId()]: 0.0;
  }
}
