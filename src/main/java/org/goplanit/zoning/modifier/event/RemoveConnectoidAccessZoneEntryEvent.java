package org.goplanit.zoning.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.zoning.connectoid.Connectoid;
import org.goplanit.utils.zoning.connectoid.ConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.modifier.ZoningModifier;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;
import org.goplanit.utils.zoning.modifier.event.ZoningModifierEventType;

/**
 * Fired when an access zone entry is removed from a connectoid through the zoning modifier, the connectoid staying in
 * the zoning
 * <p>
 * A connectoid can serve several zones, one entry each, and a network change can cost it one of them while it keeps
 * serving the others: for example the entry of a bus stop reached over a link segment that no longer allows buses,
 * while the entry for walking to a station stays. A caller that wants to know what was lost listens to this and the
 * other connectoid loss events.
 * </p>
 *
 * @author markr
 */
public class RemoveConnectoidAccessZoneEntryEvent extends EventImpl implements ZoningModificationEvent {

  /** event type fired when an access zone entry is removed from a connectoid */
  public static final ZoningModifierEventType EVENT_TYPE =
      new ZoningModifierEventType("ZONINGEVENT.REMOVE_CONNECTOID_ACCESS_ZONE_ENTRY");

  /**
   * Constructor
   *
   * @param source zoning modifier firing the event
   * @param connectoid the entry was removed from
   * @param removedAccessZoneEntry the entry removed
   */
  public RemoveConnectoidAccessZoneEntryEvent(
      ZoningModifier source, Connectoid connectoid, ConnectoidAccessZoneEntry removedAccessZoneEntry) {
    super(EVENT_TYPE, source, new Object[] { connectoid, removedAccessZoneEntry });
  }

  /**
   * The connectoid the entry was removed from
   *
   * @return connectoid
   */
  public Connectoid getConnectoid() {
    return (Connectoid) getContent()[0];
  }

  /**
   * The entry removed
   *
   * @return removed access zone entry
   */
  public ConnectoidAccessZoneEntry getRemovedAccessZoneEntry() {
    return (ConnectoidAccessZoneEntry) getContent()[1];
  }
}
