package org.planit.component.event;

import org.planit.utils.event.EventListener;

/**
 * To serve as base listener class for all PLANit component related events, where its onX method provides any graph modification event as parameter
 * 
 * @author markr
 *
 */
public interface PlanitComponentListener extends EventListener {

  /**
   * Notify method for PLANit component events. Note these relate solely to generic events on the component level, so derived classes firing events likely have their own event
   * types and events that are not derived or related to these core PLANit component events used for instantiating and populating PLANit components in a lossely coupled way.
   * 
   * @param event representing the graph modification event at hand
   */
  public abstract void onPlanitComponentEvent(final PlanitComponentEvent event);

}
