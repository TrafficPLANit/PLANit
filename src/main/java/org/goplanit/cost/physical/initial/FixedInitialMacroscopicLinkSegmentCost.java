package org.goplanit.cost.physical.initial;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;

/**
 * Initial Link Segment Cost for which all the link segments have the same cost value for a specified mode
 *
 * @author gman6028
 *
 */
public class FixedInitialMacroscopicLinkSegmentCost extends InitialMacroscopicLinkSegmentCost {

  /** generated UID */
  private static final long serialVersionUID = 535851629771497368L;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public FixedInitialMacroscopicLinkSegmentCost(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public FixedInitialMacroscopicLinkSegmentCost(FixedInitialMacroscopicLinkSegmentCost other) {
    super(other);
  }

  /**
   * Sets a given cost for all link segments for a given mode
   *
   * @param mode           the specified mode
   * @param cost           the cost of travel to be used
   * @param noLinkSegments the number of link segments
   */
  public void setAllSegmentCosts(final Mode mode, final double cost, final int noLinkSegments) {
    for (long i = 0; i < noLinkSegments; i++) {
      setSegmentCost(mode, i, cost);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FixedInitialMacroscopicLinkSegmentCost clone() {
    return new FixedInitialMacroscopicLinkSegmentCost(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
  }
}
