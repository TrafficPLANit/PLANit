package org.goplanit.demands.event;

import org.goplanit.utils.event.EventListener;

/** To serve as base listener class for all demands modification events, where its onX method provides any
 *  demands modification event as parameter
 *
 * @author markr
 *
 */
public interface DemandsModifierListener extends EventListener {

  /** Notify method for demands modification events
   *
   * @param event representing the demand modification event at hand
   */
  public abstract void onDemandsModificationEvent(DemandsModificationEvent event);

}