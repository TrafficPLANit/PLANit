package org.planit.cost.physical.initial;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;

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
   * Map to store initial cost for each mode and link segment
   */
  protected Map<Long, Map<Long, Double>> costPerModeAndLinkSegment;

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  public InitialLinkSegmentCost(IdGroupingToken groupId) {
    super(groupId);
    costPerModeAndLinkSegment = new HashMap<Long, Map<Long, Double>>();
  }

  /**
   * Are link segment costs available for the given mode
   * 
   * @param mode the mode
   * @return true when available, false otherwise
   */
  public boolean isSegmentCostsSetForMode(final Mode mode) {
    return costPerModeAndLinkSegment.containsKey(mode.getId());
  }

  /**
   * Returns the initial cost for each link segment and mode. When absent but mode is not allowed, positive infinity is used, otherwise we revert to free flow travel time and a
   * warning is logged.
   *
   * @param mode        the current mode
   * @param linkSegment the current link segment
   * @return the cost for this link segment and mode
   */
  @Override
  public double getSegmentCost(final Mode mode, final MacroscopicLinkSegment linkSegment) {
    final Map<Long, Double> costPerLinkSegment = costPerModeAndLinkSegment.get(mode.getId());
    Double initialCost = costPerLinkSegment.get(linkSegment.getId());
    if (initialCost == null) {
      if (!linkSegment.isModeAllowed(mode)) {
        initialCost = Double.POSITIVE_INFINITY;
      } else {
        initialCost = ((MacroscopicLinkSegment) linkSegment).computeFreeFlowTravelTime(mode);
        LOGGER.warning(String.format("initial cost missing for link segment %s (id:%d), reverting to free flow travel time %.2f(h)", linkSegment.getExternalId(),
            linkSegment.getId(), initialCost));
      }
    }
    return initialCost;
  }

  /**
   * Sets the initial cost for each link segment and mode
   *
   * @param mode        the current mode
   * @param linkSegment the current link segment
   * @param cost        the initial cost for this link segment and mode
   */
  @Override
  public void setSegmentCost(final Mode mode, final MacroscopicLinkSegment linkSegment, final double cost) {

    if (!costPerModeAndLinkSegment.containsKey(mode.getId())) {
      costPerModeAndLinkSegment.put(mode.getId(), new HashMap<Long, Double>());
    }
    costPerModeAndLinkSegment.get(mode.getId()).put(linkSegment.getId(), cost);
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

    if (!costPerModeAndLinkSegment.containsKey(mode.getId())) {
      costPerModeAndLinkSegment.put(mode.getId(), new HashMap<Long, Double>());
    }
    costPerModeAndLinkSegment.get(mode.getId()).put(id, cost);
  }

  /**
   * Collect initial cost id
   *
   * @return id
   */
  @Override
  public long getId() {
    return id;
  }

}
