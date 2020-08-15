package org.planit.gap;

import org.planit.utils.exceptions.PlanItException;

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
    } else {
      throw new PlanItException(String.format("unable to construct configurator for given gapFunctionType %s", gapFunctionType));
    }
  }
}
