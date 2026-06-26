package org.goplanit.demands.discrete.tour;

import org.goplanit.demands.discrete.trip.Trip;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;

/**
 * A schedule can be nested since each element may have a schedule by itself. Each element can be of a different
 * type, but what type is not specified strictly to allow for a simple interface. In practice though it is always
 * a trip or a tour instance of which only tours may have another internal schedule of their own.
 */
public class Schedule extends AbstractCollection<ScheduleElement> {

  private final List<ScheduleElement> scheduleElements;

  /**
   * Default constructor
   */
  public Schedule(){
    this.scheduleElements = new ArrayList<>(2);
  }

  /**
   * Copy constructor
   *
   * @param other other
   */
  protected Schedule(Schedule other, boolean deepCopy){
    // we do not own the elements as they are managed entities, so just always do a shallow copy
    this.scheduleElements = new ArrayList<>(other.scheduleElements);
  }

  /**
   * Copy constructor with element mapping
   *
   * @param other other
   * @param elementToElementMapping mapping
   */
  protected Schedule(Schedule other, Function<ScheduleElement, ScheduleElement> elementToElementMapping){
    // we do not own the elements as they are managed entities, so just always do a shallow copy but with mapping
    this.scheduleElements = new ArrayList<>();
    for(var otherElement : other){
      var mappedElement = elementToElementMapping.apply(otherElement);
      if(mappedElement != null) {
        scheduleElements.add(mappedElement);
      }
    }

  }


  @Override
  public boolean add(ScheduleElement element) {
    return scheduleElements.add(element);
  }

  /**
  * access to specific element
  */
  public ScheduleElement get(int index) {
    return scheduleElements.get(index);
  }

  /**
   * set specific element
   */
  public ScheduleElement set(int index, ScheduleElement element) {
    return scheduleElements.set(index, element);
  }

  /**
   * Sort all elements chronologically by their start times. Any null elements come last
   */
  public void sort() {
    this.scheduleElements.sort(
        Comparator.comparing(ScheduleElement::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
    );
  }

  /**
   * Sort all elements chronologically by their start times. Any null elements come last. When a schedule has
   * a nested schedule, also the nested schedule gets sorted
   */
  public void sortNested() {
    sort();
    for(var entry : this){
      if(entry.hasSchedule()){
        entry.getSchedule().sortNested();
      }
    }
  }

  @Override
  @Nonnull
  public Iterator<ScheduleElement> iterator() {
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
  public Schedule shallowClone(){
    return new Schedule(this, false);
  }

  /**
   * Deep clone
   * @return cloned schedule
   */
  public Schedule deepClone() {
    return new Schedule(this, true);
  }

  /**
   * Deep clone with mapping
   * @return cloned schedule
   */
  public Schedule deepCloneWithMapping(Function<ScheduleElement, ScheduleElement> elementToElementMapping) {
    return new Schedule(this, elementToElementMapping);
  }
}
