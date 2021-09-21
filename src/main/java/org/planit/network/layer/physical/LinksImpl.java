package org.planit.network.layer.physical;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntitiesImpl;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.LinkFactory;
import org.planit.utils.network.layer.physical.Links;

/**
 * 
 * Links primary managed container implementation
 * 
 * @author markr
 * 
 */
public class LinksImpl extends ManagedIdEntitiesImpl<Link> implements Links {

  /** factory to use */
  private final LinkFactory linkFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public LinksImpl(final IdGroupingToken groupId) {
    super(Link::getId, Link.EDGE_ID_CLASS);
    this.linkFactory = new LinkFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId     to use for creating ids for instances
   * @param linkFactory the factory to use
   */
  public LinksImpl(final IdGroupingToken groupId, LinkFactory linkFactory) {
    super(Link::getId, Link.EDGE_ID_CLASS);
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
  public LinkFactory getFactory() {
    return linkFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinksImpl clone() {
    return new LinksImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the additional link id class */
    IdGenerator.reset(getFactory().getIdGroupingToken(), Link.LINK_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    IdGenerator.reset(getFactory().getIdGroupingToken(), Link.LINK_ID_CLASS);
    super.reset();
  }

}
