package org.goplanit.service.routed;

import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.ManagedId;

/**
 * Base interface for routed trips. Derived interfaces and classes are to be used to define what type of routed trip, e.g., a routed trip with a schedule or frequency. However,
 * despite different types of routed trips they all fall under the same base class and share a unique id across, hence the existence of this interface with its id class.
 * 
 * @author markr
 */
public interface RoutedTrip extends ExternalIdAble, ManagedId {

  /** id class for generating ids */
  public static final Class<RoutedTrip> ROUTED_TRIP_ID_CLASS = RoutedTrip.class;

  /**
   * {@inheritDoc}
   */
  @Override
  public default Class<RoutedTrip> getIdClass() {
    return ROUTED_TRIP_ID_CLASS;
  }

}
