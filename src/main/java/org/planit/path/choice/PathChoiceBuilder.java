package org.planit.path.choice;

import org.planit.utils.exceptions.PlanItException;

/**
 * The type of Path choice is sometimes but not always a choice for any given traffic assignment
 * model. Hence, instead of equipping every traffic assignment builder with a path choice component
 * to register or create, we allow any traffic assignment builder to implement this interface to
 * make the creation and registration of path choice components possible if relevant
 *
 * @author markr
 *
 */
public interface PathChoiceBuilder {

  /**
   * create and register the path choice one desires.
   *
   * @param physicalTraveltimeCostFunctionType the type of physical cost function to use
   * @return path choice instance
   * @throws PlanItException thrown if there is an error
   */
  public PathChoice createAndRegisterPathChoice(final String physicalTraveltimeCostFunctionType)
      throws PlanItException;

}
