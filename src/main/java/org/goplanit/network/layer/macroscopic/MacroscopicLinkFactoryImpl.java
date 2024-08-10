package org.goplanit.network.layer.macroscopic;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkFactory;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.LinkFactory;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;

import java.util.logging.Logger;

/**
 * Factory for creating macroscopic links on macroscopic links container
 * 
 * @author markr
 */
public class MacroscopicLinkFactoryImpl extends GraphEntityFactoryImpl<MacroscopicLink> implements MacroscopicLinkFactory {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkFactoryImpl.class.getCanonicalName());

  /**
   * Constructor without setting graph entities
   *
   * @param groupIdToken to use for creating element ids
   */
  protected MacroscopicLinkFactoryImpl(IdGroupingToken groupIdToken) {
    super(groupIdToken);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void setGraphEntities(GraphEntities<MacroscopicLink> macroscopicLinks){
    super.setGraphEntities(macroscopicLinks);
  }

  /**
   * Constructor
   * 
   * @param groupIdToken to use for creating element ids
   * @param macroscopicLinks        to register the created instances on
   */
  public MacroscopicLinkFactoryImpl(IdGroupingToken groupIdToken, GraphEntities<MacroscopicLink> macroscopicLinks) {
    super(groupIdToken, macroscopicLinks);
  }

  @Override
  public MacroscopicLinkImpl<Node, MacroscopicLinkSegment> registerNew() {
    MacroscopicLinkImpl<Node, MacroscopicLinkSegment> newLink = new MacroscopicLinkImpl<>(getIdGroupingToken());
    getGraphEntities().register(newLink);
    return newLink;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkImpl<Node, MacroscopicLinkSegment> registerNew(final Node nodeA, final Node nodeB, double lengthKm, boolean registerOnNodes) {
    if (nodeA == null || nodeB == null) {
      LOGGER.warning("Unable to create new macroscopic link, one or more of its nodes are not defined");
      return null;
    }

    var newLink = registerNew();
    newLink.setLengthKm(lengthKm);
    if (registerOnNodes) {
      nodeA.addEdge(newLink);
      nodeB.addEdge(newLink);
    }
    return newLink;
  }

}
