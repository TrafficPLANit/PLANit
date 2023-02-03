package org.goplanit.service.routed;

import java.time.LocalTime;
import java.util.*;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.goplanit.utils.service.routed.RelativeLegTiming;
import org.goplanit.utils.service.routed.RoutedTripDepartures;
import org.goplanit.utils.service.routed.RoutedTripSchedule;

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
    this.relativeLegTimings = new ArrayList<>(1);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public RoutedTripScheduleImpl(RoutedTripScheduleImpl other, boolean deepCopy) {
    super(other, deepCopy);

    // container wrapper requires clone always
    this.departures = deepCopy ? other.departures.deepClone() : other.departures.shallowClone();

    this.relativeLegTimings = new ArrayList<>(other.getRelativeLegTimingsSize());
    other.relativeLegTimings.forEach(lt ->
            relativeLegTimings.add(
                    deepCopy ? new RelativeLegTimingImpl((RelativeLegTimingImpl) lt) : lt));
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
  public void resetChildManagedIdEntities() {
    super.resetChildManagedIdEntities();
    this.departures.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripScheduleImpl shallowClone() {
    return new RoutedTripScheduleImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripScheduleImpl deepClone() {
    return new RoutedTripScheduleImpl(this, true);
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
  public void clearDepartures() {
    departures.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RelativeLegTiming addRelativeLegSegmentTiming(ServiceLegSegment parentLegSegment, LocalTime duration, LocalTime dwellTime) {
    var newEntry = new RelativeLegTimingImpl(parentLegSegment, duration, dwellTime);
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
   * {@inheritDoc}
   */
  @Override
  public void removeLegTiming(int legTimingIndex) {
    relativeLegTimings.remove(legTimingIndex);
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

  /**
   * Iterate over currently available relative leg timings
   * @return iterator of relative leg timings
   */
  @Override
  public Iterator<RelativeLegTiming> iterator() {
    return this.relativeLegTimings.iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ServiceNode> getUsedServiceNodes() {
    Set<ServiceNode> usedServiceNodes = new HashSet<>();
    for(var relLegTiming : this){
      usedServiceNodes.add(relLegTiming.getParentLegSegment().getUpstreamServiceNode());
    }
    usedServiceNodes.add(getRelativeLegTiming(getRelativeLegTimingsSize()-1).getParentLegSegment().getDownstreamServiceNode());
    return usedServiceNodes;
  }
}
