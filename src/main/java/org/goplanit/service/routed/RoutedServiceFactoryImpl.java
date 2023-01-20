package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.service.routed.RoutedModeServices;
import org.goplanit.utils.service.routed.RoutedService;
import org.goplanit.utils.service.routed.RoutedServiceFactory;

/**
 * Factory for creating routed service instances on underlying container
 * 
 * @author markr
 */
public class RoutedServiceFactoryImpl extends ManagedIdEntityFactoryImpl<RoutedService> implements RoutedServiceFactory {

  /** container to use */
  protected final RoutedModeServices routedModeServices;

  /**
   * Create a newly created instance without registering on the container
   * 
   * @return created routed service
   */
  protected RoutedServiceImpl createNew() {
    return new RoutedServiceImpl(getIdGroupingToken(), routedModeServices.getMode());
  }

  /**
   * Constructor
   * 
   * @param tokenId            to use
   * @param routedModeServices to use
   */
  protected RoutedServiceFactoryImpl(final IdGroupingToken tokenId, final RoutedModeServices routedModeServices) {
    super(tokenId);
    this.routedModeServices = routedModeServices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedService registerNew() {
    RoutedService newRoutedService = createNew();
    routedModeServices.register(newRoutedService);
    return newRoutedService;
  }

}
