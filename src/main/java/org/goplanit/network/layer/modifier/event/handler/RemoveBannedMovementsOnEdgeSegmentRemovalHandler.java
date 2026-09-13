package org.goplanit.network.layer.modifier.event.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.goplanit.graph.directed.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.directed.BannedMovement;
import org.goplanit.utils.graph.directed.BannedMovements;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;

/**
 * Removes banned movements that reference an edge segment removed through a directed graph modifier.
 *
 * @author markr
 */
public class RemoveBannedMovementsOnEdgeSegmentRemovalHandler implements DirectedGraphModifierListener {

  /** Banned-movement container to update. */
  private final BannedMovements bannedMovements;

  /** Banned movements indexed by each edge segment they reference. */
  private final Map<EdgeSegment, Set<BannedMovement>> bannedMovementsByEdgeSegment;

  /**
   * Create the handler for a banned-movement container.
   *
   * @param bannedMovements container whose banned movements are maintained
   */
  public RemoveBannedMovementsOnEdgeSegmentRemovalHandler(final BannedMovements bannedMovements) {
    this.bannedMovements = bannedMovements;
    this.bannedMovementsByEdgeSegment = new HashMap<>();
  }

  /**
   * Add a newly registered banned movement to the edge-segment index.
   *
   * @param bannedMovement newly registered banned movement
   */
  public void onBannedMovementRegistered(final BannedMovement bannedMovement) {
    indexByEdgeSegment(bannedMovement.getSegmentFrom(), bannedMovement);
    indexByEdgeSegment(bannedMovement.getSegmentTo(), bannedMovement);
  }

  /**
   * Remove a banned movement from the edge-segment index.
   *
   * @param bannedMovement removed or replaced banned movement
   */
  public void onBannedMovementRemoved(final BannedMovement bannedMovement) {
    removeFromEdgeSegmentIndex(bannedMovement.getSegmentFrom(), bannedMovement);
    removeFromEdgeSegmentIndex(bannedMovement.getSegmentTo(), bannedMovement);
  }

  /**
   * Populate the edge-segment index with existing banned movements.
   *
   * @param bannedMovements existing banned movements to index
   */
  public void initialise(final Iterable<? extends BannedMovement> bannedMovements) {
    reset();
    bannedMovements.forEach(this::onBannedMovementRegistered);
  }

  /**
   * Clear the edge-segment index.
   */
  public void reset() {
    bannedMovementsByEdgeSegment.clear();
  }

  /**
   * Index a banned movement by one of its edge segments.
   *
   * @param edgeSegment edge segment used as the index key, may be null
   * @param bannedMovement banned movement to index
   */
  private void indexByEdgeSegment(
      final EdgeSegment edgeSegment, final BannedMovement bannedMovement) {
    if (edgeSegment != null) {
      bannedMovementsByEdgeSegment.computeIfAbsent(edgeSegment, ignored -> new HashSet<>()).add(bannedMovement);
    }
  }

  /**
   * Remove a banned movement from the index for one of its edge segments.
   *
   * @param edgeSegment edge segment used as the index key, may be null
   * @param bannedMovement banned movement to remove from the index
   */
  private void removeFromEdgeSegmentIndex(
      final EdgeSegment edgeSegment, final BannedMovement bannedMovement) {
    if (edgeSegment == null) {
      return;
    }
    var indexedMovements = bannedMovementsByEdgeSegment.get(edgeSegment);
    if (indexedMovements != null) {
      indexedMovements.remove(bannedMovement);
      if (indexedMovements.isEmpty()) {
        bannedMovementsByEdgeSegment.remove(edgeSegment);
      }
    }
  }

  /**
   * Remove all banned movements that reference an edge segment.
   *
   * @param edgeSegment removed edge segment
   */
  private void removeBannedMovementsReferencing(final EdgeSegment edgeSegment) {
    var affectedMovements = new HashSet<>(
        bannedMovementsByEdgeSegment.getOrDefault(edgeSegment, Collections.emptySet()));
    for (var bannedMovement : affectedMovements) {
      bannedMovements.remove(bannedMovement);
      onBannedMovementRemoved(bannedMovement);
    }
  }

  /** {@inheritDoc} */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { RemoveEdgeSegmentEvent.EVENT_TYPE };
  }

  /** {@inheritDoc} */
  @Override
  public void onDirectedGraphModificationEvent(final DirectedGraphModificationEvent event) {
    if (event.getType().equals(RemoveEdgeSegmentEvent.EVENT_TYPE)) {
      removeBannedMovementsReferencing(((RemoveEdgeSegmentEvent) event).getRemovedEdgeSegment());
    }
  }

  /** {@inheritDoc} */
  @Override
  public void onGraphModificationEvent(final GraphModificationEvent event) {
    // This handler only supports directed edge-segment removal events.
  }
}
