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
   * Create the output type adapter for the current output type
   *
   * @param outputType the current output type
   * @return the output type adapter corresponding to the current traffic assignment and output type
   */
  @Override
  public OutputTypeAdapter createOutputTypeAdapter(final OutputType outputType) {
    OutputTypeAdapter outputTypeAdapter = null;
    switch (outputType) {
    case LINK:
      outputTypeAdapter = new TraditionalStaticAssignmentLinkOutputTypeAdapter(outputType, this);
      break;
    case OD:
      outputTypeAdapter = new TraditionalStaticAssignmentODOutputTypeAdapter(outputType, this);
      break;
    case PATH:
      outputTypeAdapter = new TraditionalStaticPathOutputTypeAdapter(outputType, this);
      break;
    default:
      LOGGER.warning(LoggingUtils.createRunIdPrefix(getId()) + outputType.value() + " has not been defined yet.");
    }
    return outputTypeAdapter;
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
