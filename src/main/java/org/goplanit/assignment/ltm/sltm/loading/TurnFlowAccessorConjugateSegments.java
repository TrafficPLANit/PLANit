package org.goplanit.assignment.ltm.sltm.loading;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.Movement;

/**
 * Access turn flows via underlying conjugate segment information
 *
 * @author markr
 */
public class TurnFlowAccessorConjugateSegments implements TurnFlowAccessor {

  /** be able to convert entry/exit segment to their corresponding movement */
  private final MultiKeyMap<Object, ConjugateEdgeSegment> segmentPair2ConjSegmentMap;

  private final double[] turnFlowsIndexedByConjugateSegmentIds;

  private TurnFlowAccessorConjugateSegments(
          final MultiKeyMap<Object, ConjugateEdgeSegment> segmentPair2ConjSegmentMap,
          double[] turnFlowsIndexedByConjugateSegmentIds){
    this.segmentPair2ConjSegmentMap = segmentPair2ConjSegmentMap;
    this.turnFlowsIndexedByConjugateSegmentIds = turnFlowsIndexedByConjugateSegmentIds;
  }

  /**
   * Factory method
   *
   * @param segmentPair2ConjSegmentMap to be able to complete conversion
   * @param turnFlowsIndexedByConjugateSegmentIds access to turn flow data by conjugate segment id
   * @return instance
   */
  public static TurnFlowAccessorConjugateSegments of(
          final MultiKeyMap<Object, ConjugateEdgeSegment> segmentPair2ConjSegmentMap,
          double[] turnFlowsIndexedByConjugateSegmentIds){
    return new TurnFlowAccessorConjugateSegments(segmentPair2ConjSegmentMap, turnFlowsIndexedByConjugateSegmentIds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTurnFlow(EdgeSegment from, EdgeSegment to) {
    var conjSegment = segmentPair2ConjSegmentMap.get(from, to);
    return conjSegment != null ? this.turnFlowsIndexedByConjugateSegmentIds[(int)conjSegment.getId()]: 0.0;
  }
}
