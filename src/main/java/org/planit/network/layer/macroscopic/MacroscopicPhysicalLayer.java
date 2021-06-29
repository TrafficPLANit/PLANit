package org.planit.network.layer.macroscopic;

import java.util.logging.Logger;

import org.planit.network.layer.physical.PhysicalLayerImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.TopologicalLayer;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypes;
import org.planit.utils.network.layer.macroscopic.MacroscopicPhysicalLayerBuilder;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.layer.physical.PhysicalNetworkLayerBuilder;

/**
 * Macroscopic physical Network (layer) that supports one or more modes and link segment types, where the modes are registered on the network (Infrastructure network) level
 *
 * @author markr
 *
 */
public class MacroscopicPhysicalLayer extends PhysicalLayerImpl<Node, Link, MacroscopicLinkSegment> implements TopologicalLayer {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(MacroscopicPhysicalLayer.class.getCanonicalName());

  /** Generated UID */
  @SuppressWarnings("unused")
  private static final long serialVersionUID = -6844990013871601434L;

  // Protected

  /**
   * collect the builder as macroscopic network builder
   * 
   * @return macroscopic network builder of this network
   */
  protected MacroscopicPhysicalLayerBuilder<? extends Node, ? extends Link, ? extends MacroscopicLinkSegment> getMacroscopicNetworkBuilder() {
    return (MacroscopicPhysicalLayerBuilder<? extends Node, ? extends Link, ? extends MacroscopicLinkSegment>) getLayerBuilder();
  }

  // Public

  public final MacroscopicLinkSegmentTypes linkSegmentTypes;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  @SuppressWarnings("unchecked")
  public MacroscopicPhysicalLayer(final IdGroupingToken groupId) {
    super(groupId, new MacroscopicPhysicalLayerBuilderImpl(groupId));
    linkSegmentTypes = new MacroscopicLinkSegmentTypesImpl((MacroscopicPhysicalLayerBuilder<?, ?, MacroscopicLinkSegment>) getMacroscopicNetworkBuilder());
  }

  /**
   * Constructor
   * 
   * @param groupId       contiguous id generation within this group for instances of this class
   * @param customBuilder a customBuilder
   */
  @SuppressWarnings("unchecked")
  public MacroscopicPhysicalLayer(final IdGroupingToken groupId, MacroscopicPhysicalLayerBuilder<? extends Node, ? extends Link, ? extends MacroscopicLinkSegment> customBuilder) {
    super(groupId, (PhysicalNetworkLayerBuilder<Node, Link, MacroscopicLinkSegment>) customBuilder);
    linkSegmentTypes = new MacroscopicLinkSegmentTypesImpl((MacroscopicPhysicalLayerBuilder<?, ?, MacroscopicLinkSegment>) getMacroscopicNetworkBuilder());
  }

  /**
   * collect the link segment types, alternative to using the public member
   * 
   * @return the link segment types
   */
  public MacroscopicLinkSegmentTypes getLinkSegmentTypes() {
    return this.linkSegmentTypes;
  }

}
