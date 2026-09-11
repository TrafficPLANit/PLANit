package org.goplanit.demands.discrete.tour;

import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.LocalTimeUtils;

import javax.annotation.Nonnull;
import java.time.LocalTime;
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
   * Shift every time in this schedule, including those of any nested schedules, by the given number of seconds
   *
   * @param offsetSeconds offset to apply, may be negative
   */
  public void shift(int offsetSeconds) {
    shift(offsetSeconds, new HashSet<>());
  }

  /**
   * Shift every time in this schedule by the given number of seconds, skipping tours that were already shifted. A
   * tour may be shared by several participants and therefore be reachable from more than one schedule, while its
   * times must only move once
   *
   * @param offsetSeconds offset to apply, may be negative
   * @param shiftedTours tours shifted so far
   */
  private void shift(int offsetSeconds, Set<Tour> shiftedTours) {
    for (ScheduleElement element : scheduleElements) {

      if (element instanceof Trip) {
        var trip = (Trip) element;
        if (trip.getStartTime() != null) {
          trip.setStartTime(trip.getStartTime().plusSeconds(offsetSeconds));
        }
        continue;
      }
      if (!(element instanceof ParticipantTour)) {
        throw new PlanItRunTimeException("Unsupported schedule element type (%s) encountered when shifting schedule",
            element.getClass().getCanonicalName());
      }

      var participation = (ParticipantTour) element;
      if (!participation.isPrimary()) {
        continue; // a shared tour moves with its primary participant, not again with each accompanying one
      }

      var tour = participation.getTour();
      if (!shiftedTours.add(tour)) {
        continue; // already shifted via another participant of the same tour
      }

      if (tour.getStartTime() != null) {
        tour.setStartTime(tour.getStartTime().plusSeconds(offsetSeconds));
      }
      if (tour.getEndTime() != null) {
        tour.setEndTime(tour.getEndTime().plusSeconds(offsetSeconds));
      }
      if (tour.hasSchedule()) {
        tour.getSchedule().shift(offsetSeconds, shiftedTours);
      }
    }
  }

  /**
   * Set each tour's start time to the start time of the first element it contains, deepest tours first so that a
   * nested tour's own start is settled before its parent reads it. A tour starts precisely when its first trip
   * departs, the two being one and the same event, so the tour follows the trip rather than carrying a time of its
   * own.
   */
  public void syncTourStartTimesToFirstElement() {
    syncTourStartTimesToFirstElement(new HashSet<>());
  }

  /**
   * Sync tour start times as per {@link #syncTourStartTimesToFirstElement()}, skipping tours already synced because
   * they are shared by more than one participant
   *
   * @param syncedTours tours synced so far
   */
  private void syncTourStartTimesToFirstElement(Set<Tour> syncedTours) {
    for (ScheduleElement element : scheduleElements) {
      if (element instanceof Trip) {
        continue; // a trip carries no tour start to sync
      }
      if (!(element instanceof ParticipantTour)) {
        throw new PlanItRunTimeException(
            "Unsupported schedule element type (%s) encountered when syncing tour start times",
            element.getClass().getCanonicalName());
      }

      var tour = ((ParticipantTour) element).getTour();
      if (!tour.hasSchedule() || tour.getSchedule().isEmpty() || !syncedTours.add(tour)) {
        continue;
      }

      tour.getSchedule().syncTourStartTimesToFirstElement(syncedTours);

      var firstNestedStartTime = tour.getSchedule().getFirst().getStartTime();
      if (firstNestedStartTime != null) {
        tour.setStartTime(firstNestedStartTime);
      }
    }
  }

  /**
   * Verify the schedule is in chronological order, i.e. no element starts before its predecessor and no tour ends
   * before the elements it contains, across all nested levels.
   *
   * @param dayAnchorTime time of day the schedule's day is cut at, so that a schedule running past midnight is
   *                      recognised as such rather than as being out of order. When null no crossing is assumed
   *                      and times are compared as they are
   * @return true when in chronological order, false otherwise
   */
  public boolean isChronological(LocalTime dayAnchorTime) {
    return lastElapsedSecondsIfChronological(dayAnchorTime, 0) >= 0;
  }

  /**
   * Walk this level's times as start, nested schedule, tour end, carrying the elapsed seconds of the most recent
   * time across levels
   *
   * @param dayAnchorTime time of day the schedule's day is cut at, may be null
   * @param previousElapsedSeconds elapsed seconds of the most recent time seen so far
   * @return elapsed seconds of the last time seen, or -1 when the order is breached
   */
  private long lastElapsedSecondsIfChronological(LocalTime dayAnchorTime, long previousElapsedSeconds) {
    for (ScheduleElement element : scheduleElements) {

      /* verify before use, so an unsupported element is rejected rather than silently deciding the outcome */
      if (!(element instanceof ParticipantTour) && !(element instanceof Trip)) {
        throw new PlanItRunTimeException(
            "Unsupported schedule element type (%s) encountered when verifying chronological order",
            element.getClass().getCanonicalName());
      }

      previousElapsedSeconds = LocalTimeUtils.secondsFromWrapAroundDayAnchorIfNotBefore(
          dayAnchorTime, element.getStartTime(), previousElapsedSeconds);
      if (previousElapsedSeconds < 0) {
        return -1;
      }

      if (element.hasSchedule() && element.getSchedule() != null) {
        previousElapsedSeconds =
            element.getSchedule().lastElapsedSecondsIfChronological(dayAnchorTime, previousElapsedSeconds);
        if (previousElapsedSeconds < 0) {
          return -1;
        }
      }

      if (element instanceof ParticipantTour) {
        previousElapsedSeconds = LocalTimeUtils.secondsFromWrapAroundDayAnchorIfNotBefore(
            dayAnchorTime, ((ParticipantTour) element).getEndTime(), previousElapsedSeconds);
        if (previousElapsedSeconds < 0) {
          return -1;
        }
      }
    }
    return previousElapsedSeconds;
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
