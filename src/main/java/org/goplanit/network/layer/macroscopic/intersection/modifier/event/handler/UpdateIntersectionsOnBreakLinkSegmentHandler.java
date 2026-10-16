package org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler;

import java.util.List;
import java.util.logging.Logger;

import org.goplanit.graph.directed.modifier.event.BreakEdgeSegmentEvent;
import org.goplanit.network.layer.macroscopic.SpatialMovementsImpl;
import org.goplanit.network.layer.macroscopic.intersection.IntersectionsImpl;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * Keeps the intersections referring to a broken link segment consistent once a link is broken at a node: a broken
 * internal segment leaves both pieces internal, with the node broken at as a member, and an approach moves to the new
 * segment when that is the piece ending at a member node. A movement through an intersection follows its approach or
 * exit to the piece that touches the intersection
 *
 * @author markr
 */
public class UpdateIntersectionsOnBreakLinkSegmentHandler implements DirectedGraphModifierListener {

  /** logger to use */
  private static final Logger LOGGER =
      Logger.getLogger(UpdateIntersectionsOnBreakLinkSegmentHandler.class.getCanonicalName());

  /** intersections to keep consistent */
  private final IntersectionsImpl intersections;

  /** movements through the intersections to keep consistent */
  private final SpatialMovementsImpl spatialMovements;

  /**
   * Point the movements entering or leaving on a broken segment to the piece touching their intersection: the piece
   * ending where the approach ended, or starting where the exit started
   *
   * @param original the segment broken, now one of the pieces
   * @param brokenOff the other piece
   * @param brokenAt the node broken at
   */
  private void repointMovements(MacroscopicLinkSegment original, MacroscopicLinkSegment brokenOff, Node brokenAt) {
    for (var movement : List.copyOf(spatialMovements.getBySegment(original))) {
      spatialMovements.update(movement, m -> {
        if (m.getLinkSegmentFrom() == original && original.getDownstreamVertex() == brokenAt) {
          m.setLinkSegmentFrom(brokenOff);
        }
        if (m.getLinkSegmentTo() == original && original.getUpstreamVertex() == brokenAt) {
          m.setLinkSegmentTo(brokenOff);
        }
      });
    }
  }

  /**
   * Constructor
   *
   * @param intersections to keep consistent
   * @param spatialMovements to keep consistent
   */
  public UpdateIntersectionsOnBreakLinkSegmentHandler(
      final IntersectionsImpl intersections, final SpatialMovementsImpl spatialMovements) {
    this.intersections = intersections;
    this.spatialMovements = spatialMovements;
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
    if (original != brokenOff) {
      repointMovements(original, brokenOff, (Node) breakEvent.getVertexToBreakAt());
    }
    var intersection = original == brokenOff ? null : intersections.getBySegment(original);
    if (intersection == null) {
      return;
    }
    intersections.update(intersection, i -> {
      if (i instanceof SpatialIntersection &&
          ListUtils.containsByIdentity(((SpatialIntersection) i).getInternalSegments(), original)) {
        ((SpatialIntersection) i).addMemberNode((Node) breakEvent.getVertexToBreakAt());
        ((SpatialIntersection) i).addInternalSegment(brokenOff);
      } else if (!i.hasMemberNode(original.getDownstreamVertex())) {
        boolean hasOwnSignal = i.hasOwnSignal(original);
        i.removeApproachSegment(original);
        i.addApproachSegment(brokenOff);
        i.setApproachSegmentHasOwnSignal(brokenOff, hasOwnSignal);
      }
    });
  }
}
