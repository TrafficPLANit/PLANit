package org.goplanit.test.zoning;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.goplanit.utils.event.EventType;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;
import org.goplanit.utils.zoning.modifier.event.ZoningModifierListener;
import org.goplanit.zoning.Zoning;
import org.goplanit.utils.zoning.modifier.event.RemoveConnectoidAccessLinkSegmentEvent;
import org.goplanit.utils.zoning.modifier.event.RemoveConnectoidAccessZoneEntryEvent;
import org.goplanit.utils.zoning.modifier.event.RemoveConnectoidAllowedModeEvent;
import org.goplanit.utils.zoning.modifier.event.RemoveConnectoidEvent;

/**
 * Records the connectoid loss events a zoning's modifier fires, in order, so a test can check what was reported and
 * how often
 *
 * @author markr
 */
public final class ZoningLossEventRecorder implements ZoningModifierListener {

  /** the events heard, in order */
  private final List<ZoningModificationEvent> heard = new ArrayList<>();

  /**
   * Constructor, listening to the zoning's modifier from now on
   *
   * @param zoning whose loss events to record
   */
  public ZoningLossEventRecorder(Zoning zoning) {
    zoning.getZoningModifier().addListener(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { RemoveConnectoidEvent.EVENT_TYPE, RemoveConnectoidAccessZoneEntryEvent.EVENT_TYPE,
        RemoveConnectoidAccessLinkSegmentEvent.EVENT_TYPE, RemoveConnectoidAllowedModeEvent.EVENT_TYPE };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onZoningModifierEvent(ZoningModificationEvent event) {
    heard.add(event);
  }

  /**
   * The events heard so far
   *
   * @return events in the order heard
   */
  public List<ZoningModificationEvent> getHeard() {
    return List.copyOf(heard);
  }

  /**
   * The types of the events heard so far
   *
   * @return types in the order heard
   */
  public List<EventType> getHeardTypes() {
    return heard.stream().map(ZoningModificationEvent::getType).collect(Collectors.toList());
  }
}
