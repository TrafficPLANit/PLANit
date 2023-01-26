package org.goplanit.network.layer.macroscopic;

import java.util.logging.Logger;

import org.goplanit.network.layer.physical.LinksImpl;
import org.goplanit.network.layer.physical.NodesImpl;
import org.goplanit.network.layer.physical.UntypedPhysicalLayerImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.ConjugateMacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.*;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.Links;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.Nodes;
import org.goplanit.utils.network.virtual.ConjugateVirtualNetwork;
import org.locationtech.jts.geom.Envelope;

/**
 * Macroscopic physical Network (layer) that supports one or more modes and link segment types, where the modes are registered on the network (Infrastructure network) level
 *
 * @author markr
 *
 */
public class MacroscopicNetworkLayerImpl extends UntypedPhysicalLayerImpl<Node, MacroscopicLink, MacroscopicLinkSegment> implements MacroscopicNetworkLayer {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicNetworkLayerImpl.class.getCanonicalName());

  /** The container for the link segment types available across all link segments */
  public final MacroscopicLinkSegmentTypes linkSegmentTypes;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected MacroscopicNetworkLayerImpl(final IdGroupingToken groupId) {
    this(groupId, new NodesImpl(groupId), new MacroscopicLinksImpl(groupId), new MacroscopicLinkSegmentsImpl(groupId));
  }

  /**
   * Constructor
   * 
   * @param groupId      contiguous id generation within this group for instances of this class
   * @param nodes        to use
   * @param links        to use
   * @param linkSegments to use
   */
  protected MacroscopicNetworkLayerImpl(final IdGroupingToken groupId, Nodes nodes, MacroscopicLinks links, MacroscopicLinkSegments linkSegments) {
    super(groupId, nodes, links, linkSegments);
    linkSegmentTypes = new MacroscopicLinkSegmentTypesImpl(groupId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   */
  protected MacroscopicNetworkLayerImpl(MacroscopicNetworkLayerImpl other, boolean deepCopy) {
    super(other, deepCopy);

    // container wrapper so requires clone also for shallow copy
    this.linkSegmentTypes = deepCopy ? other.linkSegmentTypes.deepClone() : other.linkSegmentTypes.clone();
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void logInfo(String prefix) {
    super.logInfo(prefix);

    LOGGER.info(String.format("%s#link segment types: %d", prefix, linkSegmentTypes.size()));
  }

  /**
   * collect the link segment types, alternative to using the public member
   * 
   * @return the link segment types
   */
  @Override
  public MacroscopicLinkSegmentTypes getLinkSegmentTypes() {
    return this.linkSegmentTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinks getLinks() {
    return (MacroscopicLinks) getGraph().getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegments getLinkSegments() {
    return (MacroscopicLinkSegments) getGraph().getEdgeSegments();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Nodes getNodes() {
    return (Nodes) getGraph().getVertices();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayerImpl clone() {
    return new MacroscopicNetworkLayerImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayerImpl deepClone() {
    return new MacroscopicNetworkLayerImpl(this, true);
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
    this.linkSegmentTypes.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateMacroscopicNetworkLayer createConjugate(final IdGroupingToken idToken, final ConjugateVirtualNetwork conjugateVirtualNetwork) {
    /* empty instance */
    var conjugateLayer = new ConjugateMacroscopicNetworkLayerImpl(idToken, this);
    /* update based on state of parent network */
    conjugateLayer.update(conjugateVirtualNetwork);
    return conjugateLayer;
  }

}
