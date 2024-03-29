package org.goplanit.cost.physical.initial;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.cost.Cost;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;

/**
 * Wrapper around all initial costs per link segment per mode. Used by InitialLinkSegmentCost to store costs per time period, or agnostic to a time period
 * 
 * @author markr
 */
public class InitialModesLinkSegmentCost implements Cost<MacroscopicLinkSegment> {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(InitialModesLinkSegmentCost.class.getCanonicalName());

  /**
   * Map to store initial cost for each mode and link segment, not linked to a particular time period
   */
  protected final Map<Mode, Map<Long, Double>> costPerModeAndLinkSegment;

  /**
   * Constructor
   */
  protected InitialModesLinkSegmentCost() {
    this.costPerModeAndLinkSegment = new HashMap<>();
  }

  /**
   * Copy constructor
   * 
   * @param initialLinkSegmentCostMode to copy
   */
  protected InitialModesLinkSegmentCost(InitialModesLinkSegmentCost initialLinkSegmentCostMode) {
    this();
    initialLinkSegmentCostMode.costPerModeAndLinkSegment.forEach((mode, map) -> costPerModeAndLinkSegment.put(mode, new HashMap<>(map)));
  }

  /**
   * Are link segment costs available for the given mode
   * 
   * @param mode the mode
   * @return true when available, false otherwise
   */
  public boolean isSegmentCostsSetForMode(final Mode mode) {
    return costPerModeAndLinkSegment.containsKey(mode);
  }

  /**
   * Returns the initial cost for each link segment and mode. When absent positive infinity is returned
   *
   * @param mode        the current mode
   * @param linkSegment the current link segment
   * @return the cost for this link segment and mode
   */
  @Override
  public double getGeneralisedCost(final Mode mode, final MacroscopicLinkSegment linkSegment) {
    final Map<Long, Double> costPerLinkSegment = costPerModeAndLinkSegment.get(mode);
    if (costPerLinkSegment == null) {
      return Double.POSITIVE_INFINITY;
    }
    Double foundCost = costPerLinkSegment.get(linkSegment.getId());
    if (foundCost == null) {
      return Double.POSITIVE_INFINITY;
    }
    return foundCost;
  }

  /**
   * Sets the initial cost for each link segment and mode
   *
   * @param mode        the current mode
   * @param linkSegment the current link segment
   * @param cost        the initial cost for this link segment and mode
   */
  public void setSegmentCost(final Mode mode, final MacroscopicLinkSegment linkSegment, final double cost) {

    if (!costPerModeAndLinkSegment.containsKey(mode)) {
      costPerModeAndLinkSegment.put(mode, new HashMap<Long, Double>());
    }
    costPerModeAndLinkSegment.get(mode).put(linkSegment.getId(), cost);
  }

  /**
   * Sets the initial cost for each link segment and mode
   *
   * @param mode          the current mode
   * @param linkSegmentId the id of the current link segment
   * @param cost          the initial cost for this link segment and mode
   *
   *                      At present this method is only used in unit tests.
   */
  public void setSegmentCost(final Mode mode, final long linkSegmentId, final double cost) {

    if (!costPerModeAndLinkSegment.containsKey(mode)) {
      costPerModeAndLinkSegment.put(mode, new HashMap<Long, Double>());
    }
    costPerModeAndLinkSegment.get(mode).put(linkSegmentId, cost);
  }

  /**
   * Shallow copy
   *
   * @return shallow copy
   */
  public InitialModesLinkSegmentCost shallowClone() {
    return new InitialModesLinkSegmentCost(this);
  }

  /**
   * Resetting initial cost will cause all intial costs to be removed
   */
  public void reset() {
    costPerModeAndLinkSegment.clear();
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
    LOGGER.severe("Initial (modes) cost has no derivative, unable to compute");
    return Double.NEGATIVE_INFINITY;
  }

}