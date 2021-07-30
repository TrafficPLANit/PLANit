package org.planit.service.routed;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.service.ServiceLeg;

/**
 * Implementation of a RoutedTripSchedule interface.
 * 
 * @author markr
 */
public class RoutedTripScheduleImpl extends RoutedTripImpl implements RoutedTripSchedule {

  /** departures of this schedule */
  private final RoutedTripDepartures departures;

  /** track the relative timings of the legs, which, in combination with the departures, can be used to construct a full schedule */
  private final List<RelativeLegTiming> relativeLegTimings;

  /** default dwell time, only used to reduce size of persisted XMLs, so only present on implementation */
  private LocalTime defaultDwellTime;

  /**
   * Constructor
   * 
   * @param tokenId to use for id generation
   */
  public RoutedTripScheduleImpl(final IdGroupingToken tokenId) {
    super(tokenId);
    this.departures = new RoutedTripDeparturesImpl(tokenId);
    this.relativeLegTimings = new ArrayList<RelativeLegTiming>(1);
  }

  /**
   * Copy constructor
   * 
   * @param routedTripScheduleImpl to copy
   */
  public RoutedTripScheduleImpl(RoutedTripScheduleImpl routedTripScheduleImpl) {
    super(routedTripScheduleImpl);
    this.departures = routedTripScheduleImpl.departures.clone();
    this.relativeLegTimings = new ArrayList<RelativeLegTiming>(routedTripScheduleImpl.relativeLegTimings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripScheduleImpl clone() {
    return new RoutedTripScheduleImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripDepartures getDepartures() {
    return departures;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clearRelativeLegTimings() {
    relativeLegTimings.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RelativeLegTiming addRelativeLegTiming(ServiceLeg parentLeg, LocalTime duration, LocalTime dwellTime) {
    RelativeLegTiming newEntry = new RelativeLegTiming(parentLeg, duration, dwellTime);
    relativeLegTimings.add(newEntry);
    return newEntry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RelativeLegTiming getRelativeLegTiming(int index) {
    return relativeLegTimings.get(index);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getRelativeLegTimingsSize() {
    return relativeLegTimings.size();
  }

  /**
   * Get default
   * 
   * @return default dwell time
   */
  public LocalTime getDefaultDwellTime() {
    return defaultDwellTime;
  }

  /**
   * Set default
   * 
   * @param defaultDwellTime to use
   */
  public void setDefaultDwellTime(LocalTime defaultDwellTime) {
    this.defaultDwellTime = defaultDwellTime;
  }

}
