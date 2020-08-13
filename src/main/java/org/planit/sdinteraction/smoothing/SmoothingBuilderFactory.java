package org.planit.sdinteraction.smoothing;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * Traffic assignment builder factory for the assignment types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class SmoothingBuilderFactory {

  /**
   * Create a builder for given smoothing type
   * 
   * @param smoothingType   type of assignment the builder is created for
   * @param assignmentToken id group this builder is created for
   * @return the created builder
   * @throws PlanItException thrown if error
   */
  public static SmoothingBuilder<?> createBuilder(final String smoothingType, IdGroupingToken assignmentToken) throws PlanItException {

    if (smoothingType.equals(Smoothing.MSA)) {
      return new MSASmoothingBuilder(assignmentToken);
    } else {
      throw new PlanItException(String.format("Unable to construct builder for given smoothingType %s", smoothingType));
    }
  }
}
