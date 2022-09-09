package org.goplanit.network.layer.physical;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.LinkFactory;
import org.goplanit.utils.network.layer.physical.Links;

/**
 * 
 * Links primary managed container implementation
 * 
 * @author markr
 * 
 */
public class LinksImpl<L extends Link> extends ManagedIdEntitiesImpl<L> implements Links<L> {

  /** factory to use */
  protected final LinkFactory linkFactory;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   * @param linkFactory to use
   */
  protected LinksImpl(final IdGroupingToken groupId, LinkFactory linkFactory) {
    super(L::getId, L.EDGE_ID_CLASS);
    this.linkFactory = linkFactory;
  }

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public LinksImpl(final IdGroupingToken groupId) {
    super(L::getId, L.EDGE_ID_CLASS);
    this.linkFactory = new LinkFactoryImpl(groupId, (Links<Link>) this);
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
    IdGenerator.reset(getFactory().getIdGroupingToken(), L.LINK_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    IdGenerator.reset(getFactory().getIdGroupingToken(), L.LINK_ID_CLASS);
    super.reset();
  }

}
