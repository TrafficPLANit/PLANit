package org.planit.service.routed;

import org.planit.utils.id.ExternalIdAble;
import org.planit.utils.id.ManagedId;

/**
 * Interface to reflect one or more similar routed service trips by providing information on their route legs and schedule/frequencies.
 * 
 * @author markr
 *
 */
public interface RoutedServiceTripInfo extends ManagedId, ExternalIdAble {

  /** id class for generating ids */
  public static final Class<RoutedServiceTripInfo> ROUTED_SERVICE_TRIP_INFO_ID_CLASS = RoutedServiceTripInfo.class;

  /**
   * {@inheritDoc}
   */
  @Override
  public default Class<RoutedServiceTripInfo> getIdClass() {
    return ROUTED_SERVICE_TRIP_INFO_ID_CLASS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedServiceTripInfo clone();

}
