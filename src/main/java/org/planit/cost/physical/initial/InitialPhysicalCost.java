package org.planit.cost.physical.initial;

import java.io.Serializable;

import org.planit.component.PlanitComponent;
import org.planit.cost.physical.PhysicalCost;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.time.TimePeriod;

/**
 * The initial physical costs for the network
 * 
 * @author gman6028, markr
 *
 */
public abstract class InitialPhysicalCost extends PlanitComponent<InitialPhysicalCost> implements PhysicalCost, Serializable {

  /** generated UID */
  private static final long serialVersionUID = -7894043964147010621L;

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  public InitialPhysicalCost(IdGroupingToken groupId) {
    super(groupId, InitialPhysicalCost.class);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public InitialPhysicalCost(InitialPhysicalCost other) {
    super(other);
  }

  /**
   * Are link segment costs available for the given mode (without time period)
   * 
   * @param mode the mode
   * @return true when available, false otherwise
   */
  public abstract boolean isSegmentCostsSetForMode(final Mode mode);

  /**
   * Are link segment costs available for the given time period
   * 
   * @param timePeriod the time period
   * @return true when available, false otherwise
   */
  public abstract boolean isSegmentCostsSetForTimePeriod(final TimePeriod timePeriod);

  /**
   * Are link segment costs available for the given mode (without time period)
   * 
   * @param mode the mode
   * @return true when available, false otherwise
   */
  public abstract boolean isSegmentCostsSetForMode(final TimePeriod timePeriod, final Mode mode);

  /**
   * Set the initial cost for a specified mode and link segment
   * 
   * @param mode        the current mode
   * @param linkSegment the current link segment
   * @param cost        the initial cost for this mode and link segment
   */
  public abstract void setSegmentCost(final Mode mode, final MacroscopicLinkSegment linkSegment, double cost);

  /**
   * Set the initial cost for a specified mode and link segment
   * 
   * @param timePeriod  the timePeriod for which the cost is specifically meant
   * @param mode        the current mode
   * @param linkSegment the current link segment
   * @param cost        the initial cost for this mode and link segment
   */
  public abstract void setSegmentCost(final TimePeriod timePeriod, final Mode mode, final MacroscopicLinkSegment linkSegment, double cost);

  /**
   * get the initial cost for a specified mode and link segment
   * 
   * @param mode        the current mode
   * @param linkSegment the current link segment
   * @return cost the initial cost for this mode and link segment
   */
  public abstract double getGeneralisedCost(final Mode mode, final MacroscopicLinkSegment linkSegment);

  /**
   * get the initial cost for a specified mode and link segment
   * 
   * @param timePeriod  the timePeriod for which the cost is specifically meant
   * @param mode        the current mode
   * @param linkSegment the current link segment
   * @return cost the initial cost for this mode and link segment
   */
  public abstract double getSegmentCost(final TimePeriod timePeriod, final Mode mode, final MacroscopicLinkSegment linkSegment);

}
