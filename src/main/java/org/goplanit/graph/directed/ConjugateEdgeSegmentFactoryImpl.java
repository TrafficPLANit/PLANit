package org.goplanit.graph.directed;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.directed.ConjugateDirectedEdge;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegmentFactory;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegments;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Factory for creating conjugate edge segments on conjugate edge segments container
 * 
 * @author markr
 */
public class ConjugateEdgeSegmentFactoryImpl extends GraphEntityFactoryImpl<ConjugateEdgeSegment> implements ConjugateEdgeSegmentFactory {

  /**
   * Constructor
   * 
   * @param groupId               to use
   * @param conjugateEdgeSegments to use
   */
  protected ConjugateEdgeSegmentFactoryImpl(final IdGroupingToken groupId, ConjugateEdgeSegments conjugateEdgeSegments) {
    super(groupId, conjugateEdgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateEdgeSegment create(final ConjugateDirectedEdge parentEdge, final boolean directionAB) {
    final ConjugateEdgeSegment edgeSegment = new ConjugateEdgeSegmentImpl(getIdGroupingToken(), directionAB);
    edgeSegment.setParent(parentEdge);
    return edgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateEdgeSegment registerNew(ConjugateDirectedEdge parentEdge, boolean directionAb, boolean registerOnVertexAndEdge) {
    final var edgeSegment = new ConjugateEdgeSegmentImpl(getIdGroupingToken(), parentEdge, directionAb);
    getGraphEntities().register(edgeSegment);

    if (registerOnVertexAndEdge) {
      parentEdge.registerEdgeSegment(edgeSegment, directionAb);
    }
    return edgeSegment;
  }

}
