package org.goplanit.network.layer.physical;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkFactory;
import org.goplanit.utils.network.layer.physical.ConjugateLinks;

/**
 * 
 * Conjugated links primary managed container implementation
 * 
 * @author markr
 * 
 */
public class ConjugateLinksImpl extends ManagedIdEntitiesImpl<ConjugateLink> implements ConjugateLinks {

  /** factory to use */
  private final ConjugateLinkFactory factory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ConjugateLinksImpl(final IdGroupingToken groupId) {
    super(ConjugateLink::getId, ConjugateLink.CONJUGATE_LINK_ID_CLASS);
    this.factory = new ConjugateLinkFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   * @param factory the factory to use
   */
  public ConjugateLinksImpl(final IdGroupingToken groupId, ConjugateLinkFactory factory) {
    super(ConjugateLink::getId, ConjugateLink.CONJUGATE_LINK_ID_CLASS);
    this.factory = factory;
  }

  /**
   * Copy constructor
   * 
   * @param linksImpl to copy
   */
  public ConjugateLinksImpl(ConjugateLinksImpl linksImpl) {
    super(linksImpl);
    this.factory = linksImpl.factory;
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
  public ConjugateLinksImpl clone() {
    return new ConjugateLinksImpl(this);
  }

}
