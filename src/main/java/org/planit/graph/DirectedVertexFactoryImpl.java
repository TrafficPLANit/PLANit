package org.planit.graph;

import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.DirectedVertexFactory;
import org.planit.utils.graph.DirectedVertices;
import org.planit.utils.id.IdGroupingToken;

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
    return new DirectedVertexImpl(getIdGroupingToken());
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
