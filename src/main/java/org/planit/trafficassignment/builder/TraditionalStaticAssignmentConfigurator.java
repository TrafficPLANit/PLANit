package org.planit.trafficassignment.builder;

import org.planit.assignment.traditionalstatic.TraditionalStaticAssignment;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.utils.exceptions.PlanItException;

/**
 * Configurator for traditional static assignment
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
