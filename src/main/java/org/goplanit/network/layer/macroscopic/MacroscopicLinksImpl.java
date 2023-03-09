package org.goplanit.network.layer.macroscopic;

import org.goplanit.network.layer.physical.LinksImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkFactory;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinks;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.LinkFactory;

import java.util.function.BiConsumer;

/**
 * 
 * Macroscopic links primary managed container implementation
 * 
 * @author markr
 * 
 */
public class MacroscopicLinksImpl extends LinksImpl<MacroscopicLink> implements MacroscopicLinks {

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public MacroscopicLinksImpl(final IdGroupingToken groupId) {
    super(groupId, new MacroscopicLinkFactoryImpl(groupId));
    ((MacroscopicLinkFactoryImpl)this.linkFactory).setGraphEntities(this);
  }

  /**
   * Constructor
   *
   * @param groupId     to use for creating ids for instances
   * @param factory the factory to use
   */
  public MacroscopicLinksImpl(final IdGroupingToken groupId, MacroscopicLinkFactory factory) {
    super(groupId, factory);
  }

  /**
   * Copy constructor, also creates a new factory with reference to this container
   *
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   */
  public MacroscopicLinksImpl(MacroscopicLinksImpl other, boolean deepCopy, BiConsumer<MacroscopicLink,MacroscopicLink> mapper) {
    super(other, deepCopy, mapper);
    this.linkFactory =
            new MacroscopicLinkFactoryImpl(other.getFactory().getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkFactory getFactory() {
    return (MacroscopicLinkFactory) super.getFactory();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinksImpl shallowClone() {
    return new MacroscopicLinksImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinksImpl deepClone() {
    return new MacroscopicLinksImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinksImpl deepCloneWithMapping(BiConsumer<MacroscopicLink,MacroscopicLink> mapper) {
    return new MacroscopicLinksImpl(this, true, mapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasLink(long id) {
    return super.hasLink(id);
  }

}
