package org.planit.interactor;

/**
 * Interactor accessee. Each accessee is expected to allow access to a resource required by the accessor
 * 
 * 
 * @author markr
 *
 */
public interface InteractorAccessee {

  /**
   * this accessee provides access to this accessor
   * 
   * @return class that accessee provides to
   */
  Class<? extends InteractorAccessor<?>> getCompatibleAccessor();

}
