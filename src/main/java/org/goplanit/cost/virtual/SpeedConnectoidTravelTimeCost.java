package org.goplanit.cost.virtual;

import java.util.HashMap;
import java.util.Map;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.utils.time.TimePeriod;

/**
 * Class to calculate the connectoid travel time using connectoid speed
 *
 * @author gman6028
 *
 */
public class SpeedConnectoidTravelTimeCost extends AbstractVirtualCost {

  /** generated UID */
  private static final long serialVersionUID = 2813935702895030693L;

  /** default to apply */
  public static final double DEFAULT_CONNECTOID_SPEED_KPH = 25.0;

  /**
   * Speed used for connectoid cost calculations
   */
  private double connectoidSpeed;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public SpeedConnectoidTravelTimeCost(IdGroupingToken groupId) {
    super(groupId);
    connectoidSpeed = DEFAULT_CONNECTOID_SPEED_KPH;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public SpeedConnectoidTravelTimeCost(SpeedConnectoidTravelTimeCost other) {
    super(other);
    connectoidSpeed = other.connectoidSpeed;
  }

  /**
   * set the connectoid speed
   * 
   * @param connectoidSpeed the speed
   */
  public void setConnectoidSpeed(final double connectoidSpeed) {
    this.connectoidSpeed = connectoidSpeed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialiseBeforeSimulation(final VirtualNetwork virtualNetwork) throws PlanItException {
    // currently no specific initialization needed
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateTimePeriod(TimePeriod timePeriod) {
    // not supported that we have different fixed costs per period yet
  }

  /**
   * Return the connectoid travel time using speed
   *
   * @param mode              the mode of travel
   * @param connectoidSegment the connectoid segment
   * @return the travel time for this connectoid segment
   */
  @Override
  public double getGeneralisedCost(final Mode mode, final ConnectoidSegment connectoidSegment) {
    return connectoidSegment.getParent().getLengthKm() / connectoidSpeed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void populateWithCost(final VirtualNetwork virtualNetwork, final Mode mode, double[] costToFill) throws PlanItException {
    for (var virtualSegment : virtualNetwork.getConnectoidSegments()) {
      costToFill[(int) virtualSegment.getId()] = getGeneralisedCost(mode, virtualSegment);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SpeedConnectoidTravelTimeCost clone() {
    return new SpeedConnectoidTravelTimeCost(this);
  }

  /**
   * Chosen speed is considered configuration not internal state, so upon resetting the chosen speed remains in tact
   */
  @Override
  public void reset() {
    // Chosen speed is considered configuration not internal state, so upon resetting the chosen speed remains in tact
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTravelTimeCost(Mode mode, ConnectoidSegment connectoidSegment) {
    return getGeneralisedCost(mode, connectoidSegment);
  }

  /**
   * Fixed, so derivative is always zero
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
    settings.put("fixed-speed-connectoid-cost (km/h)", "" + connectoidSpeed);
    return settings;
  }

}
