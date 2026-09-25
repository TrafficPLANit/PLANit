package org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler;

import java.util.logging.Logger;

import org.goplanit.graph.directed.modifier.event.BreakEdgeSegmentEvent;
import org.goplanit.network.layer.macroscopic.intersection.IntersectionsImpl;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * Keeps the intersections referring to a broken link segment consistent once a link is broken at a node: a broken
 * internal segment leaves both pieces internal, with the node broken at as a member, and an approach moves to the new
 * segment when that is the piece ending at a member node
 *
 * @author markr
 */
public class UpdateIntersectionsOnBreakLinkSegmentHandler implements DirectedGraphModifierListener {

  /** logger to use */
  private static final Logger LOGGER =
      Logger.getLogger(UpdateIntersectionsOnBreakLinkSegmentHandler.class.getCanonicalName());

  /** intersections to keep consistent */
  private final IntersectionsImpl intersections;

  /**
   * Constructor
   *
   * @param intersections to keep consistent
   */
  public UpdateIntersectionsOnBreakLinkSegmentHandler(final IntersectionsImpl intersections) {
    this.intersections = intersections;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { BreakEdgeSegmentEvent.EVENT_TYPE };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    LOGGER.warning(String.format("%s only supports break edge segment events",
        UpdateIntersectionsOnBreakLinkSegmentHandler.class.getName()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    if (!event.getType().equals(BreakEdgeSegmentEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports break edge segment events",
          UpdateIntersectionsOnBreakLinkSegmentHandler.class.getName()));
      return;
    }
    var breakEvent = BreakEdgeSegmentEvent.class.cast(event);
    var original = (MacroscopicLinkSegment) breakEvent.getOriginalEdgeSegment();
    var brokenOff = (MacroscopicLinkSegment) breakEvent.getNewlyBrokenEdgeSegment();
    var intersection = original == brokenOff ? null : intersections.getBySegment(original);
    if (intersection == null) {
      return;
    }
    intersections.update(intersection, i -> {
      if (i.getInternalSegments().stream().anyMatch(segment -> segment == original)) {
        i.addMemberNode((Node) breakEvent.getVertexToBreakAt());
        i.addInternalSegment(brokenOff);
      } else if (!i.hasMemberNode(original.getDownstreamVertex())) {
        i.removeApproachSegment(original);
        i.addApproachSegment(brokenOff);
      }
    });
  }
}
