package org.planit.service.routed;

import org.planit.utils.id.ManagedIdEntities;
import org.planit.utils.mode.Mode;

/**
 * Interface for wrapper container class around RoutedModeServices for a particular mode. This container is used to store instances of a routed service for a given mode
 * 
 * @author markr
 *
 */
public interface RoutedModeServices extends ManagedIdEntities<RoutedService> {

  /**
   * The supported mode for the routed services registered
   * 
   * @return supported mode
   */
  public abstract Mode getMode();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedModeServices clone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedServiceFactory getFactory();

}
