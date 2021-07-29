package org.planit.service.routed;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntitiesImpl;

/**
 * Implementation of the RoutedServicesTrips interface.
 * 
 * @author markr
 */
public class RoutedServiceTripsImpl extends ManagedIdEntitiesImpl<RoutedServiceTripInfo> implements RoutedServiceTrips {

  /** factory for this container class */
  protected final RoutedServiceTripInfoFactory factory;

  /**
   * Constructor
   */
  protected RoutedServiceTripsImpl(final IdGroupingToken tokenId) {
    super(RoutedServiceTripInfo::getId, RoutedServiceTripInfo.ROUTED_SERVICE_TRIP_INFO_ID_CLASS);
    this.factory = new RoutedServiceTripInfoFactory(tokenId, this);
  }

  /**
   * Copy constructor
   * 
   * @param routedServiceTripsImpl to copy
   */
  public RoutedServiceTripsImpl(RoutedServiceTripsImpl routedServiceTripsImpl) {
    super(routedServiceTripsImpl);
    this.factory = routedServiceTripsImpl.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServiceTripsImpl clone() {
    return new RoutedServiceTripsImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServiceTripInfoFactory getFactory() {
    return factory;
  }

}
