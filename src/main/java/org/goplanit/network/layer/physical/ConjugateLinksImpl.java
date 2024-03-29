package org.goplanit.network.layer.physical;

import org.goplanit.utils.graph.ManagedGraphEntitiesImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkFactory;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;
import org.goplanit.utils.network.layer.physical.ConjugateLinks;

import java.util.function.BiConsumer;

/**
 * 
 * Conjugated links primary managed container implementation
 * 
 * @author markr
 * 
 */
public class ConjugateLinksImpl extends ManagedGraphEntitiesImpl<ConjugateLink> implements ConjugateLinks {

  /** factory to use */
  private final ConjugateLinkFactory factory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ConjugateLinksImpl(final IdGroupingToken groupId) {
    super(ConjugateLink::getId, ConjugateLink.EDGE_ID_CLASS);
    this.factory = new ConjugateLinkFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   * @param factory the factory to use
   */
  public ConjugateLinksImpl(final IdGroupingToken groupId, ConjugateLinkFactory factory) {
    super(ConjugateLink::getId, ConjugateLink.EDGE_ID_CLASS);
    this.factory = factory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public ConjugateLinksImpl(ConjugateLinksImpl other, boolean deepCopy, BiConsumer<ConjugateLink,ConjugateLink> mapper) {
    super(other, deepCopy, mapper);
    this.factory = new ConjugateLinkFactoryImpl(other.factory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkFactory getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinksImpl shallowClone() {
    return new ConjugateLinksImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinksImpl deepClone() {
    return new ConjugateLinksImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinksImpl deepCloneWithMapping(BiConsumer<ConjugateLink,ConjugateLink> mapper) {
    return new ConjugateLinksImpl(this, true, mapper);
  }

}
