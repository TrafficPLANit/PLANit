package org.goplanit.zoning.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.zoning.connectoid.Connectoid;
import org.goplanit.utils.zoning.modifier.ZoningModifier;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;
import org.goplanit.utils.zoning.modifier.event.ZoningModifierEventType;

/**
 * Fired when a connectoid is removed from the zoning through its modifier. The connectoid keeps the access zone entries
 * it had, so a listener can tell what it served
 * <p>
 * Changes to the network the zoning lies on can cost connectoids their access: a removed node takes the connectoids at
 * it, a removed link segment the entries reached over it. A caller that wants to know what was lost, such as a reader
 * recording diagnostics while it prunes a network, listens to this and the other connectoid loss events.
 * </p>
 *
 * @author markr
 */
public class RemoveConnectoidEvent extends EventImpl implements ZoningModificationEvent {

  /** event type fired when a connectoid is removed */
  public static final ZoningModifierEventType EVENT_TYPE =
      new ZoningModifierEventType("ZONINGEVENT.REMOVE_CONNECTOID");

  /**
   * Constructor
   *
   * @param source zoning modifier firing the event
   * @param removedConnectoid the connectoid removed, with the entries it had
   */
  public RemoveConnectoidEvent(ZoningModifier source, Connectoid removedConnectoid) {
    super(EVENT_TYPE, source, removedConnectoid);
  }

  /**
   * The connectoid removed, with the entries it had
   *
   * @return removed connectoid
   */
  public Connectoid getRemovedConnectoid() {
    return (Connectoid) getContent()[0];
  }
}
