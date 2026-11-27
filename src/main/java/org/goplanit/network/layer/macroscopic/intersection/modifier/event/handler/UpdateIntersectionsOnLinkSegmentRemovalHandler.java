package org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler;

import java.util.function.Consumer;
import java.util.logging.Logger;

import org.goplanit.utils.graph.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.network.layer.macroscopic.SpatialMovementsImpl;
import org.goplanit.network.layer.macroscopic.intersection.IntersectionsImpl;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Keeps the intersections consistent when link segments are removed: a removed segment is no longer an approach or
 * internal segment of the intersection referring to it, and the movements entering or leaving on it are removed.
 * Removing an internal segment leaves every movement as it is. A removed link is no concern of it, as removing a link
 * removes each of its link segments first
 * <p>
 * Link segments are removed when a network is cleaned, for example dangling parts or link segments no mode may use. An
 * intersection must not keep an approach that is gone, nor a movement along it, or a route search or a signal would
 * use a link segment that no longer exists.
 * </p>
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
    spatialMovements.getBySegment(segment).forEach(removeSpatialMovement);
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
    return new EventType[] { RemoveEdgeSegmentEvent.EVENT_TYPE };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    if (!event.getType().equals(RemoveEdgeSegmentEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports link segment removal events",
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
    LOGGER.warning(String.format("%s only supports link segment removal events",
        UpdateIntersectionsOnLinkSegmentRemovalHandler.class.getName()));
  }
}
