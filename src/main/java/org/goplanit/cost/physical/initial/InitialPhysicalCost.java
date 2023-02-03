package org.goplanit.cost.physical.initial;

import java.io.Serializable;

import org.goplanit.component.PlanitComponent;
import org.goplanit.cost.physical.PhysicalCost;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.time.TimePeriod;

/**
 * The initial physical costs for the network
 * 
 * @author gman6028, markr
 *
 */
public abstract class InitialPhysicalCost extends PlanitComponent<InitialPhysicalCost> implements PhysicalCost<MacroscopicLinkSegment>, Serializable {

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
   * @param deepCopy when true, create a eep copy, shallow copy otherwise
   */
  public InitialPhysicalCost(InitialPhysicalCost other, boolean deepCopy) {
    super(other, deepCopy);
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
   * @param timePeriod to use
   * @param mode       the mode
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

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract InitialPhysicalCost shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract InitialPhysicalCost deepClone();

}
