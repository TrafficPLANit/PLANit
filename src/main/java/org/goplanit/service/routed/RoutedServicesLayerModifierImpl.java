package org.goplanit.service.routed;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.goplanit.utils.service.routed.*;

/**
 * Implementation of {@link RoutedServicesLayerModifier}
 */
public class RoutedServicesLayerModifierImpl implements RoutedServicesLayerModifier {

  protected final RoutedServicesLayer routedServicesLayer;

  /**
   * @param offsetLegIndex          offset, use this leg as starting point for search, inclusive unless indicated otherwise, must be zero or higher
   * @param routedTripSchedule containing the relative leg timings
   * @return found first next available timing index for which the service network contains the referenced leg(segment)
   */
  private int findNextValidTimingLeg(int offsetLegIndex, RoutedTripSchedule routedTripSchedule, boolean searchExclusive) {
    final int relTimingSize = routedTripSchedule.getRelativeLegTimingsSize();
    if(offsetLegIndex < 0 || offsetLegIndex >= relTimingSize){
      throw new PlanItRunTimeException("provided invalid index for relative timing search offset");
    }

    boolean isInvalid = true;
    RelativeLegTiming currLegtiming = null;
    var serviceNodes = routedServicesLayer.getParentLayer().getServiceNodes();
    var legSegments = routedServicesLayer.getParentLayer().getLegSegments();
    while(isInvalid && offsetLegIndex <relTimingSize) {
      //TODO: CONTINUE HERE -> we identify if a rel timing leg segment is invalid and keep going until
      //      a valid one is found. Then determine what to return and if that covers all cases of calling this method
      //      Then, continue with the algorightm below that calls this in removing entries
      currLegtiming = routedTripSchedule.getRelativeLegTiming(offsetLegIndex);
      isInvalid =
          !legSegments.hasServiceLegSegment(currLegtiming.getParentLegSegment())
          ||
          !serviceNodes.hasServiceNode(currLegtiming.getParentLegSegment().getUpstreamServiceNode())
          ||
          !serviceNodes.hasServiceNode(currLegtiming.getParentLegSegment().getDownstreamServiceNode());
      offsetLegIndex += 1;
    }

    return -1;
  }

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
   * Truncate routed trip(s) - schedule based -  at hand to the remaining service network (which likely has undergone changes)
   * <p>
   * Ids are not recreated in this call, only truncation is performed
   * </p>
   * <p>
   *   It is possible the routed trip schedule ends up without any departures or leg timings in which case the invoker is expected to
   *   cull the schedule as it is no longer viable.
   * </p>
   *
   * @param routedTripSchedule a routed trip(s) group for a given schedule to truncate to underlying service network
   */
  private void truncateScheduledTripToServiceNetwork(RoutedTripSchedule routedTripSchedule) {
    final boolean searchExclusive = false;
    final boolean searchInclusive = true;
    // find first valid service node from given reference point (including reference point check which is assumed valid)
    RelativeLegTiming currValidLeg = null; //todo
    // find last consecutive valid service node from given reference point (excluding reference point check which is assumed valid)
    int nextValidLegIndex = findNextValidTimingLeg(-1, routedTripSchedule, searchExclusive); // todo

    //todo: continue when findNextValidTimingLeg seems ok.
//    if(currValidLeg == null && nextValidLeg == null){
//      // remove entire content of routed trip schedule
//      routedTripSchedule.clearRelativeLegTimings();
//      routedTripSchedule.clearDepartures();
//      return;
//    }
//
//    /* at least some portion remains, remove portions in between/front/back where needed */
//    do{
//
//
//      // determine if departure times needs updating in case this is the first leg(s) that are being truncated
//      if(currValidLeg == null){
//
//      }
//
//      // remove the legs that are to be truncated from the reltimings
//
//      currValidLeg = nextValidLeg;
//      ServiceNode nextValidServiceNode = null; // todo update
//    }while(currValidLeg!= null);



  }

  /**
   * Truncate routed service at hand to the remaining service network (which likely has undergone changes)
   * <p>
   *   Ids are not recreated in this call, only truncation is performed
   * </p>
   *
   * @param routedService routed service to truncate to underlying service network
   */
  private void truncateRoutedServiceToServiceNetwork(RoutedService routedService) {
    /* schedule based */
    var scheduledTrips = routedService.getTripInfo().getScheduleBasedTrips();
    var scheduleIterator = scheduledTrips.iterator();
    while(scheduleIterator.hasNext()){
      var currEntry = scheduleIterator.next();
      truncateScheduledTripToServiceNetwork(currEntry);
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
