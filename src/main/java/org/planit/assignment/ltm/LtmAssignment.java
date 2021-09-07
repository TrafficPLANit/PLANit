package org.planit.assignment.ltm;

import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.network.MacroscopicNetwork;
import org.planit.path.choice.PathChoice;
import org.planit.supply.fundamentaldiagram.FundamentalDiagramComponent;
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
  private FundamentalDiagramComponent fundamentalDiagram = null;

  /**
   * Node model to use
   */
  private NodeModelComponent nodeModel = null;

  /**
   * the path choice to use
   */
  private PathChoice pathChoice = null;

  /**
   * The used network layer
   * 
   * @return network layer used
   */
  protected MacroscopicNetworkLayer getUsedNetworkLayer() {
    return ((MacroscopicNetwork) getInfrastructureNetwork()).getTransportLayers().getFirst();
  }

  /**
   * Verify if the network contains a single compatible infrastructure layer because sLTM does not (yet) support multiple (or intermodal) network layers
   *
   * @throws PlanItException thrown if the components are not compatible
   */
  @Override
  protected void verifyNetworkDemandZoningCompatibility() throws PlanItException {
    PlanItException.throwIf(!(getInfrastructureNetwork() instanceof MacroscopicNetwork), "sLTM is only compatible with macroscopic networks");
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) getInfrastructureNetwork();
    PlanItException.throwIf(macroscopicNetwork.getTransportLayers().size() != 1,
        "LTM is currently only compatible with networks using a single transport layer in its physical network");
    MacroscopicNetworkLayer networkLayer = macroscopicNetwork.getTransportLayers().getFirst();
    if (getInfrastructureNetwork().getModes().size() != networkLayer.getSupportedModes().size()) {
      LOGGER.warning("LTM network wide modes do not match modes supported by the single available layer, consider removing unused modes");
    }
    if (getInfrastructureNetwork().getModes().size() != 1) {
      LOGGER.warning(String.format("LTM currently only supports a single mode but found %d", getInfrastructureNetwork().getModes().size()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void verifyComponentCompatibility() throws PlanItException {
    // not implemented yet
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
    this.pathChoice = sltm.pathChoice.clone();
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
  public void setFundamentalDiagram(final FundamentalDiagramComponent fundamentalDiagram) {
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

  /**
   * The path choice model to use
   * 
   * @return path choice model used
   */
  public PathChoice getPathChoice() {
    return pathChoice;
  }

  /**
   * The path choice model to use
   * 
   * @param pathChoice model used
   */
  public void setPathChoice(PathChoice pathChoice) {
    this.pathChoice = pathChoice;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
    fundamentalDiagram.reset();
    nodeModel.reset();
    pathChoice.reset();
  }

}
