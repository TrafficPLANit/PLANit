package org.planit.assignment;

import org.planit.path.choice.PathChoice;
import org.planit.path.choice.PathChoiceConfigurator;
import org.planit.path.choice.PathChoiceConfiguratorFactory;
import org.planit.utils.exceptions.PlanItException;

/**
 * Configurator for traditional static assignment
 * 
 * @author markr
 *
 */
public class DynamicAssignmentConfigurator<T extends DynamicTrafficAssignment> extends TrafficAssignmentConfigurator<T> {

  /**
   * Nested configurator for path choice within this assignment
   */
  private PathChoiceConfigurator<? extends PathChoice> pathChoiceConfigurator = null;

  /**
   * Constructor
   * 
   * @param instanceType the type we are configuring for
   * @throws PlanItException thrown if error
   */
  public DynamicAssignmentConfigurator(Class<T> instanceType) throws PlanItException {
    super(instanceType);
  }

  /**
   * choose a particular path choice implementation
   * 
   * @param pathChoiceType type to choose
   * @return path choice configurator
   * @throws PlanItException thrown if error
   */
  public PathChoiceConfigurator<? extends PathChoice> createAndRegisterPathChoice(final String pathChoiceType) throws PlanItException {
    pathChoiceConfigurator = PathChoiceConfiguratorFactory.createConfigurator(pathChoiceType);
    return pathChoiceConfigurator;
  }

  /**
   * Collect the path choice configurator
   * 
   * @return path choice configurator
   */
  public PathChoiceConfigurator<? extends PathChoice> getPathChoice() {
    return pathChoiceConfigurator;
  }

}
