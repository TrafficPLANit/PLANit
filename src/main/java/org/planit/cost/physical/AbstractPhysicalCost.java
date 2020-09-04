package org.planit.cost.physical;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;

/**
 * Class for dynamic cost functions, which calculate link segment costs for each iteration
 *
 * @author markr, gman6028
 *
 */
public abstract class AbstractPhysicalCost extends TrafficAssignmentComponent<AbstractPhysicalCost> implements PhysicalCost {

  /** generated UID */
  private static final long serialVersionUID = 3657719270477537657L;

  /**
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected AbstractPhysicalCost(IdGroupingToken groupId) {
    super(groupId, AbstractPhysicalCost.class);
  }

  /**
   * Initialize the cost parameter values in the network
   *
   * @param physicalNetwork the physical network
   * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters have been set
   */
  public abstract void initialiseBeforeSimulation(PhysicalNetwork<? extends Node, ? extends Link, ? extends LinkSegment> physicalNetwork) throws PlanItException;

}
