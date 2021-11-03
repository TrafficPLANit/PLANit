package org.goplanit.cost.virtual;

import java.io.Serializable;

import org.goplanit.component.PlanitComponent;
import org.goplanit.network.virtual.VirtualNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.time.TimePeriod;

/**
 * Object to handle the travel time cost of a virtual link
 *
 * @author markr
 *
 */
public abstract class AbstractVirtualCost extends PlanitComponent<AbstractVirtualCost> implements VirtualCost, Serializable {

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
   * Copy Constructor
   *
   * @param other to copy
   */
  public AbstractVirtualCost(final AbstractVirtualCost other) {
    super(other);
  }

  /**
   * Initialize the virtual cost component
   *
   * @param virtualNetwork the virtual network
   * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters have been set
   */
  public abstract void initialiseBeforeSimulation(final VirtualNetwork virtualNetwork) throws PlanItException;

  /**
   * Provide the cost calculation with information regarding the time period for which the cost is to be calculated
   * 
   * @param timePeriod to apply
   */
  public abstract void updateTimePeriod(final TimePeriod timePeriod);

}
