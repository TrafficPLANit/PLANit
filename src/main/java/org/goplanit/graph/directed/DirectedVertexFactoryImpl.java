package org.goplanit.graph.directed;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.DirectedVertexFactory;
import org.goplanit.utils.graph.directed.DirectedVertices;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Factory for creating vertices on vertices container
 * 
 * @author markr
 */
public class DirectedVertexFactoryImpl extends GraphEntityFactoryImpl<DirectedVertex> implements DirectedVertexFactory {

  /**
   * Constructor
   * 
   * @param groupId  to use
   * @param vertices to use
   */
  protected DirectedVertexFactoryImpl(final IdGroupingToken groupId, final DirectedVertices vertices) {
    super(groupId, vertices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertex createNew() {
    return new DirectedVertexImpl<EdgeSegment>(getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertex registerNew() {
    final DirectedVertex newVertex = createNew();
    getGraphEntities().register(newVertex);
    return newVertex;
  }

}
