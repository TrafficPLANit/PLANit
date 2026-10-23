package org.goplanit.network.virtual.graph.conjugate;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.graph.conjugate.ConjugateConnectoidDirectedEdge;
import org.goplanit.utils.network.virtual.graph.conjugate.ConjugateConnectoidEdgeFactory;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;

import java.util.logging.Logger;

/**
 * Factory for creating conjugate links on conjugate links container
 * 
 * @author markr
 */
public class ConjugateConnectoidEdgeFactoryImpl
        extends GraphEntityFactoryImpl<ConjugateConnectoidDirectedEdge> implements ConjugateConnectoidEdgeFactory {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateConnectoidEdgeFactoryImpl.class.getCanonicalName());

  /**
   * Constructor
   *
   * @param groupIdToken to use for creating element ids
   * @param container    to register the created instances on
   */
  public ConjugateConnectoidEdgeFactoryImpl(IdGroupingToken groupIdToken, GraphEntities<ConjugateConnectoidDirectedEdge> container) {
    super(groupIdToken, container);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidDirectedEdge registerNew(
      final ConjugateConnectoidNode vertexA,
      final ConjugateConnectoidNode vertexB,
      boolean registerOnNodes,
      final ConnectoidSegment original,
      boolean deriveXmlIdFromOriginal,
      String xmlIdPostFix) {

    if (vertexA == null || vertexB == null) {
      LOGGER.warning("Unable to create new conjugate link, one or more of its conjugate nodes are not defined");
      return null;
    }

    ConjugateConnectoidEdgeImpl newConjugateEdge =
        new ConjugateConnectoidEdgeImpl(getIdGroupingToken(), vertexA, vertexB, original);
    newConjugateEdge.populateXmlId(deriveXmlIdFromOriginal, xmlIdPostFix);
    getGraphEntities().register(newConjugateEdge);
    if (registerOnNodes) {
      vertexA.addEdge(newConjugateEdge);
      vertexB.addEdge(newConjugateEdge);
    }
    return newConjugateEdge;
  }

}
