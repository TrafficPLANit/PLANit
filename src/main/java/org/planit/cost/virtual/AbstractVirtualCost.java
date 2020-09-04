package org.planit.cost.virtual;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * Object to handle the travel time cost of a virtual link
 *
 * @author markr
 *
 */
public abstract class AbstractVirtualCost extends TrafficAssignmentComponent<AbstractVirtualCost> implements VirtualCost {

  /** generated UID */
  private static final long serialVersionUID = -8278650865770286434L;

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected AbstractVirtualCost(IdGroupingToken groupId) {
    super(groupId, AbstractVirtualCost.class);
  }

  /**
   * Initialize the virtual cost component
   *
   * @param virtualNetwork the virtual network
   * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters have been set
   */
  public abstract void initialiseBeforeSimulation(VirtualNetwork virtualNetwork) throws PlanItException;

}
