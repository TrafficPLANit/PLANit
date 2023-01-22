package org.goplanit.service.routed;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.service.routed.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implementation of {@link RoutedServicesLayerModifier}
 */
public class RoutedServicesLayerModifierImpl implements RoutedServicesLayerModifier {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(RoutedServicesLayerModifierImpl.class.getCanonicalName());
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

  /**
   * Recursive method that based on the given offset related to the to be removed timing segments, identifies the first
   * upcoming valid chain after this offset. If found, it creates a copy of the original schedule and removes all segments
   * around the identified valid chain and adds it to the list of new "truncated" routed trip schedules to replace the origina
   * schedule after finishing
   *
   * @param indexOffset to use, relates to index in the toBeRemovedRelativeTimingLegSegments
   * @param toBeRemovedRelativeTimingLegSegments identified leg segments to remove in the routedTripSchedule
   * @param routedTripSchedule to base the truncated routedTripSchedules on
   * @param factory to create new truncated routed trip schedules
   * @return list of newly created truncated trip schedules if any
   */
  private List<RoutedTripSchedule> truncateScheduledTripChainToServiceNetwork(
          int indexOffset, final List<Integer> toBeRemovedRelativeTimingLegSegments, final RoutedTripSchedule routedTripSchedule, RoutedTripScheduleFactory factory) {
    /* in case last chain ended at final leg timing, offset is not in range and we can stop */
    if(indexOffset >= toBeRemovedRelativeTimingLegSegments.size()){
      return null;
    }

    /* consider all to be removed entries leading up to the next valid chain until */
    List<Integer> consecutiveLegsToRemove = toBeRemovedRelativeTimingLegSegments.stream().takeWhile(
            myInt -> toBeRemovedRelativeTimingLegSegments.indexOf(myInt) + indexOffset == myInt).collect(Collectors.toList());
    boolean legsToRemoveAreAtEndOfTimingSegments = (consecutiveLegsToRemove.get(consecutiveLegsToRemove.size()-1) == routedTripSchedule.getLastRelativeLegTimingIndex());
    if(legsToRemoveAreAtEndOfTimingSegments){
      return null;
    }

    /* determine to where the departure is to be offset to, i.e., offset from initial offset to this leg (exclusive)... */
    final int lastLegIndexToRemoveBeforeChain = consecutiveLegsToRemove.get(consecutiveLegsToRemove.size()-1);
    final var allIndicesBeforeFirstChainLeg = IntStream.range(0,lastLegIndexToRemoveBeforeChain+1).boxed().collect(Collectors.toList());
    /*... then , get the durations and dwell times leading up to the valid chain of this portion of the entry and sum them
     * to serve as the departure time offset in addition to the original departure time */
    long departureShiftInNanos = allIndicesBeforeFirstChainLeg.stream().mapToLong(ltIndex ->
            routedTripSchedule.getRelativeLegTiming(ltIndex).getDuration().toNanoOfDay()
                    + routedTripSchedule.getRelativeLegTiming(ltIndex).getDwellTime().toNanoOfDay()).sum();

    /* create a copy of the original to adjust */
    var truncatedRoutedTripSchedule = factory.createUniqueCopyOf(routedTripSchedule);
    /* update the departure times */
    truncatedRoutedTripSchedule.getDepartures().allDepartLaterBy(LocalTime.ofNanoOfDay(departureShiftInNanos));

    /* identify indices after end of chain...*/
    int firstLegIndexToRemoveAfterChain = toBeRemovedRelativeTimingLegSegments.indexOf(lastLegIndexToRemoveBeforeChain)+1;
    final var allIndicesAfterLastChainLeg = IntStream.range(firstLegIndexToRemoveAfterChain,routedTripSchedule.getRelativeLegTimingsSize()).boxed().collect(Collectors.toList());

    /* ...remove all leg timings except the ones belonging to this chain */
    truncatedRoutedTripSchedule.removeLegTimingsIn(
            Stream.concat(allIndicesBeforeFirstChainLeg.stream(), allIndicesAfterLastChainLeg.stream()).collect(Collectors.toList()));

    /* call next recursion */
    var recursiveResult = truncateScheduledTripChainToServiceNetwork(
            firstLegIndexToRemoveAfterChain,
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
  private List<RoutedTripSchedule> truncateScheduledTripToServiceNetwork(final RoutedTripSchedule routedTripSchedule, RoutedTripScheduleFactory factory) {

    /* identify the rel timing leg segments */
    List<Integer> toBeRemovedRelativeTimingLegSegments = RelativeLegTimingUtils.findLegTimingsNotMappedToServiceNetwork(routedTripSchedule, this.routedServicesLayer);

    /* if none to be removed, end invocation here */
    if(toBeRemovedRelativeTimingLegSegments.isEmpty()){
      return null;
    }

    /* if all to be removed, just clear the schedule and stop */
    if(toBeRemovedRelativeTimingLegSegments.size() == routedTripSchedule.getRelativeLegTimingsSize()){
      routedTripSchedule.clear();
      return null;
    }

    /* (Recursive) replace existing routed trip schedule by one or more truncated ones, one per identified consecutive chain with adjusted departure times */
    List<RoutedTripSchedule> truncatedRoutedTripSchedules = truncateScheduledTripChainToServiceNetwork(
            0, toBeRemovedRelativeTimingLegSegments, routedTripSchedule, factory);
    if(truncatedRoutedTripSchedules.isEmpty()){
      throw new PlanItRunTimeException("Invalid truncation of routed trip schedule, expected at least on alternative schedule to be created");
    }
    /* now mark existing schedule for removal, it is replaced by partial (truncated) copies reflecting new valid sub-trips */
    routedTripSchedule.clear();

    return truncatedRoutedTripSchedules;
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

      /* truncate existing schedule and generate (unregistered) partial replacements (if any) */
      var replacementScheduledTrips = truncateScheduledTripToServiceNetwork(currEntry, scheduledTrips.getFactory());

      /* register partial replacements */
      if(replacementScheduledTrips != null){
        replacementScheduledTrips.forEach( st -> scheduledTrips.register(st));
      }

      /* remove original if it has been cleared (either because it is truncated entirely, or replaced by partials) */
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
   * remove all service network entities that are now missing. If, for some reason, the provided services by mode have no (more) entries
   * the services by mode are removed from the layer.
   * <p>
   *   Ids are not recreated in this call, only truncation is performed
   * </p>
   *
   * @param servicesByMode routed services to truncate to underlying service network
   */
  private void truncateToServiceNetworkByMode(RoutedModeServices servicesByMode) {
    if(servicesByMode.isEmpty()){
      return;
    }

    LOGGER.info(String.format("%s Truncating routed services to remaining service network for mode %s",
            LoggingUtils.routedServiceLayerPrefix(routedServicesLayer.getId()), servicesByMode.getMode()));
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
    LOGGER.info(String.format("%s Truncating routed services to remaining service network",LoggingUtils.routedServiceLayerPrefix(routedServicesLayer.getId())));
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
