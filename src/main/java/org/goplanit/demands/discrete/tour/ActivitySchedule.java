package org.goplanit.demands.discrete.tour;

import org.goplanit.utils.mode.Mode;

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
   * Sort all elements using a custom comparator. When an element has
   * a nested schedule, the nested schedule gets sorted recursively using the same comparator.
   *
   * @param comparator comparator to order schedule elements across all levels
   */
  public void sortNested(Comparator<ScheduleElement> comparator) {
    if (this.scheduleElements == null || this.scheduleElements.isEmpty()) {
      return;
    }

    // Sort current level
    if (comparator != null) {
      this.scheduleElements.sort(comparator);
    }

    // Recurse into nested schedules
    for (var element : this.scheduleElements) {
      if (element.hasSchedule() && element.getSchedule() != null) {
        element.getSchedule().sortNested(comparator);
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
   * Get the total unrolled count of elements when we consider nested elements
   *
   * @param countLeafsOnly when true only count elements that are not containing nested elements
   * @return total count
   */
  public int sizeUnrolled(boolean countLeafsOnly) {
    int unrolledCount = 0;
    for (ScheduleElement element : this.scheduleElements) {
      if (element.hasSchedule() && element.getSchedule() != null) {
        unrolledCount += element.getSchedule().sizeUnrolled(countLeafsOnly); // Recursively count children
        if(!countLeafsOnly){
          unrolledCount++; // Count the current element which is not a leaf (Tour)
        }
      }else{
        unrolledCount++; // count leaf
      }
    }
    return unrolledCount;
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

  /**
   * Find the outbound mode by traversing schedule
   *
   * @return returns the first outbound mode of any element on its (nested) schedule
   */
  public Mode getOutboundMode(){
    if(getFirst() == null){
      return null;
    }
    if(getFirst().hasSchedule()){
      return getFirst().getSchedule().getOutboundMode();
    }
    return getFirst().getOutboundMode();
  }

  /**
   * Find the inbound mode by traversing schedule
   *
   * @return returns the last inbound mode of any element on its (nested) schedule
   */
  public Mode getInboundMode(){
    if(getLast(false) == null){
      return null;
    }
    if(getLast(false).hasSchedule()){
      return getLast(false).getSchedule().getInboundMode();
    }
    return getLast(false).getInboundMode();
  }
}
