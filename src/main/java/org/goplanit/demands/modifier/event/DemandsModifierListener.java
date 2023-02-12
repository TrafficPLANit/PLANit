package org.goplanit.demands.modifier.event;

import org.goplanit.utils.event.EventListener;

/** To serve as base listener class for all graph modification events, where its onX method provides any
 *  graph modification event as parameter
 *
 * @author markr
 *
 */
public interface DemandsModifierListener extends EventListener {

  /** Notify method for graph modification events
   *
   * @param event representing the graph modification event at hand
   */
  public abstract void onDemandsModificationEvent(DemandsModificationEvent event);

}