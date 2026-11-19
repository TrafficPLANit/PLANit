package org.goplanit.zoning.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.zoning.connectoid.Connectoid;
import org.goplanit.utils.zoning.connectoid.DirectedConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.modifier.ZoningModifier;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;
import org.goplanit.utils.zoning.modifier.event.ZoningModifierEventType;

/**
 * Fired when an access link segment is removed from an access zone entry of a connectoid through the zoning modifier,
 * the entry staying on the connectoid
 * <p>
 * An entry can be reached over several access link segments, for example a stop served from both directions of a
 * road. A network change can take one of them while the others still reach the stop. A caller that wants to know what
 * was lost listens to this and the other connectoid loss events.
 * </p>
 *
 * @author markr
 */
public class RemoveConnectoidAccessLinkSegmentEvent extends EventImpl implements ZoningModificationEvent {

  /** event type fired when an access link segment is removed from an access zone entry */
  public static final ZoningModifierEventType EVENT_TYPE =
      new ZoningModifierEventType("ZONINGEVENT.REMOVE_CONNECTOID_ACCESS_LINK_SEGMENT");

  /**
   * Constructor
   *
   * @param source zoning modifier firing the event
   * @param connectoid the entry belongs to
   * @param accessZoneEntry the access link segment was removed from
   * @param removedAccessLinkSegment the access link segment removed
   */
  public RemoveConnectoidAccessLinkSegmentEvent(ZoningModifier source, Connectoid connectoid,
      DirectedConnectoidAccessZoneEntry accessZoneEntry, EdgeSegment removedAccessLinkSegment) {
    super(EVENT_TYPE, source, new Object[] { connectoid, accessZoneEntry, removedAccessLinkSegment });
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
   * The entry the access link segment was removed from
   *
   * @return access zone entry
   */
  public DirectedConnectoidAccessZoneEntry getAccessZoneEntry() {
    return (DirectedConnectoidAccessZoneEntry) getContent()[1];
  }

  /**
   * The access link segment removed
   *
   * @return removed access link segment
   */
  public EdgeSegment getRemovedAccessLinkSegment() {
    return (EdgeSegment) getContent()[2];
  }
}
