package org.planit.network.macroscopic;

import java.io.Serializable;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.utils.id.IdGroupingToken;

/**
 * Macroscopic Network which stores one or more macroscopic network layers that together form the complete (intermodal) network
 *
 * @author markr
 *
 */
public class MacroscopicNetwork extends TrafficAssignmentComponent<MacroscopicNetwork> implements Serializable {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(MacroscopicNetwork.class.getCanonicalName());

  /** Generated UID */
  private static final long serialVersionUID = -4208133694967189790L;

  // Protected

  // Public
  
  /** stores the various layers grouped by their supported modes of transport */
  public final InfrastructureLayers infrastructureLayers;
    
  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public MacroscopicNetwork(final IdGroupingToken groupId) {
    super(groupId, PhysicalNetwork.class);
    this.infrastructureLayers = new InfraSructureLayersImpl();
  }
  
}
