package org.planit.network.layer;

import java.util.logging.Logger;

import org.planit.utils.graph.GraphEntities;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntities;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.layer.physical.UntypedPhysicalLayer;

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
   * @param tokenId      contiguous id generation within this group for instances of this class
   * @param nodes        managed nodes container to use
   * @param links        managed links container to use
   * @param linkSegments managed linkSegments container to use
   */
  public <Nx extends GraphEntities<N> & ManagedIdEntities<N>, Lx extends GraphEntities<L> & ManagedIdEntities<L>, Sx extends GraphEntities<LS> & ManagedIdEntities<LS>> UntypedPhysicalLayerImpl(
      final IdGroupingToken tokenId, final Nx nodes, final Lx links, final Sx linkSegments) {
    super(tokenId, nodes, links, linkSegments);
  }

  // Getters - Setters

  /**
   * Copy constructor
   * 
   * @param physicalLayerImpl to copy
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public UntypedPhysicalLayerImpl(UntypedPhysicalLayerImpl physicalLayerImpl) {
    super(physicalLayerImpl);
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
  public abstract UntypedPhysicalLayerImpl<N, L, LS> clone();

}
