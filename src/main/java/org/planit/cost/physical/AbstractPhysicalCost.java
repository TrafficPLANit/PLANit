package org.planit.cost.physical;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
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
   * Invoker expects (mode specific ) costs in passed in array to be filled, where each entry signifies a link segment by its id.
   * This allows for more efficient implementations than having to revert to one by one updates. It does however rewuire network information
   * hence its placement here where via the initialiseBeforeSimulation, the network is provided
   * 
   * @param mode the mode these costs pertain to
   * @param costToFill array of link segment costs identified by the link segment's internal id
   * @throws PlanItException thrown if error
   */
  public abstract void populateWithCost(Mode mode, double[] costToFill) throws PlanItException;    

  /**
   * Initialize the cost parameter values in the network
   *
   * @param physicalNetwork the physical network
   * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters have been set
   */
  public abstract void initialiseBeforeSimulation(PhysicalNetwork<? extends Node, ? extends Link, ? extends LinkSegment> physicalNetwork) throws PlanItException;

}
