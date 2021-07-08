package org.planit.network.layer;

import java.util.logging.Logger;

import org.planit.utils.graph.GraphEntities;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.layer.physical.UntypedPhysicalLayer;

/**
 * Model free Network consisting of nodes and links, each of which can be iterated over. This network does not contain any transport specific information, hence the qualification
 * "model free".
 *
 * @author markr
 */
public abstract class UntypedPhysicalLayerImpl<N extends Node, NE extends GraphEntities<N>, L extends Link, LE extends GraphEntities<L>, LS extends LinkSegment, LSE extends GraphEntities<LS>>
    extends UntypedDirectedGraphLayerImpl<N, NE, L, LE, LS, LSE> implements UntypedPhysicalLayer<N, NE, L, LE, LS, LSE> {

  // INNER CLASSES

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(UntypedPhysicalLayerImpl.class.getCanonicalName());

  // Protected

  // PUBLIC

  /**
   * Constructor
   *
   * @param tokenId      contiguous id generation within this group for instances of this class
   * @param nodes        nodes container to use
   * @param links        links container to use
   * @param linkSegments linkSegments container to use
   */
  public UntypedPhysicalLayerImpl(final IdGroupingToken tokenId, final NE nodes, final LE links, final LSE linkSegments) {
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
  public LE getLinks() {
    return getGraph().getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LSE getLinkSegments() {
    return getGraph().getEdgeSegments();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NE getNodes() {
    return getGraph().getVertices();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract UntypedPhysicalLayerImpl<N, NE, L, LE, LS, LSE> clone();

}
