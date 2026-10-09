package org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler;

import java.util.logging.Logger;

import org.goplanit.graph.directed.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.graph.modifier.event.RemoveEdgeEvent;
import org.goplanit.network.layer.macroscopic.intersection.IntersectionsImpl;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;

/**
 * Keeps the intersections consistent when link segments are removed, on their own or still attached to a removed link:
 * a removed segment is no longer an approach or internal segment of the intersection referring to it
 *
 * @author markr
 */
public class UpdateIntersectionsOnLinkSegmentRemovalHandler implements DirectedGraphModifierListener {

  /** logger to use */
  private static final Logger LOGGER =
      Logger.getLogger(UpdateIntersectionsOnLinkSegmentRemovalHandler.class.getCanonicalName());

  /** intersections to keep consistent */
  private final IntersectionsImpl intersections;

  /**
   * Remove the segment from the intersection referring to it, if any
   *
   * @param segment removed
   */
  private void removeSegment(MacroscopicLinkSegment segment) {
    var intersection = intersections.getBySegment(segment);
    if (intersection == null) {
      return;
    }
    intersections.update(intersection, i -> {
      i.removeApproachSegment(segment);
      i.removeInternalSegment(segment);
    });
  }

  /**
   * Constructor
   *
   * @param intersections to keep consistent
   */
  public UpdateIntersectionsOnLinkSegmentRemovalHandler(final IntersectionsImpl intersections) {
    this.intersections = intersections;
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
    removeSegment((MacroscopicLinkSegment) RemoveEdgeSegmentEvent.class.cast(event).getRemovedEdgeSegment());
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
      link.getEdgeSegments().forEach(segment -> removeSegment((MacroscopicLinkSegment) segment));
    }
  }
}
