package org.goplanit.cost.virtual;

import java.util.HashMap;
import java.util.Map;

import org.goplanit.network.virtual.VirtualNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.time.TimePeriod;

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
   * Fixed connectoid cost for connectoid segments
   */
  protected double fixedConnectoidCost = DEFAULT_FIXED_COST;

  /**
   * Default fixed cost
   */
  public static final double DEFAULT_FIXED_COST = 0.0;

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
   * {@inheritDoc}
   */
  @Override
  public void initialiseBeforeSimulation(final VirtualNetwork virtualNetwork) throws PlanItException {
    // do nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateTimePeriod(TimePeriod timePeriod) {
    // not supported that we have different fixed costs per period yet
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getGeneralisedCost(final Mode mode, final ConnectoidSegment connectoidSegment) {
    return fixedConnectoidCost;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void populateWithCost(final VirtualNetwork virtualNetwork, final Mode mode, double[] costToFill) throws PlanItException {
    for (var virtualSegment : virtualNetwork.getConnectoidSegments()) {
      costToFill[(int) virtualSegment.getId()] = fixedConnectoidCost;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FixedConnectoidTravelTimeCost clone() {
    return new FixedConnectoidTravelTimeCost(this);
  }

  /**
   * Chosen cost is considered configuration not internal state, so upon resetting the chosen cost remains in tact
   */
  @Override
  public void reset() {
    // Chosen cost is considered configuration not internal state, so do nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTravelTimeCost(Mode mode, ConnectoidSegment connectoidSegment) {
    return getGeneralisedCost(mode, connectoidSegment);
  }

  /**
   * fixed cost so derivative is always zero
   */
  @Override
  public double getDTravelTimeDFlow(boolean uncongested, Mode mode, ConnectoidSegment connectoidSegment) {
    return 0.0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    var settings = new HashMap<String, String>();
    settings.put("fixed-connectoid-cost (h)", "" + fixedConnectoidCost);
    return settings;
  }
}
