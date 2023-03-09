package org.goplanit.network.layer.macroscopic;

import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.network.layer.physical.NodesImpl;
import org.goplanit.network.layer.physical.UntypedPhysicalLayerImpl;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.network.layer.ConjugateMacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.*;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.Nodes;
import org.goplanit.utils.network.virtual.ConjugateVirtualNetwork;

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
   * @param nodeMapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   * @param linkMapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   * @param linkSegmentMapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   * @param linkSegmentTypeMapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   */
  protected MacroscopicNetworkLayerImpl(
          MacroscopicNetworkLayerImpl other,
          boolean deepCopy,
          GraphEntityDeepCopyMapper<Node> nodeMapper,
          GraphEntityDeepCopyMapper<MacroscopicLink> linkMapper,
          GraphEntityDeepCopyMapper<MacroscopicLinkSegment> linkSegmentMapper,
          ManagedIdDeepCopyMapper<MacroscopicLinkSegmentType> linkSegmentTypeMapper) {
    super(other, deepCopy, nodeMapper, linkMapper, linkSegmentMapper);

    this.linkSegmentTypes = deepCopy ? other.linkSegmentTypes.deepCloneWithMapping(linkSegmentTypeMapper) : other.linkSegmentTypes.shallowClone();
    if(deepCopy) {
      updateLinkSegmentLinkSegmentTypes(ls -> linkSegmentTypeMapper.getMapping(ls), true);
    }
  }

  /**
   * Update the parent edge of all edge segments based on the mapping provided (if any)
   * @param lsTypeToLsTypeMapping to use should contain original link segment type as currently used on link segment and then the value is the new link segment type to replace it
   * @param removeMissingMappings when true if there is no mapping, the type is nullified, otherwise it is left in-tact
   */
  public void updateLinkSegmentLinkSegmentTypes(Function<MacroscopicLinkSegmentType, MacroscopicLinkSegmentType> lsTypeToLsTypeMapping, boolean removeMissingMappings) {
    for(var linkSegment :  getLinkSegments()){
      if(linkSegment.getLinkSegmentType() == null){
        continue;
      }
      var clonedType = lsTypeToLsTypeMapping.apply(linkSegment.getLinkSegmentType());
      if(clonedType != null || removeMissingMappings){
        linkSegment.setLinkSegmentType(clonedType);
      }
    }
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
    return (MacroscopicLinks) getDirectedGraph().getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegments getLinkSegments() {
    return (MacroscopicLinkSegments) getDirectedGraph().getEdgeSegments();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Nodes getNodes() {
    return (Nodes) getDirectedGraph().getVertices();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayerImpl shallowClone() {
    return new MacroscopicNetworkLayerImpl(
            this, false, null, null, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayerImpl deepClone() {
    return new MacroscopicNetworkLayerImpl(
            this,
            true,
            new GraphEntityDeepCopyMapper<>(),
            new GraphEntityDeepCopyMapper<>(),
            new GraphEntityDeepCopyMapper<>(),
            new ManagedIdDeepCopyMapper<>());
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
