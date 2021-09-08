package org.planit.interactor;

import java.util.logging.Logger;

/**
 * Interactor accessor. The accessor accesses its accessee.
 * 
 * @author markr
 *
 */
public interface InteractorAccessor<T extends InteractorAccessee> {

  /** logger to use */
  public static Logger LOGGER = Logger.getLogger(InteractorAccessor.class.getCanonicalName());

  /**
   * each interactor requires access from this accessee
   * 
   * @return class that accessor requires
   */
  public abstract Class<T> getCompatibleAccessee();

  /**
   * Set the accessee to allow access
   * 
   * @param accessee to use
   */
  public abstract void setAccessee(final T accessee);

  /**
   * Set the accessee to allow access
   * 
   * @param accessee to use
   */
  public default void setAccessee(final Object accessee) {
    try {
      setAccessee((T) getCompatibleAccessee().cast(accessee));
    } catch (ClassCastException e) {
      LOGGER.warning(String.format("IGNORED: Provided Interactor accessee %s is not compatible with this accessor %s", accessee.getClass().getCanonicalName(),
          this.getClass().getCanonicalName()));
    }
  }

}
