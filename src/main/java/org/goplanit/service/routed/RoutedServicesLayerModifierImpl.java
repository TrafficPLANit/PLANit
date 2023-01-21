package org.goplanit.service.routed;

import org.goplanit.utils.service.routed.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation of {@link RoutedServicesLayerModifier}
 */
public class RoutedServicesLayerModifierImpl implements RoutedServicesLayerModifier {

  protected final RoutedServicesLayer routedServicesLayer;

  /**
   * Truncate routed trip(s) - frequency based -  at hand to the remaining service network (which likely has undergone changes)
   * <p>
   * Ids are not recreated in this call, only truncation is performed
   * </p>
   *
   * @param routedTripFrequency a routed trip(s) group for a given frequency to truncate to underlying service network
   */
  private void truncateFrequencyTripToServiceNetwork(RoutedTripFrequency routedTripFrequency) {
    //todo
  }

  // todo javadoc
  private List<RoutedTripSchedule> truncateScheduledTripChainToServiceNetwork(
          int indexOffset, final List<Integer> toBeRemovedRelativeTimingLegSegments, final RoutedTripSchedule routedTripSchedule, RoutedTripScheduleFactory factory) {

    /* determine departure times for each chain of consecutive valid leg segments taking into account:
     * last valid chain final leg segment arrival time
     * gap of invalid chain travel time (duration + dwell times)
     * creating new schedules for the trips not departing in the very first chain (which we can reuse) */

    /* consider all to be removed entries leading up to the next valid chain until */
    List<Integer> consecutiveLegsToRemove = toBeRemovedRelativeTimingLegSegments.stream().takeWhile(
            myInt -> toBeRemovedRelativeTimingLegSegments.indexOf(myInt) + indexOffset == myInt).collect(Collectors.toList());

    /* determine to where the departure is to be offset to */
    int firstValidLegInChainIndex = consecutiveLegsToRemove.get(consecutiveLegsToRemove.size()-1)+1;

    /* create a copy of the original to adjust */
    var truncatedRoutedTripSchedule = factory.createUniqueCopyOf(routedTripSchedule);

    /*... then , get the durations and dwell times leading up to the valid chain of this portion of the entry and sum them
     * to serve as the departure time offset in addition to the original departure time */
    long departureShiftInNanos = IntStream.range(0,firstValidLegInChainIndex).mapToLong(ltIndex ->
            routedTripSchedule.getRelativeLegTiming(ltIndex).getDuration().toNanoOfDay()
                    + routedTripSchedule.getRelativeLegTiming(ltIndex).getDwellTime().toNanoOfDay()).sum();

    /* update the departure times */
    truncatedRoutedTripSchedule.getDepartures().allDepartLaterBy(LocalTime.ofNanoOfDay(departureShiftInNanos));

    /* remove all leg timings except the ones belonging to this chain */
    truncatedRoutedTripSchedule.removeLegTimingsIn(
            null /*todo: all leg timings BEFORE first leg of this valid chain AND all leg timings after last leg of this valid chain */);

    /* call next recursion */
    //todo check if we can call next recursion. IF there is no more NEXT then we are done...
    var recursiveResult = truncateScheduledTripChainToServiceNetwork(
            -1,  /* TODO update offset to first next entry that is not consecutive AFTER current offset index */
            toBeRemovedRelativeTimingLegSegments,
            routedTripSchedule,
            factory
            );
    if(recursiveResult == null){
      recursiveResult = new ArrayList<>(1);
    }
    recursiveResult.add(truncatedRoutedTripSchedule);
    return recursiveResult;
  }

  /**
   * Truncate routed trip(s) - schedule based -  at hand to the remaining service network (which likely has undergone changes). This means
   * removing parts of a trip schedule that is no longer present or valid, or even splitting into multiple trip schedules in case gaps within the same entry
   * are detected
   * <p>
   * Ids are not recreated in this call, only truncation is performed
   * </p>
   * <p>
   * It is possible the routed trip schedule ends up without any departures or leg timings in which case the invoker is expected to
   * cull the schedule as it is no longer viable.
   * </p>
   *
   * @param routedTripSchedule a routed trip(s) group for a given schedule to truncate to underlying service network
   * @param factory to use when we need to split a routedTripSchedule in multiple entries due to possible gaps that are identified
   */
  private void truncateScheduledTripToServiceNetwork(final RoutedTripSchedule routedTripSchedule, RoutedTripScheduleFactory factory) {

    /* identify the rel timing leg segments */
    List<Integer> toBeRemovedRelativeTimingLegSegments = RelativeLegTimingUtils.findLegTimingsNotMappedToServiceNetwork(routedTripSchedule, this.routedServicesLayer);

    /* if none to be removed, end invocation here */
    if(toBeRemovedRelativeTimingLegSegments.isEmpty()){
      return;
    }

    /* if all to be removed, clear the schedule entirely */
    if(routedTripSchedule.getRelativeLegTimingsSize() == toBeRemovedRelativeTimingLegSegments.size()){
      routedTripSchedule.clear();
      return;
    }


    /* replace existing routed trip schedule by one or more truncated ones, one per identified consecutive chain with adjusted departure times */
    List<RoutedTripSchedule> truncatedRoutedTripSchedules = truncateScheduledTripChainToServiceNetwork(
            0, toBeRemovedRelativeTimingLegSegments, routedTripSchedule, factory);

    // process these routed trip schedules
    //todo: clear existing one if we have new ones so it is deleted 

  }

  /**
   * Truncate routed service at hand to the remaining service network (which likely has undergone changes)
   * <p>
   *   Ids are not recreated in this call, only truncation is performed
   * </p>
   * @param routedService routed service to truncate to underlying service network
   */
  private void truncateRoutedServiceToServiceNetwork(RoutedService routedService) {
    /* schedule based */
    var scheduledTrips = routedService.getTripInfo().getScheduleBasedTrips();
    var scheduleIterator = scheduledTrips.iterator();
    while(scheduleIterator.hasNext()){
      var currEntry = scheduleIterator.next();
      truncateScheduledTripToServiceNetwork(currEntry, scheduledTrips.getFactory());
      if(currEntry.getDepartures().isEmpty() || !currEntry.hasRelativeLegTimings()){
        scheduleIterator.remove();
      }
    }

    /* frequency based */
    var frequencyTrips = routedService.getTripInfo().getFrequencyBasedTrips();
    var frequencyIterator = frequencyTrips.iterator();
    while(frequencyIterator.hasNext()){
      var currEntry = frequencyIterator.next();
      truncateFrequencyTripToServiceNetwork(currEntry);
      if(!currEntry.hasLegSegments() || !currEntry.hasPositiveFrequency()){
        frequencyIterator.remove();
      }
    }
  }

  /**
   * Truncate all routed services for a mode and match them to the remaining service network (which likely has undergone changes), i.e.,
   * remove all service network entities that are now missing.
   * <p>
   *   Ids are not recreated in this call, only truncation is performed
   * </p>
   *
   * @param servicesByMode routed services to truncate to underlying service network
   */
  private void truncateToServiceNetworkByMode(RoutedModeServices servicesByMode) {
    servicesByMode.forEach( routedService -> truncateRoutedServiceToServiceNetwork(routedService));
  }

  /**
   * Constructor
   *
   * @param routedServicesLayer this modifier acts upon
   */
  protected RoutedServicesLayerModifierImpl(final RoutedServicesLayer routedServicesLayer) {
    this.routedServicesLayer = routedServicesLayer;
  }


  /**
   * {@inheritDoc}
   *
   *  todo: add support for events upon making changes to routed services as with other modifier implementations
   */
  public void truncateToServiceNetwork(){
    /* identify missing service network entities per routed service mode and truncate to become consistent again */
    routedServicesLayer.getSupportedModes().forEach(m -> truncateToServiceNetworkByMode(routedServicesLayer.getServicesByMode(m)));

    /* after truncation routed services internal ids need to be recreated to ensure contiguous ids throughout the routed services */
    recreateManagedEntitiesIds();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateManagedEntitiesIds() {
    //todo
  }

}
