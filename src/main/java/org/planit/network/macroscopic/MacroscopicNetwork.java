package org.planit.network.macroscopic;

import java.util.logging.Logger;

import org.planit.network.TopologicalNetwork;
import org.planit.network.macroscopic.physical.MacroscopicPhysicalNetwork;
import org.planit.utils.id.IdGroupingToken;

/**
 * Macroscopic Network which stores one or more macroscopic network infrastructure layers that together form the complete (intermodal) network.
 *
 * @author markr
 *
 */
public class MacroscopicNetwork extends TopologicalNetwork<MacroscopicPhysicalNetwork, MacroscopicPhysicalNetworkLayers> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(MacroscopicNetwork.class.getCanonicalName());

  /** Generated UID */
  private static final long serialVersionUID = -4208133694967189790L;

  // Protected
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected MacroscopicPhysicalNetworkLayers createLayersContainer(IdGroupingToken networkIdToken) {
    return new MacroscopicPhysicalNetworkLayers(networkIdToken);
  }  

  // Public

  /**
   * Constructor
   * 
   * @param tokenId contiguous id generation within this group for instances of this class
   */
  public MacroscopicNetwork(final IdGroupingToken tokenId) {
    super(tokenId);
  }

}
