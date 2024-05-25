package org.goplanit.assignment.ltm.sltm;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.path.ManagedDirectedPathImpl;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.physical.Movement;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.ManagedDirectedPathFactory;

import java.util.Deque;
import java.util.logging.Logger;

/**
 * Factory for creating directed paths on container
 * 
 * @author markr
 */
public class StaticLtmDirectedPathFactory extends ManagedIdEntityFactoryImpl<StaticLtmDirectedPath> implements ManagedDirectedPathFactory<StaticLtmDirectedPath> {

  private static final Logger LOGGER = Logger.getLogger(StaticLtmDirectedPathFactory.class.getCanonicalName());

  /** two key mapping from segment,segment to a movement */
  private MultiKeyMap<Object, Movement> segmentPair2MovementMap;

  /**
   * Get movements from provide edge segments
   *
   * @param edgeSegments to use
   * @return movement array
   */
  private Movement[] getMovements(Deque<? extends EdgeSegment> edgeSegments) {
    Movement[] movements = new Movement[edgeSegments.size()];

    int index = 0;
    var edgeSegmentIter = edgeSegments.iterator();
    EdgeSegment prevSegment = edgeSegmentIter.next();
    while(edgeSegmentIter.hasNext()){
      var edgeSegment = edgeSegmentIter.next();
      var movement = segmentPair2MovementMap.get(prevSegment, edgeSegment);
      if(movement == null){
        LOGGER.severe(String.format("Unable to find movement for segment pair (%s)-(%s)", prevSegment.getIdsAsString(), edgeSegment.getIdsAsString()));
      }
      movements[index++] = movement;
      prevSegment = edgeSegment;

    }
    return movements;
  }

  /**
   * Constructor. Leveraging mapping between segment pairs and movements to be able to construct
   * static LTM paths internal structure which is movement based rather than segment based
   *
   * @param groupIdToken  to use for creating element ids
   * @param segmentPair2MovementMap mapping
   */
  public StaticLtmDirectedPathFactory(
      final IdGroupingToken groupIdToken, MultiKeyMap<Object, Movement> segmentPair2MovementMap){
    super(groupIdToken);
    this.segmentPair2MovementMap = segmentPair2MovementMap;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StaticLtmDirectedPath createNew() {
    return new StaticLtmDirectedPathImpl(getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StaticLtmDirectedPath createNew(Deque<? extends EdgeSegment> edgeSegments) {
    if(edgeSegments.size() < 2){
      LOGGER.warning("Cannot create static LTM path that has less than 2 edge segments");
      return null;
    }
    return new StaticLtmDirectedPathImpl(getIdGroupingToken(), getMovements(edgeSegments));
  }



}
