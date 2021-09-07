package org.planit.interactor;

/**
 * Accessee that can be accessed to obtain access to one or more traffic assignment components
 * 
 * @author markr
 *
 */
public interface TrafficAssignmentComponentAccessee extends InteractorAccessee {

  /**
   * Collect the desired traffic assignment component by its class assuming it is available on the assignment. These are traffic assignment components that are created and
   * registered upon the assignment, so not component inputs that are readily available upon creation, but components specific to the assignment itself. Derived assignments might
   * also register additional components as well beyond the standard components registered here on the base class (gapfunction, smoothing, physical, virtual cost).
   * 
   * @param <T>                  component type
   * @param planitComponentClass to collect of type T
   * @return component, null if not available
   */
  public abstract <T> T getTrafficAssignmentComponent(final Class<T> planitComponentClass);
}
