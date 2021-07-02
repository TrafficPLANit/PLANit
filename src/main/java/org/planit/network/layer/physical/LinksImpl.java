package org.planit.network.layer.physical;

import org.planit.graph.GraphEntitiesImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.LinkFactory;
import org.planit.utils.network.layer.physical.Links;

/**
 * 
 * Links implementation wrapper that simply utilises passed in edges of the desired generic type to delegate registration and creation of its links on
 * 
 * @author markr
 * 
 * @param <L> link type
 */
public class LinksImpl extends GraphEntitiesImpl<Link> implements Links<Link> {

  /** factory to use */
  private final LinkFactory<Link> linkFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public LinksImpl(final IdGroupingToken groupId) {
    super(Link::getId);
    this.linkFactory = new LinkFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId     to use for creating ids for instances
   * @param linkFactory the factory to use
   */
  public LinksImpl(final IdGroupingToken groupId, LinkFactory<Link> linkFactory) {
    super(Link::getId);
    this.linkFactory = linkFactory;
  }

  /**
   * Copy constructor
   * 
   * @param linksImpl to copy
   */
  public LinksImpl(LinksImpl linksImpl) {
    super(linksImpl);
    this.linkFactory = linksImpl.linkFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkFactory<Link> getFactory() {
    return linkFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinksImpl clone() {
    return new LinksImpl(this);
  }

}
