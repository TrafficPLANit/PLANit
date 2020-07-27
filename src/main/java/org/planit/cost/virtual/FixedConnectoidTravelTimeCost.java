package org.planit.cost.virtual;

import org.planit.exceptions.PlanItException;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 *
 * Class holding fixed connectoid costs for each connectoid segment
 *
 * @author markr
 *
 */

public class FixedConnectoidTravelTimeCost extends VirtualCost {

  /** Generate UID */
  private static final long serialVersionUID = 4907231205390412202L;

  /**
   * Fixed connectoid cost for connectoid segments - defaults to zero
   */
  protected double fixedConnectoidCost = 0.0;

  /**
   *
   * Constructor
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  public FixedConnectoidTravelTimeCost(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   *
   * Calculates the connectoid segment cost using a fixed travel time
   *
   *
   * @param mode              mode of travel
   * @param connectoidSegment the connectoid segment
   * @return the travel time for the specified connectoid segment
   */
  @Override
  public double getSegmentCost(final Mode mode, final ConnectoidSegment connectoidSegment) {
    return fixedConnectoidCost;
  }

  /**
   *
   * Initialize the virtual cost component
   *
   *
   * @param virtualNetwork the virtual network
   * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters have been set
   */
  @Override
  public void initialiseBeforeSimulation(final VirtualNetwork virtualNetwork) throws PlanItException {
    // currently no specific initialization needed
  }

  public void setFixedConnectoidCost(final double fixedConnectoidCost) {
    this.fixedConnectoidCost = fixedConnectoidCost;
  }
}
