package org.goplanit.service.routed.modifier;

import org.goplanit.service.routed.RoutedServicesLayerImpl;
import org.goplanit.service.routed.modifier.event.ModifiedRoutedServicesIdsEvent;
import org.goplanit.service.routed.modifier.event.ModifiedRoutedTripIdsEvent;
import org.goplanit.service.routed.modifier.event.handler.SyncDeparturesXmlIdToIdHandler;
import org.goplanit.service.routed.modifier.event.handler.SyncRoutedServicesXmlIdToIdHandler;
import org.goplanit.service.routed.modifier.event.handler.SyncRoutedTripsXmlIdToIdHandler;
import org.goplanit.utils.collections.IntegerListUtils;
import org.goplanit.utils.collections.ListUtils;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.service.routed.*;
import org.goplanit.utils.service.routed.modifier.RoutedServicesLayerModifier;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModificationEvent;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModifierEventType;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModifierListener;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;
import org.goplanit.utils.zoning.modifier.event.ZoningModifierListener;
import org.goplanit.zoning.modifier.event.ModifiedTripScheduleDepartureIdsEvent;
import org.goplanit.zoning.modifier.event.ModifiedZoneIdsEvent;

import java.beans.EventHandler;
import java.util.*;
import java.util.logging.Logger;

/**
 * Implementation of {@link RoutedServicesLayerModifier}
 */
public class RoutedServicesLayerModifierImpl extends EventProducerImpl implements RoutedServicesLayerModifier {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(RoutedServicesLayerModifierImpl.class.getCanonicalName());
  protected final RoutedServicesLayerImpl routedServicesLayer;

  /**
   * Recursive method supporting both #RoutedTripFrequency and #RoutedTripSchedule truncations. Based on the given offset related to the to be removed timing segments, identifies the first
   * upcoming valid chain after this offset. If found, it creates a copy of the original and removes all segments
   * around the identified valid chain and adds it to the list of new "truncated" routed trip schedules (or frequecnies) to replace the original
   * after finishing. It is assumed at least part of the schedule or frequency's segments are valid (is to be checked beforehand)
   *
   * @param indexOffset to use, relates to index in the toBeRemovedSegments, first call it should be set to -1
   * @param toBeRemovedSegments identified leg segments (or rel timing segments) to remove in the routedTrip
   * @param freqOrSched to base the truncated routedTrips  on (either a RoutedTripSchedule or RoutedTripFrequency
   * @param factory to create new truncated routed trip schedules (or frequencies)
   * @return list of newly created truncated trip schedules or frequencies (not yet registered on their routed services container), if any and maybe null
   */
  private <T extends RoutedTrip> List<T> recursiveTruncateXTripChainToServiceNetwork(
          int indexOffset, List<Integer> toBeRemovedSegments, T freqOrSched, RoutedTripFactory<T> factory) {
    /* in case last chain ended at final segment, offset is not in range and we can stop */
    if(indexOffset >= toBeRemovedSegments.size()){
      return null;
    }

    // identify which cast to what class to use for class specific parts of recursive method
    boolean isTripSchedule = freqOrSched instanceof RoutedTripSchedule;
    if(!isTripSchedule && !(freqOrSched instanceof RoutedTripFrequency)){
      throw new PlanItRunTimeException("Only routed trip schedule or routed trip frequencies are supported in recurive truncation to service network");
    }

    final int numSegments = isTripSchedule ?
            ((RoutedTripSchedule)freqOrSched).getRelativeLegTimingsSize() :
            ((RoutedTripFrequency)freqOrSched).getNumberOfLegSegments();

    boolean isFirstChainTruncation = indexOffset == -1;
    indexOffset = Math.max(0,indexOffset);

    /* check for special case where nothing is to be removed for the very first chain, i.e., first removal index is after first chain final index */
    boolean initialChainWithoutRemovalBeforeChain = isFirstChainTruncation && toBeRemovedSegments.get(0) > 0;

    /* consider all to be removed consecutive entries leading up to the next valid chain*/
    final List<Integer> consecutiveLegsToRemoveBeforeChain = initialChainWithoutRemovalBeforeChain ?
            List.of() : IntegerListUtils.getLongestConsecutiveSubList(indexOffset, toBeRemovedSegments);
    final List<Integer> allIndicesBeforeFirstChainLeg = initialChainWithoutRemovalBeforeChain ?
            List.of() : IntegerListUtils.rangeOf(0, ListUtils.getLastValue(consecutiveLegsToRemoveBeforeChain)+1);
    final int nextRecursionIndexOffset = initialChainWithoutRemovalBeforeChain ?
            0 : Math.min(indexOffset + consecutiveLegsToRemoveBeforeChain.size(), toBeRemovedSegments.size());

    if(!initialChainWithoutRemovalBeforeChain && ListUtils.getLastValue(consecutiveLegsToRemoveBeforeChain) == (numSegments-1)){
      /* no valid chain present after removal of invalid components preceding the to-be-created chain, done */
      return null;
    }

    /* create a copy of the original to adjust */
    var truncatedRoutedTripX = (T) (isTripSchedule ?
            ((RoutedTripScheduleFactory)factory).createUniqueDeepCopyOf(((RoutedTripSchedule)freqOrSched)):
            ((RoutedTripFrequencyFactory)factory).createUniqueDeepCopyOf(((RoutedTripFrequency)freqOrSched)));

    // check for special case...chain ends with last leg included, so nothing to remove beyond this
    final boolean lastChainWithoutRemovalAfter = nextRecursionIndexOffset >= toBeRemovedSegments.size();
    /* identify segments around the truncated chain to remove...*/
    List<Integer> truncatedSegmentIndicesToRemove = new ArrayList<>(allIndicesBeforeFirstChainLeg);
    if(!lastChainWithoutRemovalAfter){
      // regular case...truncated version has segments to remove after it ends
      final int firstLegIndexValueToRemoveAfterChain = toBeRemovedSegments.get(nextRecursionIndexOffset);
      final var allIndicesAfterLastChainLeg = IntegerListUtils.rangeOf(firstLegIndexValueToRemoveAfterChain,numSegments);
      truncatedSegmentIndicesToRemove.addAll(allIndicesAfterLastChainLeg);
    }

    /* ...remove all segments except the ones belonging to this chain */
    if(isTripSchedule){
      ((RoutedTripSchedule)truncatedRoutedTripX).removeLegTimingsIn(truncatedSegmentIndicesToRemove);
    }else {
      ((RoutedTripFrequency)truncatedRoutedTripX).removeLegSegmentsIn(truncatedSegmentIndicesToRemove);
    }

    /* call next recursion */
    var recursiveResult = recursiveTruncateXTripChainToServiceNetwork(
            nextRecursionIndexOffset, toBeRemovedSegments, freqOrSched, factory);
    if(recursiveResult == null){
      recursiveResult = new ArrayList<>(1);
    }
    recursiveResult.add(truncatedRoutedTripX);
    return recursiveResult;
  }

  /**
   * Truncate to service network, see {@link #recursiveTruncateXTripChainToServiceNetwork(int, List, RoutedTrip, RoutedTripFactory)} for details
   *
   * @param toBeRemovedLegSegments to apply
   * @param routedTripFrequency to apply truncation to
   * @param factory to create new instances for trunacted partials
   * @return list of partial routed trip frquencies created (if any)
   */
  private List<RoutedTripFrequency> truncateFrequencyTripChainToServiceNetwork(
          List<Integer> toBeRemovedLegSegments, RoutedTripFrequency routedTripFrequency, RoutedTripFrequencyFactory factory) {

    /* bootstrap recursive call */
    return recursiveTruncateXTripChainToServiceNetwork(-1, toBeRemovedLegSegments, routedTripFrequency, factory);
  }

  /**
   * Truncate to service network, see {@link #recursiveTruncateXTripChainToServiceNetwork(int, List, RoutedTrip, RoutedTripFactory)} for details
   *
   * @param toBeRemovedRelTimingSegments to apply
   * @param routedTripSchedule to apply truncation to
   * @param factory to create new instances for trunacted partials
   * @return list of partial routed trip frquencies created (if any)
   */
  private List<RoutedTripSchedule> truncateScheduledTripChainToServiceNetwork(
          final List<Integer> toBeRemovedRelTimingSegments, final RoutedTripSchedule routedTripSchedule, RoutedTripScheduleFactory factory) {
    /* bootstrap recursive call */
    return recursiveTruncateXTripChainToServiceNetwork(-1, toBeRemovedRelTimingSegments, routedTripSchedule, factory);
  }

  /**
   * adjust the passed in instance to the validly mapped portion of it. This means, no changes, or full removal of all
   * leg segments and reset to 0 frequency, or partial removal, meaning splitting the original entry into partially copied entries (and resetting
   * the original)
   *
   * @param routedTripFrequency a routed trip(s) group for a given schedule to truncate to underlying service network
   * @param factory to use when we need to split a routedTripSchedule in multiple entries due to possible gaps that are identified
   * @return new partial frequencies (if any)
   */
  private List<RoutedTripFrequency> truncateFrequencyTripToServiceNetwork(final RoutedTripFrequency routedTripFrequency, RoutedTripFrequencyFactory factory) {

    /* identify the no longer valid leg segments */
    var toBeRemovedLegSegments =
            RoutedTripFrequencyUtils.findServiceLegSegmentsNotMappedToServiceNetwork(routedTripFrequency, this.routedServicesLayer.getParentLayer());

    /* if none to be removed, end invocation here */
    if(toBeRemovedLegSegments.isEmpty()){
      return null;
    }

    /* if all to be removed, just reset frequency to 0 to indicate it is no longer used and should be removed */
    if(toBeRemovedLegSegments.size() == routedTripFrequency.getNumberOfLegSegments()){
      routedTripFrequency.clear();
      return null;
    }

    /* Replace existing routed trip schedule by one or more truncated ones, one per identified consecutive chain with adjusted departure times */
    List<RoutedTripFrequency> truncatedRoutedTripFrequencies =
            truncateFrequencyTripChainToServiceNetwork(toBeRemovedLegSegments, routedTripFrequency, factory);
    if(truncatedRoutedTripFrequencies == null || truncatedRoutedTripFrequencies.isEmpty()){
      throw new PlanItRunTimeException("Invalid truncation of routed trip frequency, expected at least one alternative to be created");
    }
    /* now mark existing schedule for removal, it is replaced by partial (truncated) copies reflecting new valid sub-trips */
    routedTripFrequency.clear();

    return truncatedRoutedTripFrequencies;
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
    List<RoutedTripSchedule> truncatedRoutedTripSchedules =
            truncateScheduledTripChainToServiceNetwork(toBeRemovedRelativeTimingLegSegments, routedTripSchedule, factory);
    if(truncatedRoutedTripSchedules == null || truncatedRoutedTripSchedules.isEmpty()){
      throw new PlanItRunTimeException("Invalid truncation of routed trip schedule, expected at least one alternative schedule to be created");
    }
    /* now mark existing schedule for removal, it is replaced by partial (truncated) copies reflecting new valid sub-trips */
    routedTripSchedule.clear();

    return truncatedRoutedTripSchedules;
  }

  /**
   * Truncate routed service's frequency based trips to the remaining service network (which likely has undergone changes)
   * <p>
   *   Ids are not recreated in this call, only truncation is performed
   * </p>
   * @param routedService routed service to truncate its frequency entries to underlying service network
   * @return replacement frequencies created (if any) for each entry of the routed service. When replacements exist
   * in the result, the original has been cleared and removed from the service container, it only acts as a placeholder key.
   * Created truncated frequencies have NOT yet been registered on the container
   */
  private Map<RoutedTripFrequency, Collection<RoutedTripFrequency>> truncateRoutedServiceFrequenciesToServiceNetwork(RoutedService routedService) {
    /* frequency based */
    var frequencyTrips = routedService.getTripInfo().getFrequencyBasedTrips();
    var frequencyIterator = frequencyTrips.iterator();
    Map<RoutedTripFrequency, Collection<RoutedTripFrequency>> result = new HashMap<>();
    while(frequencyIterator.hasNext()){
      var currEntry = frequencyIterator.next();

      /* truncate existing schedule and generate (unregistered) partial replacements (if any) */
      var currReplacementFrequencyTrips = truncateFrequencyTripToServiceNetwork(currEntry, frequencyTrips.getFactory());
      result.put(currEntry, currReplacementFrequencyTrips);

      /* remove original if it has been cleared (either because it is truncated entirely, or replaced by partials) */
      if(!currEntry.hasPositiveFrequency()){
        frequencyIterator.remove();
      }
    }
    return result;
  }

  /**
   * Create truncate routed service's schedules based on the remaining (mapped portion of the) service network
   * (which likely has undergone changes)
   * <p>
   *   Ids are not recreated in this call, only truncation is performed
   * </p>
   * @param routedService routed service to truncate its schedules to underlying service network
   * @return replacement schedules created (if any) for each schedule of the routed service. When replacement schedules exist
   * in the result, the original schedule has been cleared and removed from the service container, it only acts as a placeholder.
   * Created truncated schedules have NOT yet been registered on the container
   *
   */
  private Map<RoutedTripSchedule, Collection<RoutedTripSchedule>> createTruncatedRoutedServiceSchedulesToServiceNetwork(RoutedService routedService) {

//    if(routedService.getName().equals("607X")){
//      int bla = 4;
//    }

    /* schedule based */
    var scheduledTrips = routedService.getTripInfo().getScheduleBasedTrips();
    var scheduleIterator = scheduledTrips.iterator();
    Map<RoutedTripSchedule, Collection<RoutedTripSchedule>> result = new HashMap<>();
    while(scheduleIterator.hasNext()){
      var currEntry = scheduleIterator.next();

      /* truncate existing schedule and generate (unregistered) partial replacements (if any) */
      var currReplacementScheduledTrips = truncateScheduledTripToServiceNetwork(currEntry, scheduledTrips.getFactory());
      result.put(currEntry, currReplacementScheduledTrips);

      /* remove original if it has been cleared (either because it is truncated entirely, or replaced by partials) */
      if(currEntry.getDepartures().isEmpty() || !currEntry.hasRelativeLegTimings()){
        scheduleIterator.remove();
      }
    }
    return result;
  }

  /**
   * Truncate all frequency based routed services for a mode and match them to the remaining service network (which likely has undergone changes), i.e.,
   * remove all service network entities that are now missing. If, for some reason, the provided services by mode have no (more) entries
   * the services by mode are removed from the layer.
   * <p>
   *   Ids are not recreated in this call, only truncation is performed and truncated schedules are registered internally on each service
   * </p>
   *
   * @param servicesByMode routed services to truncate to underlying service network
   */
  private void truncateFrequencyServicesToServiceNetworkByMode(RoutedModeServices servicesByMode) {
    int numKeptFrequencyTrips = 0;
    int numRemovedFrequencyTrips = 0;
    int numCreatedTruncatedFrequencyTrips = 0;
    for(var routedService : servicesByMode){
      var truncatedFrequencyBasedByOriginal =  truncateRoutedServiceFrequenciesToServiceNetwork(routedService);
      for( var entry : truncatedFrequencyBasedByOriginal.entrySet()){
        if(entry.getValue() == null || entry.getValue().isEmpty()){
          boolean frequencyKept = entry.getKey().hasLegSegments() && entry.getKey().hasPositiveFrequency();
          numKeptFrequencyTrips = frequencyKept ? numKeptFrequencyTrips + 1 : numKeptFrequencyTrips;
          numRemovedFrequencyTrips = !frequencyKept ? numRemovedFrequencyTrips + 1 : numRemovedFrequencyTrips;
          continue;
        }

        ++numRemovedFrequencyTrips;
        var currCreatedTruncatedFrequencies = entry.getValue();
        numCreatedTruncatedFrequencyTrips += currCreatedTruncatedFrequencies.size();

        /* register created replacements on container */
        currCreatedTruncatedFrequencies.stream().forEach(
            singleRtf -> routedService.getTripInfo().getFrequencyBasedTrips().register(singleRtf));
      }
    }
    LOGGER.info(String.format("%s[%s] # kept frequency based trips as is : %d",
        LoggingUtils.routedServiceLayerPrefix(routedServicesLayer.getId()), servicesByMode.getMode(), numKeptFrequencyTrips));
    LOGGER.info(String.format("%s[%s] # removed/replaced frequency based trips : %d",
        LoggingUtils.routedServiceLayerPrefix(routedServicesLayer.getId()), servicesByMode.getMode(), numRemovedFrequencyTrips));
    LOGGER.info(String.format("%s[%s] # newly created partials of frequency based trips : %d",
        LoggingUtils.routedServiceLayerPrefix(routedServicesLayer.getId()), servicesByMode.getMode(), numCreatedTruncatedFrequencyTrips));
  }

  /**
   * Truncate all schedule based routed services for a mode and match them to the remaining service network (which likely has undergone changes), i.e.,
   * remove all service network entities that are now missing. If, for some reason, the provided services by mode have no (more) entries
   * the services by mode are removed from the layer.
   * <p>
   *   Ids are not recreated in this call, only truncation is performed and truncated schedules are registered internally on each service
   * </p>
   *
   * @param servicesByMode routed services to truncate to underlying service network
   */
  private void truncateScheduledServicesToServiceNetworkByMode(RoutedModeServices servicesByMode) {
    int numKeptScheduledTrips = 0;
    int numRemovedScheduledTrips = 0;
    int numCreatedTruncatedScheduledTrips = 0;
    for(var routedService : servicesByMode){
      var truncatedSchedulesByOriginal =  createTruncatedRoutedServiceSchedulesToServiceNetwork(routedService);
      for( var entry : truncatedSchedulesByOriginal.entrySet()){
        if(entry.getValue() == null || entry.getValue().isEmpty()){
          boolean scheduleKept = !entry.getKey().getDepartures().isEmpty();
          numKeptScheduledTrips = scheduleKept ? numKeptScheduledTrips + 1 : numKeptScheduledTrips;
          numRemovedScheduledTrips = !scheduleKept ? numRemovedScheduledTrips + 1 : numRemovedScheduledTrips;
          continue;
        }

        ++numRemovedScheduledTrips;
        var currCreatedTruncatedSchedules = entry.getValue();
        numCreatedTruncatedScheduledTrips += currCreatedTruncatedSchedules.size();

        /* register created replacements on container */
        currCreatedTruncatedSchedules.stream().forEach(
            singleRts -> routedService.getTripInfo().getScheduleBasedTrips().register(singleRts));
      }
    }
    LOGGER.info(String.format("%s[%s] # kept scheduled trips as is : %d",
        LoggingUtils.routedServiceLayerPrefix(routedServicesLayer.getId()), servicesByMode.getMode(), numKeptScheduledTrips));
    LOGGER.info(String.format("%s[%s] # removed/replaced scheduled trips : %d",
        LoggingUtils.routedServiceLayerPrefix(routedServicesLayer.getId()), servicesByMode.getMode(), numRemovedScheduledTrips));
    LOGGER.info(String.format("%s[%s] # newly created partials of scheduled trips : %d",
        LoggingUtils.routedServiceLayerPrefix(routedServicesLayer.getId()), servicesByMode.getMode(), numCreatedTruncatedScheduledTrips));
  }

  /**
   * Truncate all routed services for a mode and match them to the remaining service network (which likely has undergone changes), i.e.,
   * remove/truncate all routed services (and their trips) relying on service network entities that are now missing.
   * <p>
   *   Ids are not recreated in this call, only truncation is performed and truncated schedules are registered internally on each service
   * </p>
   *
   * @param servicesByMode routed services to truncate to underlying service network
   */
  private void truncateToServiceNetworkByMode(RoutedModeServices servicesByMode) {
    if(servicesByMode.isEmpty()){
      return;
    }

    LOGGER.info(String.format("%sTruncating routed services to remaining service network for mode %s",
            LoggingUtils.routedServiceLayerPrefix(routedServicesLayer.getId()), servicesByMode.getMode()));

    /* SCHEDULE BASED */
    truncateScheduledServicesToServiceNetworkByMode(servicesByMode);

    /* FREQUENCY BASED */
    truncateFrequencyServicesToServiceNetworkByMode(servicesByMode);

    /* remove unused services (does not recreate ids) */
    removeRoutedServicesWithoutTrips(false, servicesByMode.getMode());
  }


  /**
   * {@inheritDoc}
   */
  @Override
  protected void fireEvent(EventListener eventListener, Event event) {
    ((RoutedServicesModifierListener) eventListener).onRoutedServicesModifierEvent((RoutedServicesModificationEvent) event);
  }

  /**
   * Constructor
   *
   * @param routedServicesLayer this modifier acts upon
   */
  public RoutedServicesLayerModifierImpl(final RoutedServicesLayerImpl routedServicesLayer) {
    this.routedServicesLayer = routedServicesLayer;
  }


  /**
   * {@inheritDoc}
   *
   *  Note that this implementation will automatically overwrite all pre-existing XML ids with the internal ids of all managed id containers within the routed services layer to ensure
   *  uniqueness on both levels of ids.
   *
   *  If, for some reason, the provided services by mode have no (more) entries the services by mode are removed from the layer. Same goes for routed services that have
   *  no more trips
   */
  public void truncateToServiceNetwork(){
    LOGGER.info(String.format("%sTruncating routed services to remaining service network",LoggingUtils.routedServiceLayerPrefix(routedServicesLayer.getId())));

    /* identify missing service network entities per routed service mode and truncate to become consistent again */
    for( var servicesPerMode : routedServicesLayer){
      truncateToServiceNetworkByMode(servicesPerMode);
    }

    /* make sure empty entries are removed */
    removeEmptyRoutedServicesByMode(false);

    /* after truncation routed services all internal ids need to be recreated to ensure contiguous ids throughout the routed services */
    recreateManagedEntitiesIds();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeEmptyRoutedServicesByMode(boolean recreateManagedEntitiesIds) {
    boolean removedAnything = false;
    var iter = this.routedServicesLayer.iterator();
    while(iter.hasNext()){
      if(iter.next().isEmpty()){
        iter.remove();
        removedAnything = removedAnything || true ;
      }
    }

    if(recreateManagedEntitiesIds && removedAnything){
      recreateManagedEntitiesIds();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeRoutedServicesWithoutTrips(boolean recreateManagedEntitiesIds, Mode... modes) {

    boolean removedAnything = false;
    for(Mode mode : modes){
      var servicesByMode = this.routedServicesLayer.getServicesByMode(mode);
      if(servicesByMode.isEmpty()){
        continue;
      }

      int before = servicesByMode.size();
      servicesByMode.removeIf( r -> !r.getTripInfo().hasAnyTrips());
      if(before != servicesByMode.size() && !removedAnything){
        LOGGER.info(String.format("%sRemoved %d routed services without trips (remaining: %d) for mode %s",
            LoggingUtils.routedServiceLayerPrefix(routedServicesLayer.getId()),  before - servicesByMode.size(), servicesByMode.size(), servicesByMode.getMode()));
        removedAnything = true;
      }
    }

    if(recreateManagedEntitiesIds && removedAnything){
      recreateManagedEntitiesIds();
    }
  }


  /**
   * {@inheritDoc}
   *
   * All managed ids of the layer are recreated via {@link #recreateRoutedServicesIds()}, {@link #recreateRoutedTripsIds()}, and {@link #recreateRoutedTripScheduleDepartureIds()}
   * including their triggered events which will trigger callbacks to their event listeners (if any are registered)
   */
  @Override
  public void recreateManagedEntitiesIds() {
    LOGGER.info(String.format("%sRecreating all ids managed by routed service layer",LoggingUtils.routedServiceLayerPrefix(routedServicesLayer.getId())));
    // do for routed modes container
    recreateRoutedServicesIds();
    // do for all routed trips (scheduled and frequency based as they all use the same id token)
    recreateRoutedTripsIds();
    // do for all scheduled departures
    recreateRoutedTripScheduleDepartureIds();
  }

  /**
   * {@inheritDoc}
   *
   * Triggers a ModifiedTripScheduleDepartureIdsEvent upon completion
   */
  @Override
  public void recreateRoutedTripScheduleDepartureIds() {
    /* allow modifier to recreate the scheduled trips departure entity ids. because these ids are unique across all modes, services, trips schedules for which each has a container
     * we should only reset it once, otherwise it is no longer unique across those contains when recreating the ids
     */
    boolean doIdReset = true;
    for(var routedModeServices : routedServicesLayer) {
      for (var entry : routedModeServices) {
        for( var sbt : entry.getTripInfo().getScheduleBasedTrips()) {
          sbt.getDepartures().recreateIds(doIdReset);
          doIdReset = false;
        }
      }
    }

    fireEvent(new ModifiedTripScheduleDepartureIdsEvent(this, routedServicesLayer));
  }

  /**
   * {@inheritDoc}
   *
   * Triggers a ModifiedRoutedTripIdsEvent upon completion
   */
  @Override
  public void recreateRoutedTripsIds() {
    /* allow modifier to recreate the scheduled trips entity ids. because these ids are unique across all modes and services for which each has a container
     * we should only reset it once, otherwise it is no longer unique across those contains when recreating the ids
     */
    boolean doIdReset = true;
    for(var routedModeServices : routedServicesLayer) {
      for (var entry : routedModeServices) {
        if(entry.getTripInfo().getScheduleBasedTrips().getFactory().getIdGroupingToken() != entry.getTripInfo().getFrequencyBasedTrips().getFactory().getIdGroupingToken()){
          throw new PlanItRunTimeException("Expectation in updating ids is that all trips (frequency and scheduled) use same id token, this was found to not be the case, please adjust implementation");
        }
        entry.getTripInfo().getScheduleBasedTrips().recreateIds(doIdReset);
        doIdReset = false;
        entry.getTripInfo().getFrequencyBasedTrips().recreateIds(doIdReset);
      }
    }

    fireEvent(new ModifiedRoutedTripIdsEvent(this, routedServicesLayer));
  }

  /**
   * {@inheritDoc}
   *
   * Triggers a ModifiedRoutedServicesIdsEvent upon completion
   */
  @Override
  public void recreateRoutedServicesIds() {
    /* allow modifier to recreate the routed service layer managed entity ids. because routedServices' ids are unique across all modes for which each has a container
     * we should only reset it once, otherwise it is no longer unique across those contains when recreating the ids
     */
    boolean doIdReset = true;
    for(var routedModeServices : routedServicesLayer){
      /* only reset once, because all entries across all modes use the same groupIdToken */
      routedModeServices.recreateIds(doIdReset);
      doIdReset = false;
    }

    fireEvent(new ModifiedRoutedServicesIdsEvent(this, routedServicesLayer));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(RoutedServicesModifierListener listener) {
    super.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(RoutedServicesModifierListener listener, RoutedServicesModifierEventType eventType) {
    super.addListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(RoutedServicesModifierListener listener, RoutedServicesModifierEventType eventType) {
    super.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(RoutedServicesModifierListener listener) {
    super.removeListener(listener);
  }

}
