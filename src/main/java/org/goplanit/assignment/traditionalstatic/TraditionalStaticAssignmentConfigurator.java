package org.goplanit.assignment.traditionalstatic;

import org.goplanit.assignment.TrafficAssignmentConfigurator;
import org.goplanit.cost.physical.BprLinkTravelTimeCost;
import org.goplanit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.goplanit.gap.LinkBasedRelativeDualityGapFunction;
import org.goplanit.sdinteraction.smoothing.MSASmoothing;

/**
 * Configurator for traditional static assignment. It initialises the following defaults:
 * 
 * <ul>
 * <li>MSA smoothing (via configurator)</li>
 * <li>BPR function for physical cost (via configurator)</li>
 * <li>Fixed cost for virtual cost (via configurator)</li>
 * <li>Link based relative gap function (via configurator)</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class TraditionalStaticAssignmentConfigurator extends TrafficAssignmentConfigurator<TraditionalStaticAssignment>{

  /**
   * Constructor
   * 
   * @param instanceType the type we are configuring for
   */
  public TraditionalStaticAssignmentConfigurator(Class<TraditionalStaticAssignment> instanceType){
    super(instanceType);

    // initialise defaults
    this.createAndRegisterPhysicalCost(BprLinkTravelTimeCost.class.getCanonicalName());
    this.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
    this.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
    this.createAndRegisterGapFunction(LinkBasedRelativeDualityGapFunction.class.getCanonicalName());
  }

}
