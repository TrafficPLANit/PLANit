package org.goplanit.network.virtual.physical.conjugate;

import java.util.logging.Logger;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidLink;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidSegment;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidSegmentFactory;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidSegments;

/**
 * Factory for creating conjugate connectoid segments on container
 * 
 * @author markr
 */
public class ConjugateConnectoidSegmentFactoryImpl
    extends GraphEntityFactoryImpl<ConjugateConnectoidSegment> implements ConjugateConnectoidSegmentFactory {

  /** Logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateConnectoidSegmentFactoryImpl.class.getCanonicalName());

  /**
   * Constructor
   *
   * @param groupId   to use
   * @param container to use
   */
  protected ConjugateConnectoidSegmentFactoryImpl(final IdGroupingToken groupId, ConjugateConnectoidSegments container) {
    super(groupId, container);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidSegmentImpl create(final ConjugateConnectoidLink parent, final boolean directionAb) {
    return new ConjugateConnectoidSegmentImpl(getIdGroupingToken(), parent, directionAb);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidSegmentImpl registerNew(
      final ConjugateConnectoidLink parent, final boolean directionAb, boolean registerOnNodeAndLink) {

    final var conjugateEdgeSegment = create(parent, directionAb);
    getGraphEntities().register(conjugateEdgeSegment);

    if (registerOnNodeAndLink) {
      parent.registerEdgeSegment(conjugateEdgeSegment, directionAb);
    }
    return conjugateEdgeSegment;
  }

}
