package org.planit.cost.physical.initial;

import java.io.Serializable;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.cost.physical.PhysicalCost;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;

/**
 * The initial physical costs for the network
 * 
 * @author gman6028
 *
 */
public abstract class InitialPhysicalCost extends TrafficAssignmentComponent<InitialPhysicalCost> implements PhysicalCost, Serializable {

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
   * Set the initial cost for a specified mode and link segment
   * 
   * @param mode        the current mode
   * @param linkSegment the current link segment
   * @param cost        the initial cost for this mode and link segment
   */
  public abstract void setSegmentCost(final Mode mode, final MacroscopicLinkSegment linkSegment, double cost);

}
