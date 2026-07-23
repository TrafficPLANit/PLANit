package org.goplanit.demands.discrete.tour;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A schedule can be nested since each element may have a schedule by itself. Each element can be of a different
 * type, but what type is not specified strictly to allow for a simple interface. In practice though it is always
 * a trip or a tour instance of which only tours may have another internal schedule of their own.
 */
public class ActivitySchedule extends AbstractCollection<ScheduleElement> {


  private final List<ScheduleElement> scheduleElements;

  /**
   * Default constructor
   *
   */
  public ActivitySchedule(){
    this.scheduleElements = new ArrayList<>(2);
  }

  /**
   * Copy constructor
   *
   * @param other other
   * @param deepCopy deep copy or not flag
   */
  protected ActivitySchedule(ActivitySchedule other, boolean deepCopy){
    // we do not own the elements as they are managed entities, so just always do a shallow copy
    this.scheduleElements = new ArrayList<>(other.scheduleElements);
  }

  /**
   * Copy constructor with element mapping
   *
   * @param other other
   * @param elementToElementMapping mapping
   */
  protected ActivitySchedule(
      ActivitySchedule other, Function<ScheduleElement, ScheduleElement> elementToElementMapping){
    // we do not own the elements as they are managed entities, so just always do a shallow copy but with mapping
    this.scheduleElements = new ArrayList<>();
    for(var otherElement : other){
      var mappedElement = elementToElementMapping.apply(otherElement);
      if(mappedElement != null) {
        scheduleElements.add(mappedElement);
      }
    }

  }


  /**
   * {@inheritDoc}
   */
  @Override
  public boolean add(ScheduleElement element) {
    return scheduleElements.add(element);
  }

  /**
  * access to specific element
   * @param index to get
  * @return entry
  */
  public ScheduleElement get(int index) {
    if(isEmpty()){
      return null;
    }
    return scheduleElements.get(index);
  }

  /**
   * set specific element
   *
   * @param index index
   * @param element  to set
   * @return the element previously at the specified position
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

  /**
   * {@inheritDoc}
   */
  @Override
  @Nonnull
  public Iterator<ScheduleElement> iterator() {
    return scheduleElements.iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return scheduleElements.size();
  }

  /**
   * get last element, when flattened, we go through nested structure as if it is a flat list and then get
   * the last
   *
   * @param asFlattened when true flatten and obtain last element, otherwise just get last element of this list
   * @return last element found
   */
  public ScheduleElement getLast(boolean asFlattened) {
    var lastElement = scheduleElements.get(scheduleElements.size() - 1);
    if(asFlattened && lastElement.hasSchedule()){
      return  lastElement.getSchedule().getLast(asFlattened);
    }
    return lastElement;
  }

  /**
   * get first element in schedule
   * @return schedule element
   */
  public ScheduleElement getFirst() {
    return get(0);
  }

  /**
   * Shallow clone
   * @return cloned schedule
   */
  public ActivitySchedule shallowClone(){
    return new ActivitySchedule(this, false);
  }

  /**
   * Deep clone
   * @return cloned schedule
   */
  public ActivitySchedule deepClone() {
    return new ActivitySchedule(this, true);
  }

  /**
   * Deep clone with mapping
   * @param elementToElementMapping mapping to apply
   * @return cloned schedule
   */
  public ActivitySchedule deepCloneWithMapping(Function<ScheduleElement, ScheduleElement> elementToElementMapping) {
    return new ActivitySchedule(this, elementToElementMapping);
  }

  /**
   * get last of type
   * @param scheduleElementClass to check
   * @return found, otherwise null
   * @param <T> type of element
   */
  public <T extends ScheduleElement> T getLastOfType(Class<T> scheduleElementClass) {
    T last = null;
    for (ScheduleElement e : this) {
      if (scheduleElementClass.isInstance(e)) {
        last = scheduleElementClass.cast(e);
      }
    }
    return last;
  }


  /**
   * Verify if last of type
   * @param scheduleElement to check
   * @return result
   * @param <T> type of element
   */
  public <T extends ScheduleElement> boolean isLastOfType(T scheduleElement) {
    return scheduleElement.equals(getLastOfType(scheduleElement.getClass()));
  }

  /**
   * Check if any of the schedule elements or the schedules of the schedule elements conform to the predicate
   *
   * @param predicate to apply
   * @return result of any predicate matching
   */
  public boolean testNested(Predicate<ScheduleElement> predicate) {
    return stream().anyMatch( se -> se.testNested(predicate));
  }
}
