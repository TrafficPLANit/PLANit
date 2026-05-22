package org.goplanit.assignment.ltm.sltm.loading;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.CompiledRelationMapping;

/**
 * Access turn flows via underlying movement information
 *
 * @author markr
 */
public class TurnFlowAccessorMovements implements TurnFlowAccessor {

  /** be able to convert entry/exit segment to their corresponding movement */
  private final CompiledRelationMapping compiledMovementIds;

  private final double[] turnFlowsIndexedByMovementIds;

  private TurnFlowAccessorMovements(
      final CompiledRelationMapping compiledMovementIds,
          double[] turnFlowsIndexedByMovementIds){
    this.compiledMovementIds = compiledMovementIds;
    this.turnFlowsIndexedByMovementIds = turnFlowsIndexedByMovementIds;
  }

  /**
   * Factory method
   *
   * @param compiledMovementIds to be able to complete conversion
   * @param turnFlowsIndexedByMovementIds access to turn flow data by movement id
   * @return instance
   */
  public static TurnFlowAccessorMovements of(
      final CompiledRelationMapping compiledMovementIds,
      double[] turnFlowsIndexedByMovementIds){
    return new TurnFlowAccessorMovements(compiledMovementIds, turnFlowsIndexedByMovementIds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTurnFlow(EdgeSegment from, EdgeSegment to) {
    var movementId = compiledMovementIds.getMovementId(from.getId(), to.getId());
    return (movementId != -1) ? this.turnFlowsIndexedByMovementIds[(int) movementId]: 0.0;
  }
}
