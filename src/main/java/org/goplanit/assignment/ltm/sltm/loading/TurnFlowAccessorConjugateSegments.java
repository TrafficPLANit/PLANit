package org.goplanit.assignment.ltm.sltm.loading;

import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.CompiledRelationMapping;

/**
 * Access turn flows via underlying conjugate segment information
 *
 * @author markr
 */
public class TurnFlowAccessorConjugateSegments implements TurnFlowAccessor {

  /** be able to convert entry/exit segment to their corresponding movement */
  private final CompiledRelationMapping<ConjugateEdgeSegment> compiledConjugateSegmentMapping;

  private final double[] turnFlowsIndexedByConjugateSegmentIds;

  private TurnFlowAccessorConjugateSegments(
          final CompiledRelationMapping<ConjugateEdgeSegment> compiledConjugateSegmentMapping,
          double[] turnFlowsIndexedByConjugateSegmentIds){
    this.compiledConjugateSegmentMapping = compiledConjugateSegmentMapping;
    this.turnFlowsIndexedByConjugateSegmentIds = turnFlowsIndexedByConjugateSegmentIds;
  }

  /**
   * Factory method
   *
   * @param compiledConjugateSegmentMapping to be able to complete conversion
   * @param turnFlowsIndexedByConjugateSegmentIds access to turn flow data by conjugate segment id
   * @return instance
   */
  public static TurnFlowAccessorConjugateSegments of(
          final CompiledRelationMapping<ConjugateEdgeSegment> compiledConjugateSegmentMapping,
          double[] turnFlowsIndexedByConjugateSegmentIds){
    return new TurnFlowAccessorConjugateSegments(
        compiledConjugateSegmentMapping, turnFlowsIndexedByConjugateSegmentIds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTurnFlow(EdgeSegment from, EdgeSegment to) {
    var conjSegment = compiledConjugateSegmentMapping.get(from.getId(), to.getId());
    return conjSegment != null ? this.turnFlowsIndexedByConjugateSegmentIds[(int)conjSegment.getId()]: 0.0;
  }
}
