package org.goplanit.assignment.ltm.sltm.loading;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.CompiledRelationIndex;
import org.goplanit.utils.network.layer.physical.CompiledRelationMapping;

/**
 * Access turn flows via the compiled turns
 *
 * @author markr
 */
public class TurnFlowAccessorCompiledTurns implements TurnFlowAccessor {

  /** be able to convert entry/exit segment to their corresponding compiled turn */
  private final CompiledRelationIndex compiledTurnIds;

  private final double[] turnFlowsIndexedByCompiledTurnIds;

  private TurnFlowAccessorCompiledTurns(
      final CompiledRelationIndex compiledTurnIds,
      double[] turnFlowsIndexedByCompiledTurnIds){
    this.compiledTurnIds = compiledTurnIds;
    this.turnFlowsIndexedByCompiledTurnIds = turnFlowsIndexedByCompiledTurnIds;
  }

  /**
   * Factory method
   *
   * @param compiledTurnIds to be able to complete conversion
   * @param turnFlowsIndexedByCompiledTurnIds access to turn flow data by compiled turn id
   * @return instance
   */
  public static TurnFlowAccessorCompiledTurns of(
      final CompiledRelationIndex compiledTurnIds,
      double[] turnFlowsIndexedByCompiledTurnIds){
    return new TurnFlowAccessorCompiledTurns(compiledTurnIds, turnFlowsIndexedByCompiledTurnIds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTurnFlow(EdgeSegment from, EdgeSegment to) {
    var compiledTurnId = compiledTurnIds.get(from.getId(), to.getId());
    return (compiledTurnId != CompiledRelationIndex.BANNED) ?
        this.turnFlowsIndexedByCompiledTurnIds[(int) compiledTurnId]: 0.0;
  }
}
