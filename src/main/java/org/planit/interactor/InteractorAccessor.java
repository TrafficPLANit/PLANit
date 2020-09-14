package org.planit.interactor;

/**
 * Interactor accessor. The accessor accesses its accessee.
 * 
 * @author markr
 *
 */
public interface InteractorAccessor {

  /**
   * each interactor requires access from this accessee 
   * 
   * @return class that accessor requires
   */
  Class<? extends InteractorAccessee> getCompatibleAccessee();

}
