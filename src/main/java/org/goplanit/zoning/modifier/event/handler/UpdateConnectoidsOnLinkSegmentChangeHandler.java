package org.goplanit.zoning.modifier.event.handler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.goplanit.graph.directed.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.network.layer.modifier.event.ChangeLinkSegmentTypeEvent;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModificationEvent;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierListener;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.zoning.connectoid.Connectoid;
import org.goplanit.utils.zoning.connectoid.DirectedConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.connectoid.OdConnectoid;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.modifier.event.handler.UpdateConnectoidsOnLinkSegmentChangeResult.ConnectoidAccessLoss;

/**
 * Keeps the access of connectoids in step with the link segments they use as access link segments. A link segment that
 * is removed is taken out of every access zone entry using it. A link segment whose type changes, and with it loses
 * modes, matters to the entries that allow modes explicitly: it is taken out of such an entry when it allows none of
 * the entry's modes any more, and a mode none of the entry's access link segments allows any more is taken out of the
 * entry. An entry allowing every mode is left as it is, since what it allows follows from its access link segments. An
 * entry left without access link segments, or without allowed modes, is removed, and so is a connectoid left without
 * entries.
 * <p>
 * Register it on the layer's modifier, both for its graph events, which tell of removed link segments, and for its link
 * segment type events, which tell of changed link segment types. Register it once the connectoids exist and before
 * their link segments change, since it indexes the connectoids by their access link segments when created. Entries
 * without access link segments of their own are left as they are. What the connectoids lost is listed, see
 * {@link #getResult()}.
 * </p>
 *
 * @author markr
 */
public class UpdateConnectoidsOnLinkSegmentChangeHandler
    implements DirectedGraphModifierListener, LinkSegmentTypeModifierListener {

  /** An access zone entry with access link segments, with the connectoid it belongs to */
  private static final class IndexedEntry {

    /** the connectoid the entry belongs to */
    private final Connectoid connectoid;

    /** the entry */
    private final DirectedConnectoidAccessZoneEntry entry;

    /**
     * Constructor
     *
     * @param connectoid the entry belongs to
     * @param entry with access link segments
     */
    private IndexedEntry(Connectoid connectoid, DirectedConnectoidAccessZoneEntry entry) {
      this.connectoid = connectoid;
      this.entry = entry;
    }
  }

  /** logger to use */
  private static final Logger LOGGER =
      Logger.getLogger(UpdateConnectoidsOnLinkSegmentChangeHandler.class.getCanonicalName());

  /** zoning whose connectoids are kept in step */
  private final Zoning zoning;

  /** the entries using each link segment as an access link segment */
  private final Map<EdgeSegment, List<IndexedEntry>> entriesByAccessLinkSegment = new IdentityHashMap<>();

  /** the entries to stop indexing under an access link segment, collected while a change is followed and taken out of
   * the index once it is followed, see {@link #unindexCollected()}, so the index is not changed while iterated */
  private final Set<Pair<EdgeSegment, IndexedEntry>> toUnindex = new HashSet<>();

  /** what the connectoids lost, in the order it happened */
  private final UpdateConnectoidsOnLinkSegmentChangeResult result = new UpdateConnectoidsOnLinkSegmentChangeResult();

  /**
   * Index the access zone entries with access link segments of a connectoid
   *
   * @param connectoid to index
   */
  private void index(Connectoid connectoid) {
    for (var entry : connectoid) {
      if (!(entry instanceof DirectedConnectoidAccessZoneEntry) ||
          !((DirectedConnectoidAccessZoneEntry) entry).hasAccessLinkSegments()) {
        continue;
      }
      var indexedEntry = new IndexedEntry(connectoid, (DirectedConnectoidAccessZoneEntry) entry);
      for (var accessLinkSegment : indexedEntry.entry.getAccessLinkSegments()) {
        entriesByAccessLinkSegment.computeIfAbsent(accessLinkSegment, segment -> new ArrayList<>(1)).add(indexedEntry);
      }
    }
  }

  /**
   * Stop indexing the entries collected while a change was followed, see {@link #toUnindex}. Only these few are
   * removed, so the index is never copied
   */
  private void unindexCollected() {
    for (var accessLinkSegmentAndEntry : toUnindex) {
      var indexedEntries = entriesByAccessLinkSegment.get(accessLinkSegmentAndEntry.first());
      if (indexedEntries == null) {
        continue;
      }
      indexedEntries.removeIf(candidate -> candidate == accessLinkSegmentAndEntry.second());
      if (indexedEntries.isEmpty()) {
        entriesByAccessLinkSegment.remove(accessLinkSegmentAndEntry.first());
      }
    }
    toUnindex.clear();
  }

  /**
   * Remove an access zone entry from its connectoid, and the connectoid from the zoning when it has no entry left
   *
   * @param indexedEntry to remove
   */
  private void removeAccessZoneEntry(IndexedEntry indexedEntry) {
    var connectoid = indexedEntry.connectoid;
    var entry = indexedEntry.entry;
    connectoid.removeAccessZoneEntry(entry.getAccessZone(), entry.getType());
    for (var accessLinkSegment : entry.getAccessLinkSegments()) {
      toUnindex.add(Pair.of(accessLinkSegment, indexedEntry));
    }
    if (connectoid.hasAccessZoneEntries()) {
      result.addAccessLoss(ConnectoidAccessLoss.Kind.ACCESS_ZONE_ENTRY, connectoid, entry, null, null);
      return;
    }
    if (connectoid instanceof TransferConnectoid) {
      zoning.getTransferConnectoids().remove((TransferConnectoid) connectoid);
    } else if (connectoid instanceof OdConnectoid) {
      zoning.getOdConnectoids().remove((OdConnectoid) connectoid);
    }
    result.addAccessLoss(ConnectoidAccessLoss.Kind.CONNECTOID, connectoid, entry, null, null);
  }

  /**
   * Take an access link segment out of an entry, and the entry out of its connectoid when it has none left
   *
   * @param indexedEntry using the access link segment
   * @param accessLinkSegment to take out
   */
  private void removeAccessLinkSegment(IndexedEntry indexedEntry, EdgeSegment accessLinkSegment) {
    indexedEntry.entry.removeAccessLinkSegment(accessLinkSegment);
    toUnindex.add(Pair.of(accessLinkSegment, indexedEntry));
    if (!indexedEntry.entry.hasAccessLinkSegments()) {
      removeAccessZoneEntry(indexedEntry);
      return;
    }
    result.addAccessLoss(ConnectoidAccessLoss.Kind.ACCESS_LINK_SEGMENT,
        indexedEntry.connectoid, indexedEntry.entry, accessLinkSegment, null);
  }

  /**
   * Follow the modes an access link segment of an entry allowing modes explicitly lost: the link segment goes when it
   * allows none of the entry's modes, and a mode no access link segment of the entry still there allows any more goes
   * from the entry, the entry itself going when that was its last mode
   *
   * @param indexedEntry using the access link segment, allowing modes explicitly
   * @param accessLinkSegment that lost the modes
   * @param lostModes the modes it lost
   */
  private void followLostModes(
      IndexedEntry indexedEntry, MacroscopicLinkSegment accessLinkSegment, Set<Mode> lostModes) {
    var entry = indexedEntry.entry;
    if (entry.getExplicitlyAllowedModes().stream().noneMatch(accessLinkSegment::isModeAllowed)) {
      removeAccessLinkSegment(indexedEntry, accessLinkSegment);
      if (!entry.hasAccessLinkSegments()) {
        return;
      }
    }

    for (var mode : lostModes) {
      if (!entry.isModeAllowed(mode) ||
          entry.getAccessLinkSegments().stream().anyMatch(segment -> ((LinkSegment) segment).isModeAllowed(mode))) {
        continue;
      }
      if (!entry.removeExplicitAllowedMode(mode)) {
        /* its last allowed mode: without any the entry would allow every mode */
        removeAccessZoneEntry(indexedEntry);
        return;
      }
      result.addAccessLoss(
          ConnectoidAccessLoss.Kind.ALLOWED_MODE, indexedEntry.connectoid, entry, accessLinkSegment, mode);
    }
  }

  /**
   * Constructor, indexing the connectoids of the zoning by their access link segments
   *
   * @param zoning whose connectoids to keep in step
   */
  public UpdateConnectoidsOnLinkSegmentChangeHandler(final Zoning zoning) {
    this.zoning = zoning;
    zoning.getTransferConnectoids().forEach(this::index);
    zoning.getOdConnectoids().forEach(this::index);
  }

  /**
   * What the connectoids lost so far, in the order it happened
   *
   * @return the result
   */
  public UpdateConnectoidsOnLinkSegmentChangeResult getResult() {
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { RemoveEdgeSegmentEvent.EVENT_TYPE, ChangeLinkSegmentTypeEvent.EVENT_TYPE };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    LOGGER.warning(String.format("%s only supports removed link segments and changed link segment types",
        UpdateConnectoidsOnLinkSegmentChangeHandler.class.getName()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    if (!event.getType().equals(RemoveEdgeSegmentEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports removed link segments and changed link segment types",
          UpdateConnectoidsOnLinkSegmentChangeHandler.class.getName()));
      return;
    }
    var removed = ((RemoveEdgeSegmentEvent) event).getRemovedEdgeSegment();
    var indexedEntries = entriesByAccessLinkSegment.get(removed);
    if (indexedEntries == null) {
      return;
    }
    indexedEntries.forEach(indexedEntry -> removeAccessLinkSegment(indexedEntry, removed));
    unindexCollected();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onLinkSegmentTypeModifierEvent(LinkSegmentTypeModificationEvent event) {
    if (!event.getType().equals(ChangeLinkSegmentTypeEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports removed link segments and changed link segment types",
          UpdateConnectoidsOnLinkSegmentChangeHandler.class.getName()));
      return;
    }
    var change = (ChangeLinkSegmentTypeEvent) event;
    var indexedEntries = entriesByAccessLinkSegment.get(change.getLinkSegment());
    if (indexedEntries == null) {
      return;
    }

    Set<Mode> lostModes = new TreeSet<>(Comparator.comparingLong(Mode::getId));
    if (change.getPreviousType() != null) {
      lostModes.addAll(change.getPreviousType().getAllowedModes());
    }
    if (change.getNewType() != null) {
      lostModes.removeAll(change.getNewType().getAllowedModes());
    }
    if (lostModes.isEmpty()) {
      return;
    }
    indexedEntries.stream().filter(indexedEntry -> indexedEntry.entry.hasExplicitlyAllowedModes())
        .forEach(indexedEntry -> followLostModes(indexedEntry, change.getLinkSegment(), lostModes));
    unindexCollected();
  }
}
