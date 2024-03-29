package org.goplanit.zoning.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.zoning.modifier.ZoningModifier;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;
import org.goplanit.utils.zoning.modifier.event.ZoningModifierEventType;
import org.goplanit.zoning.Zoning;

/**
 * Wrapper for all events fired with zoning as a source
 * 
 * 
 * @author markr
 *
 */
public class ModifiedZoneIdsEvent extends EventImpl implements ZoningModificationEvent {

  /** event type fired off when zone ids have been modified */
  public static final ZoningModifierEventType EVENT_TYPE = new ZoningModifierEventType("ZONINGEVENT.MODIFIED_ZONE_IDS");

  /**
   * Wrapper for a modified zoning ids event indicating that one or more managed internal ids of the zoning have been changed
   * 
   * @param source zoning modifier firing the event
   * @param zoning to use
   */
  public ModifiedZoneIdsEvent(ZoningModifier source, Zoning zoning) {
    super(EVENT_TYPE, source, zoning);
  }

  /**
   * The zoning on which the modification took place
   * 
   * @return modified zoning
   */
  public Zoning getModifiedZoning() {
    return (Zoning) getContent()[0];
  }

}
