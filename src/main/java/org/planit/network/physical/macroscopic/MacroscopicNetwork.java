package org.planit.network.physical.macroscopic;

import java.util.logging.Logger;

import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.PhysicalNetworkBuilder;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentTypes;

/**
 * Macroscopic Network which stores link segment types
 *
 * @author markr
 *
 */
public class MacroscopicNetwork extends PhysicalNetwork<Node, Link, MacroscopicLinkSegment> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(MacroscopicNetwork.class.getCanonicalName());

  /** Generated UID */
  private static final long serialVersionUID = -6844990013871601434L;

  // Protected

  /**
   * collect the builder as macroscopic network builder
   * 
   * @return macroscopic network builder of this network
   */
  protected MacroscopicPhysicalNetworkBuilder<? extends Node, ? extends Link, ? extends MacroscopicLinkSegment> getMacroscopicNetworkBuilder() {
    return (MacroscopicPhysicalNetworkBuilder<? extends Node, ? extends Link, ? extends MacroscopicLinkSegment>) getNetworkBuilder();
  }

  // Public
  
  public final MacroscopicLinkSegmentTypes linkSegmentTypes;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  @SuppressWarnings("unchecked")
  public MacroscopicNetwork(final IdGroupingToken groupId) {
    super(groupId, new MacroscopicPhysicalNetworkBuilderImpl());
    linkSegmentTypes = new MacroscopicLinkSegmentTypesImpl((MacroscopicPhysicalNetworkBuilder<?, ?, MacroscopicLinkSegment>) getMacroscopicNetworkBuilder());
  }

  /**
   * Constructor
   * 
   * @param groupId       contiguous id generation within this group for instances of this class
   * @param customBuilder a customBuilder
   */
  @SuppressWarnings("unchecked")
  public MacroscopicNetwork(final IdGroupingToken groupId, MacroscopicPhysicalNetworkBuilder<? extends Node, ? extends Link, ? extends MacroscopicLinkSegment> customBuilder) {
    super(groupId, (PhysicalNetworkBuilder<Node, Link, MacroscopicLinkSegment>) customBuilder);
    linkSegmentTypes = new MacroscopicLinkSegmentTypesImpl((MacroscopicPhysicalNetworkBuilder<?, ?, MacroscopicLinkSegment>) getMacroscopicNetworkBuilder());    
  }

}
