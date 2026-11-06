package org.goplanit.network.layer.modifier;

import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModificationEvent;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierEventProducer;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierEventType;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierListener;

/**
 * Produces link segment type events to the link segment type listeners registered on it, for a modifier that cannot
 * extend the event producer itself
 *
 * @author markr
 */
public class LinkSegmentTypeModifierEventProducerImpl extends EventProducerImpl
    implements LinkSegmentTypeModifierEventProducer {

  /**
   * {@inheritDoc}
   */
  @Override
  protected void fireEvent(EventListener eventListener, Event event) {
    ((LinkSegmentTypeModifierListener) eventListener).onLinkSegmentTypeModifierEvent(
        (LinkSegmentTypeModificationEvent) event);
  }

  /**
   * Fire the event to the listeners registered for its type
   *
   * @param event to fire
   */
  public void fireEvent(LinkSegmentTypeModificationEvent event) {
    super.fireEvent(event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(LinkSegmentTypeModifierListener listener) {
    super.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(LinkSegmentTypeModifierListener listener, LinkSegmentTypeModifierEventType eventType) {
    super.addListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(LinkSegmentTypeModifierListener listener, LinkSegmentTypeModifierEventType eventType) {
    super.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(LinkSegmentTypeModifierListener listener) {
    super.removeListener(listener);
  }
}
