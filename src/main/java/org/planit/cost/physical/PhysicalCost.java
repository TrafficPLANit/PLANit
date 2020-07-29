package org.planit.cost.physical;

import org.planit.network.physical.PhysicalNetwork;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * Class for dynamic cost functions, which calculate link segment costs for each iteration
 *
 * @author markr, gman6028
 *
 */
public abstract class PhysicalCost extends TrafficAssignmentComponent<PhysicalCost> implements AbstractPhysicalCost {

  /** generated UID */
  private static final long serialVersionUID = 3657719270477537657L;

  /**
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected PhysicalCost(IdGroupingToken groupId) {
    super(groupId, PhysicalCost.class);
  }

  /** short hand for configuring physical cost with BPR function instance */
  public static final String BPR = BPRLinkTravelTimeCost.class.getCanonicalName();

  /**
   * Initialize the cost parameter values in the network
   *
   * @param physicalNetwork the physical network
   * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters have been set
   */
  public abstract void initialiseBeforeSimulation(PhysicalNetwork physicalNetwork) throws PlanItException;

}
