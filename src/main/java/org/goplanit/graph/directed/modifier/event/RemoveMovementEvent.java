package org.goplanit.graph.directed.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.modifier.DirectedGraphModifier;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;
import org.goplanit.utils.network.layer.physical.BannedMovement;

/**
 * Event for when a movement has been removed from a (sub) graph
 * 
 * @author markr
 *
 */
public class RemoveMovementEvent extends EventImpl implements DirectedGraphModificationEvent {

  /** event type fired off when sub graph edge segment has been removed */
  public static final GraphModifierEventType EVENT_TYPE =
      new DirectedGraphModifierEventType("DIRECTEDGRAPHMODIFIER.MOVEMENT.REMOVE");

  /**
   * Constructor
   *
   * @param source             directed graph modifier firing the event
   * @param removedMovement    movement that is removed
   */
  public RemoveMovementEvent(DirectedGraphModifier source, BannedMovement removedMovement) {
    super(EVENT_TYPE, source, removedMovement);
  }

  /**
   * The removed movement
   * 
   * @return removed movement
   */
  public BannedMovement getRemovedEdgeSegment() {
    return (BannedMovement) getContent()[0];
  }

}
