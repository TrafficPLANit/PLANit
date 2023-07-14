package org.goplanit.cost.physical;

import java.util.Map;

import org.goplanit.component.PlanitComponent;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.time.TimePeriod;

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
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public FreeFlowLinkTravelTimeCost(FreeFlowLinkTravelTimeCost other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialiseBeforeSimulation(LayeredNetwork<?, ?> network) throws PlanItException {
    PlanItException.throwIf(!(network instanceof MacroscopicNetwork), "Free flow  travel time cost is only compatible with macroscopic networks");
    var macroscopicNetwork = (MacroscopicNetwork) network;
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
   *
   */
  @Override
  public double getGeneralisedCost(final Mode mode, final MacroscopicLinkSegment linkSegment) {
    return linkSegment.computeFreeFlowTravelTimeHour(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTravelTimeCost(final Mode mode, final MacroscopicLinkSegment linkSegment) {
    return getGeneralisedCost(mode, linkSegment);
  }

  /**
   * Derivative of free flow travel time is zero
   */
  @Override
  public double getDTravelTimeDFlow(boolean uncongested, final Mode mode, final MacroscopicLinkSegment linkSegment) {
    return 0.0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FreeFlowLinkTravelTimeCost shallowClone() {
    return new FreeFlowLinkTravelTimeCost(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FreeFlowLinkTravelTimeCost deepClone() {
    return new FreeFlowLinkTravelTimeCost(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // do nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    // no settings
    return null;
  }

}
