package org.planit.cost.physical;

import org.planit.network.MacroscopicNetwork;
import org.planit.network.TransportLayerNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.time.TimePeriod;

/**
 * Simplest possible travel time cost, namely fixed to free flow travel time regardless of the flows measured.
 *
 * @author markr
 */
public class FreeFlowLinkTravelTimeCost extends AbstractPhysicalCost {

  /** use generated UID */
  private static final long serialVersionUID = 4465724624295866542L;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public FreeFlowLinkTravelTimeCost(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy Constructor
   * 
   * @param other to copy
   */
  public FreeFlowLinkTravelTimeCost(FreeFlowLinkTravelTimeCost other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialiseBeforeSimulation(TransportLayerNetwork<?, ?> network) throws PlanItException {
    PlanItException.throwIf(!(network instanceof MacroscopicNetwork), "Free flow  travel time cost is only compatible with macroscopic networks");
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) network;
    PlanItException.throwIf(macroscopicNetwork.getTransportLayers().size() != 1,
        "Free flow travel time cost is currently only compatible with networks using a single infrastructure layer");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateTimePeriod(TimePeriod timePeriod) {
    // no support for different free flow travel times across time periods, use fixed free flow travel time instead
  }

  /**
   * Return the free flow travel time for the current link for a given mode
   *
   * If the input data are invalid, this method returns a negative value.
   *
   * @param mode        the current Mode of travel
   * @param linkSegment the current link segment
   * @return the travel time for the current link (in hours)
   * @throws PlanItException when cost cannot be computed
   *
   */
  @Override
  public double getSegmentCost(final Mode mode, final MacroscopicLinkSegment linkSegment) throws PlanItException {
    return linkSegment.computeFreeFlowTravelTimeHour(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FreeFlowLinkTravelTimeCost clone() {
    return new FreeFlowLinkTravelTimeCost(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // do nothing
  }

}
