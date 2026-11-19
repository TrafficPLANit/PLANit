package org.goplanit.zoning.connectoid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.goplanit.graph.directed.modifier.event.BreakEdgeSegmentEvent;
import org.goplanit.graph.directed.modifier.event.MoveEdgeEndsAtVertexEvent;
import org.goplanit.graph.directed.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.network.layer.macroscopic.SpatialIntersectionUtils;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.CollapseSpatialIntersectionEvent;
import org.goplanit.network.layer.modifier.event.ChangeLinkSegmentTypeEvent;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModificationEvent;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierListener;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegmentUtils;
import org.goplanit.utils.zoning.connectoid.ConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.connectoid.DirectedConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;

/**
 * Keeps the transfer connectoids of a container in step with the network of the zoning holding it: besides following
 * removed nodes, as for every connectoid, they follow the link segments their entries are reached over
 * <p>
 * A removed link segment is taken out of every entry reached over it, and a link segment losing modes withdraws them
 * from the entries allowing modes explicitly. For example, a bus stop reached only over a road that stops allowing buses
 * loses its vehicle stop entry. Each loss is reported once, at the level it stops. A connectoid also follows the links it
 * is reached over when their ends move to another node, moving with them, or splitting when it is also reached over a
 * link left behind; that loses nothing. When an intersection is collapsed, the entries reached over its internal
 * segments for a collapsed mode are held from the start of the collapse, and left alone meanwhile; once it is done,
 * they are reached at its centre node for the collapsed modes that reached them, see {@link #moveHeldEntriesToCentre}.
 * </p>
 *
 * @author markr
 */
public class UpdateTransferConnectoidsOnNetworkChangeHandler
    extends UpdateConnectoidsOnNetworkChangeHandler<TransferConnectoid, TransferConnectoidsImpl>
    implements LinkSegmentTypeModifierListener {

  /**
   * An entry over an internal segment of an intersection being collapsed that allows a collapsed mode the segment
   * allows, held from the collapse's START to its DONE, with what brings each such mode to the segment or takes it
   * from there, worked out while the intersection is spatial
   */
  private static final class HeldEntry {

    /** the connectoid of the entry */
    private final TransferConnectoid connectoid;

    /** the entry held */
    private final DirectedConnectoidAccessZoneEntry entry;

    /** the internal segment the entry is reached over */
    private final LinkSegment internalSegment;

    /** true when the internal segment enters the connectoid's node, so the entry gives access, false when it leaves
     * it, as when the collapse started */
    private final boolean entersItsNode;

    /** per collapsed mode the entry and the internal segment allow: the segments entering the intersection from
     * outside from which it reaches the internal segment when that enters the connectoid's node, or the exits it
     * reaches from the internal segment when that leaves it; empty when none */
    private final Map<Mode, List<LinkSegment>> reachedByMode = new LinkedHashMap<>();

    /** the segments at the centre node bringing the modes reached into it, or taking them from it, found once the
     * collapse is done */
    private final Set<LinkSegment> segmentsAtCentre = new LinkedHashSet<>();

    /** the modes reached for which a segment at the centre node was found */
    private final Set<Mode> modesAtCentre = new LinkedHashSet<>();

    /**
     * Constructor, nothing reached yet
     *
     * @param connectoid of the entry
     * @param entry held
     * @param internalSegment the entry is reached over
     * @param modes collapsed that the entry and the internal segment allow
     */
    private HeldEntry(TransferConnectoid connectoid, DirectedConnectoidAccessZoneEntry entry,
        LinkSegment internalSegment, List<Mode> modes) {
      this.connectoid = connectoid;
      this.entry = entry;
      this.internalSegment = internalSegment;
      this.entersItsNode = internalSegment.getDownstreamVertex() == connectoid.getReferenceVertex();
      modes.forEach(mode -> reachedByMode.put(mode, List.of()));
    }

    /**
     * Find, once the collapse is done, the segments at the centre node for each mode reached: an approach or exit that
     * collapse moved to the centre itself, otherwise the segments joining its member node to the centre for the mode,
     * into the centre for access and out of it for egress. A mode for which none is found is not reached
     *
     * @param centre node of the intersection
     */
    private void findSegmentsAtCentre(DirectedVertex centre) {
      reachedByMode.forEach((mode, reached) -> reached.forEach(segment -> {
        var nearEnd = entersItsNode ? segment.getDownstreamVertex() : segment.getUpstreamVertex();
        var atCentre = nearEnd == centre ? List.of(segment) : LinkSegmentUtils.findOnward(segment, entersItsNode,
            onward -> (entersItsNode ? onward.getDownstreamVertex() : onward.getUpstreamVertex()) == centre
                && onward.isModeAllowed(mode));
        if (!atCentre.isEmpty()) {
          segmentsAtCentre.addAll(atCentre);
          modesAtCentre.add(mode);
        }
      }));
    }

    /**
     * Verify whether the entry still needs its internal segment once the collapse is done: it allows a mode the segment
     * still allows, one that is not collapsed
     *
     * @return true when it keeps the internal segment, false otherwise
     */
    private boolean keepsInternalSegment() {
      return entry.isAnyModeAllowed(((MacroscopicLinkSegment) internalSegment).getAllowedModes());
    }
  }

  /** what this handler holds for an intersection being collapsed, from its START to its DONE */
  private static final class CollapseInProgress {

    /** the entries held */
    private final List<HeldEntry> heldEntries = new ArrayList<>();

    /** per original connectoid, the connectoid split off it onto the centre node while collapsing, by identity */
    private final Map<TransferConnectoid, TransferConnectoid> splitOntoCentre = new IdentityHashMap<>();
  }

  /** per intersection being collapsed that holds entries, its record, by its centre node, by identity */
  private final Map<DirectedVertex, CollapseInProgress> collapsesInProgressByCentre = new IdentityHashMap<>();

  /** the entries held by the collapses in progress, by identity, which the other handling leaves alone */
  private final Set<DirectedConnectoidAccessZoneEntry> heldEntries = Collections.newSetFromMap(new IdentityHashMap<>());

  /**
   * The transfer connectoids at either node of a link segment, the only ones it can be an access link segment of
   *
   * @param segment to look up the connectoids of
   * @return the connectoids at its upstream and its downstream node
   */
  private List<TransferConnectoid> findConnectoidsAtNodesOf(EdgeSegment segment) {
    var found = new ArrayList<>(getConnectoids().getByAccessNode(segment.getUpstreamVertex()));
    found.addAll(getConnectoids().getByAccessNode(segment.getDownstreamVertex()));
    return found;
  }

  /**
   * Remove an entry from a connectoid, and the connectoid whole when it is its last remaining entry, so the loss is
   * reported once, at the level it stops
   *
   * @param connectoid to remove the entry from
   * @param entry to remove
   */
  private void removeAccessZoneEntryAndConnectoidIfLastRemaining(
      TransferConnectoid connectoid, ConnectoidAccessZoneEntry entry) {
    if (connectoid.getNumberOfAccessZoneEntries() > 1) {
      getConnectoids().getZoningModifier().removeAccessZoneEntry(connectoid, entry.getAccessZone(), entry.getType());
    } else {
      getConnectoids().getZoningModifier().removeConnectoid(connectoid);
    }
  }

  /**
   * Take an access link segment out of an entry, and the entry when it is its last remaining one, so the loss is
   * reported once, at the level it stops
   *
   * @param connectoid the entry belongs to
   * @param entry to take the segment out of
   * @param segment to take out
   */
  private void removeAccessLinkSegmentAndEntryIfLastRemaining(
      TransferConnectoid connectoid, DirectedConnectoidAccessZoneEntry entry, EdgeSegment segment) {
    if (entry.getAccessLinkSegments().size() > 1) {
      getConnectoids().getZoningModifier().removeAccessLinkSegment(connectoid, entry, segment);
    } else {
      removeAccessZoneEntryAndConnectoidIfLastRemaining(connectoid, entry);
    }
  }

  /**
   * Take out of an entry the lost modes it allows explicitly that none of its access link segments allows, through the
   * zoning modifier; when one is its last mode the entry goes instead, or the connectoid when it was its last entry
   *
   * @param connectoid the entry belongs to
   * @param entry to take the modes out of
   * @param lostModes the modes lost
   */
  private void removeExplicitlyAllowedModesNoAccessLinkSegmentAllows(
      TransferConnectoid connectoid, DirectedConnectoidAccessZoneEntry entry, Collection<Mode> lostModes) {
    for (var mode : entry.findExplicitlyAllowedModesNoAccessLinkSegmentAllows(lostModes)) {
      if (!getConnectoids().getZoningModifier().removeExplicitAllowedMode(connectoid, entry, mode)) {
        removeAccessZoneEntryAndConnectoidIfLastRemaining(connectoid, entry);
        break;
      }
    }
  }

  /**
   * Split a transfer connectoid off another at a node: a new one there with the same name and external id, and an xml
   * id of its own id, without entries yet
   *
   * @param connectoid to split off
   * @param node of the split
   * @return the split
   */
  private TransferConnectoid splitOff(TransferConnectoid connectoid, DirectedVertex node) {
    var split = getConnectoids().getFactory().registerNew(node);
    split.setName(connectoid.getName());
    split.setExternalId(connectoid.getExternalId());
    split.setXmlId(String.valueOf(split.getId()));
    return split;
  }

  /**
   * Take an access link segment out of the access zone entries using it. When it was an entry's only segment the entry
   * goes with it, and when that was the connectoid's only entry the connectoid goes. For example, a bus stop reached
   * over two roads keeps the other road, while one reached only over that road loses its entry. An entry or connectoid
   * that goes is reported once, as removed, and a connectoid once removed is left alone. An entry held while its
   * intersection is collapsed is left alone, see {@link #holdEntriesOverInternalSegments}
   *
   * @param removed access link segment to take out
   */
  private void removeAccessLinkSegmentFromEntriesUsing(EdgeSegment removed) {
    for (var connectoid : findConnectoidsAtNodesOf(removed)) {
      for (var entry : connectoid.getDirectedAccessZoneEntriesFor(removed)) {
        if (!getConnectoids().containsValue(connectoid)) {
          break;
        }
        if (heldEntries.contains(entry)) {
          continue;
        }
        removeAccessLinkSegmentAndEntryIfLastRemaining(connectoid, entry, removed);
      }
    }
  }

  /**
   * Use another access link segment in place of one, in the entries holding it of the connectoids at the nodes of the
   * replacement, so those entries reach their connectoid's node over the replacement. An entry already holding the
   * replacement is left as it is. Not a loss, so nothing is reported
   *
   * @param replaced access link segment to stop using
   * @param replacement access link segment to use instead, starting or ending at the node of each connectoid concerned
   */
  private void replaceAccessLinkSegmentInEntries(EdgeSegment replaced, EdgeSegment replacement) {
    if (replaced == replacement) {
      return;
    }
    for (var connectoid : findConnectoidsAtNodesOf(replacement)) {
      connectoid.getDirectedAccessZoneEntriesFor(replaced)
          .forEach(entry -> entry.replaceAccessLinkSegment(replaced, replacement));
    }
  }

  /**
   * Keep each transfer connectoid at the vertex the edge ends moved away from reaching its node over the moved edges'
   * segments. A connectoid reached only over them moves to the new end vertex with all its entries. One also reached
   * over an edge left at the vertex is split: a new transfer connectoid at the new end vertex, with the same name and
   * external id, takes the entries over the moved segments. Neither is a loss, so nothing is reported. A connectoid
   * there without an access link segment on the moved edges is left as it is. A split onto the centre node of an
   * intersection being collapsed is recorded, so the connectoid has one connectoid at the centre once the collapse is
   * done
   *
   * @param moved the event naming the edges whose ends moved and both vertices
   */
  private void followMovedEdgeEnds(MoveEdgeEndsAtVertexEvent moved) {
    Set<EdgeSegment> movedSegments = Collections.newSetFromMap(new IdentityHashMap<>());
    moved.getEdges().forEach(edge -> movedSegments.addAll(edge.getEdgeSegments()));
    for (var connectoid : List.copyOf(getConnectoids().getByAccessNode(moved.getPreviousEndVertex()))) {
      var accessLinkSegments = connectoid.getExplicitAccessLinkSegmentsStream().collect(Collectors.toList());
      if (accessLinkSegments.stream().noneMatch(movedSegments::contains)) {
        continue;
      }
      if (accessLinkSegments.stream().allMatch(movedSegments::contains)) {
        connectoid.setReferenceVertex(moved.getNewEndVertex());
      } else {
        var split = splitOff(connectoid, moved.getNewEndVertex());
        connectoid.moveAccessLinkSegmentsTo(movedSegments, split);
        var inProgress = collapsesInProgressByCentre.get(moved.getNewEndVertex());
        if (inProgress != null) {
          inProgress.splitOntoCentre.put(connectoid, split);
        }
      }
    }
  }

  /**
   * Withdraw the modes a link segment lost from the access zone entries using it, such as a road that no longer allows
   * buses. Only entries listing their modes explicitly change; an entry allowing every mode allows what its segments
   * allow. For each such entry:
   * <ul>
   * <li>when the segment allows none of the entry's modes any more, the segment is taken out of the entry, and the
   * entry with it when it was its only segment;</li>
   * <li>each lost mode no other segment of the entry allows is taken out of the entry; when it was the entry's only
   * mode the entry goes instead, since an entry without modes would allow every mode.</li>
   * </ul>
   * For example, a bus stop entry reached only over that road goes, while an entry for bus and car keeps the road and
   * loses bus. An entry or connectoid that goes is reported once, as removed, and a connectoid once removed is left
   * alone. An entry held while its intersection is collapsed is left alone, see
   * {@link #holdEntriesOverInternalSegments}
   *
   * @param segment that lost the modes
   * @param lostModes the modes it lost, in mode id order
   */
  private void removeAccessFromEntriesUsingSegmentThatLostModes(MacroscopicLinkSegment segment, Set<Mode> lostModes) {
    for (var connectoid : findConnectoidsAtNodesOf(segment)) {
      for (var entry : connectoid.getDirectedAccessZoneEntriesFor(segment)) {
        if (!getConnectoids().containsValue(connectoid) || !entry.hasExplicitlyAllowedModes()
            || heldEntries.contains(entry)) {
          continue;
        }
        if (!entry.isAnyExplicitlyAllowedModeAllowedBy(segment)) {
          boolean lastSegment = entry.getAccessLinkSegments().size() == 1;
          removeAccessLinkSegmentAndEntryIfLastRemaining(connectoid, entry, segment);
          if (lastSegment) {
            continue;
          }
        }
        removeExplicitlyAllowedModesNoAccessLinkSegmentAllows(connectoid, entry, lostModes);
      }
    }
  }

  /**
   * Prepare for the collapse of a spatial intersection, changing nothing: hold the entries over its internal segments
   * that allow a collapsed mode the segment allows, and work out, while the intersection is still spatial, banned turns
   * respected, what brings each such mode to the segment, for an entry giving access, or takes it from there, for
   * egress. Nothing is held for an intersection without such entries
   * <p>
   * Once collapse withdraws the modes from the internal segments, nothing can tell any more which approaches reached a
   * stop inside the intersection, so it is worked out first. For example, for a stop reached over the footway between
   * two corners, the approaches from which pedestrians can walk onto that footway
   * </p>
   *
   * @param started the event reporting the intersection as started
   */
  private void holdEntriesOverInternalSegments(CollapseSpatialIntersectionEvent started) {
    var intersection = started.getIntersection();
    var inProgress = new CollapseInProgress();
    for (var segment : intersection.getInternalSegments()) {
      var modes = started.getCollapsedModes().stream().filter(segment::isModeAllowed).collect(Collectors.toList());
      if (modes.isEmpty()) {
        continue;
      }
      for (var connectoid : findConnectoidsAtNodesOf(segment)) {
        for (var entry : connectoid.getDirectedAccessZoneEntriesFor(segment)) {
          var entryModes = modes.stream().filter(entry::isModeAllowed).collect(Collectors.toList());
          if (!entryModes.isEmpty()) {
            inProgress.heldEntries.add(new HeldEntry(connectoid, entry, segment, entryModes));
          }
        }
      }
    }
    if (inProgress.heldEntries.isEmpty()) {
      return;
    }

    /* the searches of one mode are shared by the entries giving access */
    var reachingByMode = new HashMap<Mode, Map<LinkSegment, List<LinkSegment>>>();
    for (var held : inProgress.heldEntries) {
      for (var reached : held.reachedByMode.entrySet()) {
        var mode = reached.getKey();
        if (held.entersItsNode) {
          reached.setValue(reachingByMode.computeIfAbsent(mode, m -> SpatialIntersectionUtils
              .findSegmentsEnteringFromOutsideReachingEachInternalSegment(started.getLayer(), intersection, m, true))
              .getOrDefault(held.internalSegment, List.of()));
        } else {
          reached.setValue(List.copyOf(SpatialIntersectionUtils.findExitsReached(
              started.getLayer(), intersection, held.internalSegment, mode, true).keySet()));
        }
      }
    }
    collapsesInProgressByCentre.put(started.getCentreNode(), inProgress);
    inProgress.heldEntries.forEach(held -> heldEntries.add(held.entry));
  }

  /**
   * The connectoid at the centre node an original connectoid's held entries move to: the original itself when nothing
   * of it stays at its node, because its node was removed or each of its entries is left without access link segments,
   * otherwise one split off it there, the one split off it during the collapse for a link moved to the centre when
   * there is one. When the original itself moves, a split of it made there is joined into it and removed, neither a
   * loss
   *
   * @param connectoid original, its held entries without the internal segments they no longer need
   * @param held its held entries
   * @param centre node of the intersection
   * @param inProgress record of the collapse
   * @return the connectoid at the centre
   */
  private TransferConnectoid findOrMakeConnectoidAtCentre(TransferConnectoid connectoid, List<HeldEntry> held,
      DirectedVertex centre, CollapseInProgress inProgress) {
    long emptied = held.stream().map(h -> h.entry).distinct().filter(entry -> !entry.hasAccessLinkSegments()).count();
    if (connectoid.getReferenceVertex() != centre && connectoid.getNumberOfAccessZoneEntries() > emptied) {
      return inProgress.splitOntoCentre.computeIfAbsent(connectoid, original -> splitOff(original, centre));
    }
    if (connectoid.getReferenceVertex() != centre) {
      connectoid.setReferenceVertex(centre);
    }
    var split = inProgress.splitOntoCentre.remove(connectoid);
    if (split != null) {
      for (var entry : split) {
        var directed = (DirectedConnectoidAccessZoneEntry) entry;
        connectoid.addAccessLinkSegmentsForZoneAndTypeOf(
            directed, directed.getAccessLinkSegments(), directed.getExplicitlyAllowedModes());
      }
      getConnectoids().remove(split);
    }
    return connectoid;
  }

  /**
   * Bring the held entries of an original connectoid up to date once what moves is at the centre node. Each loses,
   * without a zoning event, the moved modes it allows explicitly that none of its remaining segments allows, and goes,
   * without one, when left without access link segments and nothing is lost. A collapsed mode nothing reached is lost
   * as the other handling would have lost it, reported once: the entry goes when it is left without access link
   * segments, or the connectoid when that was its last entry, otherwise the mode is taken out of it
   *
   * @param connectoid original
   * @param held its held entries
   */
  private void settleHeldEntriesLeftAtOriginal(TransferConnectoid connectoid, List<HeldEntry> held) {
    for (var entry : held.stream().map(h -> h.entry).distinct().collect(Collectors.toList())) {
      Set<Mode> moved = new HashSet<>();
      Set<Mode> unreached = new LinkedHashSet<>();
      held.stream().filter(h -> h.entry == entry).forEach(h -> {
        moved.addAll(h.modesAtCentre);
        unreached.addAll(h.reachedByMode.keySet());
      });
      unreached.removeAll(moved);
      if (!entry.hasAccessLinkSegments()) {
        if (unreached.isEmpty()) {
          connectoid.removeAccessZoneEntry(entry.getAccessZone(), entry.getType());
        } else {
          removeAccessZoneEntryAndConnectoidIfLastRemaining(connectoid, entry);
        }
        continue;
      }
      entry.findExplicitlyAllowedModesNoAccessLinkSegmentAllows(moved).forEach(entry::removeExplicitAllowedMode);
      removeExplicitlyAllowedModesNoAccessLinkSegmentAllows(connectoid, entry, unreached);
    }
  }

  /**
   * Make the changes prepared when the collapse of a spatial intersection started, now that it is done
   * <p>
   * The centre node stands for the whole former spatial intersection for the collapsed modes, so each held entry is
   * reached there for the collapsed modes that reached its internal segment, over the segments that now bring them into
   * the centre, or take them from it for egress. First each entry loses the internal segment it no longer needs, so its
   * connectoid can move; then each original connectoid's held entries go to its one connectoid at the centre, see
   * {@link #findOrMakeConnectoidAtCentre}; then what is left at the original is brought up to date, see
   * {@link #settleHeldEntriesLeftAtOriginal}. A move is not a loss, so only a mode nothing reached is reported. For
   * example, a stop on the footway between two corners of a crossing is reached at the centre over the footways that
   * led to it
   * </p>
   *
   * @param done the event reporting the intersection as done
   */
  private void moveHeldEntriesToCentre(CollapseSpatialIntersectionEvent done) {
    var inProgress = collapsesInProgressByCentre.remove(done.getCentreNode());
    if (inProgress == null) {
      return;
    }
    var centre = done.getCentreNode();
    for (var held : inProgress.heldEntries) {
      heldEntries.remove(held.entry);
      held.findSegmentsAtCentre(centre);
      if (!held.keepsInternalSegment()) {
        held.entry.removeAccessLinkSegment(held.internalSegment);
      }
    }
    var heldByConnectoid = inProgress.heldEntries.stream()
        .collect(Collectors.groupingBy(held -> held.connectoid, LinkedHashMap::new, Collectors.toList()));
    heldByConnectoid.forEach((connectoid, held) -> {
      if (held.stream().anyMatch(h -> !h.modesAtCentre.isEmpty())) {
        var atCentre = findOrMakeConnectoidAtCentre(connectoid, held, centre, inProgress);
        held.stream().filter(h -> !h.modesAtCentre.isEmpty()).forEach(h ->
            atCentre.addAccessLinkSegmentsForZoneAndTypeOf(h.entry, h.segmentsAtCentre, h.modesAtCentre));
      }
      settleHeldEntriesLeftAtOriginal(connectoid, held);
    });
  }

  /**
   * Constructor
   *
   * @param transferConnectoids container whose transfer connectoids to keep in step with the network
   */
  public UpdateTransferConnectoidsOnNetworkChangeHandler(final TransferConnectoidsImpl transferConnectoids) {
    super(transferConnectoids);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return Stream.concat(Arrays.stream(super.getKnownSupportedEventTypes()),
        Stream.of(RemoveEdgeSegmentEvent.EVENT_TYPE, ChangeLinkSegmentTypeEvent.EVENT_TYPE,
            BreakEdgeSegmentEvent.EVENT_TYPE, MoveEdgeEndsAtVertexEvent.EVENT_TYPE)).toArray(EventType[]::new);
  }

  /**
   * Listen, as an internal listener, to the graph, intersection and link segment type changes made through a layer
   * modifier
   *
   * @param layerModifier to listen to
   */
  @Override
  public void listenTo(MacroscopicNetworkLayerModifier layerModifier) {
    super.listenTo(layerModifier);
    layerModifier.addInternalListener((LinkSegmentTypeModifierListener) this);
  }

  /**
   * Stop listening to a layer modifier
   *
   * @param layerModifier to stop listening to
   */
  @Override
  public void stopListeningTo(MacroscopicNetworkLayerModifier layerModifier) {
    super.stopListeningTo(layerModifier);
    layerModifier.removeInternalListener((LinkSegmentTypeModifierListener) this);
  }

  /**
   * Take a removed link segment out of the entries using it, and follow a broken link: breaking link A-B at C keeps the
   * original segments on A-C and makes new link segments for C-B, so an entry of a connectoid at B reached over the
   * original, now running A-C, uses the new link segment C-B instead, while a connectoid at A keeps its segment
   *
   * @param event of the graph change
   */
  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    if (event.getType().equals(RemoveEdgeSegmentEvent.EVENT_TYPE)) {
      removeAccessLinkSegmentFromEntriesUsing(((RemoveEdgeSegmentEvent) event).getRemovedEdgeSegment());
    } else if (event.getType().equals(BreakEdgeSegmentEvent.EVENT_TYPE)) {
      var broken = (BreakEdgeSegmentEvent) event;
      replaceAccessLinkSegmentInEntries(broken.getOriginalEdgeSegment(), broken.getNewlyBrokenEdgeSegment());
    } else if (event.getType().equals(MoveEdgeEndsAtVertexEvent.EVENT_TYPE)) {
      followMovedEdgeEnds((MoveEdgeEndsAtVertexEvent) event);
    }
  }

  /**
   * Withdraw the modes a link segment lost with its new type from the entries reached over it
   *
   * @param event of the link segment type change
   */
  @Override
  public void onLinkSegmentTypeModifierEvent(LinkSegmentTypeModificationEvent event) {
    if (!event.getType().equals(ChangeLinkSegmentTypeEvent.EVENT_TYPE)) {
      return;
    }
    var change = (ChangeLinkSegmentTypeEvent) event;
    Set<Mode> lostModes = new TreeSet<>(Comparator.comparingLong(Mode::getId));
    if (change.getPreviousType() != null) {
      lostModes.addAll(change.getPreviousType().getAllowedModes());
    }
    if (change.getNewType() != null) {
      lostModes.removeAll(change.getNewType().getAllowedModes());
    }
    if (!lostModes.isEmpty()) {
      removeAccessFromEntriesUsingSegmentThatLostModes(change.getLinkSegment(), lostModes);
    }
  }

  /**
   * Follow the collapse of a spatial intersection as for every connectoid, and when it starts, hold the entries over
   * its internal segments, see {@link #holdEntriesOverInternalSegments}, and when it is done, move them to its centre
   * node, see {@link #moveHeldEntriesToCentre}. The connectoids at its removed member nodes are at the centre by then
   *
   * @param event of the intersection change
   */
  @Override
  public void onIntersectionModifierEvent(IntersectionModificationEvent event) {
    super.onIntersectionModifierEvent(event);
    if (!event.getType().equals(CollapseSpatialIntersectionEvent.EVENT_TYPE)) {
      return;
    }
    var collapse = (CollapseSpatialIntersectionEvent) event;
    switch (collapse.getState().orElseThrow()) {
      case START:
        holdEntriesOverInternalSegments(collapse);
        break;
      case DONE:
        moveHeldEntriesToCentre(collapse);
        break;
    }
  }
}
