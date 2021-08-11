package org.planit.assignment.traditionalstatic;

import org.planit.assignment.TrafficAssignmentBuilder;
import org.planit.assignment.TrafficAssignmentConfigurator;
import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.TransportLayerNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.zoning.Zoning;

/**
 * Builder for a traditional static assignment. It injects the following defaults into the underlying assignment instance:
 * 
 * <ul>
 * <li>Link based relative duality gap function (default via base assignment implementation)</li>
 * <li>MSA smoothing (via configurator)</li>
 * <li>BPR function for physical cost (via configurator)</li>
 * <li>Fixed cost for virtual cost (via configurator)</li>
 * <li>Default StopCriterion implementation (default via base assignment implementation)</li>
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
   * @throws PlanItException thrown if error
   */
  @Override
  protected TrafficAssignmentConfigurator<TraditionalStaticAssignment> createConfigurator() throws PlanItException {
    return new TraditionalStaticAssignmentConfigurator(TraditionalStaticAssignment.class);
  }

  /**
   * Constructor
   * 
   * @param projectToken id grouping token
   * @param inputBuilder the inputBuilder
   * @param demands      the demands
   * @param zoning       the zoning
   * @param network      the network
   * @throws PlanItException thrown if there is an error
   */
  public TraditionalStaticAssignmentBuilder(final IdGroupingToken projectToken, final InputBuilderListener inputBuilder, final Demands demands, final Zoning zoning,
      final TransportLayerNetwork<?, ?> network) throws PlanItException {
    super(TraditionalStaticAssignment.class, projectToken, inputBuilder, demands, zoning, network);
  }
}
