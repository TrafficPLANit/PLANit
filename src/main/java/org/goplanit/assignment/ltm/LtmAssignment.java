package org.goplanit.assignment.ltm;

import java.util.logging.Logger;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.path.choice.PathChoice;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagramComponent;
import org.goplanit.supply.network.nodemodel.NodeModelComponent;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.time.TimePeriod;

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

  /** track most recent time period that is being simulated */
  protected TimePeriod mostRecentTimePeriod = null;

  protected void setTimePeriod(TimePeriod timePeriod){
    this.mostRecentTimePeriod = timePeriod;
  }

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
    var macroscopicNetwork = (MacroscopicNetwork) getInfrastructureNetwork();
    PlanItException.throwIf(macroscopicNetwork.getTransportLayers().size() != 1,
        "LTM is currently only compatible with networks using a single transport layer in its physical network");
    var networkLayer = macroscopicNetwork.getTransportLayers().getFirst();
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
  protected void verifyComponentCompatibility(){
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
   * @param other to copy
   * @param deepCopy when true, create a eep copy, shallow copy otherwise
   */
  protected LtmAssignment(final LtmAssignment other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * Provide access to current/most recently simulated time period
   *
   * @return time period
   */
  @Override
  public TimePeriod getTimePeriod() {
    return this.mostRecentTimePeriod;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract LtmAssignment shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract LtmAssignment deepClone();

  // Getters - Setters

  /**
   * Set the fundamental diagram
   * 
   * @param fundamentalDiagram the fundamental diagram
   */
  public void setFundamentalDiagram(final FundamentalDiagramComponent fundamentalDiagram) {
    logRegisteredComponentName(fundamentalDiagram, true);
    registerComponent(FundamentalDiagramComponent.class, fundamentalDiagram);
  }

  /**
   * The node model to use
   * 
   * @param nodeModel to use
   */
  public void setNodeModel(final NodeModelComponent nodeModel) {
    logRegisteredComponentName(nodeModel, true);
    registerComponent(NodeModelComponent.class, nodeModel);
  }

  /**
   * The path choice model to use
   * 
   * @return path choice model used
   */
  public PathChoice getPathChoice() {
    return getTrafficAssignmentComponent(PathChoice.class);
  }

  /**
   * The path choice model to use
   * 
   * @param pathChoice model used
   */
  public void setPathChoice(PathChoice pathChoice) {
    logRegisteredComponentName(pathChoice, true);
    registerComponent(PathChoice.class, pathChoice);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
  }

}
