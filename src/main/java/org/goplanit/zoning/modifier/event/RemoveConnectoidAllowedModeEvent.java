package org.goplanit.zoning.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.connectoid.Connectoid;
import org.goplanit.utils.zoning.connectoid.ConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.modifier.ZoningModifier;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;
import org.goplanit.utils.zoning.modifier.event.ZoningModifierEventType;

/**
 * Fired when an explicitly allowed mode is removed from an access zone entry of a connectoid through the zoning
 * modifier, the entry keeping its other modes
 * <p>
 * An entry allowing modes explicitly, such as a stop for bus and car, loses a mode when none of its access link
 * segments allows it any more, for example once the only road reaching it stops allowing buses. A caller that wants
 * to know what was lost listens to this and the other connectoid loss events.
 * </p>
 *
 * @author markr
 */
public class RemoveConnectoidAllowedModeEvent extends EventImpl implements ZoningModificationEvent {

  /** event type fired when an explicitly allowed mode is removed from an access zone entry */
  public static final ZoningModifierEventType EVENT_TYPE =
      new ZoningModifierEventType("ZONINGEVENT.REMOVE_CONNECTOID_ALLOWED_MODE");

  /**
   * Constructor
   *
   * @param source zoning modifier firing the event
   * @param connectoid the entry belongs to
   * @param accessZoneEntry the mode was removed from
   * @param removedMode the mode removed
   */
  public RemoveConnectoidAllowedModeEvent(
      ZoningModifier source, Connectoid connectoid, ConnectoidAccessZoneEntry accessZoneEntry, Mode removedMode) {
    super(EVENT_TYPE, source, new Object[] { connectoid, accessZoneEntry, removedMode });
  }

  /**
   * The connectoid the entry belongs to
   *
   * @return connectoid
   */
  public Connectoid getConnectoid() {
    return (Connectoid) getContent()[0];
  }

  /**
   * The entry the mode was removed from
   *
   * @return access zone entry
   */
  public ConnectoidAccessZoneEntry getAccessZoneEntry() {
    return (ConnectoidAccessZoneEntry) getContent()[1];
  }

  /**
   * The mode removed
   *
   * @return removed mode
   */
  public Mode getRemovedMode() {
    return (Mode) getContent()[2];
  }
}
