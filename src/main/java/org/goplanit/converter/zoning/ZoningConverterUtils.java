package org.goplanit.converter.zoning;

import java.util.*;
import java.util.function.Function;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitEntityGeoUtils;
import org.goplanit.utils.geo.PlanitGraphGeoUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.locale.DrivingDirectionDefaultByCountry;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.TrackModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.zoning.DirectedConnectoid;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LinearLocation;

/**
 * Utilities regarding Zoning conversions that might be useful for implementations supporting the mapping of
 * non-PLANit geometry based entities to PLANit transfer zones and connectoids
 *
 * @author markr
 *
 */
public class ZoningConverterUtils {

  /** Verify if based on passed in parameters either of the two overrides is active
   *
   * @param waitingAreaSourceId these link segments pertain to
   * @param node to verify
   * @param accessLinkSourceId source id of the access link used, this will be matched against the overwritten access link source id (if provided)
   * @param getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId mapping from waiting area source id to nominated access link source id, which if a match to provided access link source id
   *                                                               makes sure that even if there exists crossing traffic this does not render its access link segment(s) invalid, may be null
   * @param getOverwrittenWaitingAreaSourceId function that provides the overwritten waiting area source id for a given access node, maybe return null if not overwrritten
   * @return found link segments that are deemed valid given the constraints
   */
  private static boolean isOverwriteActive(
      final String waitingAreaSourceId,
      final Node node,
      String accessLinkSourceId,
      final Function<String,String> getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,
      final Function<Node,String> getOverwrittenWaitingAreaSourceId){
    boolean isOverwriteActive = false;
    /* in both cases: When a match, we must use the user overwrite value. We will still try to remove access link segments
     * that are invalid, but if due to this check no matches remain, we revert this and instead use all entry link segments on the osm way
     * since the user has indicated to explicitly use this combination which overrules the automatic filter we would ordinarily apply */

    /* stopLocation point -> waiting area overwrite */
    if (getOverwrittenWaitingAreaSourceId!= null && getOverwrittenWaitingAreaSourceId.apply(node) != null) {
      isOverwriteActive = !(waitingAreaSourceId == getOverwrittenWaitingAreaSourceId.apply(node));
    }
    /* waiting area -> link (source id) overwrite */
    if (getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId!= null && getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId.apply(waitingAreaSourceId) != null) {
      isOverwriteActive = isOverwriteActive || getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId.apply(waitingAreaSourceId).equals(accessLinkSourceId);
    }
    return isOverwriteActive;
  }

  /** Verify if the provided line segment (somewhere along the geometry of the access link segment) resides on the correct side
   * of the waiting area location, assuming this matters
   *
   * @param waitingAreaGeometry for which to check location
   * @param accessLinkSegment the location resides on
   * @param accessLinkSegmentLineSegment to use
   * @param geoUtils to use
   * @return true when left of, false otherwise
   */
  public static boolean isWaitingAreaLeftOfAccessLineSegment(
      final Geometry waitingAreaGeometry,
      final MacroscopicLinkSegment accessLinkSegment,
      final LineSegment accessLinkSegmentLineSegment,
      PlanitJtsCrsUtils geoUtils) {

    var localLineSegment = new LineSegment(accessLinkSegmentLineSegment);
    boolean reverseLinearLocationGeometry = accessLinkSegment.isDirectionAb()!=accessLinkSegment.getParent().isGeometryInAbDirection();
    if(reverseLinearLocationGeometry) {
      localLineSegment.reverse();
    }
    return geoUtils.isGeometryLeftOf(waitingAreaGeometry, localLineSegment.p0, localLineSegment.p1);
  }

  /** Verify if the provided existing internal location of the link would be valid as access node with upstream access link segment if it were to
   * be used, i,e., if the link were to be broken at this point. Only in case the upstream link segment of this point is one-way and if used for the waiting area
   * geometry and then would reside on the wrong side of the road (for modes where this matters such as bus), then this method will return false. In all other situation, e.g. two-way roads
   * or relative location of waiting area is valid, or mode does not require a specific location relative to road (train), then it will return true. Note that this might occur
   * if the waiting area geometry when assessed only by the linear linea between extreme nodes is residing on the correct side, but internal geometry is more complex causing the internal point
   * to be wrongly located. That is what this method is verifying.
   *
   * @param waitingAreaSourceId that goes with the geometry
   * @param waitingAreaGeometry for which to check location
   * @param accessLink the location resides on
   * @param connectoidLocation to verify
   * @param accessMode for the location
   * @param getOverwrittenWaitingAreaSourceId function that provides the overwritten waiting area source id for a given access node, maybe return null if not overwritten
   * @param countryName we are considering from which we will extract whether it is a left or right hand drive country
   * @param geoUtils to use
   * @return true when deemed valid for the restrictions checked, false otherwise
   */
  @Deprecated // to be replaced by #isWaitingAreaAccessLinkSegmentInternalLocationModeCombinationDirectionallyValid using link segments rather than links and with link segments we can ignore the one way check which is bad
  private static boolean isWaitingAreaAccessLinkSegmentInternalLocationModeCombinationDirectionallyValid(
      String waitingAreaSourceId,
      final Geometry waitingAreaGeometry,
      final MacroscopicLink accessLink,
      final Point connectoidLocation,
      final Mode accessMode,
      final Function<Point,String> getOverwrittenWaitingAreaSourceId,
      String countryName,
      PlanitJtsCrsUtils geoUtils) {

    boolean mustAvoidCrossingTraffic =
        isAvoidCrossTrafficForAccessModeOrAccessNodeWaitingAreaOverwritten(
            accessMode, waitingAreaSourceId, connectoidLocation, getOverwrittenWaitingAreaSourceId);

    MacroscopicLinkSegment oneWayLinkSegment = accessLink.getLinkSegmentIfLinkIsOneWayForMode(accessMode);
    if(mustAvoidCrossingTraffic && oneWayLinkSegment != null) {
      /* special case: one way link and internal existing coordinate chosen. If the upstream geometry of this coordinate (when extrapolated to the waiting area)
       * is on the wrong side of the waiting area, it would be discarded, yet it might be that a projected location closest to the waiting area would be valid
       * due to a bend in the road in the downstream direction at this very coordinate. Hence, we only accept this existing coordinate when we are sure
       * it will not be discarded due to residing on the wrong side of the road infrastructure (when extrapolated) */
      Coordinate[] linkCoordinates = accessLink.getGeometry().getCoordinates();
      int coordinateIndex = PlanitJtsUtils.getCoordinateIndexOf(connectoidLocation.getCoordinate(), linkCoordinates);
      if(coordinateIndex <= 0) {
        throw new PlanItRunTimeException("Unable to locate link internal location %s for access link even though it is expected to exist for waiting area %s",accessLink.getExternalId(), waitingAreaSourceId);
      }

      LineSegment segment = new LineSegment(linkCoordinates[coordinateIndex-1], linkCoordinates[coordinateIndex]);
      boolean reverseLinearLocationGeometry = oneWayLinkSegment.isDirectionAb()!=oneWayLinkSegment.getParent().isGeometryInAbDirection();
      if(reverseLinearLocationGeometry) {
        segment.reverse();
      }
      return geoUtils.isGeometryLeftOf(waitingAreaGeometry, segment.p0, segment.p1) == DrivingDirectionDefaultByCountry.isLeftHandDrive(countryName);
    }
    return true;
  }

  /** Verify if the waiting area for an access mode's access link(segment) must be on the logical relative location (left hand side for left hand drive) or not.
   * In case the mapping is overwritten it is assumed the driving direction does not matter as it is user defined to be explicitly mapped.
   *
   * @param <T> type of access entity
   * @param accessMode to check
   * @param waitingAreaSourceId required to check if user overwrite is present for this waiting area, may be null to
   * @param accessEntity the access entity that may be mapped to the waiting area under investigation, may be null if not available
   * @param getOverwrittenWaitingAreaSourceId function that provides the overwritten waiting area source id for a given access node, maybe return null if not overwrritten
   * @return true when restricted for driving direction, false otherwise
   */
  public static <T> boolean isAvoidCrossTrafficForAccessModeOrAccessNodeWaitingAreaOverwritten(
      final Mode accessMode, final String waitingAreaSourceId, T accessEntity, Function<T,String> getOverwrittenWaitingAreaSourceId) {

    if(accessEntity != null && getOverwrittenWaitingAreaSourceId != null && getOverwrittenWaitingAreaSourceId.apply(accessEntity) != null) {
      /* ... exception : user override with mapping to this zone for this node, in which case we allow crossing traffic regardless */
      return !waitingAreaSourceId.equals(getOverwrittenWaitingAreaSourceId.apply(accessEntity));
    }
    return isAvoidCrossTrafficForAccessMode(accessMode);
  }

  /** Verify if the waiting area for an access mode's access link(segment) must be on the logical relative location (left hand side for left hand drive) or not.
   *
   * @param accessMode to check
   * @return true when restricted for driving direction, false otherwise
   */
  public static boolean isAvoidCrossTrafficForAccessMode(final Mode accessMode) {
    return accessMode.getPhysicalFeatures().getTrackType().equals(TrackModeType.ROAD);
  }

  /** create a subset of links from the passed in ones, removing all links for which we can be certain that geometry is located on the wrong side of the road infrastructure geometry. Note that
   * rails is not excluded in this exercise, i.e., it is assumed rail lines ar bi-rectional.
   * Right or wrong side is verified by checking if the link is one-way. If so, we can be sure (based on the driving direction of the country) if the geometry is located to the closest by (logical)
   * driving direction given the placement of the geometry, i.e., on the left hand side for left hand drive countries, on the right hand side for right hand driving countries
   *
   * @param location representing the location of concern expected to have access to the link
   * @param links to remove in-eligible ones from
   * @param isLeftHandDrive flag
   * @param accessModes to consider
   * @param geoUtils to use
   * @return remaining links that are deemed eligible
   */
  public static Collection<MacroscopicLink> excludeLinksOnWrongSideOf(
      Geometry location, Collection<MacroscopicLink> links, boolean isLeftHandDrive, Collection<? extends Mode> accessModes, PlanitJtsCrsUtils geoUtils) {
    Collection<MacroscopicLink> matchedLinks = new HashSet<>(links);
    for(var link : links) {
      for(Mode accessMode : accessModes){

        /* road based PT modes are only accessible on one side, so they must stop with the waiting area in the correct driving direction, i.e., must avoid cross traffic, because otherwise they
         * have no doors at the right side, e.g., travellers have to cross the road to get to the vehicle, which should not happen */
        boolean mustAvoidCrossingTraffic = ZoningConverterUtils.isAvoidCrossTrafficForAccessMode(accessMode);

        MacroscopicLinkSegment oneWayLinkSegment = link.getLinkSegmentIfLinkIsOneWayForMode(accessMode);
        if(oneWayLinkSegment != null && mustAvoidCrossingTraffic) {
          /* use line geometry closest to connectoid location */
          LineSegment finalLineSegment = PlanitEntityGeoUtils.extractClosestLineSegmentToGeometryFromLinkSegment(location, oneWayLinkSegment, geoUtils);
          /* determine location relative to infrastructure */
          boolean isStationLeftOfOneWayLinkSegment = geoUtils.isGeometryLeftOf(location, finalLineSegment.p0, finalLineSegment.p1);
          if(isStationLeftOfOneWayLinkSegment != isLeftHandDrive) {
            /* travellers cannot reach doors of mode on this side of the road, so deemed not eligible */
            matchedLinks.remove(link);
            break; // from mode loop
          }
        }
      }
    }
    return matchedLinks;
  }

  /** Find the access link segments ineligible given the intended location and the access mode.
   * When transfer zone location differs from the connectoid location determine on which side of the infrastructure it exists and based on the country's driving direction
   * and access mode determine the access link segments
   *
   * @param accessLinkSegments to filter
   * @param location to identify incorrectly located link segments for
   * @param leftHandDrive is infrastructure left hand drive or not
   * @param geoUtils to use for determining geographic eligibility
   * @return ineligible link segments to be access link segments for connectoid at this location
   */
  public static Collection<LinkSegment> identifyLinkSegmentsOnWrongSideOf(Geometry location,
          final Collection<LinkSegment> accessLinkSegments, boolean leftHandDrive, final PlanitJtsCrsUtils geoUtils) {

    List<LinkSegment> invalidAccessLinkSegments = new ArrayList<>(accessLinkSegments.size());
    /* use line geometry closest to connectoid location */
    for (LinkSegment linkSegment : accessLinkSegments) {
      LineSegment finalLineSegment = PlanitGraphGeoUtils.extractClosestLineSegmentTo(location, linkSegment, geoUtils);
      /* determine location relative to infrastructure */
      boolean isTransferZoneLeftOfInfrastructure = geoUtils.isGeometryLeftOf(location, finalLineSegment.p0, finalLineSegment.p1);
      if (isTransferZoneLeftOfInfrastructure != leftHandDrive) {
        /* not viable opposite traffic directions needs to be crossed on the link to get to stop location --> remove */
        invalidAccessLinkSegments.add(linkSegment);
      }
    }
    return invalidAccessLinkSegments;
  }

    /**
     * Exclude the closest link if it is situated on the wrong side of the road, then if links are remaining keep going with removing the then closest if it is also
     * on the wrong side of the road etc.
     *
     * @param location to verify against, typically a transfer zone location
     * @param links to update
     * @param isLeftHandDrive network driving direction
     * @param accessModes for the location
     * @param geoUtils to use
     * @return pair with remaining closest link found and boolean indicating if any closest links were removed before finding a compatible closest links (true when one or more closest links are removed, false otherwise)
     */
  public static Pair<MacroscopicLink,Boolean> excludeClosestLinksIncrementallyOnWrongSideOf(
      Geometry location, Collection<MacroscopicLink> links, boolean isLeftHandDrive, Collection<? extends Mode> accessModes, PlanitJtsCrsUtils geoUtils) {
    boolean entriesRemoved = false;
    MacroscopicLink closestLink = null;
    do{
      closestLink = (MacroscopicLink) PlanitGraphGeoUtils.findEdgeClosest(location, links, geoUtils);
      Collection<MacroscopicLink> result =
          ZoningConverterUtils.excludeLinksOnWrongSideOf(location, Collections.singleton(closestLink), isLeftHandDrive,  accessModes, geoUtils);
      if(result!=null && !result.isEmpty()){
        /* closest is also viable, stop removal */
        break;
      }

      /* closest link is on the wrong side of the waiting area*/
      links.remove(closestLink);
      entriesRemoved = true;
      closestLink = null;

    }while(!links.isEmpty());

    if(!links.isEmpty() && closestLink==null){
      closestLink = (MacroscopicLink) PlanitGraphGeoUtils.findEdgeClosest(location, links, geoUtils);
    }

    return Pair.of(closestLink,entriesRemoved);
  }

  // Same as node based version, only now we do not know yet which node is our reference node, so we consider both as options
  public static Collection<? extends LinkSegment> findAccessLinkSegmentsForWaitingArea(
      String waitingAreaSourceId,
      Geometry waitingAreaGeometry,
      MacroscopicLink accessLink,
      String accessLinkSourceId,
      Mode accessMode,
      String countryName,
      boolean mustAvoidCrossingTraffic,
      Function<String,String> getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,
      Function<Node,String> getOverwrittenWaitingAreaSourceId,
      PlanitJtsCrsUtils geoUtils) {

    // node A of potential access link
    var accessLinkSegments = findAccessEntryLinkSegmentsForWaitingArea(waitingAreaSourceId,
        waitingAreaGeometry, accessLink, accessLinkSourceId, accessLink.getNodeA(), accessMode, countryName, mustAvoidCrossingTraffic, getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, getOverwrittenWaitingAreaSourceId, geoUtils);

    // node B of potential access link
    var accessLinkSegmentsB = findAccessEntryLinkSegmentsForWaitingArea(waitingAreaSourceId,
        waitingAreaGeometry, accessLink, accessLinkSourceId, accessLink.getNodeB(), accessMode, countryName, mustAvoidCrossingTraffic, getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, getOverwrittenWaitingAreaSourceId, geoUtils);

    if(accessLinkSegments!=null){
      accessLinkSegments.addAll(accessLinkSegmentsB);
    }else{
      accessLinkSegments = accessLinkSegmentsB;
    }
    return accessLinkSegments;
  }

  /** Find the link segments that are accessible for the given access link, node, mode combination taking into account the relative location of the transfer zone if needed and
   * mode compatibility.
   *
   * @param waitingAreaSourceId these link segments pertain to
   * @param waitingAreaGeometry these link segments pertain to
   * @param accessLink that is nominated
   * @param accessLinkSourceId source id of the access link used, this will be matched gainst the overwritten access link source id (if provided)
   * @param node extreme node of the link
   * @param accessMode eligible access mode
   * @param countryName we are considering from which we will extract whether it is a left or right hand drive country
   * @param mustAvoidCrossingTraffic flag indicating if cross traffic should be avoided for the access link segments that are deemed eligible
   * @param getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId mapping from waiting area source id to nominated access link source id, which if a match to provided access link source id
   *                                                               makes sure that even if there exists crossing traffic this does not render its access link segment(s) invalid, may be null
   * @param getOverwrittenWaitingAreaSourceId function that provides the overwritten waiting area source id for a given access node, maybe return null if not overwrritten
   * @param geoUtils to use
   * @return found link segments that are deemed valid given the constraints
   */
  public static Collection<LinkSegment> findAccessEntryLinkSegmentsForWaitingArea(
      String waitingAreaSourceId,
      Geometry waitingAreaGeometry,
      MacroscopicLink accessLink,
      String accessLinkSourceId,
      Node node,
      Mode accessMode,
      String countryName,
      boolean mustAvoidCrossingTraffic,
      Function<String,String> getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,
      Function<Node,String> getOverwrittenWaitingAreaSourceId,
      PlanitJtsCrsUtils geoUtils) {

    /* potential link segments based on mode compatibility and access link restriction */
    List<LinkSegment> accessLinkSegments = new ArrayList<>(4);
    for (var linkSegment : node.<LinkSegment>getEntryLinkSegments()) {
      if (linkSegment.isModeAllowed(accessMode) && (linkSegment.getParent().idEquals(accessLink))) {
        accessLinkSegments.add(linkSegment);
      }
    }

    if (accessLinkSegments == null || accessLinkSegments.isEmpty()) {
      return accessLinkSegments;
    }

    /* user overwrite checks and special treatment */
    boolean removeInvalidAccessLinkSegmentsIfNoMatchLeft = !isOverwriteActive(
        waitingAreaSourceId,node, accessLinkSourceId, getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,getOverwrittenWaitingAreaSourceId);

    /* accessible link segments for planit node based on relative location of waiting area compared to infrastructure*/
    if(mustAvoidCrossingTraffic) {

      boolean isLeftHandDrive = DrivingDirectionDefaultByCountry.isLeftHandDrive(countryName);
      Collection<LinkSegment> toBeRemovedAccessLinkSegments =
          identifyLinkSegmentsOnWrongSideOf(waitingAreaGeometry, accessLinkSegments, isLeftHandDrive, geoUtils);

      if(!toBeRemovedAccessLinkSegments.isEmpty() &&
          (removeInvalidAccessLinkSegmentsIfNoMatchLeft || toBeRemovedAccessLinkSegments.size() < accessLinkSegments.size())) {
        /* filter because "normal" situation or there are still matches left even after filtering despite the explicit user override for this  combination */
        accessLinkSegments.removeAll(toBeRemovedAccessLinkSegments);
      }
      /* else  keep the access link segments so far */
    }
    return accessLinkSegments;
  }

  /** Verify if given link segment is potentially viable as access link segment for the given extreme node and access mode, taking into account
   * any explicit overwrites that may exist that are not bounded by any limitations on compatibility.
   *
   * @param waitingAreaSourceId we're checking for
   * @param waitingAreaGeometry we're checking for
   * @param accessLinkSegment nominated
   * @param accessLinkSourceId original source id of the PLANit access link
   * @param accessNode that is nominated
   * @param accessMode used
   * @param getOverwrittenWaitingAreaSourceId function that provides the overwritten waiting area source id for a given access node, maybe return null if not overwritten
   * @param getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId mapping from waiting area source id to nominated access link source id, which if a match to provided access link source id
   *                                                               makes sure that even if there exists crossing traffic this does not render its access link segment(s) invalid, may be null
   * @param countryName to extract driving direction from
   * @param geoUtils gis functionality to apply in finding connectoid location
   * @return true when at least one valid access link segment exists, false otherwise
   */
  public static boolean isPotentialAccessEntryLinkSegmentForWaitingArea(
      String waitingAreaSourceId,
      Geometry waitingAreaGeometry,
      final MacroscopicLinkSegment accessLinkSegment,
      String accessLinkSourceId,
      final Node accessNode,
      final Mode accessMode,
      final Function<Node,String> getOverwrittenWaitingAreaSourceId,
      final Function<String,String> getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,
      String countryName,
      final PlanitJtsCrsUtils geoUtils) {

    boolean mustAvoidCrossingTraffic =
        isAvoidCrossTrafficForAccessModeOrAccessNodeWaitingAreaOverwritten(
            accessMode, waitingAreaSourceId, accessNode, getOverwrittenWaitingAreaSourceId);

    /* user overwrite checks and special treatment */
    boolean removeInvalidAccessLinkSegment = !isOverwriteActive(
        waitingAreaSourceId, accessLinkSegment.getDownstreamNode(), accessLinkSourceId, getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,getOverwrittenWaitingAreaSourceId);
    if(!removeInvalidAccessLinkSegment){
      return true;
    }

    /* accessible link segments for planit node based on relative location of waiting area compared to infrastructure*/
    if(mustAvoidCrossingTraffic) {
      boolean isLeftHandDrive = DrivingDirectionDefaultByCountry.isLeftHandDrive(countryName);
      boolean onWrongSide = !identifyLinkSegmentsOnWrongSideOf(
          waitingAreaGeometry, Collections.singleton(accessLinkSegment), isLeftHandDrive, geoUtils).isEmpty();

      if(onWrongSide) {
        return false;
      }
    }
    return true;
  }

  /** Verify if any valid access link segments exist for the given combination of link, one of its extreme nodes, and the access mode, taking into account
   * any explicit overwrites that may exist that are not bounded by any limitations on compatibility.
   *
   * @param waitingAreaSourceId we're checking for
   * @param waitingAreaGeometry we're checking for
   * @param accessLink nominated
   * @param accessLinkSourceId original source id of the PLANit access link
   * @param accessNode that is nominated
   * @param accessMode used
   * @param getOverwrittenWaitingAreaSourceId function that provides the overwritten waiting area source id for a given access node, maybe return null if not overwritten
   * @param getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId mapping from waiting area source id to nominated access link source id, which if a match to provided access link source id
   *                                                               makes sure that even if there exists crossing traffic this does not render its access link segment(s) invalid, may be null
   * @param countryName to extract driving direction from
   * @param geoUtils gis functionality to apply in finding connectoid location
   * @return true when at least one valid access link segment exists, false otherwise
   */
  public static boolean hasWaitingAreaPotentialAccessLinkSegmentForLinkNodeModeCombination(
      String waitingAreaSourceId,
      Geometry waitingAreaGeometry,
      final MacroscopicLink accessLink,
      String accessLinkSourceId,
      final Node accessNode,
      final Mode accessMode,
      final Function<Node,String> getOverwrittenWaitingAreaSourceId,
      final Function<String,String> getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,
      String countryName,
      final PlanitJtsCrsUtils geoUtils) {

    boolean mustAvoidCrossingTraffic =
        isAvoidCrossTrafficForAccessModeOrAccessNodeWaitingAreaOverwritten(
            accessMode, waitingAreaSourceId, accessNode, getOverwrittenWaitingAreaSourceId);

    /* now collect the available access link segments (if any) - switch of logging of issues, since we are only interested in determining if this is feasible, we are not creating anything yet */
    Collection<LinkSegment> accessLinkSegments =
        findAccessEntryLinkSegmentsForWaitingArea(
            waitingAreaSourceId, waitingAreaGeometry, accessLink, accessLinkSourceId, accessNode, accessMode, countryName, mustAvoidCrossingTraffic, getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, getOverwrittenWaitingAreaSourceId, geoUtils);

    return !accessLinkSegments.isEmpty();
  }

  /** find a suitable connectoid location on the given link based on the constraints that it must be able to reside on a link segment that is in the correct relative position
   * to the transfer zone and supports the access mode on at least one of the designated link segment(s) that is eligible (if any). If not null is returned, otherwise the returned
   * location may be an existing extreme node's location, or a location internal to the link if the extreme node does not fall within the constraints provided
   *
   * @param waitingAreaSourceId supplies a relevant source id of the waiting area in question to use for exceptions or logging when needed
   * @param waitingAreaGeometry to find location for (which is either sourced from a PLANit transfer zone, or to be created transfer zone) that will reflect this waiting area)
   * @param accessLinkSegment to find location on
   * @param accessLinkSourceId the access link's source id
   * @param accessMode to be compatible with
   * @param maxAllowedDistanceMeters the maximum allowed distance between stop and waiting area that we allow
   * @param getOverwrittenWaitingAreaSourceIdForNode function that provides the overwritten waiting area source id for a given access node, maybe return null if not overwritten
   * @param getOverwrittenWaitingAreaSourceIdForPoint function that provides the overwritten waiting area source id for a given access node, maybe return null if not overwritten
   * @param getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId mapping from waiting area source id to nominated access link source id, which if a match to provided access link source id
   *                                                               makes sure that even if there exists crossing traffic this does not render its access link segment(s) invalid, may be null
   * @param countryName to extract driving direction from
   * @param geoUtils gis functionality to apply in finding connectoid location
   * @return found location either existing node or projected location that is nearest and does not exist as a shape point on the link yet, or null if no valid position could be found
   */
  public static Point findConnectoidLocationForWaitingAreaOnLinkSegment(
      String waitingAreaSourceId,
      final Geometry waitingAreaGeometry,
      final MacroscopicLinkSegment accessLinkSegment,
      final String accessLinkSourceId,
      final Mode accessMode,
      double maxAllowedDistanceMeters,
      final Function<Node,String> getOverwrittenWaitingAreaSourceIdForNode,
      final Function<Point,String> getOverwrittenWaitingAreaSourceIdForPoint,
      final Function<String,String> getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,
      String countryName,
      final PlanitJtsCrsUtils geoUtils) {

    /* when override, no restrictinos are placed on a match, as long as a location can be created */
    boolean isOverride =  getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId!= null &&
        getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId.apply(waitingAreaSourceId) != null &&
        getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId.apply(waitingAreaSourceId).equals(accessLinkSourceId);

    var accessLinkSegmentGeometry = accessLinkSegment.getParentLink().getGeometry();
    /* exclude the extreme node at the upstream end because if this is access node, the preceding access link segment should be chosen */
    int startIndex = accessLinkSegment.isParentGeometryInSegmentDirection(false) ? 1 : 0;
    int endIndex = accessLinkSegment.isParentGeometryInSegmentDirection(false) ? accessLinkSegmentGeometry.getNumPoints()-1 : accessLinkSegmentGeometry.getNumPoints()-2;
    Coordinate closestExistingCoordinate = geoUtils.getClosestExistingLineStringCoordinateToGeometry(
        waitingAreaGeometry, accessLinkSegmentGeometry, startIndex, endIndex);
    double distanceToExistingCoordinateOnLinkInMeters = geoUtils.getClosestDistanceInMeters(closestExistingCoordinate, waitingAreaGeometry);

    /* 1) verify if extreme node is deemed acceptable*/
    if(accessLinkSegment.getDownstreamVertex().isPositionEqual2D(closestExistingCoordinate, Precision.EPSILON_6) &&
        (distanceToExistingCoordinateOnLinkInMeters < maxAllowedDistanceMeters)){

      if(isPotentialAccessEntryLinkSegmentForWaitingArea(
          waitingAreaSourceId, waitingAreaGeometry, accessLinkSegment, accessLinkSourceId, accessLinkSegment.getDownstreamVertex(), accessMode, getOverwrittenWaitingAreaSourceIdForNode, getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, countryName, geoUtils)) {
        return PlanitJtsUtils.createPoint(closestExistingCoordinate);
      }else{
        return null;
      }
    }

    /* 2) if close enough utilise internal node location as stop_position/connectoid, otherwise create artificial point on closest projected location which
     * in most cases will be closer and within threshold */
    LineSegment lineSegmentForLocation = null;
    if(distanceToExistingCoordinateOnLinkInMeters < maxAllowedDistanceMeters){
      var linkCoordinates = accessLinkSegmentGeometry.getCoordinates();
      final int coordinateIndex = PlanitJtsUtils.getCoordinateIndexOf(closestExistingCoordinate, linkCoordinates);
      if(coordinateIndex <= 0 || coordinateIndex==(accessLinkSegmentGeometry.getCoordinates().length-1)) {
        throw new PlanItRunTimeException("Unable to locate link internal location even though it is expected to exist when identifying connectoid location for waiting area %s", waitingAreaSourceId);
      }
      lineSegmentForLocation = new LineSegment(linkCoordinates[coordinateIndex-1], linkCoordinates[coordinateIndex]);
    }
    /* 3) too far, check if breaking the existing link in appropriate location instead would work */
    else{
      LinearLocation projectedLinLocOnLink = PlanitEntityGeoUtils.extractClosestProjectedLinearLocationToGeometryFromEdge(
          waitingAreaGeometry, accessLinkSegment.getParentLink(), geoUtils);

      /* verify projected location is valid */
      Coordinate closestProjectedCoordinate = projectedLinLocOnLink.getCoordinate(accessLinkSegmentGeometry);
      if( !closestExistingCoordinate.equals2D(closestProjectedCoordinate) &&
          (isOverride || geoUtils.getClosestDistanceInMeters(closestProjectedCoordinate, waitingAreaGeometry) <= maxAllowedDistanceMeters)) {

        /* acceptable location that is internal, create proposed line segment location as result and update chosen coordinate*/
        lineSegmentForLocation = new LineSegment(
            accessLinkSegmentGeometry.getCoordinates()[projectedLinLocOnLink.getSegmentIndex()], closestProjectedCoordinate);
      }
    }

    if(lineSegmentForLocation ==null) {
      return null;
    }

    var chosenCoordinate = lineSegmentForLocation.getCoordinate(1);
    Point connectoidLocation = PlanitJtsUtils.createPoint(chosenCoordinate);
    if(isOverride){
      return connectoidLocation;
    }

    /* if location dependent check the result, otherwise found location is accepted */
    boolean mustAvoidCrossingTraffic =
        isAvoidCrossTrafficForAccessModeOrAccessNodeWaitingAreaOverwritten(
            accessMode, waitingAreaSourceId, connectoidLocation, getOverwrittenWaitingAreaSourceIdForPoint);
    if(!mustAvoidCrossingTraffic){
      return connectoidLocation;
    }

    var isLeftOfWaitingArea = isWaitingAreaLeftOfAccessLineSegment(waitingAreaGeometry, accessLinkSegment, lineSegmentForLocation, geoUtils);
    return (isLeftOfWaitingArea == DrivingDirectionDefaultByCountry.isLeftHandDrive(countryName)) ? connectoidLocation : null;
  }

  /** find a suitable connectoid location on the given link based on the constraints that it must be able to reside on a link segment that is in the correct relative position
   * to the transfer zone and supports the access mode on at least one of the designated link segment(s) that is eligible (if any). If not null is returned, otherwise the returned
   * location may be an existing extreme node's location, or a location internal to the link if the extreme node does not fall within the constraints provided
   *
   * @param waitingAreaSourceId supplies a relevant source id of the waiting area in question to use for exceptions or logging when needed
   * @param waitingAreaGeometry to find location for (which is either sourced from a PLANit transfer zone, or to be created transfer zone) that will reflect this waiting area)
   * @param accessLink to find location on
   * @param accessLinkSourceId the access link's source id
   * @param accessMode to be compatible with
   * @param maxAllowedDistanceMeters the maximum allowed distance between stop and waiting area that we allow
   * @param getOverwrittenWaitingAreaSourceIdForNode function that provides the overwritten waiting area source id for a given access node, maybe return null if not overwritten
   * @param getOverwrittenWaitingAreaSourceIdForPoint function that provides the overwritten waiting area source id for a given access node, maybe return null if not overwritten
   * @param getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId mapping from waiting area source id to nominated access link source id, which if a match to provided access link source id
   *                                                               makes sure that even if there exists crossing traffic this does not render its access link segment(s) invalid, may be null
   * @param countryName to extract driving direction from
   * @param geoUtils gis functionality to apply in finding connectoid location
   * @return found location either existing node or projected location that is nearest and does not exist as a shape point on the link yet, or null if no valid position could be found
   */
  @Deprecated // usage should be replaced by using the link segment equivalent above #findConnectoidLocationForWaitingAreaOnLinkSegment
  public static Point findConnectoidLocationForWaitingAreaOnLink(
      String waitingAreaSourceId,
      final Geometry waitingAreaGeometry,
      final MacroscopicLink accessLink,
      final String accessLinkSourceId,
      final Mode accessMode,
      double maxAllowedDistanceMeters,
      final Function<Node,String> getOverwrittenWaitingAreaSourceIdForNode,
      final Function<Point,String> getOverwrittenWaitingAreaSourceIdForPoint,
      final Function<String,String> getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,
      String countryName,
      final PlanitJtsCrsUtils geoUtils) {

    Coordinate closestExistingCoordinate = geoUtils.getClosestExistingLineStringCoordinateToGeometry(
        waitingAreaGeometry, accessLink.getGeometry());
    double distanceToExistingCoordinateOnLinkInMeters = geoUtils.getClosestDistanceInMeters(closestExistingCoordinate, waitingAreaGeometry);

    /* if close enough utilise existing node location as stop_position/connectoid, otherwise create artificial point on closest projected location which
     * in most cases will be closer and within threshold */
    Point connectoidLocation = null;
    if(distanceToExistingCoordinateOnLinkInMeters < maxAllowedDistanceMeters) {

      /* close enough, see if it can be reused:
       * 1) node is an extreme node
       * 2) or node is internal to link
       * */

      /* 1) verify if extreme node */
      if(accessLink.getVertexA().isPositionEqual2D(closestExistingCoordinate, Precision.EPSILON_6)) {
        /* because it is an extreme node there is only one of the two directions accessible since an access link segments are assumed to be directly upstream of the node. This
         * can result in choosing a connectoid location that is not feasible when only considering the proximity and not the link segment specific information such as the mode
         * and relative location to the transfer zone (left or right of the road). Therefore, we must check this here before accepting this pre-existing extreme node. If this is a problem,
         * we do not create the location on the existing location, but instead choose a location on the link so that we can use the access link segment in the opposite direction instead */
        if(hasWaitingAreaPotentialAccessLinkSegmentForLinkNodeModeCombination(
            waitingAreaSourceId, waitingAreaGeometry, accessLink, accessLinkSourceId, accessLink.getNodeA(), accessMode, getOverwrittenWaitingAreaSourceIdForNode, getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, countryName, geoUtils)) {
          connectoidLocation = PlanitJtsUtils.createPoint(closestExistingCoordinate);
        }
      }else if(accessLink.getVertexB().isPositionEqual2D(closestExistingCoordinate, Precision.EPSILON_6)) {
        if(hasWaitingAreaPotentialAccessLinkSegmentForLinkNodeModeCombination(
            waitingAreaSourceId, waitingAreaGeometry, accessLink, accessLinkSourceId, accessLink.getNodeB(), accessMode, getOverwrittenWaitingAreaSourceIdForNode, getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, countryName, geoUtils)){
          connectoidLocation = PlanitJtsUtils.createPoint(closestExistingCoordinate);
        }
      }else {

        /* 2) must be internal if not an extreme node */
        int coordinateIndex = PlanitJtsUtils.getCoordinateIndexOf(closestExistingCoordinate, accessLink.getGeometry().getCoordinates());
        if(coordinateIndex <= 0 || coordinateIndex==(accessLink.getGeometry().getCoordinates().length-1)) {
          throw new PlanItRunTimeException("Unable to locate link internal location even though it is expected to exist when identifying connectoid location for waiting area %s", waitingAreaSourceId);
        }

        connectoidLocation = PlanitJtsUtils.createPoint(closestExistingCoordinate);
        if(!isWaitingAreaAccessLinkSegmentInternalLocationModeCombinationDirectionallyValid(
              waitingAreaSourceId, waitingAreaGeometry, accessLink, connectoidLocation, accessMode, getOverwrittenWaitingAreaSourceIdForPoint, countryName, geoUtils)) {
          /* special case: if one way link and internal existing coordinate chosen results in waiting area on the wrong side of geometry (due to bend in the road directly
           * preceding the location (and mode is susceptible to waiting area location). Then we do not accept this existing coordinate and instead try
           * to use projected location not residing at this (possible) bend, but in between existing coordinates on straight section of road (hopefully), therefore
           * reset location and continue */
          connectoidLocation=null;
        }
      }
    }

    if(connectoidLocation == null) {
      /* too far, or identified existing location is not suitable, so we must check if breaking the existing link in appropriate location instead would work */
      LinearLocation projectedLinLocOnLink = PlanitEntityGeoUtils.extractClosestProjectedLinearLocationToGeometryFromEdge(waitingAreaGeometry, accessLink, geoUtils);

      /* verify projected location is valid */
      Coordinate closestProjectedCoordinate = projectedLinLocOnLink.getCoordinate(accessLink.getGeometry());
      if( closestExistingCoordinate.equals2D(closestProjectedCoordinate) ||
          geoUtils.getClosestDistanceInMeters(closestProjectedCoordinate, waitingAreaGeometry) > maxAllowedDistanceMeters) {
        /* no option to break link, the projected closest point is too far away or deemed not suitable */
      }else {
        /* acceptable location, create proposed connectoid location as result */
        connectoidLocation = PlanitJtsUtils.createPoint(closestProjectedCoordinate);
      }
    }

    return connectoidLocation;
  }

  /** create directed connectoid for the link segment provided, all related to the given transfer zone and with access modes provided. When the link segment does not have any of the
   * passed in modes listed as allowed, no connectoid is created and null is returned
   *
   * @param zoning to register on
   * @param accessZone to relate connectoids to
   * @param downstreamAccessNode when true access node is chosen as the downstream node, when false, upstream node is chosen
   * @param linkSegment to create connectoid for
   * @param allowedModes used for the connectoid
   * @return created connectoid when at least one of the allowed modes is also allowed on the link segment
   */
  public static DirectedConnectoid createAndRegisterDirectedConnectoid(
      Zoning zoning, final TransferZone accessZone, final boolean downstreamAccessNode, final MacroscopicLinkSegment linkSegment, final Set<Mode> allowedModes){
    final Set<Mode> realAllowedModes = linkSegment.getAllowedModesFrom(allowedModes);
    if(realAllowedModes!= null && !realAllowedModes.isEmpty()) {
      var connectoid =
          zoning.getTransferConnectoids().getFactory().registerNew(downstreamAccessNode, linkSegment, accessZone, true, realAllowedModes);
      return connectoid;
    }
    return null;
  }

  /** create directed connectoids, one per link segment provided, all related to the given transfer zone and with access modes provided. connectoids are only created
   * when the access link segment has at least one of the allowed modes as an eligible mode
   *
   * @param zoning to register on
   * @param transferZone to relate connectoids to
   * @param accessNode the access node the connectoid utilises (determine the up/downstream connection of the attached link segment(s)
   * @param linkSegments to create connectoids for (one per segment)
   * @param allowedModes used for each connectoid
   * @return created connectoids
   */
  public static Collection<DirectedConnectoid> createAndRegisterDirectedConnectoids(
      Zoning zoning,
      final TransferZone transferZone,
      final Node accessNode,
      final Iterable<? extends MacroscopicLinkSegment> linkSegments,
      final Set<Mode> allowedModes){
    Set<DirectedConnectoid> createdConnectoids = new HashSet<>();

    // we sort to ensure connectoids are always created in same deterministic order
    IterableUtils.asStream(linkSegments).sorted(Comparator.comparing(MacroscopicLinkSegment::getId)).forEach( linkSegment -> {
      boolean downstreamAccessNode = linkSegment.isDownstreamNode(accessNode);
      if(!linkSegment.hasNode(accessNode)){
        throw new PlanItRunTimeException("Chosen access node %s not attached to link segment %s", accessNode.getIdsAsString(), linkSegment.getIdsAsString());
      }
      DirectedConnectoid newConnectoid = createAndRegisterDirectedConnectoid(
          zoning, transferZone, downstreamAccessNode, linkSegment, allowedModes);
      if(newConnectoid != null) {
        createdConnectoids.add(newConnectoid);
      }
    });

    return createdConnectoids;
  }
}
