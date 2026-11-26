package org.goplanit.network.layer.macroscopic;

import org.goplanit.algorithms.shortest.ShortestPathBreadthFirst;
import org.goplanit.algorithms.shortest.ShortestPathBreadthFirstResult;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.algorithms.ConnectedComponents;
import org.goplanit.utils.graph.directed.Connectivity;
import org.goplanit.utils.graph.directed.algorithms.StronglyConnectedComponents;
import org.goplanit.utils.misc.binning.BinBuilder;
import org.goplanit.utils.misc.binning.BinnedCount;
import org.goplanit.utils.misc.binning.BinningConfiguration;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurn;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.LinkUtils;
import org.goplanit.utils.network.layer.physical.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utilities operating on a macroscopic network layer as a whole, i.e. requiring both its graph and the mode
 * information its link segment types carry.
 *
 * @author markr
 */
public class MacroscopicNetworkLayerUtils {

  /** The link segments allowing a mode in one of its subnetworks, with the number of nodes the subnetwork counts as */
  private static final class ModeSubNetwork {

    /** the link segments allowing the mode in this subnetwork */
    private final List<MacroscopicLinkSegment> linkSegments;

    /** the number of nodes this subnetwork counts as */
    private final int numberOfNodes;

    /**
     * Constructor
     *
     * @param linkSegments allowing the mode in this subnetwork
     * @param numberOfNodes the subnetwork counts as
     */
    private ModeSubNetwork(List<MacroscopicLinkSegment> linkSegments, int numberOfNodes) {
      this.linkSegments = linkSegments;
      this.numberOfNodes = numberOfNodes;
    }
  }

  /**
   * Subnetwork sizes reported against, in nodes, see {@link #restrictModeAccessToConnectedSubNetworks} for how they
   * are counted. Not configurable for now: the bins let a size threshold be chosen from a single run, and this spread
   * is fine grained where that choice is, among the small subnetworks.
   */
  private static final BinningConfiguration<Integer> SUBNETWORK_SIZE_BINS =
      BinBuilder.ofInclusiveIntegerLowerBounds(1, 2, 5, 20, 50, 200);

  /**
   * Partition the link segments allowing a mode into the mode's subnetworks under the given connectivity, largest first
   * by number of nodes. Under strong connectivity a subnetwork is a set of link segments the mode can travel between
   * both ways through the turns not banned for it. Its size is the number of different nodes its link segments start
   * at. Under weak connectivity it is a set of nodes joined by links the mode may use in either direction. Its size is
   * its number of nodes, and it holds the link segments allowing the mode that start at them. A node without any link
   * segment of the mode is then a subnetwork of its own, without link segments
   *
   * @param layer to inspect
   * @param mode to partition for
   * @param connectivity what constitutes a subnetwork
   * @return subnetworks, largest first
   */
  private static List<ModeSubNetwork> collectSubNetworks(
      final MacroscopicNetworkLayer layer, final Mode mode, final Connectivity connectivity) {
    final var subNetworks = new ArrayList<ModeSubNetwork>();
    if (connectivity.isStrong()) {
      final var linkSegmentsOfMode =
          layer.getLinkSegments().stream().filter(ls -> ls.isModeAllowed(mode)).collect(Collectors.toList());
      for (var component : StronglyConnectedComponents.executeOnEdgeSegments(
          linkSegmentsOfMode, (from, onto) -> !isTurnBannedForMode(layer, from, onto, mode)).getComponents()) {
        subNetworks.add(new ModeSubNetwork(
            component, (int) component.stream().map(LinkSegment::getUpstreamVertex).distinct().count()));
      }
    } else {
      final var linkSegmentsOfModeByUpstreamNode = layer.getLinkSegments().stream()
          .filter(ls -> ls.isModeAllowed(mode)).collect(Collectors.groupingBy(LinkSegment::getUpstreamVertex));
      for (var component : ConnectedComponents.execute(
          layer.getNodes(), LinkUtils.permitsModeMatchingOnAnySegment(mode::equals))) {
        subNetworks.add(new ModeSubNetwork(component.stream().flatMap(
            node -> linkSegmentsOfModeByUpstreamNode.getOrDefault(node, List.of()).stream())
            .collect(Collectors.toList()), component.size()));
      }
    }
    subNetworks.sort(Comparator.comparingInt((ModeSubNetwork subNetwork) -> subNetwork.numberOfNodes).reversed());
    return subNetworks;
  }

  /** Constructor to disallow instance creation */
  private MacroscopicNetworkLayerUtils(){}

  /**
   * Withdraw a mode's access from its subnetworks that do not meet the given criteria, so the mode keeps access only
   * where it has a network worth having.
   * <p>
   * The counterpart of dangling subnetwork removal, but judged per mode rather than per track type, and withdrawing
   * access rather than removing infrastructure. Both differences are necessary, not stylistic. Track type is too coarse
   * to see the problem: a one way street usually still permits pedestrians against the flow, so it is bidirectional as
   * road infrastructure while being a dead end for cars. And removal is the wrong remedy, as the link a car cannot use
   * typically carries another mode perfectly well.
   * </p>
   * <p>
   * What counts as a subnetwork follows the given {@link Connectivity}: weakly connected components when ignoring
   * direction, strongly connected components when a mode must be able to both reach and leave. The size thresholds
   * apply to those components exactly as for dangling subnetwork removal, so {@code belowSize} at its maximum with
   * {@code alwaysKeepLargest} and {@link Connectivity#STRONG} leaves the mode a single network it can route across in
   * full.
   * </p>
   * <p>
   * Under strong connectivity the mode moves from link segment to link segment through the turns not banned for it, so
   * what keeps the mode can be routed across with every banned turn in place. A link segment that banned turns cut
   * off, such as an on-ramp every turn onto which is banned, is then a small subnetwork like one behind a one way trap,
   * and loses the mode in the same way; no banned turn is changed. Turning back onto the other direction of a link is a
   * turn like any other, allowed unless banned, as a router allows it: without it, the last link segment of every
   * dead-end street would have no way out. Ignoring direction, a banned turn has no meaning, so weak connectivity
   * leaves banned turns out.
   * </p>
   * <p>
   * The size of a subnetwork is its number of nodes. Under strong connectivity that is the number of different nodes
   * its link segments start at; without banned turns these are the nodes a node to node partition finds, so the
   * thresholds mean the same thing either way.
   * </p>
   * <p>
   * Nothing is removed here, not even a segment left with no permitted mode at all: it is given a type granting
   * nothing, and {@link MacroscopicNetworkLayerModifier#removeInfrastructureWithoutModeAccess()} clears such
   * segments once every mode is done. Keeping the two apart lets modes be processed independently, as removing as we
   * go would change the graph under the modes not yet processed.
   * </p>
   * <p>
   * Connectoids are not considered: they are not part of the physical network, so what they reference does not decide
   * what the network may do. Access is withdrawn by changing the segment's type through the layer's modifier, so the
   * zoning side can follow it with a listener, see {@link org.goplanit.zoning.connectoid.TransferConnectoidsImpl}.
   * </p>
   *
   * @param layer to apply this to
   * @param mode the mode whose access is judged
   * @param belowSize withdraw access on subnetworks of fewer nodes than this
   * @param aboveSize withdraw access on subnetworks of more nodes than this, typically the maximum value
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

    var subNetworks = collectSubNetworks(layer, mode, connectivity);
    final int largestSize = subNetworks.isEmpty() ? 0 : subNetworks.get(0).numberOfNodes;

    int retainedSubNetworks = 0;
    var discardedSubNetworks = BinnedCount.of(SUBNETWORK_SIZE_BINS);
    var withdrawnLinkSegments = BinnedCount.of(SUBNETWORK_SIZE_BINS);
    /* so a withdrawn segment can be attributed to the size of the subnetwork it belonged to, which is what makes
     * the distribution readable rather than just the totals */
    final Map<MacroscopicLinkSegment, Integer> discardedSubNetworkSizeByLinkSegment = new HashMap<>();

    for (var subNetwork : subNetworks) {
      final int size = subNetwork.numberOfNodes;
      /* deliberately the same rule dangling subnetwork removal applies, so that the thresholds mean the same
       * thing whichever of the two a caller reaches for */
      boolean withdraw = (size < largestSize || !alwaysKeepLargest) && (size < belowSize || size > aboveSize);
      if (!withdraw) {
        ++retainedSubNetworks;
        continue;
      }
      discardedSubNetworks.increment(size);
      for (var linkSegment : subNetwork.linkSegments) {
        discardedSubNetworkSizeByLinkSegment.put(linkSegment, size);
      }
    }

    var modifiedTypes = new ModifiedLinkSegmentTypes();
    int withdrawn = 0;
    int protectedSegments = 0;

    /* in the layer's order rather than the subnetworks', so the link segment type variants withdrawing creates are
     * made in the same order whatever order the partition finds the subnetworks in */
    for (var linkSegment : layer.getLinkSegments()) {
      final Integer size = discardedSubNetworkSizeByLinkSegment.get(linkSegment);
      if (size == null) {
        continue;
      }
      if (retainAccessRegardless != null && retainAccessRegardless.test(linkSegment)) {
        ++protectedSegments;
        continue;
      }
      withdrawModeAccess(layer, modifiedTypes, linkSegment, mode);
      ++withdrawn;
      withdrawnLinkSegments.increment(size);
    }
    return new ModeAccessRestrictionResult(mode, withdrawn, protectedSegments, subNetworks.size(), retainedSubNetworks,
        largestSize, discardedSubNetworks, withdrawnLinkSegments);
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
   * everywhere the type is used. A variant is found or created once per (original type, withdrawn mode) pair. The
   * type is replaced through the layer's modifier, so its listeners hear of it, see
   * {@link MacroscopicNetworkLayerModifier#changeLinkSegmentType(MacroscopicLinkSegment, MacroscopicLinkSegmentType)}.
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

    layer.getLayerModifier().changeLinkSegmentType(
        linkSegment, findOrRegisterTypeWithoutModes(layer, modifiedTypes, original, Collections.singleton(mode)));
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
      final MacroscopicNetworkLayer layer, LinkSegment from, LinkSegment to, Mode mode) {
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
   * Ban a turn for the given modes. When the turn is banned already, its banned turn gains the modes rather than a
   * second one being registered: a banned turn that bans every mode stays as it is, and one that names modes names
   * these too, or every mode of the layer when none are given. A turn ban is physical, between link segments of the
   * layer, so a turn onto or off a connectoid segment of an integrated zoning is not banned
   *
   * @param layer to register the banned turn on
   * @param from link segment arriving at the node
   * @param to link segment leaving it
   * @param explicitlyBannedModes the modes to ban; when empty every mode is banned
   * @return the banned turn registered or extended, null when either segment is not a link segment of the layer
   */
  public static MacroscopicBannedTurn banTurnForModes(
      final MacroscopicNetworkLayer layer, LinkSegment from, LinkSegment to, Collection<Mode> explicitlyBannedModes) {
    if (!(from instanceof MacroscopicLinkSegment && to instanceof MacroscopicLinkSegment)) {
      return null;
    }
    var existing = layer.getBannedTurns().getBySegment(from).stream()
        .filter(banned -> banned.getSegmentFrom() == from && banned.getSegmentTo() == to)
        .map(MacroscopicBannedTurn.class::cast).findFirst().orElse(null);
    if (existing == null) {
      return layer.getBannedTurns().getFactory().registerNew(
          (MacroscopicLinkSegment) from, (MacroscopicLinkSegment) to, explicitlyBannedModes);
    }
    if (existing.hasExplicitlyBannedModes()) {
      (explicitlyBannedModes.isEmpty() ? layer.getSupportedModes() : explicitlyBannedModes)
          .forEach(existing::addExplicitlyBannedMode);
    }
    return existing;
  }
}
