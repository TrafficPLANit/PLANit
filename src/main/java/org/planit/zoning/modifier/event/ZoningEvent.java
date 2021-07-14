package org.planit.zoning.modifier.event;

import java.io.Serializable;

import org.djutils.event.EventType;
import org.planit.zoning.Zoning;

/**
 * Wrapper for all events fired with zoning as a source
 * 
 * 
 * @author markr
 *
 */
public class ZoningEvent extends org.djutils.event.Event {

  /** event type fired off when zone ids have been modified */
  public static final EventType MODIFIED_ZONE_IDS = new EventType("ZONINGEVENT.MODIFIED_ZONE_IDS");

  /**
   * generated UID
   */
  private static final long serialVersionUID = 3492000377880154764L;

  /**
   * Wrapper for all events with a zonign source
   * 
   * @param zoningSource firing the event
   * @param type         event type
   * @param content      content of the event
   */
  public ZoningEvent(Zoning zoningSource, EventType type, Serializable content) {
    super(type, zoningSource.getId(), content);
  }

  /**
   * Wrapper for all events with a zoning source without additional content
   * 
   * @param zoningSource firing the event
   * @param type         event type
   */
  public ZoningEvent(Zoning zoningSource, EventType type) {
    this(zoningSource, type, null);
  }

  /**
   * Collect source id as zoning id
   */
  public long getZoningId() {
    return (Long) super.getSourceId();
  }

}
