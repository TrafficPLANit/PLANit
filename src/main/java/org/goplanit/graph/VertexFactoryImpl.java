package org.goplanit.graph;

import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.VertexFactory;
import org.goplanit.utils.graph.Vertices;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Factory for creating vertices on vertices container
 * 
 * @author markr
 */
public class VertexFactoryImpl extends GraphEntityFactoryImpl<Vertex> implements VertexFactory {

  /**
   * Constructor
   * 
   * @param groupId  to use
   * @param vertices to use
   */
  protected VertexFactoryImpl(final IdGroupingToken groupId, final Vertices vertices) {
    super(groupId, vertices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Vertex createNew() {
    return new VertexImpl(getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Vertex registerNew() {
    final Vertex newVertex = createNew();
    getGraphEntities().register(newVertex);
    return newVertex;
  }

}
