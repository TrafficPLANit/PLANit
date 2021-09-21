package org.planit.service.routed;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;

/**
 * Factory for creating routed service instances on underlying container
 * 
 * @author markr
 */
public class RoutedServiceFactory extends ManagedIdEntityFactoryImpl<RoutedService> {

  /** container to use */
  protected final RoutedModeServices routedModeServices;

  /**
   * Create a newly created instance without registering on the container
   * 
   * @return created routed service
   */
  protected RoutedServiceImpl createNew() {
    return new RoutedServiceImpl(getIdGroupingToken());
  }

  /**
   * Constructor
   * 
   * @param tokenId            to use
   * @param routedModeServices to use
   */
  protected RoutedServiceFactory(final IdGroupingToken tokenId, final RoutedModeServices routedModeServices) {
    super(tokenId);
    this.routedModeServices = routedModeServices;
  }

  /**
   * Register a newly created instance on the underlying container
   * 
   * @return created instance
   */
  public RoutedService registerNew() {
    RoutedService newRoutedService = createNew();
    routedModeServices.register(newRoutedService);
    return newRoutedService;
  }

}
