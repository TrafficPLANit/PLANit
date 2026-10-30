package org.goplanit.cost.virtual;

import org.goplanit.cost.Cost;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.network.virtual.VirtualNetwork;

/**
 * Interface to classify costs of virtual links
 * 
 * @author markr
 *
 */
public interface VirtualCost extends Cost<ConnectoidSegment> {

  /** short hand for configuring fixed virtual cost instance */
  public static final String FIXED = FixedConnectoidTravelTimeCost.class.getCanonicalName();

  /** short hand for configuring speed based virtual cost instance */
  public static final String SPEED = SpeedConnectoidTravelTimeCost.class.getCanonicalName();

  /** short hand for configuring steady state based virtual cost instance */
  public static final String STEADY_STATE = SteadyStateConnectoidTravelTimeCost.class.getCanonicalName();

  /**
   * Invoker expects (mode specific ) costs in passed in array to be filled, where each entry signifies a link segment by its id
   * 
   * @param virtualNetwork the cost pertains to
   * @param mode           the mode these costs pertain to
   * @param costToFill     array of link segment costs identified by the link segment's internal id
   */
  public void populateWithCost(final VirtualNetwork virtualNetwork, Mode mode, double[] costToFill);

  /**
   * Name of this cost type, should be based on the String representation that are made publicly available, e.g.,
   * FIXED, or SPEED.
   *
   * @return name of this type of cost
   */
  public abstract String getName();
}
