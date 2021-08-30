package org.planit.cost.virtual;

import org.planit.network.virtual.VirtualNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.virtual.ConnectoidSegment;

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
   * Return the connectoid travel time using speed
   *
   * @param mode              the mode of travel
   * @param connectoidSegment the connectoid segment
   * @return the travel time for this connectoid segment
   */
  @Override
  public double getSegmentCost(final Mode mode, final ConnectoidSegment connectoidSegment) {
    return connectoidSegment.getParentEdge().getLengthKm() / connectoidSpeed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void populateWithCost(final VirtualNetwork virtualNetwork, final Mode mode, double[] costToFill) throws PlanItException {
    for (ConnectoidSegment virtualSegment : virtualNetwork.getConnectoidSegments()) {
      costToFill[(int) virtualSegment.getId()] = getSegmentCost(mode, virtualSegment);
    }
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
  public SpeedConnectoidTravelTimeCost clone() {
    return new SpeedConnectoidTravelTimeCost(this);
  }

}
