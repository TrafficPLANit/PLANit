package org.planit.cost.physical.initial;

import java.util.HashMap;
import java.util.Map;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;

/**
 * Initial Link Segment Costs stored by mode
 *
 * @author gman6028
 *
 */
public class InitialLinkSegmentCost extends InitialPhysicalCost {

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
   * Returns the initial cost for each link segment and mode
   *
   * @param mode        the current mode
   * @param linkSegment the current link segment
   * @return the cost for this link segment and mode
   */
  @Override
  public double getSegmentCost(final Mode mode, final LinkSegment linkSegment) {
    final Map<Long, Double> costPerLinkSegment = costPerModeAndLinkSegment.get(mode.getId());
    return costPerLinkSegment.get(linkSegment.getId());
  }

  /**
   * Sets the initial cost for each link segment and mode
   *
   * @param mode        the current mode
   * @param linkSegment the current link segment
   * @param cost        the initial cost for this link segment and mode
   */
  @Override
  public void setSegmentCost(final Mode mode, final LinkSegment linkSegment, final double cost) {

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
