package org.goplanit.network.layer.macroscopic.tollroad.modifier.event.handler;

import java.util.function.Consumer;
import java.util.logging.Logger;

import org.goplanit.utils.graph.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.network.layer.macroscopic.tollroad.TollRoadImpl;
import org.goplanit.network.layer.macroscopic.tollroad.TollRoadsImpl;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Keeps the toll roads consistent when link segments are removed: a removed link segment is taken off its toll road,
 * and a toll road left without link segments is handed to the layer modifier, which removes it and announces that to
 * its listeners. A removed link is no concern of it, as removing a link removes each of its link segments first
 * <p>
 * Link segments are removed when a network is cleaned, for example dangling parts or link segments no mode may use.
 * A toll road must not keep referring to a link segment that is gone, and one left with none has nothing left to
 * toll, so it goes too. Its removal event lets a network pricing take the road off the schemes charging on it.
 * </p>
 *
 * @author markr
 */
public class UpdateTollRoadsOnLinkSegmentRemovalHandler implements DirectedGraphModifierListener {

  /** logger to use */
  private static final Logger LOGGER =
      Logger.getLogger(UpdateTollRoadsOnLinkSegmentRemovalHandler.class.getCanonicalName());

  /** toll roads to keep consistent */
  private final TollRoadsImpl tollRoads;

  /** removes a toll road left without link segments from the layer, with its removal event */
  private final Consumer<TollRoad> removeTollRoad;

  /**
   * Take the link segment off the toll road it is on, if any, and have a toll road left without link segments removed
   *
   * @param linkSegment removed
   */
  private void removeLinkSegment(LinkSegment linkSegment) {
    var tollRoad = tollRoads.getByLinkSegment(linkSegment);
    if (tollRoad == null) {
      return;
    }
    tollRoads.update(tollRoad, t -> ((TollRoadImpl) t).removeLinkSegment(linkSegment));
    if (tollRoad.getLinkSegments().isEmpty()) {
      removeTollRoad.accept(tollRoad);
    }
  }

  /**
   * Constructor
   *
   * @param tollRoads to keep consistent
   * @param removeTollRoad removes a toll road left without link segments from the layer, with its removal event
   */
  public UpdateTollRoadsOnLinkSegmentRemovalHandler(
      final TollRoadsImpl tollRoads,
      final Consumer<TollRoad> removeTollRoad) {
    this.tollRoads = tollRoads;
    this.removeTollRoad = removeTollRoad;
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
          UpdateTollRoadsOnLinkSegmentRemovalHandler.class.getName()));
      return;
    }
    removeLinkSegment((LinkSegment) RemoveEdgeSegmentEvent.class.cast(event).getRemovedEdgeSegment());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    LOGGER.warning(String.format("%s only supports link segment removal events",
        UpdateTollRoadsOnLinkSegmentRemovalHandler.class.getName()));
  }
}
