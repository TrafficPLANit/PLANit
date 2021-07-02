package org.planit.graph;

import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.VertexFactory;
import org.planit.utils.graph.Vertices;
import org.planit.utils.id.IdGroupingToken;

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
