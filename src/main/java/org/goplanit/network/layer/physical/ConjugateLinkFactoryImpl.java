package org.goplanit.network.layer.physical;

import java.util.logging.Logger;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkFactory;
import org.goplanit.utils.network.layer.physical.Link;

/**
 * Factory for creating conjugate links on conjugate links container
 * 
 * @author markr
 */
public class ConjugateLinkFactoryImpl extends GraphEntityFactoryImpl<ConjugateLink> implements ConjugateLinkFactory {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateLinkFactoryImpl.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param groupIdToken to use for creating element ids
   * @param container    to register the created instances on
   */
  public ConjugateLinkFactoryImpl(IdGroupingToken groupIdToken, GraphEntities<ConjugateLink> container) {
    super(groupIdToken, container);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLink registerNew(final ConjugateDirectedVertex nodeA, final ConjugateDirectedVertex nodeB, boolean registerOnNodes, final Link originalLink1,
      final Link originalLink2) {
    if (nodeA == null || nodeB == null) {
      LOGGER.warning("Unable to create new conjugate link, one or more of its conjugate nodes are not defined");
      return null;
    }

    ConjugateLinkImpl newLink = new ConjugateLinkImpl(getIdGroupingToken(), nodeA, nodeB, originalLink1, originalLink2);
    getGraphEntities().register(newLink);
    if (registerOnNodes) {
      nodeA.addEdge(newLink);
      nodeB.addEdge(newLink);
    }
    return newLink;
  }

}
