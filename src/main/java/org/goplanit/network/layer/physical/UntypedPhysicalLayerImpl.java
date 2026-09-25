package org.goplanit.network.layer.physical;

import java.util.logging.Logger;

import org.goplanit.network.layer.UntypedNetworkLayerImpl;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.ManagedGraphEntities;
import org.goplanit.utils.graph.directed.BannedMovement;
import org.goplanit.utils.graph.directed.BannedMovements;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.network.layer.physical.*;

/**
 * Model free Network consisting of managed nodes, links, and link segments, each of which can be iterated over.
 * This network does not contain any transport specific information, hence the qualification "model free".
 *
 * @author markr
 *
 * @param <L> type of links
 * @param <N> type of nodes
 * @param <LS> type of segments
 */
public abstract class UntypedPhysicalLayerImpl<N extends Node, L extends Link, LS extends LinkSegment>
    extends UntypedNetworkLayerImpl<N, L, LS>
    implements UntypedPhysicalLayer<N, L, LS> {

  // INNER CLASSES

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(UntypedPhysicalLayerImpl.class.getCanonicalName());

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
   * @param bannedMovements    managed movements container to use
   */
  public <Nx extends ManagedGraphEntities<N>, Lx extends ManagedGraphEntities<L>, Sx extends ManagedGraphEntities<LS>>
  UntypedPhysicalLayerImpl(
      final IdGroupingToken tokenId,
      final Nx nodes,
      final Lx links,
      final Sx linkSegments,
      final BannedMovements bannedMovements) {
    super(tokenId, nodes, links, linkSegments, bannedMovements);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   * @param nodeMapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   * @param linkMapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   * @param linkSegmentMapper to apply in case of deep copy to each original to copy combination
   *                          (when provided, may be null)
   * @param movementMapper to apply in case of deep copy to each original to copy combination
   *                       (when provided, may be null)
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public UntypedPhysicalLayerImpl(
          UntypedPhysicalLayerImpl other,
          boolean deepCopy,
          GraphEntityDeepCopyMapper<N> nodeMapper,
          GraphEntityDeepCopyMapper<L> linkMapper,
          GraphEntityDeepCopyMapper<LS> linkSegmentMapper,
          ManagedIdDeepCopyMapper<BannedMovement> movementMapper) {
    super(other, deepCopy, nodeMapper, linkMapper, linkSegmentMapper, movementMapper);
  }


  // Getters - Setters


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
    LOGGER.info(String.format("%s#movements: %d", prefix, getBannedMovements().size()));
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
