package org.goplanit.network.layer.macroscopic;

import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.algorithms.ConnectedComponents;
import org.goplanit.utils.graph.directed.Connectivity;
import org.goplanit.utils.graph.directed.algorithms.StronglyConnectedComponents;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentUtils;
import org.goplanit.utils.network.layer.physical.Node;

import java.util.Collections;
import java.util.HashSet;
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
   * nothing, and {@link #removeInfrastructureWithoutModeAccess(MacroscopicNetworkLayer)} clears such segments once
   * every mode has been dealt with. Keeping the two apart is what allows modes to be processed independently,
   * since removing as we go would mutate the graph underneath the modes not yet processed.
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
  public static Result restrictModeAccessToConnectedSubNetworks(
      final MacroscopicNetworkLayer layer,
      final Mode mode,
      final int belowSize,
      final int aboveSize,
      final boolean alwaysKeepLargest,
      final Connectivity connectivity,
      final Predicate<MacroscopicLinkSegment> retainAccessRegardless) {

    var retainedVertices = collectRetainedVertices(
        layer, mode, belowSize, aboveSize, alwaysKeepLargest, connectivity);

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
    }
    return new Result(mode, withdrawn, protectedSegments);
  }

  /**
   * Remove what no mode can use any more: link segments granting no mode access, links left without segments, and
   * nodes left without edges.
   * <p>
   * Meant to run once after every mode has had
   * {@link #restrictModeAccessToConnectedSubNetworks(MacroscopicNetworkLayer, Mode, int, int, boolean,
   * Connectivity, Predicate)} applied, since only then is it settled what nothing can use.
   * </p>
   *
   * @param layer to clean up
   * @return what was removed
   */
  public static CleanupResult removeInfrastructureWithoutModeAccess(final MacroscopicNetworkLayer layer) {

    var modifier = layer.getLayerModifier();
    var cleanup = new CleanupResult();

    /* collected before removing throughout, since removal mutates the containers being iterated */
    List<MacroscopicLinkSegment> unusableSegments = layer.getLinkSegments().stream()
        .filter(linkSegment -> !linkSegment.hasLinkSegmentType() ||
            !linkSegment.getLinkSegmentType().hasAllowedModes())
        .collect(Collectors.toList());
    for (var linkSegment : unusableSegments) {
      modifier.removeEdgeSegment(linkSegment);
      ++cleanup.removedLinkSegments;
    }

    List<MacroscopicLink> emptyLinks = layer.getLinks().stream()
        .filter(link -> !link.hasEdgeSegmentAb() && !link.hasEdgeSegmentBa())
        .collect(Collectors.toList());
    for (var link : emptyLinks) {
      modifier.removeEdge(link);
      ++cleanup.removedLinks;
    }

    List<Node> danglingNodes = layer.getNodes().stream()
        .filter(node -> node.getEdges() == null || node.getEdges().isEmpty())
        .collect(Collectors.toList());
    for (var node : danglingNodes) {
      modifier.removeVertex(node);
      ++cleanup.removedNodes;
    }

    /* a type granting nothing has no purpose once the segments carrying it are gone */
    var unusedTypes = layer.getLinkSegmentTypes().stream()
        .filter(type -> !type.hasAllowedModes())
        .collect(Collectors.toList());
    for (var type : unusedTypes) {
      layer.getLinkSegmentTypes().remove(type);
      ++cleanup.removedLinkSegmentTypes;
    }

    return cleanup;
  }

  /**
   * Collect the vertices whose subnetwork for this mode meets the criteria.
   *
   * @param layer to inspect
   * @param mode to collect for
   * @param belowSize threshold
   * @param aboveSize threshold
   * @param alwaysKeepLargest whether the largest is exempt from the thresholds
   * @param connectivity what constitutes a subnetwork
   * @return vertices whose subnetwork is retained
   */
  private static Set<Vertex> collectRetainedVertices(
      final MacroscopicNetworkLayer layer,
      final Mode mode,
      final int belowSize,
      final int aboveSize,
      final boolean alwaysKeepLargest,
      final Connectivity connectivity) {

    List<List<Node>> components;
    if (connectivity.isStrong()) {
      components = StronglyConnectedComponents.execute(
          layer.getNodes(), MacroscopicLinkSegmentUtils.permitsMode(mode)).getComponents();
    } else {
      components = ConnectedComponents.execute(layer.getNodes(), edgeCarriesMode(mode));
    }

    /* both partitions come back largest first */
    final int largestSize = components.isEmpty() ? 0 : components.get(0).size();

    var retained = new HashSet<Vertex>();
    for (var component : components) {
      final int size = component.size();
      /* deliberately the same rule dangling subnetwork removal applies, so that the thresholds mean the same
       * thing whichever of the two a caller reaches for */
      boolean withdraw = (size < largestSize || !alwaysKeepLargest) && (size < belowSize || size > aboveSize);
      if (!withdraw) {
        retained.addAll(component);
      }
    }
    return retained;
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
   * Replace the segment's type with one identical except that it no longer grants the given mode access.
   * <p>
   * Types are shared by many segments, so the type is never changed in place: that would silently withdraw access
   * everywhere the type is used. A variant is found or created once per (original type, withdrawn mode) pair.
   * </p>
   *
   * @param layer owning the link segment types
   * @param modifiedTypes cache of already created variants
   * @param linkSegment to adjust
   * @param mode to withdraw
   */
  private static void withdrawModeAccess(
      final MacroscopicNetworkLayer layer,
      final ModifiedLinkSegmentTypes modifiedTypes,
      final MacroscopicLinkSegment linkSegment,
      final Mode mode) {

    var original = linkSegment.getLinkSegmentType();
    if (original == null) {
      return;
    }

    final Set<Mode> withdrawnModes = Collections.singleton(mode);
    var replacement = modifiedTypes.getModifiedLinkSegmentType(
        original, Collections.emptySet(), withdrawnModes);
    if (replacement == null) {
      replacement = layer.getLinkSegmentTypes().getFactory().createUniqueDeepCopyOf(original);
      layer.getLinkSegmentTypes().register(replacement);
      replacement.setXmlId(Long.toString(replacement.getId()));

      final String modified = String.format("MODIFIED[%d]:", replacement.getId());
      if (replacement.hasExternalId()) {
        replacement.setExternalId(modified + replacement.getExternalId());
      }
      if (replacement.hasName()) {
        replacement.setName(modified + replacement.getName());
      }

      replacement.removeModeAccess(withdrawnModes);
      modifiedTypes.addModifiedLinkSegmentType(
          original, replacement, Collections.emptySet(), withdrawnModes);
    }

    linkSegment.setLinkSegmentType(replacement);
  }

  /**
   * What restricting a single mode's access came to, as a value so the caller decides what to log.
   *
   * @author markr
   */
  public static class Result {

    private final Mode mode;

    private final int withdrawn;

    private final int protectedSegments;

    Result(Mode mode, int withdrawn, int protectedSegments) {
      this.mode = mode;
      this.withdrawn = withdrawn;
      this.protectedSegments = protectedSegments;
    }

    /**
     * The mode this concerns
     *
     * @return mode
     */
    public Mode getMode() {
      return mode;
    }

    /**
     * Number of link segments the mode lost access to
     *
     * @return count
     */
    public int getWithdrawn() {
      return withdrawn;
    }

    /**
     * Number of link segments that kept access despite not qualifying, because they were protected
     *
     * @return count
     */
    public int getProtected() {
      return protectedSegments;
    }

    /**
     * Verify whether anything changed
     *
     * @return true when nothing was withdrawn
     */
    public boolean isEmpty() {
      return withdrawn == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return String.format("%s access withdrawn on %d link segments%s",
          mode.getName(), withdrawn,
          protectedSegments > 0 ? String.format(", %d protected", protectedSegments) : "");
    }
  }

  /**
   * What the final pass removed.
   *
   * @author markr
   */
  public static class CleanupResult {

    private int removedLinkSegments = 0;

    private int removedLinks = 0;

    private int removedNodes = 0;

    private int removedLinkSegmentTypes = 0;

    /**
     * Number of link segments removed because no mode could use them
     *
     * @return count
     */
    public int getRemovedLinkSegments() {
      return removedLinkSegments;
    }

    /**
     * Number of links removed because they were left without any segment
     *
     * @return count
     */
    public int getRemovedLinks() {
      return removedLinks;
    }

    /**
     * Number of nodes removed because they were left without any edge
     *
     * @return count
     */
    public int getRemovedNodes() {
      return removedNodes;
    }

    /**
     * Number of link segment types removed because they granted no mode access
     *
     * @return count
     */
    public int getRemovedLinkSegmentTypes() {
      return removedLinkSegmentTypes;
    }

    /**
     * Verify whether anything was removed
     *
     * @return true when nothing was removed
     */
    public boolean isEmpty() {
      return removedLinkSegments == 0 && removedLinks == 0 && removedNodes == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return String.format(
          "removed %d link segments, %d links and %d nodes without any mode access (%d link segment types)",
          removedLinkSegments, removedLinks, removedNodes, removedLinkSegmentTypes);
    }
  }
}
