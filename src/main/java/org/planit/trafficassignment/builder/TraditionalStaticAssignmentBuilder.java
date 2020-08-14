package org.planit.trafficassignment.builder;

import org.planit.assignment.traditionalstatic.TraditionalStaticAssignment;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentODOutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticPathOutputTypeAdapter;
import org.planit.output.enums.OutputType;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.utils.configurator.Configurator;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;

/**
 * Builder for a traditional static assignment. It injects the following defaults into the underlying assignment instance:
 * 
 * <ul>
 * <li>Link based relative duality gap function</li>
 * <li>MSA smoothing</li>
 * <li>BPR function for physical cost</li>
 * <li>Fixed cost for virtual cost</li>
 * <li>Default StopCriterion implementation</li>
 * </ul>
 *
 * @author markr
 *
 */
public class TraditionalStaticAssignmentBuilder extends TrafficAssignmentBuilder<TraditionalStaticAssignment> {

  /**
   * the user will configure this builder via this configurator
   * 
   * @return TraditionalStaticAssignmentConfigurator instance
   */
  @Override
  protected Configurator<TraditionalStaticAssignment> createConfigurator() {
    return new TraditionalStaticAssignmentConfigurator(TraditionalStaticAssignment.class);
  }


  /**
   * Constructor
   * 
   * @param projectToken    id grouping token
   * @param inputBuilder    the inputBuilder
   * @param demands         the demands
   * @param zoning          the zoning
   * @param physicalNetwork the physical network
   * @throws PlanItException thrown if there is an error
   */
  protected TraditionalStaticAssignmentBuilder(final IdGroupingToken projectToken, final InputBuilderListener inputBuilder, final Demands demands, final Zoning zoning,
      final PhysicalNetwork<?, ?, ?> physicalNetwork) throws PlanItException {
    super(TraditionalStaticAssignment.class, projectToken, inputBuilder, demands, zoning, physicalNetwork);

    // initialise defaults
    this.createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
    this.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
    this.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
  }
}
