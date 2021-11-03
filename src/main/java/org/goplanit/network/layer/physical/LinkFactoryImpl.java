package org.goplanit.network.layer.physical;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.LinkFactory;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * Factory for creating links on links container
 * 
 * @author markr
 */
public class LinkFactoryImpl extends GraphEntityFactoryImpl<Link> implements LinkFactory {

  /**
   * Constructor
   * 
   * @param groupIdToken to use for creating element ids
   * @param links        to register the created instances on
   */
  public LinkFactoryImpl(IdGroupingToken groupIdToken, GraphEntities<Link> links) {
    super(groupIdToken, links);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkImpl registerNew(Node nodeA, Node nodeB, double lengthKm, boolean registerOnNodes) {
    LinkImpl newLink = new LinkImpl(getIdGroupingToken(), nodeA, nodeB);
    getGraphEntities().register(newLink);
    newLink.setLengthKm(lengthKm);
    if (registerOnNodes) {
      nodeA.addEdge(newLink);
      nodeB.addEdge(newLink);
    }
    return newLink;
  }

}
