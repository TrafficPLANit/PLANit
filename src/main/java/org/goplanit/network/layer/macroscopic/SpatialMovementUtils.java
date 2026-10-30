package org.goplanit.network.layer.macroscopic;

import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.goplanit.algorithms.shortest.ShortestPathBreadthFirstResult;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;

/**
 * Utilities for the movements of a spatial intersection. A movement records neither the internal segments it passes
 * nor the modes it is banned for implicitly, see {@link SpatialMovement}. These utilities work them out from the
 * network inside the intersection as it is now, so the answers follow every change of internal segments, mode access
 * and banned turns.
 *
 * @author markr
 */
public class SpatialMovementUtils {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(SpatialMovementUtils.class.getCanonicalName());

  /**
   * The shortest connection over the internal segments of a spatial intersection from an approach to each exit they
   * connect for any of the given modes, banned turns ignored. Modes are tried in order of id; of connections of equal
   * length, the first found is kept
   *
   * @param layer of the intersection
   * @param intersection to search through
   * @param approach to start from
   * @param modes to search for
   * @return for each exit reached, the search that reached it over the shortest connection
   */
  private static Map<LinkSegment, ShortestPathBreadthFirstResult<LinkSegment>> findShortestConnectionToEachExitForModes(
      final MacroscopicNetworkLayer layer, final SpatialIntersection intersection, final LinkSegment approach,
      final Stream<Mode> modes) {
    var shortestOfExit = new LinkedHashMap<LinkSegment, ShortestPathBreadthFirstResult<LinkSegment>>();
    for (var mode : modes.sorted(Comparator.comparingLong(Mode::getId)).collect(Collectors.toList())) {
      var search =
          MacroscopicNetworkLayerUtils.searchThroughSpatialIntersection(layer, intersection, approach, mode, false);
      search.getReached().stream().filter(intersection::isExitSegment).forEach(exit -> {
        var shortest = shortestOfExit.get(exit);
        if (shortest == null || search.getCostToReach(exit) < shortest.getCostToReach(exit)) {
          shortestOfExit.put(exit, search);
        }
      });
    }
    return shortestOfExit;
  }

  /** Constructor to disallow instance creation */
  private SpatialMovementUtils() {
  }

  /**
   * The search holding the shortest connection of a movement of a spatial intersection, see
   * {@link #findShortestConnectionToEachExit}
   *
   * @param layer of the intersection
   * @param intersection the movement belongs to
   * @param movement to find the connection of
   * @return the search, null when the movement's from and to segments are no longer connected
   */
  private static ShortestPathBreadthFirstResult<LinkSegment> findShortestConnection(
      final MacroscopicNetworkLayer layer, final SpatialIntersection intersection, final SpatialMovement movement) {
    /* active modes included: a movement only they make has its connection for them, any other its connection for the
     * motorised road and rail modes, however the movements were stored */
    return findShortestConnectionToEachExit(layer, intersection, movement.getLinkSegmentFrom(), true)
        .get(movement.getLinkSegmentTo());
  }

  /**
   * The internal segments on the shortest connection of a movement, without its from and to segments
   *
   * @param search holding the shortest connection of the movement
   * @param movement the connection is of
   * @return internal segments in order, empty when the from and to segments meet at one member node
   */
  private static List<LinkSegment> findInternalSegmentsOnConnection(
      final ShortestPathBreadthFirstResult<LinkSegment> search, final SpatialMovement movement) {
    var onConnection = search.getEntitiesOnConnectionTo(movement.getLinkSegmentTo());
    return List.copyOf(onConnection.subList(1, onConnection.size() - 1));
  }

  /**
   * Find the shortest connection over the internal segments of a spatial intersection from an approach to each exit
   * they connect, banned turns ignored, for the motorised road and rail modes allowed on the approach, see
   * {@link Mode#isMotorisedRoadOrRailMode()}. When active modes are included, an exit no such mode reaches, such as a
   * footway, gets its shortest connection for the other modes allowed on the approach, such as pedestrians; an exit a
   * motorised mode reaches keeps its connection for the motorised modes. A movement's length is that of its
   * connection, so this gives the length of each movement from the approach. Modes are tried in order of id; of
   * connections of equal length, the first found is kept
   *
   * @param layer of the intersection
   * @param intersection to search through
   * @param approach to start from
   * @param includeActiveModes true to reach the exits only active modes such as pedestrians and bicycles reach too,
   *                           false for the exits the motorised road and rail modes reach only
   * @return for each exit reached, the search that reached it over the shortest connection: its cost to reach the exit
   *         is the length in metres, and the link segments on its connection to the exit run from the approach to the
   *         exit. A connectoid segment leaving a member node is an exit too, see
   *         {@link MacroscopicNetworkLayerUtils#searchThroughSpatialIntersection}
   */
  public static Map<LinkSegment, ShortestPathBreadthFirstResult<LinkSegment>> findShortestConnectionToEachExit(
      final MacroscopicNetworkLayer layer, final SpatialIntersection intersection,
      final LinkSegment approach, boolean includeActiveModes) {
    var shortestOfExit = findShortestConnectionToEachExitForModes(
        layer, intersection, approach, approach.getAllowedModes().stream().filter(Mode::isMotorisedRoadOrRailMode));
    if (includeActiveModes) {
      findShortestConnectionToEachExitForModes(layer, intersection, approach,
          approach.getAllowedModes().stream().filter(mode -> !mode.isMotorisedRoadOrRailMode()))
          .forEach(shortestOfExit::putIfAbsent);
    }
    return shortestOfExit;
  }

  /**
   * Find the internal segments a movement of a spatial intersection passes: those on the shortest connection from its
   * from segment to its to segment, the connection its length is measured on, see
   * {@link #findShortestConnectionToEachExit}. So banned turns are ignored, as for the length. A movement entering and
   * leaving at the same member node passes none
   *
   * @param layer of the intersection
   * @param intersection the movement belongs to
   * @param movement to find the internal segments of
   * @return internal segments passed, in order; empty when it passes none, null when its from and to segments are no
   *         longer connected
   */
  public static List<LinkSegment> findInternalSegmentsPassed(
      final MacroscopicNetworkLayer layer, final SpatialIntersection intersection, final SpatialMovement movement) {
    var search = findShortestConnection(layer, intersection, movement);
    return search == null ? null : findInternalSegmentsOnConnection(search, movement);
  }

  /**
   * Find the internal segments a movement of a spatial intersection passes, see
   * {@link #findInternalSegmentsPassed(MacroscopicNetworkLayer, SpatialIntersection, SpatialMovement)}, and log a
   * warning when their connection is longer than expected, such as longer than the intersection is expected to be
   * across
   *
   * @param layer of the intersection
   * @param intersection the movement belongs to
   * @param movement to find the internal segments of
   * @param warningLengthMeters longest connection expected, in metres
   * @return internal segments passed, in order; empty when it passes none, null when its from and to segments are no
   *         longer connected
   */
  public static List<LinkSegment> findInternalSegmentsPassed(
      final MacroscopicNetworkLayer layer, final SpatialIntersection intersection, final SpatialMovement movement,
      double warningLengthMeters) {
    var search = findShortestConnection(layer, intersection, movement);
    if (search == null) {
      return null;
    }
    double meters = search.getCostToReach(movement.getLinkSegmentTo());
    if (meters > warningLengthMeters) {
      LOGGER.warning(String.format(
          "Movement %s of spatial intersection %s passes %.1f m of internal segments, more than the %.1f m expected",
          movement.getXmlId(), intersection.getXmlId(), meters, warningLengthMeters));
    }
    return findInternalSegmentsOnConnection(search, movement);
  }

  /**
   * Verify if a movement of a spatial intersection is banned implicitly for a mode, from the network as it is now: the
   * mode has access to its from and to segments, but every connection between them over the internal segments passes
   * a turn banned for the mode at a member node, or an internal segment the mode has no access to. This is not stored
   * on the movement, unlike its direct ban, see {@link SpatialMovement#isModeDirectlyBanned(Mode)}
   *
   * @param layer holding the banned turns
   * @param intersection the movement belongs to
   * @param movement to verify
   * @param mode to verify
   * @return true when banned implicitly for the mode, false otherwise, also when the mode has no access to the from or
   *         to segment
   */
  public static boolean isImplicitlyBanned(final MacroscopicNetworkLayer layer, final SpatialIntersection intersection,
      final SpatialMovement movement, final Mode mode) {
    var from = movement.getLinkSegmentFrom();
    var to = movement.getLinkSegmentTo();
    return from.isModeAllowed(mode) && to.isModeAllowed(mode) &&
        !MacroscopicNetworkLayerUtils.findExitsReachedThroughSpatialIntersection(layer, intersection, from, mode, true)
            .containsKey(to);
  }

  /**
   * Verify if a movement of a spatial intersection is banned for a mode with access to its from and to segments: by its
   * direct ban, see {@link SpatialMovement#isModeDirectlyBanned(Mode)}, or implicitly, see {@link #isImplicitlyBanned},
   * as chosen. A mode without access to the from or to segment is not banned, it has no access
   *
   * @param layer holding the banned turns
   * @param intersection the movement belongs to
   * @param movement to verify
   * @param mode to verify
   * @param includeDirectBan when true the movement's direct ban counts
   * @param includeImplicitBan when true an implicit ban counts
   * @return true when banned for the mode by a ban that counts, false otherwise
   */
  public static boolean isBanned(final MacroscopicNetworkLayer layer, final SpatialIntersection intersection,
      final SpatialMovement movement, final Mode mode, boolean includeDirectBan, boolean includeImplicitBan) {
    if (!movement.getLinkSegmentFrom().isModeAllowed(mode) || !movement.getLinkSegmentTo().isModeAllowed(mode)) {
      return false;
    }
    return (includeDirectBan && movement.isModeDirectlyBanned(mode)) ||
        (includeImplicitBan && isImplicitlyBanned(layer, intersection, movement, mode));
  }

  /**
   * Verify if a movement of a spatial intersection is banned for every mode with access to its from and to segments,
   * see {@link #isBanned}, so no mode may use it. Also true when no mode has access to both
   *
   * @param layer holding the banned turns
   * @param intersection the movement belongs to
   * @param movement to verify
   * @param includeDirectBan when true the movement's direct ban counts
   * @param includeImplicitBan when true an implicit ban counts
   * @return true when banned for every mode with access, false otherwise
   */
  public static boolean isBannedForEveryMode(final MacroscopicNetworkLayer layer,
      final SpatialIntersection intersection, final SpatialMovement movement, boolean includeDirectBan,
      boolean includeImplicitBan) {
    var to = movement.getLinkSegmentTo();
    return movement.getLinkSegmentFrom().getAllowedModes().stream().filter(to::isModeAllowed)
        .allMatch(mode -> isBanned(layer, intersection, movement, mode, includeDirectBan, includeImplicitBan));
  }

  /**
   * Find the modes a movement of a spatial intersection is banned for, among the modes with access to its from and to
   * segments, see {@link #isBanned}. The movement stores only its direct ban, so the answer follows every change of
   * mode access and of banned turns
   *
   * @param layer holding the banned turns
   * @param intersection the movement belongs to
   * @param movement to find the banned modes of
   * @param includeDirectBan when true the movement's direct ban counts
   * @param includeImplicitBan when true an implicit ban counts
   * @return modes the movement is banned for, empty when none
   */
  public static Set<Mode> findBannedModes(final MacroscopicNetworkLayer layer, final SpatialIntersection intersection,
      final SpatialMovement movement, boolean includeDirectBan, boolean includeImplicitBan) {
    Set<Mode> modes = Collections.newSetFromMap(new IdentityHashMap<>());
    movement.getLinkSegmentFrom().getAllowedModes().stream()
        .filter(mode -> isBanned(layer, intersection, movement, mode, includeDirectBan, includeImplicitBan))
        .forEach(modes::add);
    return modes;
  }
}
