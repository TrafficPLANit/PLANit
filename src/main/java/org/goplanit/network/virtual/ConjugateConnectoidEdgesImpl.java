package org.goplanit.network.virtual;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.virtual.*;

/**
 * 
 * Conjugate connectoid edge container implementation
 * 
 * @author markr
 *
 */
public class ConjugateConnectoidEdgesImpl extends ManagedIdEntitiesImpl<ConjugateConnectoidEdge> implements ConjugateConnectoidEdges {

  /** factory to use */
  private final ConjugateConnectoidEdgeFactory factory;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public ConjugateConnectoidEdgesImpl(final IdGroupingToken groupId) {
    super(ConjugateConnectoidEdge::getId, ConnectoidEdge.EDGE_ID_CLASS);
    this.factory = new ConjugateConnectoidEdgeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   *
   * @param groupId               to use for creating ids for instances
   * @param factory               the factory to use
   */
  public ConjugateConnectoidEdgesImpl(final IdGroupingToken groupId, ConjugateConnectoidEdgeFactory factory) {
    super(ConjugateConnectoidEdge::getId, ConnectoidEdge.EDGE_ID_CLASS);
    this.factory = factory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   *
   * @param conjugateConnectoidSegmentImpl to copy
   */
  public ConjugateConnectoidEdgesImpl(ConjugateConnectoidEdgesImpl conjugateConnectoidSegmentImpl) {
    super(conjugateConnectoidSegmentImpl);
    this.factory =
            new ConjugateConnectoidEdgeFactoryImpl(conjugateConnectoidSegmentImpl.factory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidEdgeFactory getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the additional connectoid edge id class */
    IdGenerator.reset(getFactory().getIdGroupingToken(), ConnectoidEdge.CONNECTOID_EDGE_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidEdgesImpl clone() {
    return new ConjugateConnectoidEdgesImpl(this);
  }

  /**
   * clear the container
   */
  public void clear() {
    getMap().clear();
  }

}
