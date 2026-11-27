package org.goplanit.graph;

import org.goplanit.utils.graph.ConjugateVertex;
import org.goplanit.utils.graph.ConjugateVertexFactory;
import org.goplanit.utils.graph.ConjugateVertices;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Factory for creating vertices on vertices container
 * 
 * @author markr
 */
public class ConjugateVertexFactoryImpl extends GraphEntityFactoryImpl<ConjugateVertex> implements ConjugateVertexFactory {

  /**
   * Constructor
   * 
   * @param groupId           to use
   * @param conjugateVertices to use
   */
  protected ConjugateVertexFactoryImpl(final IdGroupingToken groupId, final ConjugateVertices conjugateVertices) {
    super(groupId, conjugateVertices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVertex createNew(final EdgeSegment original) {
    return new ConjugateVertexImpl(getIdGroupingToken(), original);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVertex registerNew(final EdgeSegment original, boolean deriveFromOriginalXmlId, String xmlIdPostFix) {
    final ConjugateVertex newVertex = createNew(original);
    newVertex.populateXmlId(deriveFromOriginalXmlId, xmlIdPostFix);
    getGraphEntities().register(newVertex);
    return newVertex;
  }

}
