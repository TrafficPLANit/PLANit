package org.goplanit.network.layer.macroscopic.tollroad.modifier.event.handler;

import java.util.logging.Logger;

import org.goplanit.utils.graph.modifier.event.BreakEdgeSegmentEvent;
import org.goplanit.network.layer.macroscopic.tollroad.TollRoadImpl;
import org.goplanit.network.layer.macroscopic.tollroad.TollRoadsImpl;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Keeps the toll roads consistent once a link is broken at a node: when the broken link segment is on a toll road,
 * the new link segment broken off it is added to that toll road too, right after it, so the toll road still holds the
 * whole stretch it held before. Only the direction that was tolled is affected
 * <p>
 * Links are broken at nodes all the time while a network is built, for example to place a stop or join a crossing
 * road. Without this, the part of a tolled carriageway beyond the node would no longer be tolled, and a route using it
 * would pay nothing there. The layer modifier registers it as an internal listener, so it runs before any listener
 * from outside, such as a network pricing following the same break.
 * </p>
 *
 * @author markr
 */
public class UpdateTollRoadsOnBreakLinkSegmentHandler implements DirectedGraphModifierListener {

  /** logger to use */
  private static final Logger LOGGER =
      Logger.getLogger(UpdateTollRoadsOnBreakLinkSegmentHandler.class.getCanonicalName());

  /** toll roads to keep consistent */
  private final TollRoadsImpl tollRoads;

  /**
   * Constructor
   *
   * @param tollRoads to keep consistent
   */
  public UpdateTollRoadsOnBreakLinkSegmentHandler(final TollRoadsImpl tollRoads) {
    this.tollRoads = tollRoads;
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
        UpdateTollRoadsOnBreakLinkSegmentHandler.class.getName()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    if (!event.getType().equals(BreakEdgeSegmentEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports break edge segment events",
          UpdateTollRoadsOnBreakLinkSegmentHandler.class.getName()));
      return;
    }
    var breakEvent = BreakEdgeSegmentEvent.class.cast(event);
    var original = (LinkSegment) breakEvent.getOriginalEdgeSegment();
    var brokenOff = breakEvent.getNewlyBrokenEdgeSegment();
    var tollRoad = original == brokenOff ? null : tollRoads.getByLinkSegment(original);
    if (tollRoad == null || !(brokenOff instanceof MacroscopicLinkSegment)) {
      return;
    }
    tollRoads.update(tollRoad,
        t -> ((TollRoadImpl) t).addLinkSegmentAfter(original, (MacroscopicLinkSegment) brokenOff));
  }
}
