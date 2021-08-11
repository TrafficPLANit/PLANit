package org.planit.assignment.ltm;

import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.network.MacroscopicNetwork;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.network.nodemodel.NodeModelComponent;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;

/**
 * Link Transmission Model implementation base implementation for network loading based on LTM network loading paradigm.
 *
 * @author markr
 *
 */
public abstract class LtmAssignment extends TrafficAssignment {

  /**
   * generated UID
   */
  private static final long serialVersionUID = -8595729519062817426L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(LtmAssignment.class.getCanonicalName());

  /**
   * Fundamental diagram to use
   */
  protected FundamentalDiagram fundamentalDiagram = null;

  /**
   * Node model to use
   */
  protected NodeModelComponent nodeModel = null;

  /**
   * Verify if the network contains a single compatible infrastructure layer because sLTM does not (yet) support multiple (or intermodal) network layers
   *
   * @throws PlanItException thrown if the components are not compatible
   */
  @Override
  protected void verifyComponentCompatibility() throws PlanItException {
    /* network compatibility */
    PlanItException.throwIf(!(transportNetwork.getInfrastructureNetwork() instanceof MacroscopicNetwork), "sLTM is only compatible with macroscopic networks");
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) transportNetwork.getInfrastructureNetwork();
    PlanItException.throwIf(macroscopicNetwork.getTransportLayers().size() != 1,
        "sLTM is currently only compatible with networks using a single transport layer in its physical network");
    MacroscopicNetworkLayer networkLayer = macroscopicNetwork.getTransportLayers().getFirst();
    if (transportNetwork.getInfrastructureNetwork().getModes().size() != networkLayer.getSupportedModes().size()) {
      LOGGER.warning("network wide modes do not match modes supported by the single available layer, consider removing unused modes");
    }
  }

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected LtmAssignment(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy Constructor
   * 
   * @param sltm to copy
   */
  protected LtmAssignment(LtmAssignment sltm) {
    super(sltm);
    this.fundamentalDiagram = sltm.fundamentalDiagram.clone();
    this.nodeModel = sltm.nodeModel.clone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract LtmAssignment clone();

  // Getters - Setters

  /**
   * Set the fundamental diagram
   * 
   * @param fundamentalDiagram the fundamental diagram
   */
  public void setFundamentalDiagram(final FundamentalDiagram fundamentalDiagram) {
    this.fundamentalDiagram = fundamentalDiagram;
  }

  /**
   * The node model to use
   * 
   * @param nodeModel to use
   */
  public void setNodeModel(final NodeModelComponent nodeModel) {
    this.nodeModel = nodeModel;
  }

}
