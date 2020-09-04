package org.planit.cost.virtual;

import org.planit.cost.Cost;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 * Interface to classify costs of virtual links
 * 
 * @author markr
 *
 */
public interface VirtualCost extends  Cost<ConnectoidSegment> {
  
  /** short hand for configuring fixed virtual cost instance */
  public static final String FIXED = FixedConnectoidTravelTimeCost.class.getCanonicalName();

  /** short hand for configuring speed based virtual cost instance */
  public static final String SPEED = SpeedConnectoidTravelTimeCost.class.getCanonicalName();  

  /**
   * Invoker expects (mode specific ) costs in passed in array to be filled, where each entry signifies a link segment by its id
   * 
   * @param mode the mode these costs pertain to
   * @param costToFill array of link segment costs identified by the link segment's internal id
   * @throws PlanItException thrown if error
   */
  public void populateWithCost(Mode mode, double[] costToFill) throws PlanItException; 
}
