package org.goplanit.network.layer.physical;

import java.util.logging.Logger;

import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.network.layer.UntypedNetworkLayerImpl;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.ManagedGraphEntities;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.locationtech.jts.geom.Envelope;

/**
 * Model free Network consisting of managed nodes, links, and link segments, each of which can be iterated over. This network does not contain any transport specific information,
 * hence the qualification "model free".
 *
 * @author markr
 */
public abstract class UntypedPhysicalLayerImpl<N extends Node, L extends Link, LS extends LinkSegment> extends UntypedNetworkLayerImpl<N, L, LS>
    implements UntypedPhysicalLayer<N, L, LS> {

  // INNER CLASSES

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(UntypedPhysicalLayerImpl.class.getCanonicalName());

  // Protected

  // PUBLIC

  /**
   * Constructor
   *
   * @param <Nx>         type of managed nodes container
   * @param <Lx>         type of managed links container
   * @param <Sx>         type of managed link segments container
   * @param tokenId      contiguous id generation within this group for instances of this class
   * @param nodes        managed nodes container to use
   * @param links        managed links container to use
   * @param linkSegments managed linkSegments container to use
   */
  public <Nx extends ManagedGraphEntities<N>, Lx extends ManagedGraphEntities<L>, Sx extends ManagedGraphEntities<LS>> UntypedPhysicalLayerImpl(final IdGroupingToken tokenId,
      final Nx nodes, final Lx links, final Sx linkSegments) {
    super(tokenId, nodes, links, linkSegments);
  }

  // Getters - Setters

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   * @param nodeMapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   * @param linkMapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   * @param linkSegmentMapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public UntypedPhysicalLayerImpl(
          UntypedPhysicalLayerImpl other,
          boolean deepCopy,
          GraphEntityDeepCopyMapper<N> nodeMapper,
          GraphEntityDeepCopyMapper<L> linkMapper,
          GraphEntityDeepCopyMapper<LS> linkSegmentMapper) {
    super(other, deepCopy, nodeMapper, linkMapper, linkSegmentMapper);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void logInfo(String prefix) {
    super.logInfo(prefix);

    /* log infrastructure components */
    LOGGER.info(String.format("%s#links: %d", prefix, getLinks().size()));
    LOGGER.info(String.format("%s#link segments: %d", prefix, getLinkSegments().size()));
    LOGGER.info(String.format("%s#nodes: %d", prefix, getNodes().size()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract UntypedPhysicalLayerImpl<N, L, LS> shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract UntypedPhysicalLayerImpl<N, L, LS> deepClone();

}
