package org.goplanit.demands.event.discrete;

import org.goplanit.demands.event.DemandsModificationEvent;
import org.goplanit.utils.event.EventListener;

/** To serve as base listener class for all discrete demands modification events, where its onX method provides any
 *  demands modification event as parameter
 *
 * @author markr
 *
 */
public interface DiscreteDemandsModifierListener extends EventListener {

  /** Notify method for discrete demands modification events
   *
   * @param event representing the discrete demand modification event at hand
   */
  public abstract void onDiscreteDemandsModificationEvent(DiscreteDemandsModificationEvent event);

}