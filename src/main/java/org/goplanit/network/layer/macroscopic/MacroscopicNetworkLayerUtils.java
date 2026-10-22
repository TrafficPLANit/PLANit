package org.goplanit.network.layer.macroscopic;

import org.goplanit.algorithms.shortest.ShortestPathBreadthFirst;
import org.goplanit.algorithms.shortest.ShortestPathBreadthFirstResult;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.algorithms.ConnectedComponents;
import org.goplanit.utils.graph.directed.Connectivity;
import org.goplanit.utils.graph.directed.algorithms.StronglyConnectedComponents;
import org.goplanit.utils.misc.binning.BinBuilder;
import org.goplanit.utils.misc.binning.BinnedCount;
import org.goplanit.utils.misc.binning.BinningConfiguration;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurn;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentUtils;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;
import org.goplanit.utils.network.layer.physical.Node;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Utilities operating on a macroscopic network layer as a whole, i.e. requiring both its graph and the mode
 * information its link segment types carry.
 *
 * @author markr
 */
public class MacroscopicNetworkLayerUtils {

  /** Constructor to disallow instance creation */
  private MacroscopicNetworkLayerUtils(){}

  /**
   * Subnetwork sizes reported against, in vertices. Deliberately not configurable for now: the bins exist to make a
   * size threshold choosable from a single run, and this spread is fine grained where the choice actually is, namely
   * among the small subnetworks.
   */
  private static final BinningConfiguration<Integer> SUBNETWORK_SIZE_BINS =
      BinBuilder.ofInclusiveIntegerLowerBounds(1, 2, 5, 20, 50, 200);

  /**
   * Withdraw a mode's access from the subnetworks of that mode which do not meet the given criteria, leaving its
   * access only where the mode has a network worth having.
   * <p>
   * The counterpart of dangling subnetwork removal, judged per mode rather than per track type, and withdrawing
   * access rather than removing infrastructure. Both differences are necessary rather than stylistic. Track type
   * is too coarse to see the problem, since a one way street usually still permits pedestrians against the flow
   * and is therefore bidirectional as road infrastructure while being a dead end for cars. And removal is the
   * wrong remedy, because the link a car cannot use is typically carrying another mode perfectly well.
   * </p>
   * <p>
   * What counts as a subnetwork follows the given {@link Connectivity}: weakly connected components when
   * ignoring direction, strongly connected components when a mode must be able to both reach and leave. The size
   * thresholds then apply to those components exactly as they do for dangling subnetwork removal, so
   * {@code belowSize} at its maximum together with {@code alwaysKeepLargest} and {@link Connectivity#STRONG}
   * leaves the mode with a single network it can route across in full.
   * </p>
   * <p>
   * Nothing is removed here, not even a segment left with no permitted mode at all: it is given a type granting
   * nothing, and {@link MacroscopicNetworkLayerModifier#removeInfrastructureWithoutModeAccess()} clears such
   * segments once every mode has been dealt with. Keeping the two apart is what allows modes to be processed
   * independently, since removing as we go would mutate the graph underneath the modes not yet processed.
   * </p>
   * <p>
   * Connectoids are deliberately not considered. They are not part of the physical network, so what they
   * reference does not decide what the network may do; a connectoid left pointing at a segment that no longer
   * grants its mode access is a zoning concern to be handled on the zoning side.
   * </p>
   *
   * @param layer to apply this to
   * @param mode the mode whose access is judged
   * @param belowSize withdraw access on subnetworks smaller than this
   * @param aboveSize withdraw access on subnetworks larger than this, typically the maximum value
   * @param alwaysKeepLargest when true the largest subnetwork keeps its access whatever the thresholds say
   * @param connectivity what constitutes a subnetwork of this mode
   * @param retainAccessRegardless segments that keep their access whatever the outcome, may be null. An escape
   *          hatch for a caller that knows something this does not; note that every segment it protects leaves
   *          the mode's network that much less connected
   * @return what was withdrawn for this mode
   */
  public static ModeAccessRestrictionResult restrictModeAccessToConnectedSubNetworks(
      final MacroscopicNetworkLayer layer,
      final Mode mode,
      final int belowSize,
      final int aboveSize,
      final boolean alwaysKeepLargest,
      final Connectivity connectivity,
      final Predicate<MacroscopicLinkSegment> retainAccessRegardless) {

    var components = collectSubNetworks(layer, mode, connectivity);
    final int largestSize = components.isEmpty() ? 0 : components.get(0).size();

    var retainedVertices = new HashSet<Vertex>();
    int retainedSubNetworks = 0;
    var discardedSubNetworks = BinnedCount.of(SUBNETWORK_SIZE_BINS);
    var withdrawnLinkSegments = BinnedCount.of(SUBNETWORK_SIZE_BINS);
    /* so a withdrawn segment can be attributed to the size of the subnetwork it belonged to, which is what makes
     * the distribution readable rather than just the totals */
    final Map<Vertex, Integer> discardedSubNetworkSizeByVertex = new HashMap<>();

    for (var component : components) {
      final int size = component.size();
      /* deliberately the same rule dangling subnetwork removal applies, so that the thresholds mean the same
       * thing whichever of the two a caller reaches for */
      boolean withdraw = (size < largestSize || !alwaysKeepLargest) && (size < belowSize || size > aboveSize);
      if (!withdraw) {
        retainedVertices.addAll(component);
        ++retainedSubNetworks;
        continue;
      }
      discardedSubNetworks.increment(size);
      for (var vertex : component) {
        discardedSubNetworkSizeByVertex.put(vertex, size);
      }
    }

    var modifiedTypes = new ModifiedLinkSegmentTypes();
    int withdrawn = 0;
    int protectedSegments = 0;

    for (var linkSegment : layer.getLinkSegments()) {
      if (!linkSegment.isModeAllowed(mode)) {
        continue;
      }
      if (retainedVertices.contains(linkSegment.getUpstreamVertex()) &&
          retainedVertices.contains(linkSegment.getDownstreamVertex())) {
        continue;
      }
      if (retainAccessRegardless != null && retainAccessRegardless.test(linkSegment)) {
        ++protectedSegments;
        continue;
      }
      withdrawModeAccess(layer, modifiedTypes, linkSegment, mode);
      ++withdrawn;

      /* attribute to whichever end sat in a discarded subnetwork, the upstream one when both did */
      var size = discardedSubNetworkSizeByVertex.get(linkSegment.getUpstreamVertex());
      if (size == null) {
        size = discardedSubNetworkSizeByVertex.get(linkSegment.getDownstreamVertex());
      }
      if (size != null) {
        withdrawnLinkSegments.increment(size);
      }
    }
    return new ModeAccessRestrictionResult(mode, withdrawn, protectedSegments, components.size(), retainedSubNetworks,
        largestSize, discardedSubNetworks, withdrawnLinkSegments);
  }

  /**
   * Partition the layer into this mode's subnetworks under the given notion of connectivity, largest first.
   *
   * @param layer to inspect
   * @param mode to partition for
   * @param connectivity what constitutes a subnetwork
   * @return subnetworks, largest first
   */
  private static List<List<Node>> collectSubNetworks(
      final MacroscopicNetworkLayer layer, final Mode mode, final Connectivity connectivity) {
    if (connectivity.isStrong()) {
      return StronglyConnectedComponents.execute(
          layer.getNodes(), MacroscopicLinkSegmentUtils.permitsMode(mode)).getComponents();
    }
    return ConnectedComponents.execute(layer.getNodes(), edgeCarriesMode(mode));
  }

  /**
   * Condition for an edge taking part in a mode's undirected network, i.e. either of its segments permits the mode
   *
   * @param mode to test for
   * @return predicate to use
   */
  private static Predicate<? super Edge> edgeCarriesMode(final Mode mode) {
    return edge -> {
      if (!(edge instanceof MacroscopicLink)) {
        return false;
      }
      var link = (MacroscopicLink) edge;
      return (link.hasEdgeSegmentAb() && link.getLinkSegmentAb().isModeAllowed(mode)) ||
             (link.hasEdgeSegmentBa() && link.getLinkSegmentBa().isModeAllowed(mode));
    };
  }

  /**
   * Find or register a link segment type identical to the given one except that it no longer grants the given modes
   * access. Types are shared by many segments, so the original type is never changed: the variant is a copy, found
   * in the cache when made before for the same original and withdrawn modes, and created and registered on the layer
   * otherwise, with a {@code MODIFIED[id]:} prefix on its external id and name
   *
   * @param layer owning the link segment types
   * @param modifiedTypes cache of already created variants
   * @param original the type to vary
   * @param withdrawnModes the modes the variant no longer grants access to
   * @return the variant, or the original itself when no mode is withdrawn
   */
  public static MacroscopicLinkSegmentType findOrRegisterTypeWithoutModes(
      final MacroscopicNetworkLayer layer,
      final ModifiedLinkSegmentTypes modifiedTypes,
      final MacroscopicLinkSegmentType original,
      final Set<Mode> withdrawnModes) {
    if (withdrawnModes.isEmpty()) {
      return original;
    }
    var variant = modifiedTypes.getModifiedLinkSegmentType(original, Collections.emptySet(), withdrawnModes);
    if (variant == null) {
      variant = layer.getLinkSegmentTypes().getFactory().createUniqueDeepCopyOf(original);
      layer.getLinkSegmentTypes().register(variant);
      variant.setXmlId(Long.toString(variant.getId()));

      final String modified = String.format("MODIFIED[%d]:", variant.getId());
      if (variant.hasExternalId()) {
        variant.setExternalId(modified + variant.getExternalId());
      }
      if (variant.hasName()) {
        variant.setName(modified + variant.getName());
      }

      variant.removeModeAccess(withdrawnModes);
      modifiedTypes.addModifiedLinkSegmentType(original, variant, Collections.emptySet(), withdrawnModes);
    }
    return variant;
  }

  /**
   * Replace the segment's type with one identical except that it no longer grants the given mode access, see
   * {@link #findOrRegisterTypeWithoutModes}. The type is never changed in place: that would silently withdraw access
   * everywhere the type is used. A variant is found or created once per (original type, withdrawn mode) pair.
   *
   * @param layer owning the link segment types
   * @param modifiedTypes cache of already created variants
   * @param linkSegment to adjust
   * @param mode to withdraw
   */
  public static void withdrawModeAccess(
      final MacroscopicNetworkLayer layer,
      final ModifiedLinkSegmentTypes modifiedTypes,
      final MacroscopicLinkSegment linkSegment,
      final Mode mode) {

    var original = linkSegment.getLinkSegmentType();
    if (original == null) {
      return;
    }

    linkSegment.setLinkSegmentType(
        findOrRegisterTypeWithoutModes(layer, modifiedTypes, original, Collections.singleton(mode)));
  }

  /**
   * Verify if a turn from one link segment onto the next is banned for a mode. A banned turn of a macroscopic layer
   * names the modes it bans, or bans every mode when it names none
   *
   * @param layer holding the banned turns
   * @param from link segment arriving at the node
   * @param to link segment leaving it
   * @param mode to verify
   * @return true when a banned turn forbids the turn to the mode, false otherwise
   */
  public static boolean isTurnBannedForMode(
      final MacroscopicNetworkLayer layer, MacroscopicLinkSegment from, MacroscopicLinkSegment to, Mode mode) {
    return layer.getBannedTurns().getBySegment(from).stream()
        .anyMatch(banned -> banned.getSegmentFrom() == from && banned.getSegmentTo() == to &&
            (!(banned instanceof MacroscopicBannedTurn) || ((MacroscopicBannedTurn) banned).isModeBanned(mode)));
  }

  /**
   * Find the shortest connections from a node to the nodes within a distance, over the links that may be followed,
   * such as those a motorised road mode may use, in either direction, one-way or not
   *
   * @param source node to start from
   * @param canFollow true for a link the connections may follow
   * @param canReach true for a node a connection may reach and pass
   * @param maximumDistanceMeters furthest a node reached may be
   * @return the nodes reached, the source included, with their distances in metres and the connections to them
   */
  public static ShortestPathBreadthFirstResult<Node> findShortestConnections(
      Node source, Predicate<? super Edge> canFollow, Predicate<Node> canReach, double maximumDistanceMeters) {
    return ShortestPathBreadthFirst.executeAlongEdges(source,
        edge -> canFollow.test(edge) ? edge.getLengthMeters() : Double.POSITIVE_INFINITY, canReach,
        maximumDistanceMeters);
  }

  /**
   * Search from an approach over the internal segments of a spatial intersection for a mode, see
   * {@link #findExitsReachedThroughSpatialIntersection}. The search never goes back along the link it just came along,
   * nor onto the approach's own link. Otherwise a mode could get round a turn banned at a member node by driving into a
   * two-way inner link and straight back, which no driver does inside a junction, and the movement would count as one
   * the mode can make
   *
   * @param layer holding the banned turns
   * @param intersection to search through
   * @param approach to start from
   * @param mode travelling, only over link segments allowing it
   * @param respectBannedTurns when true a turn banned for the mode is not taken, when false banned turns are ignored
   * @return the internal segments and exits reached, the approach included, each with the shortest connection to it
   *         and its distance from where the approach ends, in metres
   */
  static ShortestPathBreadthFirstResult<MacroscopicLinkSegment> searchThroughSpatialIntersection(
      final MacroscopicNetworkLayer layer, final SpatialIntersection intersection,
      final MacroscopicLinkSegment approach, final Mode mode, boolean respectBannedTurns) {
    Predicate<MacroscopicLinkSegment> isInternal =
        segment -> ListUtils.containsByIdentity(intersection.getInternalSegments(), segment);
    return ShortestPathBreadthFirst.<MacroscopicLinkSegment>executeAlongEdgeSegments(approach, true,
        (from, onto) -> onto.isModeAllowed(mode) && onto.getParent() != approach.getParent() &&
            onto.getParent() != from.getParent() &&
            (isInternal.test(onto) || intersection.isExitSegment(onto)) &&
            !(respectBannedTurns && isTurnBannedForMode(layer, from, onto, mode)),
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
   * @param approach to start from, or any other segment entering a member node from outside
   * @param mode travelling, only over link segments allowing it
   * @param respectBannedTurns when true a turn banned for the mode is not taken, when false banned turns are ignored
   * @return each exit reached with its distance from where the approach ends, in metres
   */
  public static Map<MacroscopicLinkSegment, Double> findExitsReachedThroughSpatialIntersection(
      final MacroscopicNetworkLayer layer, final SpatialIntersection intersection,
      final MacroscopicLinkSegment approach, final Mode mode, boolean respectBannedTurns) {
    var reached = searchThroughSpatialIntersection(layer, intersection, approach, mode, respectBannedTurns);
    var exitsReached = new LinkedHashMap<MacroscopicLinkSegment, Double>();
    reached.getReached().stream().filter(intersection::isExitSegment)
        .forEach(exit -> exitsReached.put(exit, reached.getCostToReach(exit)));
    return exitsReached;
  }

  /**
   * Ban a turn for the given modes. When the turn is banned already, its banned turn gains the modes rather than a
   * second one being registered: a banned turn that bans every mode stays as it is, and one that names modes names
   * these too, or every mode of the layer when none are given
   *
   * @param layer to register the banned turn on
   * @param from link segment arriving at the node
   * @param to link segment leaving it
   * @param explicitlyBannedModes the modes to ban; when empty every mode is banned
   * @return the banned turn registered or extended
   */
  public static MacroscopicBannedTurn banTurnForModes(
      final MacroscopicNetworkLayer layer, MacroscopicLinkSegment from, MacroscopicLinkSegment to,
      Collection<Mode> explicitlyBannedModes) {
    var existing = layer.getBannedTurns().getBySegment(from).stream()
        .filter(banned -> banned.getSegmentFrom() == from && banned.getSegmentTo() == to)
        .map(MacroscopicBannedTurn.class::cast).findFirst().orElse(null);
    if (existing == null) {
      return layer.getBannedTurns().getFactory().registerNew(from, to, explicitlyBannedModes);
    }
    if (existing.hasExplicitlyBannedModes()) {
      (explicitlyBannedModes.isEmpty() ? layer.getSupportedModes() : explicitlyBannedModes)
          .forEach(existing::addExplicitlyBannedMode);
    }
    return existing;
  }
}
