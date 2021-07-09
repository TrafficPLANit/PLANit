package org.planit.network.layer.physical;

import org.planit.graph.GraphEntityFactoryImpl;
import org.planit.utils.graph.GraphEntities;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.LinkFactory;
import org.planit.utils.network.layer.physical.Node;

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
