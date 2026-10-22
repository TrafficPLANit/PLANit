package org.goplanit.converter.zoning;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitEntityGeoUtils;
import org.goplanit.utils.geo.PlanitGraphGeoUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.ExternalIdAbleUtils;
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
import org.goplanit.utils.zoning.*;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LinearLocation;

import javax.annotation.Nullable;

/**
 * Utilities regarding Zoning conversions that might be useful for implementations supporting the mapping of
 * non-PLANit geometry based entities to PLANit transfer zones and connectoids
 *
 * @author markr
 *
 */
public class ZoningConverterUtils {

  /** an end node within this distance of a stop's ideal stop point is reused rather than breaking the link, see
   * {@link #findStopLocationForWaitingAreaOnLinkSegment} */
  public static final double DEFAULT_MAX_END_NODE_TO_IDEAL_STOP_POINT_METERS = 10;

  /** when breaking a link for a stop, an inner shape point within this distance of its ideal stop point is used rather
   * than breaking it right next to it, see {@link #findStopLocationForWaitingAreaOnLinkSegment} */
  public static final double DEFAULT_MAX_SHAPE_POINT_TO_IDEAL_STOP_POINT_METERS = 5;

  /** Verify if based on passed in parameters either of the two overrides is active
   *
   * @param waitingAreaSourceId these link segments pertain to
   * @param node to verify
   * @param accessLinkSourceId source id of the access link used, this will be matched against the overwritten access
   *                           link source id (if provided)
   * @param getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId mapping from waiting area source id to nominated
   *                                                               access link source id, which if a match to provided
   *                                                               access link source id
   *                                                               makes sure that even if there exists crossing
   *                                                               traffic this does not render its access link
   *                                                               segment(s) invalid, may be null
   * @param getOverwrittenWaitingAreaSourceId function that provides the overwritten waiting area source id for a
   *                                          given access node, maybe return null if not overwritten
   * @return found link segments that are deemed valid given the constraints
   */
  private static boolean isOverwriteActive(
      final String waitingAreaSourceId,
      final Node node,
      String accessLinkSourceId,
      final Function<String,String> getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,
      final Function<Node,String> getOverwrittenWaitingAreaSourceId){
    boolean isOverwriteActive = false;
    /* in both cases: When a match, we must use the user overwrite value. We will still try to remove access link
    segments
     * that are invalid, but if due to this check no matches remain, we revert this and instead use all entry
     link segments on the osm way
     * since the user has indicated to explicitly use this combination which overrules the automatic filter we
     would ordinarily apply */

    /* stopLocation point -> waiting area overwrite */
    isOverwriteActive = ExternalIdAbleUtils.isMappedToId(getOverwrittenWaitingAreaSourceId, node, waitingAreaSourceId);
    /* waiting area -> link (source id) overwrite */
    isOverwriteActive = isOverwriteActive || ExternalIdAbleUtils.isMappedToId(
        getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, waitingAreaSourceId, accessLinkSourceId);
    return isOverwriteActive;
  }

  /**
   * Of several candidate links about equally close to a waiting area, prefer the highest capacity for a road access
   * mode, the premise being that public transport services tend to run on main roads. Access segments with a lower
   * capacity than the highest are dropped, and a link stays while it has an access segment left. Nothing is dropped
   * for other modes, or when the access segments all have the same capacity
   *
   * @param links the candidates
   * @param accessLinkSegments the candidates' access link segments, from which lower capacity ones are removed
   * @param firstAccessMode the access mode that decides whether the highest capacity is preferred
   * @return the remaining candidate links
   */
  private static Set<MacroscopicLink> keepHighestCapacityCandidates(
      Set<MacroscopicLink> links,
      SortedSet<MacroscopicLinkSegment> accessLinkSegments,
      Mode firstAccessMode) {
    if(firstAccessMode.getPhysicalFeatures().getTrackType() != TrackModeType.ROAD || accessLinkSegments.stream().map(
        MacroscopicLinkSegment::getCapacityOrDefaultPcuHLane).distinct().count() <= 1) {
      return links;
    }

    double maxCapacity = accessLinkSegments.stream().mapToDouble(
        MacroscopicLinkSegment::getCapacityOrDefaultPcuHLane).max().getAsDouble();
    accessLinkSegments.removeIf(linkSegment -> Precision.smaller(
        linkSegment.getCapacityOrDefaultPcuHLane(), maxCapacity, Precision.EPSILON_6));
    return accessLinkSegments.stream().map(MacroscopicLinkSegment::getParent).collect(Collectors.toSet());
  }

  /**
   * Verify if a candidate is nearer than the one chosen so far, or equally near while having a lower id
   *
   * @param meters of the candidate
   * @param id of the candidate
   * @param chosenMeters of the candidate chosen so far, infinite when none
   * @param chosenId of the candidate chosen so far
   * @return true when nearer, or equally near with a lower id, false otherwise
   */
  private static boolean isNearerOrEquallyNearWithLowerId(double meters, long id, double chosenMeters, long chosenId) {
    return Precision.smaller(meters, chosenMeters, Precision.EPSILON_6) ||
        (Precision.equal(meters, chosenMeters, Precision.EPSILON_6) && id < chosenId);
  }

  /**
   * The end of the given piece of an access link segment's link as stop location, when the waiting area lies on the
   * side of the road its access mode needs. Any side will do for a user nominated access link, a mode that may cross
   * traffic, or a waiting area the user mapped to the location
   *
   * @param waitingAreaSourceId the waiting area's source id
   * @param waitingAreaGeometry the waiting area's geometry
   * @param accessLinkSegment the piece lies on
   * @param accessMode the stop location is for
   * @param piece of the link, in the link's geometry direction, ending at the stop location
   * @param isNominatedAccessLink whether the user nominated the access link for the waiting area
   * @param getOverwrittenWaitingAreaSourceIdForPoint function that provides the overwritten waiting area source id
   *                                                  for a given location, may return null if not overwritten
   * @param countryName to extract driving direction from
   * @param geoUtils to use
   * @return the location, null when the waiting area lies on the wrong side of the road
   */
  private static Point locationAtLineSegmentEndOnWaitingAreaSideOfRoad(
      String waitingAreaSourceId,
      final Geometry waitingAreaGeometry,
      final MacroscopicLinkSegment accessLinkSegment,
      final Mode accessMode,
      final LineSegment piece,
      boolean isNominatedAccessLink,
      final Function<Point,String> getOverwrittenWaitingAreaSourceIdForPoint,
      String countryName,
      final PlanitJtsCrsUtils geoUtils) {
    Point connectoidLocation = PlanitJtsUtils.createPoint(piece.getCoordinate(1));
    if(isNominatedAccessLink){
      return connectoidLocation;
    }

    /* if location dependent check the result, otherwise found location is accepted */
    boolean mustAvoidCrossingTraffic =
        isAvoidCrossTrafficForAccessModeOrAccessNodeWaitingAreaOverwritten(
            accessMode, waitingAreaSourceId, connectoidLocation, getOverwrittenWaitingAreaSourceIdForPoint);
    if(!mustAvoidCrossingTraffic){
      return connectoidLocation;
    }

    var isLeftOfWaitingArea =
        isWaitingAreaLeftOfAccessLineSegment(waitingAreaGeometry, accessLinkSegment, piece, geoUtils);
    return (isLeftOfWaitingArea ==
        DrivingDirectionDefaultByCountry.isLeftHandDrive(countryName)) ? connectoidLocation : null;
  }

  /**
   * Verify if the access mode can arrive at the upstream node of an access link segment along exactly one other link,
   * with the waiting area on the side of the road the mode needs where it must not cross traffic, see
   * {@link #findAccessEntryLinkSegmentsForWaitingArea}, and can leave that node only along the segment, turning back
   * along the link it arrived on aside. A stop at that node then serves the segment's vehicles before they set off
   * along it. When the mode can arrive there along more than one other link, for example at a junction, it cannot be
   * told which of them the vehicles stopping at the waiting area come from. When it can also leave along another link,
   * for example where a road splits, a vehicle stopping there may never pass the waiting area. Either way the node is
   * not used
   *
   * @param waitingAreaSourceId the waiting area's source id
   * @param waitingAreaGeometry the waiting area's geometry
   * @param accessLinkSegment whose upstream node is checked
   * @param accessMode to check for
   * @param getOverwrittenWaitingAreaSourceIdForNode function that provides the overwritten waiting area source id
   *                                                 for a given access node, may return null if not overwritten
   * @param getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId mapping from waiting area source id to the access
   *                                                               link source id the user nominated, may be null
   * @param countryName to extract driving direction from
   * @param geoUtils to use
   * @return true when exactly one other link has a segment the mode may use entering the node, it is such an access
   * link segment, and no link but that one and the segment's own has a segment the mode may use leaving the node, false
   * otherwise
   */
  private static boolean isUpstreamNodeOneOtherLinkInAndOnlySegmentOutForWaitingArea(
      String waitingAreaSourceId,
      final Geometry waitingAreaGeometry,
      final MacroscopicLinkSegment accessLinkSegment,
      final Mode accessMode,
      final Function<Node,String> getOverwrittenWaitingAreaSourceIdForNode,
      final Function<String,String> getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,
      String countryName,
      final PlanitJtsCrsUtils geoUtils) {
    Node upstreamNode = accessLinkSegment.getUpstreamNode();
    MacroscopicLink onlyEnteringLink = null;
    for(var entrySegment : upstreamNode.<LinkSegment>getEntryLinkSegments()) {
      if(entrySegment.getParent().idEquals(accessLinkSegment.getParent()) || !entrySegment.isModeAllowed(accessMode)) {
        continue;
      }
      if(onlyEnteringLink != null) {
        return false;
      }
      onlyEnteringLink = (MacroscopicLink) entrySegment.getParent();
    }
    if(onlyEnteringLink == null) {
      return false;
    }
    for(var exitSegment : upstreamNode.<LinkSegment>getExitLinkSegments()) {
      if(exitSegment.isModeAllowed(accessMode) && !exitSegment.getParent().idEquals(accessLinkSegment.getParent()) &&
          !exitSegment.getParent().idEquals(onlyEnteringLink)) {
        return false;
      }
    }
    boolean mustAvoidCrossingTraffic = isAvoidCrossTrafficForAccessModeOrAccessNodeWaitingAreaOverwritten(
        accessMode, waitingAreaSourceId, upstreamNode, getOverwrittenWaitingAreaSourceIdForNode);
    var enteringSegments = findAccessEntryLinkSegmentsForWaitingArea(waitingAreaSourceId, waitingAreaGeometry,
        onlyEnteringLink, null, upstreamNode, accessMode, countryName, mustAvoidCrossingTraffic,
        getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, getOverwrittenWaitingAreaSourceIdForNode, geoUtils);
    return enteringSegments != null && !enteringSegments.isEmpty();
  }

  /** Verify if the provided line segment (somewhere along the geometry of the access link segment)
   * resides on the correct side
   * of the waiting area location, assuming this matters. The side is judged from the waiting area's centre, see
   * {@link PlanitJtsUtils#extractCentre(Geometry)}, so a long platform is judged by its middle rather than by an end
   * that happens to come nearer to the road
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
    boolean reverseLinearLocationGeometry =
        accessLinkSegment.isDirectionAb()!=accessLinkSegment.getParent().isGeometryInAbDirection();
    if(reverseLinearLocationGeometry) {
      localLineSegment.reverse();
    }
    return geoUtils.isGeometryLeftOf(
        PlanitJtsUtils.extractCentre(waitingAreaGeometry), localLineSegment.p0, localLineSegment.p1);
  }

  /** Verify if the waiting area for an access mode's access link(segment) must be on the logical relative location
   * (left hand side for left hand drive) or not.
   * In case the mapping is overwritten it is assumed the driving direction does not matter as it is user defined
   * to be explicitly mapped.
   *
   * @param <T> type of access entity
   * @param accessMode to check
   * @param waitingAreaSourceId required to check if user overwrite is present for this waiting area, may be null to
   * @param accessEntity the access entity that may be mapped to the waiting area under investigation, may be null
   *                     if not available
   * @param getOverwrittenWaitingAreaSourceId function that provides the overwritten waiting area source id for a
   *                                          given access node, maybe return null if not overwritten
   * @return true when restricted for driving direction, false otherwise
   */
  public static <T> boolean isAvoidCrossTrafficForAccessModeOrAccessNodeWaitingAreaOverwritten(
      final Mode accessMode,
      final String waitingAreaSourceId,
      T accessEntity,
      Function<T,String> getOverwrittenWaitingAreaSourceId) {

    if(accessEntity != null && ExternalIdAbleUtils.hasMappedId(getOverwrittenWaitingAreaSourceId, accessEntity)) {
      /* ... exception : user override with mapping to this zone for this node, in which case we allow crossing
      traffic regardless */
      return !ExternalIdAbleUtils.isMappedToId(getOverwrittenWaitingAreaSourceId, accessEntity, waitingAreaSourceId);
    }
    return isAvoidCrossTrafficForAccessMode(accessMode);
  }

  /** Verify if the waiting area for an access mode's access link(segment) must be on the logical relative location
   * (left hand side for left hand drive) or not.
   *
   * @param accessMode to check
   * @return true when restricted for driving direction, false otherwise
   */
  public static boolean isAvoidCrossTrafficForAccessMode(final Mode accessMode) {
    return accessMode.getPhysicalFeatures().getTrackType().equals(TrackModeType.ROAD);
  }

  /** create a subset of links from the passed in ones, removing all links for which we can be certain that geometry
   * is located on the wrong side of the road infrastructure geometry. Note that
   * rails is not excluded in this exercise, i.e., it is assumed rail lines ar bidirectional.
   * Right or wrong side is verified by checking if the link is one-way. If so, we can be sure (based on the driving
   * direction of the country) if the geometry is located to the closest by (logical)
   * driving direction given the placement of the geometry, i.e., on the left hand side for left hand drive countries,
   * on the right hand side for right hand driving countries. The side is judged from the location's centre, see
   * {@link PlanitJtsUtils#extractCentre(Geometry)}
   *
   * @param location representing the location of concern expected to have access to the link
   * @param links to remove in-eligible ones from
   * @param isLeftHandDrive flag
   * @param accessModes to consider
   * @param geoUtils to use
   * @return remaining links that are deemed eligible
   */
  public static Collection<MacroscopicLink> excludeLinksOnWrongSideOf(
      Geometry location,
      Collection<MacroscopicLink> links,
      boolean isLeftHandDrive,
      Collection<? extends Mode> accessModes,
      PlanitJtsCrsUtils geoUtils) {

    Collection<MacroscopicLink> matchedLinks = new HashSet<>(links);
    for(var link : links) {
      for(Mode accessMode : accessModes){

        /* road based PT modes are only accessible on one side, so they must stop with the waiting area in the
        correct driving direction, i.e., must avoid cross traffic, because otherwise they
         * have no doors at the right side, e.g., travellers have to cross the road to get to the vehicle, which
         should not happen */
        boolean mustAvoidCrossingTraffic = ZoningConverterUtils.isAvoidCrossTrafficForAccessMode(accessMode);

        LinkSegment oneWayLinkSegment = link.getLinkSegmentIfLinkIsOneWayForMode(accessMode);
        if(oneWayLinkSegment != null && mustAvoidCrossingTraffic) {
          /* determine location relative to infrastructure, at its line geometry closest to the location's centre */
          if(PlanitGraphGeoUtils.isGeometryLeftOf(
              PlanitJtsUtils.extractCentre(location), oneWayLinkSegment, geoUtils) != isLeftHandDrive) {
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
   * When transfer zone location differs from the connectoid location determine on which side of the
   * infrastructure it exists and based on the country's driving direction
   * and access mode determine the access link segments. The side is judged from the location's centre, see
   * {@link PlanitJtsUtils#extractCentre(Geometry)}
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
    for (LinkSegment linkSegment : accessLinkSegments) {
      /* determine location relative to infrastructure, at its line geometry closest to the location's centre */
      if (PlanitGraphGeoUtils.isGeometryLeftOf(
          PlanitJtsUtils.extractCentre(location), linkSegment, geoUtils) != leftHandDrive) {
        /* not viable opposite traffic directions needs to be crossed on the link to get to stop location --> remove */
        invalidAccessLinkSegments.add(linkSegment);
      }
    }
    return invalidAccessLinkSegments;
  }

    /**
     * Exclude the closest link if it is situated on the wrong side of the road, then if links are remaining keep
     * going with removing the then closest if it is also
     * on the wrong side of the road etc.
     *
     * @param location to verify against, typically a transfer zone location
     * @param links to update
     * @param isLeftHandDrive network driving direction
     * @param accessModes for the location
     * @param geoUtils to use
     * @return pair with remaining closest link found and boolean indicating if any closest links were removed
     * before finding a compatible closest links (true when one or more closest links are removed, false otherwise)
     */
  public static Pair<MacroscopicLink,Boolean> excludeClosestLinksIncrementallyOnWrongSideOf(
      Geometry location,
      Collection<MacroscopicLink> links,
      boolean isLeftHandDrive,
      Collection<? extends Mode> accessModes,
      PlanitJtsCrsUtils geoUtils) {
    boolean entriesRemoved = false;
    MacroscopicLink closestLink = null;
    do{
      closestLink = (MacroscopicLink) PlanitGraphGeoUtils.findEdgeClosest(location, links, geoUtils);
      Collection<MacroscopicLink> result =
          ZoningConverterUtils.excludeLinksOnWrongSideOf(
              location, Collections.singleton(closestLink), isLeftHandDrive,  accessModes, geoUtils);
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

  // Same as node based version, only now we do not know yet which node is our reference node, so we
  // consider both as options
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
        waitingAreaGeometry, accessLink, accessLinkSourceId, accessLink.getNodeA(), accessMode, countryName,
        mustAvoidCrossingTraffic, getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,
        getOverwrittenWaitingAreaSourceId, geoUtils);

    // node B of potential access link
    var accessLinkSegmentsB = findAccessEntryLinkSegmentsForWaitingArea(waitingAreaSourceId,
        waitingAreaGeometry, accessLink, accessLinkSourceId, accessLink.getNodeB(), accessMode, countryName,
        mustAvoidCrossingTraffic, getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,
        getOverwrittenWaitingAreaSourceId, geoUtils);

    if(accessLinkSegments!=null){
      accessLinkSegments.addAll(accessLinkSegmentsB);
    }else{
      accessLinkSegments = accessLinkSegmentsB;
    }
    return accessLinkSegments;
  }

  /**
   * Find the access link segments of the given links for a waiting area, per access mode: the segments entering either
   * end of a link that the mode may use, of which a mode that must not cross traffic keeps only those with the waiting
   * area on the side of the road it drives on, see
   * {@link #findAccessLinkSegmentsForWaitingArea(String, Geometry, MacroscopicLink, String, Mode, String, boolean,
   * Function, Function, PlanitJtsCrsUtils)}
   *
   * @param waitingAreaSourceId the waiting area's source id
   * @param waitingAreaGeometry the waiting area's geometry
   * @param links to find access link segments on
   * @param linkToSourceId gives a link's source id, matched against the access link source id the user nominated
   * @param accessModes to find access link segments for
   * @param getOverwrittenAccessLinkSourceIdByMode per access mode the mapping from waiting area source id to the access
   *                                               link source id the user nominated, which may be null for a mode
   * @param countryName to take the driving direction from
   * @param geoUtils to use
   * @return the access link segments found, in id order
   */
  public static SortedSet<MacroscopicLinkSegment> findAccessLinkSegmentsForWaitingAreaOnLinks(
      String waitingAreaSourceId,
      Geometry waitingAreaGeometry,
      Collection<MacroscopicLink> links,
      Function<MacroscopicLink, String> linkToSourceId,
      Collection<Mode> accessModes,
      Function<Mode, Function<String, String>> getOverwrittenAccessLinkSourceIdByMode,
      String countryName,
      PlanitJtsCrsUtils geoUtils) {

    var accessLinkSegments =
        new TreeSet<MacroscopicLinkSegment>(Comparator.comparingLong(MacroscopicLinkSegment::getId));
    for(var accessMode : accessModes) {
      var getOverwrittenAccessLinkSourceId = getOverwrittenAccessLinkSourceIdByMode.apply(accessMode);
      for(var link : links) {
        var linkAccessLinkSegments = findAccessLinkSegmentsForWaitingArea(
            waitingAreaSourceId, waitingAreaGeometry, link, linkToSourceId.apply(link), accessMode, countryName,
            isAvoidCrossTrafficForAccessMode(accessMode), getOverwrittenAccessLinkSourceId, null, geoUtils);
        if(linkAccessLinkSegments != null) {
          linkAccessLinkSegments.forEach(linkSegment -> accessLinkSegments.add((MacroscopicLinkSegment) linkSegment));
        }
      }
    }
    return accessLinkSegments;
  }

  /**
   * Choose the link a waiting area's stop goes on, from candidate access link segments, judging how close a link is
   * by the distance to its geometry:
   * <ol>
   *   <li>keep the candidates a stop location can be made on, for an access mode they allow,</li>
   *   <li>of several links, keep those within the buffer beyond the link closest to the waiting area,</li>
   *   <li>of several links, prefer the highest capacity when the first access mode is a road mode,</li>
   *   <li>take the closest remaining link.</li>
   * </ol>
   * Closeness is measured from the waiting area's geometry to each link's geometry.
   *
   * @param waitingAreaGeometry the waiting area's geometry
   * @param candidateAccessLinkSegments to choose from
   * @param accessModes the stop serves, of which the first decides whether the highest capacity is preferred
   * @param hasStopLocation whether a stop location can be made on an access link segment for an access mode
   * @param closestLinkBufferMeters beyond the distance of the closest link, within which links are compared further
   * @param geoUtils to measure distances with
   * @return the chosen link with its remaining access link segments in id order, null when no candidate has a stop
   * location
   */
  public static Pair<MacroscopicLink, SortedSet<MacroscopicLinkSegment>>
      findMostAppropriateStopLocationLinkByDistanceToLinkGeometry(
          Geometry waitingAreaGeometry,
          Collection<MacroscopicLinkSegment> candidateAccessLinkSegments,
          Collection<Mode> accessModes,
          BiPredicate<MacroscopicLinkSegment, Mode> hasStopLocation,
          double closestLinkBufferMeters,
          PlanitJtsCrsUtils geoUtils) {

    /* 1) the candidates a stop location can be made on, in id order so nothing that follows depends on the order of
     * the candidates */
    var accessLinkSegments =
        new TreeSet<MacroscopicLinkSegment>(Comparator.comparingLong(MacroscopicLinkSegment::getId));
    for(var accessMode : accessModes) {
      for(var candidate : candidateAccessLinkSegments) {
        if(candidate.isModeAllowed(accessMode) && hasStopLocation.test(candidate, accessMode)) {
          accessLinkSegments.add(candidate);
        }
      }
    }
    if(accessLinkSegments.isEmpty()) {
      return null;
    }
    Set<MacroscopicLink> links =
        accessLinkSegments.stream().map(MacroscopicLinkSegment::getParent).collect(Collectors.toSet());
    if(links.size() == 1) {
      return Pair.of(links.iterator().next(), accessLinkSegments);
    }

    /* 2) the links within the buffer beyond the closest one */
    links = new HashSet<>(PlanitGraphGeoUtils.findEdgesWithinClosestDistanceDeltaToGeometry(
        waitingAreaGeometry, links, closestLinkBufferMeters, geoUtils).keySet());
    final var linksWithinBuffer = links;
    accessLinkSegments.removeIf(linkSegment -> !linksWithinBuffer.contains(linkSegment.getParent()));
    if(links.size() == 1) {
      return Pair.of(links.iterator().next(), accessLinkSegments);
    }

    /* 3) the highest capacity */
    links = keepHighestCapacityCandidates(
        links, accessLinkSegments, accessModes.iterator().next());

    /* 4) the closest remaining link */
    MacroscopicLink chosenLink = links.iterator().next();
    if(links.size() > 1) {
      chosenLink = (MacroscopicLink) PlanitGraphGeoUtils.findEdgeClosest(waitingAreaGeometry, links, geoUtils);
    }
    final var finalChosenLink = chosenLink;
    accessLinkSegments.removeIf(linkSegment -> !linkSegment.getParent().equals(finalChosenLink));
    return Pair.of(chosenLink, accessLinkSegments);
  }

  /**
   * Choose the link a waiting area's stop goes on, from candidate access link segments, judging how close a link is
   * by its stop location: how close the places a stop could take on its segments come to the waiting area's centre,
   * see {@link StopLocationOnLinkSegment}. A link whose only stop locations come from the fallback check ranks after
   * every link with a stop location of its own.
   * <ol>
   *   <li>keep the candidates with a stop location, for an access mode they allow,</li>
   *   <li>of several links, keep those whose stop lies within the buffer beyond the nearest stop,</li>
   *   <li>of several links, prefer the highest capacity when the first access mode is a road mode,</li>
   *   <li>take the remaining link with the nearest stop, the lowest id when equally near.</li>
   * </ol>
   *
   * @param candidateAccessLinkSegments to choose from
   * @param accessModes the stop serves, of which the first decides whether the highest capacity is preferred
   * @param stopLocationOnSegment the stop location on an access link segment for an access mode, null when it has none
   * @param closestStopBufferMeters beyond the nearest stop, within which links are compared further
   * @return the chosen link with its remaining access link segments in id order, null when no candidate has a stop
   * location or no link remains
   */
  public static Pair<MacroscopicLink, SortedSet<MacroscopicLinkSegment>>
      findMostAppropriateStopLocationLinkByDistanceToStopLocation(
          Collection<MacroscopicLinkSegment> candidateAccessLinkSegments,
          Collection<Mode> accessModes,
          BiFunction<MacroscopicLinkSegment, Mode, StopLocationOnLinkSegment> stopLocationOnSegment,
          double closestStopBufferMeters) {

    /* 1) the candidates with a stop location, each link judged by its nearest stop, a link whose stops all come
     * from the fallback check after every other link */
    var accessLinkSegments =
        new TreeSet<MacroscopicLinkSegment>(Comparator.comparingLong(MacroscopicLinkSegment::getId));
    var nearestStopByLink = new HashMap<MacroscopicLink, StopLocationOnLinkSegment>();
    Comparator<StopLocationOnLinkSegment> nearerStop = Comparator.comparing(
        StopLocationOnLinkSegment::isFromFallbackCheck).thenComparingDouble(
            StopLocationOnLinkSegment::getMetersFromWaitingAreaCentreToNearestPlace);
    for(var accessMode : accessModes) {
      for(var candidate : candidateAccessLinkSegments) {
        if(!candidate.isModeAllowed(accessMode)) {
          continue;
        }
        var stopLocation = stopLocationOnSegment.apply(candidate, accessMode);
        if(stopLocation == null) {
          continue;
        }
        accessLinkSegments.add(candidate);
        nearestStopByLink.merge(candidate.getParent(), stopLocation,
            (stop, otherStop) -> nearerStop.compare(otherStop, stop) < 0 ? otherStop : stop);
      }
    }
    if(accessLinkSegments.isEmpty()) {
      return null;
    }
    if(nearestStopByLink.size() == 1) {
      return Pair.of(nearestStopByLink.keySet().iterator().next(), accessLinkSegments);
    }

    /* 2) the links whose stop lies within the buffer beyond the nearest stop */
    var nearestStop = nearestStopByLink.values().stream().min(nearerStop).get();
    Set<MacroscopicLink> links = new TreeSet<>(Comparator.comparingLong(MacroscopicLink::getId));
    nearestStopByLink.forEach((link, stop) -> {
      if(stop.isFromFallbackCheck() == nearestStop.isFromFallbackCheck() &&
          stop.getMetersFromWaitingAreaCentreToNearestPlace() <=
              nearestStop.getMetersFromWaitingAreaCentreToNearestPlace() + closestStopBufferMeters) {
        links.add(link);
      }
    });
    accessLinkSegments.removeIf(linkSegment -> !links.contains(linkSegment.getParent()));
    if(links.size() == 1) {
      return Pair.of(links.iterator().next(), accessLinkSegments);
    }

    /* 3) the highest capacity */
    var remainingLinks = keepHighestCapacityCandidates(
        links, accessLinkSegments, accessModes.iterator().next());

    /* 4) the remaining link with the nearest stop, the lowest id when equally near */
    MacroscopicLink chosenLink = null;
    for(var link : remainingLinks.stream().sorted(
        Comparator.comparingLong(MacroscopicLink::getId)).collect(Collectors.toList())) {
      if(chosenLink == null || Precision.smaller(
          nearestStopByLink.get(link).getMetersFromWaitingAreaCentreToNearestPlace(),
          nearestStopByLink.get(chosenLink).getMetersFromWaitingAreaCentreToNearestPlace(), Precision.EPSILON_6)) {
        chosenLink = link;
      }
    }
    if(chosenLink == null) {
      return null;
    }
    final var finalChosenLink = chosenLink;
    accessLinkSegments.removeIf(linkSegment -> !linkSegment.getParent().equals(finalChosenLink));
    return Pair.of(chosenLink, accessLinkSegments);
  }

  /**
   * Choose a stop's location on its access link, from the locations its access link segments give: the nearest
   * location at an end node of its segment, its end or its start, so the link is not broken, otherwise the nearest
   * location. When
   * equally near, the segment with the lowest id wins, so the choice does not depend on the order of the segments. A
   * location the caller marks as a last resort is only used when no segment gives another location
   *
   * @param accessLinkSegments of the access link to choose from
   * @param locationOnSegment the stop location on a segment, null when it has none
   * @param metersToWaitingArea the distance from a location to the waiting area
   * @param isLastResort whether a segment's location is only used when no segment gives another location
   * @return the chosen location, null when no segment has one
   */
  public static Point chooseStopLocationOnAccessLink(
      Collection<? extends MacroscopicLinkSegment> accessLinkSegments,
      Function<MacroscopicLinkSegment, Point> locationOnSegment,
      ToDoubleFunction<Point> metersToWaitingArea,
      BiPredicate<MacroscopicLinkSegment, Point> isLastResort) {
    Point nearest = null, nearestReusingEndNode = null, nearestLastResort = null;
    double nearestMeters = Double.POSITIVE_INFINITY, nearestReusingEndNodeMeters = Double.POSITIVE_INFINITY;
    double nearestLastResortMeters = Double.POSITIVE_INFINITY;
    long nearestId = Long.MAX_VALUE, nearestReusingEndNodeId = Long.MAX_VALUE, nearestLastResortId = Long.MAX_VALUE;
    for(var accessLinkSegment : accessLinkSegments) {
      var location = locationOnSegment.apply(accessLinkSegment);
      if(location == null || location.isEmpty()) {
        continue;
      }
      double meters = metersToWaitingArea.applyAsDouble(location);
      long id = accessLinkSegment.getId();
      if(isLastResort.test(accessLinkSegment, location)) {
        if(isNearerOrEquallyNearWithLowerId(meters, id, nearestLastResortMeters, nearestLastResortId)) {
          nearestLastResort = location;
          nearestLastResortMeters = meters;
          nearestLastResortId = id;
        }
        continue;
      }
      if(isNearerOrEquallyNearWithLowerId(meters, id, nearestMeters, nearestId)) {
        nearest = location;
        nearestMeters = meters;
        nearestId = id;
      }
      boolean reusesEndNode =
          accessLinkSegment.getDownstreamVertex().isPositionEqual2D(location.getCoordinate(), Precision.EPSILON_6) ||
          accessLinkSegment.getUpstreamVertex().isPositionEqual2D(location.getCoordinate(), Precision.EPSILON_6);
      if(reusesEndNode &&
          isNearerOrEquallyNearWithLowerId(meters, id, nearestReusingEndNodeMeters, nearestReusingEndNodeId)) {
        nearestReusingEndNode = location;
        nearestReusingEndNodeMeters = meters;
        nearestReusingEndNodeId = id;
      }
    }
    return nearestReusingEndNode != null ? nearestReusingEndNode : (nearest != null ? nearest : nearestLastResort);
  }

  /** Find the link segments that are accessible for the given access link, node, mode combination taking into
   * account the relative location of the transfer zone if needed and
   * mode compatibility.
   *
   * @param waitingAreaSourceId these link segments pertain to
   * @param waitingAreaGeometry these link segments pertain to
   * @param accessLink that is nominated
   * @param accessLinkSourceId source id of the access link used, this will be matched against the overwritten access
   *                           link source id (if provided)
   * @param node extreme node of the link
   * @param accessMode eligible access mode
   * @param countryName we are considering from which we will extract whether it is a left or right hand drive country
   * @param mustAvoidCrossingTraffic flag indicating if cross traffic should be avoided for the access link segments
   *                                 that are deemed eligible
   * @param getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId mapping from waiting area source id to nominated
   *                                                               access link source id, which if a match to provided
   *                                                               access link source id
   *                                                               makes sure that even if there exists crossing
   *                                                               traffic this does not render its access link
   *                                                               segment(s) invalid, may be null
   * @param getOverwrittenWaitingAreaSourceId function that provides the overwritten waiting area source id for a
   *                                          given access node, maybe return null if not overwritten
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
        waitingAreaSourceId,node, accessLinkSourceId,
        getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,getOverwrittenWaitingAreaSourceId);

    /* accessible link segments for planit node based on relative location of waiting area compared to infrastructure*/
    if(mustAvoidCrossingTraffic) {

      boolean isLeftHandDrive = DrivingDirectionDefaultByCountry.isLeftHandDrive(countryName);
      Collection<LinkSegment> toBeRemovedAccessLinkSegments =
          identifyLinkSegmentsOnWrongSideOf(waitingAreaGeometry, accessLinkSegments, isLeftHandDrive, geoUtils);

      if(!toBeRemovedAccessLinkSegments.isEmpty() &&
          (removeInvalidAccessLinkSegmentsIfNoMatchLeft ||
              toBeRemovedAccessLinkSegments.size() < accessLinkSegments.size())) {
        /* filter because "normal" situation or there are still matches left even after filtering despite the explicit
        user override for this  combination */
        accessLinkSegments.removeAll(toBeRemovedAccessLinkSegments);
      }
      /* else  keep the access link segments so far */
    }
    return accessLinkSegments;
  }

  /** Verify if given link segment is potentially viable as access link segment for the given extreme node and access
   * mode, taking into account
   * any explicit overwrites that may exist that are not bounded by any limitations on compatibility.
   *
   * @param waitingAreaSourceId we're checking for
   * @param waitingAreaGeometry we're checking for
   * @param accessLinkSegment nominated
   * @param accessLinkSourceId original source id of the PLANit access link
   * @param accessNode that is nominated
   * @param accessMode used
   * @param getOverwrittenWaitingAreaSourceId function that provides the overwritten waiting area source id for a given
   *                                          access node, maybe return null if not overwritten
   * @param getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId mapping from waiting area source id to nominated
   *                                                               access link source id, which if a match to provided
   *                                                               access link source id
   *                                                               makes sure that even if there exists crossing
   *                                                               traffic this does not render its access link
   *                                                               segment(s) invalid, may be null
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
        waitingAreaSourceId, accessLinkSegment.getDownstreamNode(), accessLinkSourceId,
        getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,getOverwrittenWaitingAreaSourceId);
    if(!removeInvalidAccessLinkSegment){
      return true;
    }

    /* accessible link segments for planit node based on relative location of waiting area compared to infrastructure*/
    if(mustAvoidCrossingTraffic) {
      boolean isLeftHandDrive = DrivingDirectionDefaultByCountry.isLeftHandDrive(countryName);
      boolean onWrongSide = !identifyLinkSegmentsOnWrongSideOf(
          waitingAreaGeometry, Collections.singleton(accessLinkSegment), isLeftHandDrive, geoUtils).isEmpty();
      return !onWrongSide;
    }
    return true;
  }

  /** find a suitable connectoid location on the given link based on the constraints that it must be able to reside
   * on a link segment that is in the correct relative position
   * to the transfer zone and supports the access mode on at least one of the designated link segment(s) that is
   * eligible (if any). If not null is returned, otherwise the returned
   * location may be an existing extreme node's location, or a location internal to the link if the extreme node
   * does not fall within the constraints provided
   *
   * @param waitingAreaSourceId supplies a relevant source id of the waiting area in question to use for exceptions
   *                            or logging when needed
   * @param waitingAreaGeometry to find location for (which is either sourced from a PLANit transfer zone, or to be
   *                            created transfer zone) that will reflect this waiting area)
   * @param accessLinkSegment to find location on
   * @param accessLinkSourceId the access link's source id
   * @param accessMode to be compatible with
   * @param maxAllowedDistanceMeters the maximum allowed distance between stop and waiting area that we allow
   * @param getOverwrittenWaitingAreaSourceIdForNode function that provides the overwritten waiting area source id
   *                                                 for a given access node, maybe return null if not overwritten
   * @param getOverwrittenWaitingAreaSourceIdForPoint function that provides the overwritten waiting area source id
   *                                                  for a given access node, maybe return null if not overwritten
   * @param getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId mapping from waiting area source id to nominated
   *                                                               access link source id, which if a match to provided
   *                                                               access link source id
   *                                                               makes sure that even if there exists crossing
   *                                                               traffic this does not render its access link
   *                                                               segment(s) invalid, may be null
   * @param countryName to extract driving direction from
   * @param geoUtils gis functionality to apply in finding connectoid location
   * @return found location either existing node or projected location that is nearest and does not exist as a
   * shape point on the link yet, or null if no valid position could be found
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
    boolean isOverride = ExternalIdAbleUtils.isMappedToId(
        getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, waitingAreaSourceId, accessLinkSourceId);

    var accessLinkSegmentGeometry = accessLinkSegment.getParent().getGeometry();
    /* exclude the extreme node at the upstream end because if this is access node, the preceding access link
    segment should be chosen */
    int startIndex = accessLinkSegment.isParentGeometryInSegmentDirection(false) ? 1 : 0;
    int endIndex = accessLinkSegment.isParentGeometryInSegmentDirection(false) ?
        accessLinkSegmentGeometry.getNumPoints()-1 : accessLinkSegmentGeometry.getNumPoints()-2;
    Coordinate closestExistingCoordinate = geoUtils.getClosestExistingLineStringCoordinateToGeometry(
        waitingAreaGeometry, accessLinkSegmentGeometry, startIndex, endIndex);
    double distanceToExistingCoordinateOnLinkInMeters = geoUtils.getClosestDistanceInMeters(
        closestExistingCoordinate, waitingAreaGeometry);

    /* 1) verify if extreme node is deemed acceptable*/
    if(accessLinkSegment.getDownstreamVertex().isPositionEqual2D(closestExistingCoordinate, Precision.EPSILON_6) &&
        (distanceToExistingCoordinateOnLinkInMeters < maxAllowedDistanceMeters)){

      if(isPotentialAccessEntryLinkSegmentForWaitingArea(
          waitingAreaSourceId, waitingAreaGeometry, accessLinkSegment, accessLinkSourceId,
              accessLinkSegment.getDownstreamNode(), accessMode, getOverwrittenWaitingAreaSourceIdForNode,
              getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, countryName, geoUtils)) {
        return PlanitJtsUtils.createPoint(closestExistingCoordinate);
      }else{
        return null;
      }
    }

    /* 2) if close enough utilise internal node location as stop_position/connectoid, otherwise create artificial
    point on closest projected location which
     * in most cases will be closer and within threshold */
    LineSegment lineSegmentForLocation = null;
    if(distanceToExistingCoordinateOnLinkInMeters < maxAllowedDistanceMeters){
      var linkCoordinates = accessLinkSegmentGeometry.getCoordinates();
      final int coordinateIndex = PlanitJtsUtils.getCoordinateIndexOf(closestExistingCoordinate, linkCoordinates);
      if(coordinateIndex <= 0 || coordinateIndex==(accessLinkSegmentGeometry.getCoordinates().length-1)) {
        throw new PlanItRunTimeException("Unable to locate link internal location even though it is expected to" +
            " exist when identifying connectoid location for waiting area %s", waitingAreaSourceId);
      }
      lineSegmentForLocation = new LineSegment(linkCoordinates[coordinateIndex-1], linkCoordinates[coordinateIndex]);
    }
    /* 3) too far, check if breaking the existing link in appropriate location instead would work */
    else{
      LinearLocation projectedLinLocOnLink =
          PlanitEntityGeoUtils.extractClosestProjectedLinearLocationToGeometryFromEdge(
            waitingAreaGeometry, accessLinkSegment.getParent(), geoUtils);

      /* verify projected location is valid */
      Coordinate closestProjectedCoordinate = projectedLinLocOnLink.getCoordinate(accessLinkSegmentGeometry);
      if( !closestExistingCoordinate.equals2D(closestProjectedCoordinate) &&
          (isOverride || geoUtils.getClosestDistanceInMeters(closestProjectedCoordinate, waitingAreaGeometry) <=
              maxAllowedDistanceMeters)) {

        /* acceptable location that is internal, create proposed line segment location as result and
        update chosen coordinate*/
        lineSegmentForLocation = new LineSegment(
            accessLinkSegmentGeometry.getCoordinates()[projectedLinLocOnLink.getSegmentIndex()],
            closestProjectedCoordinate);
      }
    }

    if(lineSegmentForLocation ==null) {
      return null;
    }
    return locationAtLineSegmentEndOnWaitingAreaSideOfRoad(waitingAreaSourceId, waitingAreaGeometry, accessLinkSegment,
        accessMode, lineSegmentForLocation, isOverride, getOverwrittenWaitingAreaSourceIdForPoint, countryName,
        geoUtils);
  }

  /**
   * Find where a waiting area's stop goes on an access link segment. The ideal stop point is the point of the link
   * closest to the waiting area's centre, see {@link PlanitJtsUtils#extractCentre(Geometry)}:
   * <ol>
   *   <li>an end node of the segment is reused, so the link is not broken, when it lies within reach of the waiting
   *   area and within the given distance of the ideal stop point, and the access mode can use it: the segment's end
   *   node when the mode can arrive at it along the segment, its start node when the mode can arrive at it along
   *   exactly one other link and can leave it only along the segment, turning back aside, so every vehicle stopping
   *   there comes from that link and passes the waiting area; the one nearer the ideal
   *   stop point when both qualify,</li>
   *   <li>otherwise the link is broken at the ideal stop point, when it lies inside the link, within reach and on the
   *   side of the road the access mode needs; at the inner shape point nearest to it instead when that lies within the
   *   given distance of it and on the side of the road too,</li>
   *   <li>otherwise the stop goes where the nearest existing point check puts it, see
   *   {@link #findConnectoidLocationForWaitingAreaOnLinkSegment(String, Geometry, MacroscopicLinkSegment, String,
   *   Mode, double, Function, Function, Function, String, PlanitJtsCrsUtils)}, for example for a waiting area before
   *   the start of the network.</li>
   * </ol>
   * For a user nominated access link the stop goes where the nearest existing point check puts it.
   *
   * @param waitingAreaSourceId the waiting area's source id
   * @param waitingAreaGeometry the waiting area's geometry
   * @param accessLinkSegment to find the stop location on
   * @param accessLinkSourceId the access link's source id
   * @param accessMode to be compatible with
   * @param maxAllowedDistanceMeters the maximum allowed distance between stop location and waiting area
   * @param maxEndNodeToIdealStopPointMeters the maximum distance from the ideal stop point to an end node that is
   *                                         reused
   * @param maxShapePointToIdealStopPointMeters the maximum distance from the ideal stop point to a shape point the
   *                                            link is broken at instead
   * @param getOverwrittenWaitingAreaSourceIdForNode function that provides the overwritten waiting area source id
   *                                                 for a given access node, may return null if not overwritten
   * @param getOverwrittenWaitingAreaSourceIdForPoint function that provides the overwritten waiting area source id
   *                                                  for a given location, may return null if not overwritten
   * @param getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId mapping from waiting area source id to the access
   *                                                               link source id the user nominated, may be null
   * @param countryName to extract driving direction from
   * @param geoUtils to use
   * @return the stop location, null when the segment has none
   */
  public static StopLocationOnLinkSegment findStopLocationForWaitingAreaOnLinkSegment(
      String waitingAreaSourceId,
      final Geometry waitingAreaGeometry,
      final MacroscopicLinkSegment accessLinkSegment,
      final String accessLinkSourceId,
      final Mode accessMode,
      double maxAllowedDistanceMeters,
      double maxEndNodeToIdealStopPointMeters,
      double maxShapePointToIdealStopPointMeters,
      final Function<Node,String> getOverwrittenWaitingAreaSourceIdForNode,
      final Function<Point,String> getOverwrittenWaitingAreaSourceIdForPoint,
      final Function<String,String> getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,
      String countryName,
      final PlanitJtsCrsUtils geoUtils) {

    var centre = PlanitJtsUtils.extractCentre(waitingAreaGeometry);
    if(ExternalIdAbleUtils.isMappedToId(
        getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, waitingAreaSourceId, accessLinkSourceId)) {
      return StopLocationOnLinkSegment.of(
          findConnectoidLocationForWaitingAreaOnLinkSegment(
            waitingAreaSourceId, waitingAreaGeometry, accessLinkSegment, accessLinkSourceId, accessMode,
            maxAllowedDistanceMeters, getOverwrittenWaitingAreaSourceIdForNode, getOverwrittenWaitingAreaSourceIdForPoint,
            getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, countryName, geoUtils),
          centre,
          false,
          geoUtils);
    }

    var linkGeometry = accessLinkSegment.getParent().getGeometry();
    var linkCoordinates = linkGeometry.getCoordinates();
    boolean inGeometryDirection =
        accessLinkSegment.isParentGeometryInSegmentDirection(false);
    Coordinate downstreamEnd = inGeometryDirection ? linkCoordinates[linkCoordinates.length-1] : linkCoordinates[0];
    Coordinate upstreamEnd = inGeometryDirection ? linkCoordinates[0] : linkCoordinates[linkCoordinates.length-1];

    /* the ideal stop point: the point of the link closest to the waiting area's centre */
    LinearLocation idealLinearLocation =
        PlanitEntityGeoUtils.extractClosestProjectedLinearLocationToGeometryFromEdge(
          centre, accessLinkSegment.getParent(), geoUtils);
    Coordinate idealStopPoint = idealLinearLocation.getCoordinate(linkGeometry);

    /* the end nodes within reach and near the ideal stop point the access mode can use: the downstream one when it
     * arrives there along the segment, the upstream one when it arrives there along exactly one other link and can
     * only leave along the segment */
    var idealStopPointLocation = PlanitJtsUtils.createPoint(idealStopPoint);
    double downstreamEndToIdealMeters = geoUtils.getClosestDistanceInMeters(downstreamEnd, idealStopPointLocation);
    boolean reuseDownstreamEnd =
        geoUtils.getClosestDistanceInMeters(downstreamEnd, waitingAreaGeometry) < maxAllowedDistanceMeters &&
        isPotentialAccessEntryLinkSegmentForWaitingArea(
            waitingAreaSourceId, waitingAreaGeometry, accessLinkSegment, accessLinkSourceId,
            accessLinkSegment.getDownstreamNode(), accessMode, getOverwrittenWaitingAreaSourceIdForNode,
            getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, countryName, geoUtils) &&
        downstreamEndToIdealMeters <= maxEndNodeToIdealStopPointMeters;
    double upstreamEndToIdealMeters = geoUtils.getClosestDistanceInMeters(upstreamEnd, idealStopPointLocation);
    boolean reuseUpstreamEnd =
        geoUtils.getClosestDistanceInMeters(upstreamEnd, waitingAreaGeometry) < maxAllowedDistanceMeters &&
        upstreamEndToIdealMeters <= maxEndNodeToIdealStopPointMeters &&
        isUpstreamNodeOneOtherLinkInAndOnlySegmentOutForWaitingArea(waitingAreaSourceId, waitingAreaGeometry,
            accessLinkSegment, accessMode, getOverwrittenWaitingAreaSourceIdForNode,
            getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId, countryName, geoUtils);
    Coordinate reusedEnd =
        reuseDownstreamEnd && (!reuseUpstreamEnd || downstreamEndToIdealMeters <= upstreamEndToIdealMeters) ?
            downstreamEnd : (reuseUpstreamEnd ? upstreamEnd : null);

    /* the break point: the ideal stop point inside the link, within reach and on the side of the road */
    Point breakLocation = null;
    if(!idealStopPoint.equals2D(downstreamEnd) && !idealStopPoint.equals2D(upstreamEnd) &&
        geoUtils.getClosestDistanceInMeters(idealStopPoint, waitingAreaGeometry) <= maxAllowedDistanceMeters) {
      int index = idealLinearLocation.getSegmentIndex();
      /* the piece of the link arriving at the break point, also when it falls on an inner shape point */
      var piece = linkCoordinates[index].equals2D(idealStopPoint) && index > 0 ?
          new LineSegment(linkCoordinates[index-1], idealStopPoint) :
          new LineSegment(linkCoordinates[index], idealStopPoint);
      breakLocation =
          locationAtLineSegmentEndOnWaitingAreaSideOfRoad(
              waitingAreaSourceId, waitingAreaGeometry,
              accessLinkSegment, accessMode, piece, false,
              getOverwrittenWaitingAreaSourceIdForPoint, countryName,geoUtils);
    }

    /* the link is broken at an existing shape point near the ideal stop point rather than right next to it */
    if(breakLocation != null) {
      int nearestIndex = -1;
      double nearestMeters = Double.POSITIVE_INFINITY;
      for(int index = 1; index < linkCoordinates.length-1; index++) {
        double meters = geoUtils.getClosestDistanceInMeters(linkCoordinates[index], idealStopPointLocation);
        if(meters < nearestMeters) {
          nearestMeters = meters;
          nearestIndex = index;
        }
      }
      if(nearestIndex > 0 && nearestMeters <= maxShapePointToIdealStopPointMeters &&
          !linkCoordinates[nearestIndex].equals2D(idealStopPoint)) {
        var shapePointLocation = locationAtLineSegmentEndOnWaitingAreaSideOfRoad(waitingAreaSourceId, waitingAreaGeometry,
            accessLinkSegment, accessMode,
            new LineSegment(linkCoordinates[nearestIndex-1], linkCoordinates[nearestIndex]), false,
            getOverwrittenWaitingAreaSourceIdForPoint, countryName, geoUtils);
        if(shapePointLocation != null) {
          breakLocation = shapePointLocation;
        }
      }
    }

    /* how close the places a stop could take on this segment come to the waiting area's centre */
    double metersToNearestPlace = Math.min(Math.min(
        reuseDownstreamEnd ? geoUtils.getClosestDistanceInMeters(downstreamEnd, centre) : Double.POSITIVE_INFINITY,
        reuseUpstreamEnd ? geoUtils.getClosestDistanceInMeters(upstreamEnd, centre) : Double.POSITIVE_INFINITY),
        breakLocation != null ? geoUtils.getClosestDistanceInMeters(idealStopPoint, centre) : Double.POSITIVE_INFINITY);
    if(reusedEnd != null) {
      return new StopLocationOnLinkSegment(
          PlanitJtsUtils.createPoint(reusedEnd), metersToNearestPlace, false);
    }
    if(breakLocation != null) {
      return new StopLocationOnLinkSegment(breakLocation, metersToNearestPlace, false);
    }
    return StopLocationOnLinkSegment.of(
        findConnectoidLocationForWaitingAreaOnLinkSegment(
            waitingAreaSourceId,
            waitingAreaGeometry,
            accessLinkSegment,
            accessLinkSourceId,
            accessMode,
            maxAllowedDistanceMeters,
            getOverwrittenWaitingAreaSourceIdForNode,
            getOverwrittenWaitingAreaSourceIdForPoint,
            getOverwrittenAccessLinkSourceIdForWaitingAreaSourceId,
            countryName,
            geoUtils),
        centre,
        true,
        geoUtils);
  }

  /** create transfer connectoid for the link segment provided, all related to the given transfer zone and with access
   * modes provided. When the link segment does not have any of the passed in modes listed as allowed, no connectoid
   * is created and null is returned
   *
   * @param connectoidExternalId external id (allowed to be null)
   * @param zoning to register on
   * @param accessZone to relate connectoids to
   * @param downstreamAccessNode when true access node is chosen as the downstream node, when false, upstream node is chosen
   * @param accessLinkSegment to create connectoid for
   * @param allowedModes used for the connectoid
   * @param type         the type of the zone connectoid combination reflecting how it is envisaged to be used
   * @return created connectoid when at least one of the allowed modes is also allowed on the link segment
   */
  public static TransferConnectoid createAndRegisterTransferConnectoidWithDirectedAccessEntry(
      @Nullable String connectoidExternalId,
      Zoning zoning,
      final TransferZone accessZone,
      final boolean downstreamAccessNode,
      final MacroscopicLinkSegment accessLinkSegment,
      final Set<Mode> allowedModes,
      final ZoneConnectoidType type){

    final Set<Mode> realAllowedModes = accessLinkSegment.getAllowedModesFrom(allowedModes);
    if(realAllowedModes!= null && !realAllowedModes.isEmpty()) {
      // CREATE
      var connectoid = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
          accessZone, downstreamAccessNode, accessLinkSegment, true, realAllowedModes, type);
      connectoid.setExternalId(connectoidExternalId);
      return connectoid;
    }
    return null;
  }

  /** create transfer connectoid for the link segment provided, all related to the given transfer zone and with access
   * modes provided. When the link segment does not have any of the passed in modes listed as allowed, no connectoid
   * is created and null is returned
   *
   * @param connectoidExternalId external id (allowed to be null)
   * @param zoning to register on
   * @param accessZone to relate connectoids to
   * @param accessNode when true access node is chosen as the downstream node, when false, upstream node is chosen
   * @param allowedModes used for the connectoid
   * @param type         the type of the zone connectoid combination reflecting how it is envisaged to be used
   * @return created connectoid when at least one of the allowed modes is also allowed on the link segment
   */
  public static TransferConnectoid createAndRegisterTransferConnectoidWithUndirectedAccessEntry(
      @Nullable String connectoidExternalId,
      Zoning zoning,
      final TransferZone accessZone,
      final DirectedVertex accessNode,
      final Set<Mode> allowedModes,
      final ZoneConnectoidType type){

    // CREATE
    var connectoid = zoning.getTransferConnectoids().getFactory().registerNew(accessNode);
    connectoid.setExternalId(connectoidExternalId);

    // undirected entry
    var entry = connectoid.createUndirectedAccessZoneEntry(accessZone, type);
    entry.addAllowedModes(allowedModes);

    return connectoid;
  }

  /** create connectoids with directed entries, all related to the given transfer zone and with
   * access modes provided. Connectoids are only created when the access link segment has at least one of the
   * allowed modes as an eligible mode.
   *
   * @param connectoidExternalId external id (allowed to be null)
   * @param zoning to register on
   * @param transferZone to relate connectoids to
   * @param accessVertex the access node the connectoid utilises (determine the up/downstream connection of the
   *                   attached link segment(s)
   * @param accessLinkSegments to create connectoids for (one per segment)
   * @param allowedModes used for each connectoid
   * @param type         the type of the zone connectoid combination reflecting how it is envisaged to be used
   * @return created connectoids
   */
  public static Collection<TransferConnectoid> createAndRegisterTransferConnectoids(
      @Nullable String connectoidExternalId,
      Zoning zoning,
      final TransferZone transferZone,
      final DirectedVertex accessVertex,
      final Iterable<? extends MacroscopicLinkSegment> accessLinkSegments,
      final Set<Mode> allowedModes,
      ZoneConnectoidType type){
    Set<TransferConnectoid> createdConnectoids = new HashSet<>();

    // we sort to ensure connectoids are always created in same deterministic order
    IterableUtils.asStream(accessLinkSegments).sorted(
            Comparator.comparing(MacroscopicLinkSegment::getId)).forEach(
                accessLinkSegment -> {

                  boolean downstreamAccessNode = accessLinkSegment.isDownstreamVertex(accessVertex);
                  if(!accessLinkSegment.hasAnyVertex(accessVertex)){
                    throw new PlanItRunTimeException(
                      "Chosen access node %s not attached to link segment %s",
                      accessVertex.getIdsAsString(), accessLinkSegment.getIdsAsString());
                  }
                  // CREATE
                  TransferConnectoid newConnectoid = createAndRegisterTransferConnectoidWithDirectedAccessEntry(
                      connectoidExternalId, zoning, transferZone, downstreamAccessNode,
                      accessLinkSegment, allowedModes, type);
                  if(newConnectoid != null) {
                    createdConnectoids.add(newConnectoid);
                  }
    });

    return createdConnectoids;
  }

  /**
   * Identify transfer zones from provided container that have connectoids supporting any of the provided modes for
   * PT_STOP labelled connectoids as well as the transfer zones from the set that on top do not support
   * any of the access/egress type modes provided.
   *
   * todo: not great should be for single accessEgress mode at the time, as now partial support is classified
   *  as no support (some modes supported, some not, then we still say no support)
   *
   * @param connectoidsByTransferZoneMapping mapping can be obtained from zoning
   * @param ptStopAccessModes to filter by (pt stop is type filter)
   * @param accessEgressModes modes to search for mismatches within pt stop mode(s) transfer zones
   * @return identified transfer zones supporting main mode(s) [first] and of those transfer zones, the ones not
   * supporting alt modes while supporting one or more main mode(s), so a subset [second]
   */
  public static Pair<Collection<TransferZone>,Collection<TransferZone>>
  findPtStopModeTransferZonesWithoutAccessEgressModeSupport(
          Map<Zone, Set<TransferConnectoid>> connectoidsByTransferZoneMapping,
          Collection<Mode> ptStopAccessModes,
          Set<Mode> accessEgressModes) {

    var accessEgressTypes =
        Set.of(ZoneConnectoidType.ZONE_ACCESS,ZoneConnectoidType.ZONE_EGRESS, ZoneConnectoidType.ZONE_ACCESS_EGRESS);

    // reduce to zones with any main mode connectoid listed
    Set<TransferZone> mainModeTransferZones = new TreeSet<>();
    for(var entry : connectoidsByTransferZoneMapping.entrySet()){
      var transferZone = entry.getKey();
      var connectoidsForTransferZone = entry.getValue();
      boolean mainModeTransferZonesSupport = connectoidsForTransferZone.stream().flatMap(
          tc -> tc.getAccessZoneEntriesStream(ZoneConnectoidType.PT_VEHICLE_STOP)).filter(
            e -> e.getAccessZone().equals(transferZone)).anyMatch(
              connectoidZoneEntryForTransferZone ->
                connectoidZoneEntryForTransferZone.isAnyModeAllowed(ptStopAccessModes));
      if(mainModeTransferZonesSupport){
        mainModeTransferZones.add((TransferZone) transferZone);
      }
    }

    // reduce to transfer zones with main mode but NO (single) alt mode connectoids
    Set<TransferZone> transferZonesDisconnectedFromAltModes = new TreeSet<>();
    for(var transferZone : mainModeTransferZones){
      // check connectoid-zone-type entries of transfer zone without any alt mode support
      boolean transferZoneWithoutSupport =
          connectoidsByTransferZoneMapping.get(transferZone).stream().flatMap(
            tc -> tc.getAccessZoneEntriesStream(accessEgressTypes)).filter(
                e -> e.getAccessZone().equals(transferZone)).noneMatch(
                    connectoidZoneEntryForTransferZone ->
                        !connectoidZoneEntryForTransferZone.isAnyModeAllowed(ptStopAccessModes) &&
                            connectoidZoneEntryForTransferZone.isAnyModeAllowed(accessEgressModes));
      if(transferZoneWithoutSupport){
        transferZonesDisconnectedFromAltModes.add(transferZone);
      }
    }
    return Pair.of(mainModeTransferZones, transferZonesDisconnectedFromAltModes);
  }

  /**
   * for a given connectoid add a new or expand existing partial undirected entry (ACCESS/EGRESS) with the provided mode
   *
   * @param connectoid to use
   * @param transferZone to use
   * @param candidateMode the mode
   * @param accessOrEgressType required for the entry
   * @return true when success false otherwise
   */
  public static boolean expandConnectoidWithModeForGeneralAccessOrEgress(
      TransferConnectoid connectoid,
      TransferZone transferZone,
      Mode candidateMode,
      ZoneConnectoidType accessOrEgressType) {

    boolean entryExists = connectoid.hasAccessZoneEntry(transferZone, accessOrEgressType);
    if(!entryExists){
      var entry = connectoid.createUndirectedAccessZoneEntry(transferZone, accessOrEgressType);
      entry.addAllowedModes(candidateMode);
    }else {
      var zoneConnectoidEntry = connectoid.getAccessZoneEntry(transferZone, accessOrEgressType);
      if (!zoneConnectoidEntry.isAllModesAllowed()) {
        zoneConnectoidEntry.addAllowedModes(candidateMode);
      }
    }
    return true;
  }

  /**
   * For given connectoids, check if any of the connected segments to the connectoid vertex support the provided modes for
   * access and egress. If so, expand the connectoid by adding a new undirected access entry for the compatible modes,
   * or expand existing undirected entry with the eligible modes currently not listed
   *
   * @param transferZone transferZone
   * @param directedConnectoids to check if ok to add any of the provided modes to
   * @param bannedModes the candidate entry link segments are not allowed to support any of the banned modes
   * @param modesToAdd the modes to add
   * @return modes added to one or more connectoids for their access (first) or egress (second)
   */
  public static Pair<Set<Mode>,Set<Mode>> expandTransferConnectoidsWithEligibleUndirectedAccessEgressEntries(
      TransferZone transferZone,
      Set<TransferConnectoid> directedConnectoids,
      Set<Mode> bannedModes,
      Set<Mode> modesToAdd) {

    var remainingAccessModesToAdd = new TreeSet<>(modesToAdd);
    var remainingEgressModesToAdd = new TreeSet<>(modesToAdd);
    for (var connectoid : directedConnectoids) {
      if (remainingAccessModesToAdd.isEmpty() && remainingEgressModesToAdd.isEmpty()) {
        break;
      }
      var refNode = connectoid.getReferenceVertex();

      /* access is travel towards the zone, arriving at the node on a segment entering it; egress is travel away from
       * the zone, leaving the node on a segment exiting it. Eligible segments are not tracked explicitly, they only
       * tell whether a mode can use the connectoid */
      Set<Mode> validCandidateAccessModes = new TreeSet<>();
      Set<Mode> validCandidateEgressModes = new TreeSet<>();
      refNode.streamEdgeSegments().filter(ls -> ls instanceof LinkSegment).map(ls -> (LinkSegment) ls).
          // no banned modes
          filter(ls -> !ls.isAnyModeAllowed(bannedModes)).forEach(ls ->
          {
            if (ls.isDownstreamVertex(refNode)) {
              validCandidateAccessModes.addAll(ls.getAllowedModesFrom(remainingAccessModesToAdd));
            } else {
              validCandidateEgressModes.addAll(ls.getAllowedModesFrom(remainingEgressModesToAdd));
            }
          }
      );

      if(validCandidateAccessModes.isEmpty() && validCandidateEgressModes.isEmpty()){
        continue;
      }

      // intersection of access/egress
      var validCombinedAccessEgressModes = new TreeSet<>(validCandidateAccessModes);
      validCombinedAccessEgressModes.retainAll(validCandidateEgressModes);

      validCombinedAccessEgressModes.forEach(m -> {
        expandConnectoidWithModeForGeneralAccessOrEgress(
            connectoid, transferZone, m, ZoneConnectoidType.ZONE_ACCESS_EGRESS);
      });

      // remaining access/egress separate to add
      validCandidateAccessModes.removeAll(validCombinedAccessEgressModes);
      validCandidateEgressModes.removeAll(validCombinedAccessEgressModes);

      validCandidateAccessModes.forEach(m -> {
        expandConnectoidWithModeForGeneralAccessOrEgress(
            connectoid, transferZone, m, ZoneConnectoidType.ZONE_ACCESS);
      });
      validCandidateEgressModes.forEach(m -> {
        expandConnectoidWithModeForGeneralAccessOrEgress(
            connectoid, transferZone, m, ZoneConnectoidType.ZONE_EGRESS);
      });

      // remaining after added modes removed
      remainingAccessModesToAdd.removeAll(validCombinedAccessEgressModes);
      remainingAccessModesToAdd.removeAll(validCandidateAccessModes);
      remainingEgressModesToAdd.removeAll(validCombinedAccessEgressModes);
      remainingEgressModesToAdd.removeAll(validCandidateEgressModes);
    }
    var addedAccessModes = new TreeSet<>(modesToAdd);
    addedAccessModes.removeAll(remainingAccessModesToAdd);
    var addedEgressModes = new TreeSet<>(modesToAdd);
    addedEgressModes.removeAll(remainingEgressModesToAdd);
    return Pair.of(addedAccessModes, addedEgressModes);
  }

  /**
   * Find closest mode compatible node (so any attached link with an entry segment supporting the mode constraints)
   * of provided links that do not have any banned modes but will have at
   * least one of the supported modes
   *
   * @param geometry reference centroid of geometry used
   * @param mode to check
   * @param eligibleLinkSegments link segments to consider
   * @param selectUpstreamNode flag
   * @param maxSearchRadius only consider if within search radius
   * @param suppressSpatialWarnings flag for logging
   * @param geoUtils to use
   * @return pair of closest node and distance (null if not available)
   */
  public static Pair<DirectedVertex, Double> findClosestModeCompatibleNode(
      Geometry geometry,
      Mode mode,
      Stream<MacroscopicLinkSegment> eligibleLinkSegments,
      boolean selectUpstreamNode,
      PlanitJtsCrsUtils geoUtils,
      double maxSearchRadius,
      boolean suppressSpatialWarnings) {

    var refCoord = geometry.getCentroid().getCoordinate();
    var modeCompatibleNearbyNodes = eligibleLinkSegments.filter(
        ls -> ls.isModeAllowed(mode)).map(
        ls -> selectUpstreamNode ? ls.getUpstreamNode() : ls.getDownstreamNode()).collect(Collectors.toList());
    return PlanitEntityGeoUtils.findPlanitEntityClosest(
        refCoord,
        modeCompatibleNearbyNodes,
        maxSearchRadius,
        suppressSpatialWarnings,
        geoUtils);
  }

  /**
   * Obtain explicit allowed modes across all connectoids all directed access segments that are explicitly
   * registered
   *
   * @param connectoids to collect from
   * @return modes found
   */
  public static Set<Mode> extractExplicitAllowedModesFromDirectedAccessEntries(Set<TransferConnectoid> connectoids) {
    return connectoids.stream().flatMap(TransferConnectoid::getExplicitAccessLinkSegmentsStream).flatMap(
        ls -> ((MacroscopicLinkSegment)ls).getAllowedModes().stream()).collect(Collectors.toSet());
  }
}
