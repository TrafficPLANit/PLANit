package org.goplanit.network.layer.macroscopic.intersection.modifier;

import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierEventProducer;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierEventType;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierListener;

/**
 * Produces intersection events to the intersection listeners registered on it, for a modifier that cannot extend the
 * event producer itself
 *
 * @author markr
 */
public class IntersectionModifierEventProducerImpl extends EventProducerImpl
    implements IntersectionModifierEventProducer {

  /**
   * {@inheritDoc}
   */
  @Override
  protected void fireEvent(EventListener eventListener, Event event) {
    ((IntersectionModifierListener) eventListener).onIntersectionModifierEvent((IntersectionModificationEvent) event);
  }

  /**
   * Fire the event to the listeners registered for its type
   *
   * @param event to fire
   */
  public void fireEvent(IntersectionModificationEvent event) {
    super.fireEvent(event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(IntersectionModifierListener listener) {
    super.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(IntersectionModifierListener listener, IntersectionModifierEventType eventType) {
    super.addListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(IntersectionModifierListener listener, IntersectionModifierEventType eventType) {
    super.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(IntersectionModifierListener listener) {
    super.removeListener(listener);
  }
}
