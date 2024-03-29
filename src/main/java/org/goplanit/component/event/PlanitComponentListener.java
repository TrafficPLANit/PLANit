package org.goplanit.component.event;

import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.exceptions.PlanItException;

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
   * @throws PlanItException thrown if error during processing of event
   */
  public abstract void onPlanitComponentEvent(final PlanitComponentEvent event) throws PlanItException;

}
