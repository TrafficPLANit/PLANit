package org.goplanit.network.layer.macroscopic;

import java.util.logging.Logger;

import org.goplanit.network.layer.UntypedNetworkLayerImpl;
import org.goplanit.network.layer.physical.ConjugateLinkSegmentsImpl;
import org.goplanit.network.layer.physical.ConjugateLinksImpl;
import org.goplanit.network.layer.physical.ConjugateNodesImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.ConjugateMacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegments;
import org.goplanit.utils.network.layer.physical.ConjugateLinks;
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.layer.physical.ConjugateNodes;

/**
 * Conjugate of macroscopic physical Network (layer), i.e. the edge-to-vertex dual of its original form
 *
 * @author markr
 *
 */
public class ConjugateMacroscopicNetworkLayerImpl extends UntypedNetworkLayerImpl<ConjugateNode, ConjugateLink, ConjugateLinkSegment> implements ConjugateMacroscopicNetworkLayer {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateMacroscopicNetworkLayerImpl.class.getCanonicalName());

  /** original layer this conjugate layer is based on */
  protected final MacroscopicNetworkLayer originalLayer;

  /**
   * Reset and re-populate entire conjugate network layer based on current state of original layer this is the conjugate of
   */
  protected void update() {
    reset();

    CONTINUE HERE;
  }

  /**
   * Constructor
   * 
   * @param groupId       contiguous id generation within this group for instances of this class
   * @param originalLayer this conjugate is based on
   */
  protected ConjugateMacroscopicNetworkLayerImpl(final IdGroupingToken groupId, final MacroscopicNetworkLayer originalLayer) {
    this(groupId, new ConjugateNodesImpl(groupId), new ConjugateLinksImpl(groupId), new ConjugateLinkSegmentsImpl(groupId), originalLayer);
  }

  /**
   * Constructor
   * 
   * @param groupId               contiguous id generation within this group for instances of this class
   * @param conjugateNodes        to use
   * @param conjugateLinks        to use
   * @param conjugateLinkSegments to use
   * @param originalLayer         this conjugate is based on
   */
  protected ConjugateMacroscopicNetworkLayerImpl(final IdGroupingToken groupId, ConjugateNodes conjugateNodes, ConjugateLinks conjugateLinks,
      ConjugateLinkSegments conjugateLinkSegments, final MacroscopicNetworkLayer originalLayer) {
    super(groupId, conjugateNodes, conjugateLinks, conjugateLinkSegments);
    this.originalLayer = originalLayer;
  }

  /**
   * Copy constructor
   * 
   * @param conjugateMacroscopicNetworkLayerImpl to copy
   */
  protected ConjugateMacroscopicNetworkLayerImpl(ConjugateMacroscopicNetworkLayerImpl conjugateMacroscopicNetworkLayerImpl) {
    super(conjugateMacroscopicNetworkLayerImpl);
    this.originalLayer = conjugateMacroscopicNetworkLayerImpl.originalLayer;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void logInfo(String prefix) {
    super.logInfo(prefix);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinks getConjugateLinks() {
    return (ConjugateLinks) getGraph().getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkSegments getConjugateLinkSegments() {
    return (ConjugateLinkSegments) getGraph().getEdgeSegments();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateNodes getConjugateNodes() {
    return (ConjugateNodes) getGraph().getVertices();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateMacroscopicNetworkLayerImpl clone() {
    return new ConjugateMacroscopicNetworkLayerImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
    this.resetChildManagedIdEntities();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetChildManagedIdEntities() {
    super.resetChildManagedIdEntities();
    // no others at this point
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayer getOriginalLayer() {
    return originalLayer;
  }

}
