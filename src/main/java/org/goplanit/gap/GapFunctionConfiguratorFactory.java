package org.goplanit.gap;

import org.goplanit.utils.exceptions.PlanItException;

/**
 * factory for the gap function types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class GapFunctionConfiguratorFactory {

  /**
   * Create a configurator for given gap function type
   * 
   * @param gapFunctionType type of gap function to create
   * @return the created configurator
   * @throws PlanItException thrown if error
   */
  public static GapFunctionConfigurator<? extends GapFunction> createConfigurator(final String gapFunctionType) throws PlanItException {

    if (gapFunctionType.equals(GapFunction.LINK_BASED_RELATIVE_GAP)) {
      return new LinkBasedRelativeGapConfigurator();
    } else if (gapFunctionType.equals(GapFunction.NORM_BASED_GAP)) {
      return new NormBasedGapConfigurator();
    } else {
      // TODO use value of string to use reflection and try and instantiate instead before throwing exception
      throw new PlanItException(String.format("unable to construct configurator for given gapFunctionType %s", gapFunctionType));
    }
  }
}
