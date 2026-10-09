package org.goplanit.network.virtual.physical.conjugate;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.physical.ConnectoidLink;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidLink;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidLinkFactory;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;

import java.util.logging.Logger;

/**
 * Factory for creating conjugate links on conjugate links container
 * 
 * @author markr
 */
public class ConjugateConnectoidLinkFactoryImpl
    extends GraphEntityFactoryImpl<ConjugateConnectoidLink> implements ConjugateConnectoidLinkFactory {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateConnectoidLinkFactoryImpl.class.getCanonicalName());

  /**
   * Constructor
   *
   * @param groupIdToken to use for creating element ids
   * @param container    to register the created instances on
   */
  public ConjugateConnectoidLinkFactoryImpl(
          IdGroupingToken groupIdToken, GraphEntities<ConjugateConnectoidLink> container) {
    super(groupIdToken, container);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidLink registerNew(
      final ConjugateConnectoidNode vertexA,
      final ConjugateConnectoidNode vertexB,
      boolean registerOnNodes,
      final ConnectoidSegment original) {
    if (vertexA == null || vertexB == null) {
      LOGGER.warning("Unable to create new conjugate link, one or more of its conjugate nodes are not defined");
      return null;
    }

    // entry is assumed dummy so only exit is provided via original
    ConjugateConnectoidLinkImpl newConjugateLink =
        new ConjugateConnectoidLinkImpl(getIdGroupingToken(), vertexA, vertexB, null, original);
    getGraphEntities().register(newConjugateLink);
    if (registerOnNodes) {
      vertexA.addEdge(newConjugateLink);
      vertexB.addEdge(newConjugateLink);
    }
    return newConjugateLink;
  }

}
