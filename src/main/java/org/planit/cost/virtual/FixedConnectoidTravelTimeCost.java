package org.planit.cost.virtual;

import org.planit.network.virtual.VirtualNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 *
 * Class holding fixed connectoid costs for each connectoid segment
 *
 * @author markr
 *
 */

public class FixedConnectoidTravelTimeCost extends AbstractVirtualCost {

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
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public FixedConnectoidTravelTimeCost(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   *
   * Copy Constructor
   *
   * @param other to copy
   */
  public FixedConnectoidTravelTimeCost(FixedConnectoidTravelTimeCost other) {
    super(other);
    this.fixedConnectoidCost = other.fixedConnectoidCost;
  }

  /**
   * set the fixed cost used for all relevant link segments
   * 
   * @param fixedConnectoidCost the fixed cost to use
   */
  public void setFixedConnectoidCost(final double fixedConnectoidCost) {
    this.fixedConnectoidCost = fixedConnectoidCost;
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

  @Override
  public void populateWithCost(Mode mode, double[] costToFill) throws PlanItException {
    // TODO Auto-generated method stub
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FixedConnectoidTravelTimeCost clone() {
    return new FixedConnectoidTravelTimeCost(this);
  }
}
