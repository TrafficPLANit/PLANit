package org.planit.assignment.traditionalstatic;

import org.planit.assignment.TrafficAssignmentBuilder;
import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.utils.builder.Configurator;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

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
   * @throws PlanItException 
   */
  @Override
  protected Configurator<TraditionalStaticAssignment> createConfigurator() throws PlanItException {
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
  public TraditionalStaticAssignmentBuilder(final IdGroupingToken projectToken, final InputBuilderListener inputBuilder, final Demands demands, final Zoning zoning,
      final PhysicalNetwork<?, ?, ?> physicalNetwork) throws PlanItException {
    super(TraditionalStaticAssignment.class, projectToken, inputBuilder, demands, zoning, physicalNetwork);
  }
}
