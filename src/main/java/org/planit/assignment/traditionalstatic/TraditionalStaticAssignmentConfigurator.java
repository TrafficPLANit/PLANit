package org.planit.assignment.traditionalstatic;

import org.planit.assignment.TrafficAssignmentConfigurator;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.utils.exceptions.PlanItException;

/**
 * Configurator for traditional static assignment. It initialises the following defaults:
 * 
 * <ul>
 * <li>MSA smoothing (via configurator)</li>
 * <li>BPR function for physical cost (via configurator)</li>
 * <li>Fixed cost for virtual cost (via configurator)</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class TraditionalStaticAssignmentConfigurator extends TrafficAssignmentConfigurator<TraditionalStaticAssignment> {

  /**
   * Constructor
   * 
   * @param instanceType the type we are configuring for
   * @throws PlanItException thrown if error
   */
  public TraditionalStaticAssignmentConfigurator(Class<TraditionalStaticAssignment> instanceType) throws PlanItException {
    super(instanceType);

    // initialise defaults
    this.createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
    this.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
    this.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
  }

}
