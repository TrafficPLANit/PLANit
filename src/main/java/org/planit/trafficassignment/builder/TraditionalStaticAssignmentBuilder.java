package org.planit.trafficassignment.builder;

import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.utils.exceptions.PlanItException;

/**
 * Builder for a traditional static assignment. It injects the following defaults into the underlying assignment instance:
 * 
 * <li>Link based relative duality gap function</li>
 * <li>MSA smoothing</li>
 * <li>BPR function for physical cost</li>
 * <li>Fixed cost for virtual cost</li>
 * <li>Default StopCriterion implementation</li>
 *
 * @author markr
 *
 */
public class TraditionalStaticAssignmentBuilder extends TrafficAssignmentBuilder {

  /**
   * Constructor
   * 
   * @param parentAssignment               the parent assignment
   * @param trafficComponentCreateListener the input builder
   * @param demands                        the demands
   * @param zoning                         the zoning
   * @param physicalNetwork                the physical network
   * @throws PlanItException thrown if there is an error
   */
  public TraditionalStaticAssignmentBuilder(final TrafficAssignment parentAssignment, final InputBuilderListener trafficComponentCreateListener, final Demands demands,
      final Zoning zoning, final PhysicalNetwork physicalNetwork) throws PlanItException {
    super(parentAssignment, trafficComponentCreateListener, demands, zoning, physicalNetwork);
  }

  /**
   * Traditional static assignment has the following defaults set for it: - PhysicalCost: BPR - Virtualcost: FIXED - Smoothing: MSA - Gapfunction: LinkBasedRelativeDualityGap
   */
  @Override
  public void initialiseDefaults() throws PlanItException {
    super.initialiseDefaults();
    // add defaults for traditional static assignment
    this.createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
    this.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
    this.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
  }

}
