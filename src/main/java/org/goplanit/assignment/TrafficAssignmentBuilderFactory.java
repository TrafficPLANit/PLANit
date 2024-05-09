package org.goplanit.assignment;

import org.goplanit.assignment.ltm.sltm.StaticLtmTrafficAssignmentBuilder;
import org.goplanit.assignment.traditionalstatic.TraditionalStaticAssignmentBuilder;
import org.goplanit.demands.Demands;
import org.goplanit.input.InputBuilderListener;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.zoning.Zoning;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;

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
      Zoning theZoning, LayeredNetwork<?, ?> theNetwork) throws PlanItException {

    if (trafficAssignmentType.equals(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT)) {
      return new TraditionalStaticAssignmentBuilder(projectToken, inputBuilder, theDemands, theZoning, theNetwork);
    } else if (trafficAssignmentType.equals(TrafficAssignment.SLTM)) {
      return new StaticLtmTrafficAssignmentBuilder(projectToken, inputBuilder, theDemands, theZoning, theNetwork);

    } else {
      throw new PlanItException(
              "Unable to construct builder for given trafficAssignmentType %s", trafficAssignmentType);
    }
  }
}
