package org.planit.service.routed;

import org.planit.utils.id.ContainerisedManagedIdEntityFactory;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedId;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;

/**
 * Factory for creating routed service trip info instances on underlying container
 * 
 * @author markr
 */
public class RoutedServiceTripInfoFactory extends ManagedIdEntityFactoryImpl<RoutedServiceTripInfo> implements ContainerisedManagedIdEntityFactory<RoutedServiceTripInfo> {

  /** container to use */
  protected final RoutedServiceTrips routedServiceTrips;

  /**
   * Create a newly created instance without registering on the container
   * 
   * @return created instance
   */
  protected RoutedServiceTripInfoImpl createNew() {
    return new RoutedServiceTripInfoImpl(getIdGroupingToken());
  }

  /**
   * Constructor
   * 
   * @param tokenId            to use
   * @param routedServiceTrips to use
   */
  protected RoutedServiceTripInfoFactory(final IdGroupingToken tokenId, final RoutedServiceTrips routedServiceTrips) {
    super(tokenId);
    this.routedServiceTrips = routedServiceTrips;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServiceTripInfo registerUniqueCopyOf(ManagedId routedServiceTripInfo) {
    RoutedServiceTripInfo copy = createUniqueCopyOf(routedServiceTripInfo);
    routedServiceTrips.register(copy);
    return copy;
  }

  /**
   * Register a newly created instance on the underlying container
   * 
   * @return created instance
   */
  public RoutedServiceTripInfo registerNew() {
    RoutedServiceTripInfoImpl newRoutedServiceTripInfo = createNew();
    routedServiceTrips.register(newRoutedServiceTripInfo);
    return newRoutedServiceTripInfo;
  }

}
