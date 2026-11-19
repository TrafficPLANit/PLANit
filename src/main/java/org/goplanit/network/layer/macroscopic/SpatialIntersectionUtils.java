package org.goplanit.network.layer.macroscopic;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.goplanit.algorithms.shortest.ShortestPathBreadthFirst;
import org.goplanit.algorithms.shortest.ShortestPathBreadthFirstResult;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Utilities answering what modes can do inside a spatial intersection: where a mode can get from a segment over its
 * internal segments, and which of its movements a mode cannot make
 * <p>
 * A spatial intersection keeps its own inner links, so what a mode can do across it follows from those links, their
 * mode access and the turns banned at its member nodes, not from its movements alone. These utilities search the
 * network inside the intersection as it is now. For example, whether pedestrians arriving on a footway can reach the
 * footway leaving on the far side, or which of a junction's movements cars cannot make because a turn is banned at one
 * corner. Collapsing an intersection asks them before it changes anything.
 * </p>
 *
 * @author markr
 */
public class SpatialIntersectionUtils {

  /**
   * Search from an approach over the internal segments of a spatial intersection for a mode, see
   * {@link #findExitsReached}. The search never goes back along the link it just came along, nor onto the approach's
   * own link. Otherwise a mode could get round a turn banned at a member node by driving into a two-way inner link and
   * straight back, which no driver does inside a junction, and the movement would count as one the mode can make
   *
   * A member node can also hold connectoid segments, once the zoning is integrated with the network, for example to
   * write it or to assign on it. The search runs over link segments of either kind: a connectoid segment has the
   * centroid of its zone at one end, so it is never internal, and is at most reached as an exit
   *
   * @param layer holding the banned turns
   * @param intersection to search through
   * @param approach to start from
   * @param mode travelling, only over link segments allowing it
   * @param respectBannedTurns when true a turn banned for the mode is not taken, when false banned turns are ignored
   * @return the internal segments and exits reached, the approach included, each with the shortest connection to it
   *         and its distance from where the approach ends, in metres
   */
  public static ShortestPathBreadthFirstResult<LinkSegment> searchThrough(
      final MacroscopicNetworkLayer layer, final SpatialIntersection intersection,
      final LinkSegment approach, final Mode mode, boolean respectBannedTurns) {
    Predicate<LinkSegment> isInternal =
        segment -> ListUtils.containsByIdentity(intersection.getInternalSegments(), segment);
    return ShortestPathBreadthFirst.<LinkSegment>executeAlongEdgeSegments(approach, true,
        (from, onto) -> onto.isModeAllowed(mode) && onto.getParent() != approach.getParent() &&
            onto.getParent() != from.getParent() &&
            (isInternal.test(onto) || intersection.isExitSegment(onto)) &&
            !(respectBannedTurns && MacroscopicNetworkLayerUtils.isTurnBannedForMode(layer, from, onto, mode)),
        isInternal, segment -> segment.getParent().getLengthMeters(), Double.POSITIVE_INFINITY);
  }

  /**
   * Find the exits a mode can reach from an approach over the internal segments of a spatial intersection, along the
   * shortest connection. An exit is a link segment starting at a member node and ending outside; the exit back along
   * the approach's own link is not counted, and a connection never goes back along the link it just came along. The
   * search runs over link segments rather than nodes, so a banned turn between two segments at a member node is
   * respected even when the node can be reached another way
   *
   * @param layer holding the banned turns
   * @param intersection to search through
   * @param approach to start from, or any other segment entering a member node from outside, or an internal segment
   * @param mode travelling, only over link segments allowing it
   * @param respectBannedTurns when true a turn banned for the mode is not taken, when false banned turns are ignored
   * @return each exit reached with its distance from where the approach ends, in metres, a connectoid segment leaving a
   *         member node included, see {@link #searchThrough}
   */
  public static Map<LinkSegment, Double> findExitsReached(
      final MacroscopicNetworkLayer layer, final SpatialIntersection intersection,
      final LinkSegment approach, final Mode mode, boolean respectBannedTurns) {
    var reached = searchThrough(layer, intersection, approach, mode, respectBannedTurns);
    var exitsReached = new LinkedHashMap<LinkSegment, Double>();
    reached.getReached().stream().filter(intersection::isExitSegment)
        .forEach(exit -> exitsReached.put(exit, reached.getCostToReach(exit)));
    return exitsReached;
  }

  /**
   * For each internal segment of a spatial intersection a mode can reach from outside, the segments entering the
   * intersection from outside from which it can, banned turns respected or not, see {@link #searchThrough}: its
   * approaches, and the other segments entering a member node from outside
   * <p>
   * For what is reached over an internal segment, such as a stop, and has to be reached from outside once the internal
   * segments are gone, for example when the intersection is collapsed for the mode. One search per segment entering
   * from outside serves every internal segment
   * </p>
   *
   * @param layer holding the banned turns
   * @param intersection to search through
   * @param mode travelling, only over link segments allowing it
   * @param respectBannedTurns when true a turn banned for the mode is not taken, when false banned turns are ignored
   * @return per internal segment reached, by identity, the segments entering from outside that reach it; an internal
   *         segment none reaches is absent
   */
  public static Map<LinkSegment, List<LinkSegment>> findSegmentsEnteringFromOutsideReachingEachInternalSegment(
      final MacroscopicNetworkLayer layer, final SpatialIntersection intersection, final Mode mode,
      boolean respectBannedTurns) {
    var reaching = new IdentityHashMap<LinkSegment, List<LinkSegment>>();
    for (var entering : IntersectionUtils.<LinkSegment>findSegmentsEnteringFromOutside(
        intersection, segment -> segment.isModeAllowed(mode))) {
      searchThrough(layer, intersection, entering, mode, respectBannedTurns).getReached().stream()
          .filter(segment -> ListUtils.containsByIdentity(intersection.getInternalSegments(), segment))
          .forEach(segment -> reaching.computeIfAbsent(segment, s -> new ArrayList<>()).add(entering));
    }
    return reaching;
  }

  /**
   * For each movement of a spatial intersection, the given modes that have access to its from and to segments but
   * cannot make it, see {@link SpatialMovementUtils#findBannedModes}: the movement is banned for the mode directly,
   * every connection for the mode passes a turn banned at a member node, or every connection passes an inner link the
   * mode may not use. In the last case no ban says so at all, such as for an inner link that is one-way for cars and
   * two-way for pedestrians, so every movement is worked out
   *
   * @param layer holding the banned turns
   * @param intersection to work out
   * @param modes to find the banned ones among
   * @param includeDirectBan when true a movement's direct ban counts
   * @param includeImplicitBan when true an implicit ban counts
   * @return the given modes banned, in their order, per movement with at least one, in movement order
   */
  public static Map<SpatialMovement, List<Mode>> findModesBannedOfEachMovement(final MacroscopicNetworkLayer layer,
      final SpatialIntersection intersection, final List<Mode> modes, boolean includeDirectBan,
      boolean includeImplicitBan) {
    var bannedOfMovement = new LinkedHashMap<SpatialMovement, List<Mode>>();
    for (var movement : intersection.getMovements()) {
      var bannedModes = SpatialMovementUtils.findBannedModes(
          layer, intersection, movement, includeDirectBan, includeImplicitBan);
      var banned = modes.stream().filter(bannedModes::contains).collect(Collectors.toList());
      if (!banned.isEmpty()) {
        bannedOfMovement.put(movement, banned);
      }
    }
    return bannedOfMovement;
  }

  /**
   * For each of the given modes, the exits each segment entering a member node from outside that is not an approach can
   * reach, see {@link #findExitsReached}. No movement starts from such a segment, such as a footway entering a
   * signalised junction whose approaches come from its vehicle signals, so this is what the mode can do from it
   *
   * @param layer holding the banned turns
   * @param intersection to search through
   * @param modes to search for
   * @param respectBannedTurns when true a turn banned for the mode is not taken, when false banned turns are ignored
   * @return per mode with any, each entering segment that is not an approach with an exit it can reach
   */
  public static Map<Mode, List<Pair<LinkSegment, LinkSegment>>>
      findExitsReachedFromSegmentsEnteringFromOutsideThatAreNotApproaches(final MacroscopicNetworkLayer layer,
      final SpatialIntersection intersection, final List<Mode> modes, boolean respectBannedTurns) {
    var exitsReached = new LinkedHashMap<Mode, List<Pair<LinkSegment, LinkSegment>>>();
    for (var entering : IntersectionUtils.<LinkSegment>findSegmentsEnteringFromOutsideThatAreNotApproaches(
        intersection, segment -> true)) {
      for (var mode : modes) {
        if (!entering.isModeAllowed(mode)) {
          continue;
        }
        findExitsReached(layer, intersection, entering, mode, respectBannedTurns).keySet().forEach(exit ->
            exitsReached.computeIfAbsent(mode, key -> new ArrayList<>()).add(Pair.of(entering, exit)));
      }
    }
    return exitsReached;
  }
}
