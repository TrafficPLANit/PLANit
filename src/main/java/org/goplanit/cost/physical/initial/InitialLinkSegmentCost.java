package org.goplanit.cost.physical.initial;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.time.TimePeriod;

/**
 * Initial Link Segment Costs stored by mode
 *
 * @author gman6028, markr
 *
 */
public class InitialLinkSegmentCost extends InitialPhysicalCost {

  private static final Logger LOGGER = Logger.getLogger(InitialLinkSegmentCost.class.getCanonicalName());

  /** generated UID */
  private static final long serialVersionUID = 2164407379859550420L;

  /**
   * Store initial cost for each mode and link segment, not linked to a particular time period
   */
  protected final InitialModesLinkSegmentCost timePeriodAgnosticCosts;

  /**
   * Map to store initial cost for each mode and link segment, linked to a particular time period
   */
  protected Map<TimePeriod, InitialModesLinkSegmentCost> timePeriodCosts;

  /**
   * Returns the initial cost. When absent but mode is not allowed on link segment, positive infinity is used, otherwise we revert to free flow travel time and a warning is logged.
   *
   * @param initialCostsByMode to use
   * @param mode               the current mode
   * @param linkSegment        the current link segment
   * @return the cost for this link segment and mode
   */
  protected double getSegmentCost(InitialModesLinkSegmentCost initialCostsByMode, Mode mode, MacroscopicLinkSegment linkSegment) {
    boolean present = (initialCostsByMode != null);

    double initialCost = Double.POSITIVE_INFINITY;
    if (present) {
      initialCost = initialCostsByMode.getGeneralisedCost(mode, linkSegment);
      present = (initialCost != Double.POSITIVE_INFINITY);
    }

    if (!present) {
      if (!linkSegment.isModeAllowed(mode)) {
        initialCost = Double.POSITIVE_INFINITY;
      } else {
        initialCost = ((MacroscopicLinkSegment) linkSegment).computeFreeFlowTravelTimeHour(mode);
        LOGGER.warning(String.format("initial cost missing for link segment %s (id:%d), reverting to free flow travel time %.2f(h)", linkSegment.getXmlId(), linkSegment.getId(),
            initialCost));
      }
    }
    return initialCost;
  }

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public InitialLinkSegmentCost(IdGroupingToken groupId) {
    super(groupId);
    timePeriodAgnosticCosts = new InitialModesLinkSegmentCost();
    timePeriodCosts = new HashMap<TimePeriod, InitialModesLinkSegmentCost>();
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public InitialLinkSegmentCost(InitialLinkSegmentCost other) {
    super(other);
    this.timePeriodAgnosticCosts = other.timePeriodAgnosticCosts.clone();
    this.timePeriodCosts = new HashMap<TimePeriod, InitialModesLinkSegmentCost>();
    other.timePeriodCosts.forEach((k, v) -> timePeriodCosts.put(k, v.clone()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSegmentCostsSetForMode(final Mode mode) {
    return timePeriodAgnosticCosts.isSegmentCostsSetForMode(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSegmentCostsSetForTimePeriod(final TimePeriod timePeriod) {
    return timePeriodCosts.containsKey(timePeriod);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSegmentCostsSetForMode(final TimePeriod timePeriod, final Mode mode) {
    return isSegmentCostsSetForTimePeriod(timePeriod) ? this.timePeriodCosts.containsKey(timePeriod) : timePeriodCosts.get(timePeriod).isSegmentCostsSetForMode(mode);
  }

  /**
   * Returns the initial cost for each link segment and mode for time period agnostic registrations. When absent but mode is not allowed on link segment, positive infinity is used,
   * otherwise we revert to free flow travel time and a warning is logged.
   *
   * @param mode        the current mode
   * @param linkSegment the current link segment
   * @return the cost for this link segment and mode
   */
  @Override
  public double getGeneralisedCost(final Mode mode, final MacroscopicLinkSegment linkSegment) {
    return getSegmentCost(timePeriodAgnosticCosts, mode, linkSegment);
  }

  /**
   * Returns the initial cost for each link segment and mode for time period specific registrations. When absent but mode is not allowed on link segment, positive infinity is used,
   * otherwise we revert to free flow travel time and a warning is logged.
   *
   * @param timePeriod  the time period
   * @param mode        the current mode
   * @param linkSegment the current link segment
   * @return the cost for this link segment and mode
   */
  @Override
  public double getSegmentCost(final TimePeriod timePeriod, final Mode mode, final MacroscopicLinkSegment linkSegment) {
    return getSegmentCost(timePeriodCosts.get(timePeriod), mode, linkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setSegmentCost(final Mode mode, final MacroscopicLinkSegment linkSegment, final double cost) {
    timePeriodAgnosticCosts.setSegmentCost(mode, linkSegment, cost);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setSegmentCost(final TimePeriod timePeriod, final Mode mode, final MacroscopicLinkSegment linkSegment, final double cost) {
    InitialModesLinkSegmentCost initialCosts = timePeriodCosts.get(timePeriod);
    if (initialCosts == null) {
      initialCosts = new InitialModesLinkSegmentCost();
      timePeriodCosts.put(timePeriod, initialCosts);
    }
    initialCosts.setSegmentCost(mode, linkSegment, cost);
  }

  /**
   * same as {@link #setSegmentCost(Mode, MacroscopicLinkSegment, double)} only based on link segment's id
   */
  public void setSegmentCost(final Mode mode, final long linkSegmentId, final double cost) {
    timePeriodAgnosticCosts.setSegmentCost(mode, linkSegmentId, cost);
  }

  /**
   * Provide the time period agnostic costs
   * 
   * @return time period agnostic costs
   */
  public InitialModesLinkSegmentCost getTimePeriodAgnosticCosts() {
    return timePeriodAgnosticCosts;
  }

  /**
   * The time period specific costs available
   * 
   * @param timePeriod to collect for
   * @return costs registered, null if not present
   */
  public InitialModesLinkSegmentCost getTimePeriodCosts(final TimePeriod timePeriod) {
    return timePeriodCosts.get(timePeriod);
  }

  /**
   * The registered time periods that have initial costs
   * 
   * @return time periods
   */
  public Set<TimePeriod> getTimePeriods() {
    return Collections.unmodifiableSet(timePeriodCosts.keySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InitialLinkSegmentCost clone() {
    return new InitialLinkSegmentCost(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    timePeriodAgnosticCosts.reset();
    timePeriodCosts.forEach((k, v) -> v.reset());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTravelTimeCost(final Mode mode, final MacroscopicLinkSegment linkSegment) {
    return getGeneralisedCost(mode, linkSegment);
  }

  /**
   * Not supported returns -infinity for all calls and logs severe warning
   */
  @Override
  public double getDTravelTimeDFlow(boolean uncongested, final Mode mode, final MacroscopicLinkSegment linkSegment) {
    LOGGER.severe("Initial cost has no derivative, unable to compute");
    return Double.NEGATIVE_INFINITY;
  }

}
