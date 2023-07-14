package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.service.routed.RoutedTripDeparture;
import org.goplanit.utils.service.routed.RoutedTripFrequency;
import org.goplanit.utils.service.routed.RoutedTripSchedule;
import org.goplanit.utils.service.routed.RoutedTripsSchedule;

import java.util.function.BiConsumer;

/**
 * Implementation of the RoutedTripsSchedule interface.
 * 
 * @author markr
 */
public class RoutedTripsScheduleImpl extends RoutedTripsImpl<RoutedTripSchedule> implements RoutedTripsSchedule {

  /**
   * Constructor
   * 
   * @param tokenId to use
   */
  protected RoutedTripsScheduleImpl(final IdGroupingToken tokenId) {
    super(tokenId);
    setFactory(new RoutedTripScheduleFactoryImpl(tokenId, this));
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to use for tracking mapping between original and copied entity (may be null)
   */
  public RoutedTripsScheduleImpl(RoutedTripsScheduleImpl other, boolean deepCopy, BiConsumer<RoutedTripSchedule, RoutedTripSchedule> mapper) {
    super(other, deepCopy, mapper);
    setFactory(new RoutedTripScheduleFactoryImpl(other.getFactory().getIdGroupingToken(), this));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsScheduleImpl shallowClone() {
    return new RoutedTripsScheduleImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsScheduleImpl deepClone() {
    return new RoutedTripsScheduleImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsScheduleImpl deepCloneWithMapping(BiConsumer<RoutedTripSchedule, RoutedTripSchedule> mapper) {
    return new RoutedTripsScheduleImpl(this, true, mapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripScheduleFactoryImpl getFactory() {
    return (RoutedTripScheduleFactoryImpl) super.getFactory();
  }

}
