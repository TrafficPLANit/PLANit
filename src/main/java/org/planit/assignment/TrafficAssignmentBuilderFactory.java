package org.planit.assignment;

import org.planit.assignment.traditionalstatic.TraditionalStaticAssignmentBuilder;
import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.InfrastructureNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.zoning.Zoning;

/**
 * Traffic assignment builder factory for the assignment types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class TrafficAssignmentBuilderFactory {

  /**
   * Create a builder for given assignment type
   * 
   * @param trafficAssignmentType type of assignment the builder is created for
   * @param projectToken          id group this builder is created for
   * @param inputBuilder          the input builder
   * @param theNetwork            network to register
   * @param theZoning             zoning to register
   * @param theDemands            demands to register
   * @return the created builder
   * @throws PlanItException thrown if error
   */
  public static TrafficAssignmentBuilder<?> createBuilder(final String trafficAssignmentType, IdGroupingToken projectToken, InputBuilderListener inputBuilder, Demands theDemands,
      Zoning theZoning, InfrastructureNetwork<?> theNetwork) throws PlanItException {

    if (trafficAssignmentType.equals(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT)) {
      return new TraditionalStaticAssignmentBuilder(projectToken, inputBuilder, theDemands, theZoning, theNetwork);
    } else {
      throw new PlanItException(String.format("Unable to construct builder for given trafficAssignmentType %s", trafficAssignmentType));
    }
  }
}
