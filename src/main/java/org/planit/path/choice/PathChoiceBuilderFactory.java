package org.planit.path.choice;

import org.planit.input.InputBuilderListener;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * Path choice builder factory for the path choice types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class PathChoiceBuilderFactory {

  /**
   * Create a builder for given path choice type
   * 
   * @param pathChoiceType type of assignment the builder is created for
   * @param projectToken          id group this builder is created for
   * @param inputBuilder          the input builder
   * @return the created builder
   * @throws PlanItException thrown if error
   */
  public static PathChoiceBuilder<? extends PathChoice> createBuilder(
      final String pathChoiceType, 
      final IdGroupingToken projectToken, 
      InputBuilderListener inputBuilder) throws PlanItException {

    if (pathChoiceType.equals(PathChoice.STOCHASTIC)) {
      return new StochasticPathChoiceBuilder(projectToken, inputBuilder);
    } else {
      throw new PlanItException(String.format("Unable to construct builder for given path choice type %s", pathChoiceType));
    }
  }
}
