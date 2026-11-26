package org.goplanit.interactor;

/**
 * Accessee that can be accessed to obtain access to one or more traffic assignment components
 * 
 * @author markr
 *
 */
public interface TrafficAssignmentComponentAccessee extends InteractorAccessee {

  /**
   * Collect the desired traffic assignment component by its class assuming it is available on the assignment.
   * These are components created and registered on the assignment, specific to the assignment itself, not component
   * inputs available upon creation. Derived assignments might register more components beyond the standard ones
   * registered on the base class (gapfunction, smoothing, physical, virtual cost).
   * 
   * @param <T>                  component type
   * @param planitComponentClass to collect of type T
   * @return component, null if not available
   */
  public abstract <T> T getTrafficAssignmentComponent(final Class<T> planitComponentClass);

  /**
   * Verify if the desired traffic assignment component (by its class) is available on the assignment.
   * These are components created and registered on the assignment, specific to the assignment itself, not component
   * inputs available upon creation. Derived assignments might register more components beyond the standard ones
   * registered on the base class (gapfunction, smoothing, physical, virtual cost).
   *
   * @param <T>                  component type
   * @param planitComponentClass to collect of type T
   * @return true if found, false otherwise
   */
  public abstract <T> boolean hasTrafficAssignmentComponent(final Class<T> planitComponentClass);
}
