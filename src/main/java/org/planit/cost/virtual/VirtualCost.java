package org.planit.cost.virtual;

import org.planit.cost.Cost;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 * Object to handle the travel time cost of a virtual link
 *
 * @author markr
 *
 */
public abstract class VirtualCost extends TrafficAssignmentComponent<VirtualCost> implements Cost<ConnectoidSegment> {

  /** generated UID */
  private static final long serialVersionUID = -8278650865770286434L;

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected VirtualCost(IdGroupingToken groupId) {
    super(groupId, VirtualCost.class);
  }

  /** short hand for configuring fixed virtual cost instance */
  public static final String FIXED = FixedConnectoidTravelTimeCost.class.getCanonicalName();

  /** short hand for configuring speed based virtual cost instance */
  public static final String SPEED = SpeedConnectoidTravelTimeCost.class.getCanonicalName();

  /**
   * Initialize the virtual cost component
   *
   * @param virtualNetwork the virtual network
   * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters have been set
   */
  public abstract void initialiseBeforeSimulation(VirtualNetwork virtualNetwork) throws PlanItException;

}
