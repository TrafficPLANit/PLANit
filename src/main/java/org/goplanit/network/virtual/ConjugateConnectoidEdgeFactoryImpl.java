package org.goplanit.network.virtual;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.ConjugateConnectoidEdge;
import org.goplanit.utils.network.virtual.ConjugateConnectoidEdgeFactory;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.ConnectoidEdge;

import java.util.logging.Logger;

/**
 * Factory for creating conjugate links on conjugate links container
 * 
 * @author markr
 */
public class ConjugateConnectoidEdgeFactoryImpl extends GraphEntityFactoryImpl<ConjugateConnectoidEdge> implements ConjugateConnectoidEdgeFactory {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateConnectoidEdgeFactoryImpl.class.getCanonicalName());

  /**
   * Constructor
   *
   * @param groupIdToken to use for creating element ids
   * @param container    to register the created instances on
   */
  public ConjugateConnectoidEdgeFactoryImpl(IdGroupingToken groupIdToken, GraphEntities<ConjugateConnectoidEdge> container) {
    super(groupIdToken, container);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidEdge registerNew(final ConjugateConnectoidNode vertexA, final ConjugateConnectoidNode vertexB, boolean registerOnNodes, final ConnectoidEdge originalConnectoidEdge) {
    if (vertexA == null || vertexB == null) {
      LOGGER.warning("Unable to create new conjugate link, one or more of its conjugate nodes are not defined");
      return null;
    }

    ConjugateConnectoidEdgeImpl newConjugateEdge = new ConjugateConnectoidEdgeImpl(getIdGroupingToken(), vertexA, vertexB, originalConnectoidEdge);
    getGraphEntities().register(newConjugateEdge);
    if (registerOnNodes) {
      vertexA.addEdge(newConjugateEdge);
      vertexB.addEdge(newConjugateEdge);
    }
    return newConjugateEdge;
  }

}
