package org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler;

import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.goplanit.graph.directed.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.graph.modifier.event.RemoveEdgeEvent;
import org.goplanit.network.layer.macroscopic.SpatialMovementsImpl;
import org.goplanit.network.layer.macroscopic.intersection.IntersectionsImpl;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Keeps the intersections consistent when link segments are removed, on their own or still attached to a removed link:
 * a removed segment is no longer an approach or internal segment of the intersection referring to it, and the
 * movements entering or leaving on it are removed. Removing an internal segment leaves every movement as it is
 *
 * @author markr
 */
public class UpdateIntersectionsOnLinkSegmentRemovalHandler implements DirectedGraphModifierListener {

  /** logger to use */
  private static final Logger LOGGER =
      Logger.getLogger(UpdateIntersectionsOnLinkSegmentRemovalHandler.class.getCanonicalName());

  /** intersections to keep consistent */
  private final IntersectionsImpl intersections;

  /** movements through the intersections to keep consistent */
  private final SpatialMovementsImpl spatialMovements;

  /** removes a movement from its intersection and the layer, with its removal event */
  private final Consumer<SpatialMovement> removeSpatialMovement;

  /**
   * Remove the movements entering or leaving on the segment, then the segment from the intersection referring to it,
   * if any
   *
   * @param segment removed
   */
  private void removeSegment(LinkSegment segment) {
    List.copyOf(spatialMovements.getBySegment(segment)).forEach(removeSpatialMovement);
    var intersection = intersections.getBySegment(segment);
    if (intersection == null) {
      return;
    }
    intersections.update(intersection, i -> {
      i.removeApproachSegment(segment);
      if (i instanceof SpatialIntersection) {
        ((SpatialIntersection) i).removeInternalSegment(segment);
      }
    });
  }

  /**
   * Constructor
   *
   * @param intersections to keep consistent
   * @param spatialMovements to keep consistent
   * @param removeSpatialMovement removes a movement from its intersection and the layer, with its removal event
   */
  public UpdateIntersectionsOnLinkSegmentRemovalHandler(final IntersectionsImpl intersections,
      final SpatialMovementsImpl spatialMovements,
      final Consumer<SpatialMovement> removeSpatialMovement) {
    this.intersections = intersections;
    this.spatialMovements = spatialMovements;
    this.removeSpatialMovement = removeSpatialMovement;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { RemoveEdgeSegmentEvent.EVENT_TYPE, RemoveEdgeEvent.EVENT_TYPE };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    if (!event.getType().equals(RemoveEdgeSegmentEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports link segment and link removal events",
          UpdateIntersectionsOnLinkSegmentRemovalHandler.class.getName()));
      return;
    }
    removeSegment((LinkSegment) RemoveEdgeSegmentEvent.class.cast(event).getRemovedEdgeSegment());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    if (!event.getType().equals(RemoveEdgeEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports link segment and link removal events",
          UpdateIntersectionsOnLinkSegmentRemovalHandler.class.getName()));
      return;
    }
    /* segments still attached to the removed link, if any */
    var link = (DirectedEdge) RemoveEdgeEvent.class.cast(event).getRemovedEdge();
    if (link.hasEdgeSegment()) {
      link.getEdgeSegments().forEach(segment -> removeSegment((LinkSegment) segment));
    }
  }
}
