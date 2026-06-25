package org.goplanit.demands.discrete.tour;

import org.goplanit.demands.discrete.trip.Trip;

import java.util.*;

/**
 * A tour schedule can be nested since each element may have a schedule by itself. Each element can be of a different
 * type, but what type is not specified strictly to allow for a simple interface. In practice though it is always
 * a trip or a tour instance of which only tours may have another internal schedule of their own.
 */
public class TourSchedule extends AbstractCollection<TourScheduleElement> {

  private final List<TourScheduleElement> scheduleElements;

  /**
   * Default constructor
   */
  public TourSchedule(){
    this.scheduleElements = new ArrayList<>(2);
  }

  /**
   * Copy constructor
   *
   * @param other other
   * @param deepCopy clone flag
   */
  public TourSchedule(TourSchedule other, boolean deepCopy){
    // we do not own the elements as they are managed entities, so just always do a shallow copy
    this.scheduleElements = new ArrayList<>(other.scheduleElements);
  }


  @Override
  public boolean add(TourScheduleElement element) {
    return scheduleElements.add(element);
  }

  /**
  * access to specific element
  */
  public TourScheduleElement get(int index) {
    return scheduleElements.get(index);
  }

  /**
   * set specific element
   */
  public TourScheduleElement set(int index, TourScheduleElement element) {
    return scheduleElements.set(index, element);
  }

  @Override
  public Iterator<TourScheduleElement> iterator() {
    return scheduleElements.iterator();
  }

  @Override
  public int size() {
    return scheduleElements.size();
  }

  /**
   * Shallow clone
   * @return cloned schedule
   */
  public TourSchedule shallowClone(){
    return new TourSchedule(this, false);
  }

  /**
   * deep clone
   * @return cloned schedule
   */
  public TourSchedule deepClone(){
    return new TourSchedule(this, true);
  }

}
