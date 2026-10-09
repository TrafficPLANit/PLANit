package org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.goplanit.graph.modifier.event.RemoveVertexEvent;
import org.goplanit.network.layer.macroscopic.intersection.IntersectionsImpl;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * Keeps the intersections consistent when nodes are removed: a removed node leaves its intersection, together with the
 * approach and internal segments referring to it. The node's links are detached from it before its removal is heard,
 * so those segments are found by no longer meeting the member nodes their role requires. An intersection left without
 * member nodes is kept for the modifier to remove, see {@link #drainIntersectionsLeftWithoutMemberNodes()}
 *
 * @author markr
 */
public class UpdateIntersectionsOnVertexRemovalHandler implements DirectedGraphModifierListener {

  /** logger to use */
  private static final Logger LOGGER =
      Logger.getLogger(UpdateIntersectionsOnVertexRemovalHandler.class.getCanonicalName());

  /** intersections to keep consistent */
  private final IntersectionsImpl intersections;

  /** intersections left without member nodes since they were last drained */
  private final List<Intersection> leftWithoutMemberNodes = new ArrayList<>();

  /**
   * Constructor
   *
   * @param intersections to keep consistent
   */
  public UpdateIntersectionsOnVertexRemovalHandler(final IntersectionsImpl intersections) {
    this.intersections = intersections;
  }

  /**
   * Collect the intersections left without member nodes since the last call, and forget them
   *
   * @return intersections left without member nodes, in the order it happened
   */
  public List<Intersection> drainIntersectionsLeftWithoutMemberNodes() {
    var drained = List.copyOf(leftWithoutMemberNodes);
    leftWithoutMemberNodes.clear();
    return drained;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { RemoveVertexEvent.EVENT_TYPE };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    if (!event.getType().equals(RemoveVertexEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports vertex removal events",
          UpdateIntersectionsOnVertexRemovalHandler.class.getName()));
      return;
    }
    var node = (Node) RemoveVertexEvent.class.cast(event).getRemovedVertex();
    var intersection = intersections.getByMemberNode(node);
    if (intersection == null) {
      return;
    }
    intersections.update(intersection, i -> {
      i.removeMemberNode(node, false);
      List.copyOf(i.getApproachSegments()).stream()
          .filter(segment -> !i.hasMemberNode(segment.getDownstreamVertex())).forEach(i::removeApproachSegment);
      List.copyOf(i.getInternalSegments()).stream()
          .filter(segment -> !i.hasMemberNode(segment.getUpstreamVertex()) || !i.hasMemberNode(segment.getDownstreamVertex()))
          .forEach(i::removeInternalSegment);
    });
    if (intersection.getMemberNodes().isEmpty()) {
      leftWithoutMemberNodes.add(intersection);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    LOGGER.warning(String.format("%s only supports vertex removal events",
        UpdateIntersectionsOnVertexRemovalHandler.class.getName()));
  }
}
