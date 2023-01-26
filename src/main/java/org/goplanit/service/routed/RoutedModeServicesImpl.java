package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.service.routed.RoutedModeServices;
import org.goplanit.utils.service.routed.RoutedService;
import org.goplanit.utils.service.routed.RoutedServiceFactory;

/**
 * Implementation of the RoutedModeServices interface
 * 
 * @author markr
 */
public class RoutedModeServicesImpl extends ManagedIdEntitiesImpl<RoutedService> implements RoutedModeServices {

  /** mode all services adhere to */
  private final Mode supportedMode;

  /** factory to use to create routed service instances */
  private final RoutedServiceFactoryImpl factory;

  /**
   * Constructor
   * 
   * @param tokenId       to use for id generation
   * @param supportedMode mode all routed services adhere to
   */
  public RoutedModeServicesImpl(final IdGroupingToken tokenId, final Mode supportedMode) {
    super(RoutedService::getId, RoutedService.ROUTED_SERVICE_ID_CLASS);
    this.supportedMode = supportedMode;
    this.factory = new RoutedServiceFactoryImpl(tokenId, this);
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param routedModeServicesImpl to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public RoutedModeServicesImpl(RoutedModeServicesImpl routedModeServicesImpl, boolean deepCopy) {
    super(routedModeServicesImpl, deepCopy);
    this.supportedMode = routedModeServicesImpl.supportedMode;
    this.factory =
            new RoutedServiceFactoryImpl(routedModeServicesImpl.factory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedModeServicesImpl clone() {
    return new RoutedModeServicesImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedModeServicesImpl deepClone() {
    return new RoutedModeServicesImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServiceFactoryImpl getFactory() {
    return factory;
  }

  /**
   * The supported mode for the routed services registered
   * 
   * @return supported mode
   */
  public final Mode getMode() {
    return supportedMode;
  }

}
