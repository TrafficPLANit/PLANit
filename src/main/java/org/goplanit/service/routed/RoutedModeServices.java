package org.goplanit.service.routed;

import org.goplanit.utils.id.ManagedIdEntities;
import org.goplanit.utils.mode.Mode;

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
