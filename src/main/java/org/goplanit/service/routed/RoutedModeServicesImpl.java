package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.mode.Mode;

/**
 * Implementation of the RoutedModeServices interface
 * 
 * @author markr
 */
public class RoutedModeServicesImpl extends ManagedIdEntitiesImpl<RoutedService> implements RoutedModeServices {

  /** mode all services adhere to */
  private final Mode supportedMode;

  /** factory to use to create routed service instances */
  private final RoutedServiceFactory factory;

  /**
   * Constructor
   * 
   * @param tokenId       to use for id generation
   * @param supportedMode mode all routed services adhere to
   */
  public RoutedModeServicesImpl(final IdGroupingToken tokenId, final Mode supportedMode) {
    super(RoutedService::getId, RoutedService.ROUTED_SERVICE_ID_CLASS);
    this.supportedMode = supportedMode;
    this.factory = new RoutedServiceFactory(tokenId, this);
  }

  /**
   * Copy constructor
   * 
   * @param routedModeServicesImpl to copy
   */
  public RoutedModeServicesImpl(RoutedModeServicesImpl routedModeServicesImpl) {
    super(routedModeServicesImpl);
    this.supportedMode = routedModeServicesImpl.supportedMode;
    this.factory = new RoutedServiceFactory(routedModeServicesImpl.factory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedModeServicesImpl clone() {
    return new RoutedModeServicesImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServiceFactory getFactory() {
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
