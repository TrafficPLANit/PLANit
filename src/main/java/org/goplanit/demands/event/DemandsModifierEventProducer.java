package org.goplanit.demands.event;

import org.goplanit.utils.event.EventProducer;
import java.util.Set;

/** Interface dedicated to the {@link DemandsModifierListener} event related exposed methods.
 * Specifies method signatures  for registering
 * the appropriate listeners and event types that are supported on a demands modifier.
 *
 * @author markr
 *
 */
public interface DemandsModifierEventProducer extends EventProducer{

  /**
   * The event types a demands modifier fires: {@link RecreatedDemandsEntitiesManagedIdsEvent}
   */
  public static final Set<DemandsModifierEventType> FIRED_DEMANDS_EVENT_TYPES =
      Set.of(RecreatedDemandsEntitiesManagedIdsEvent.EVENT_TYPE);

  /**
   * The event types of this kind the producer fires, see {@link #FIRED_DEMANDS_EVENT_TYPES}
   *
   * @return unmodifiable set of the event types fired
   */
  public default Set<DemandsModifierEventType> getFiredDemandsEventTypes() {
    return FIRED_DEMANDS_EVENT_TYPES;
  }

  /**
   * Recreate managed ids of all underlying managed entities id containers and fire a
   * #RecreatedDemandsEntitiesManagedIdsEvent for each container that was updated
   */
  public abstract void recreateManagedEntitiesIds();

  /**
   * Register listener for all its supported types fired by the modifier
   *
   *  @param listener to register
   */
  public abstract void addListener(DemandsModifierListener listener);

  /**
   * Register listeners for events fired by the graph modifier
   *
   *  @param listener to register
   *  @param eventType to register listener for
   */
  public abstract void addListener(DemandsModifierListener listener, DemandsModifierEventType eventType);

  /**
   * Remove listener for given event type
   *
   *  @param listener to remove
   *  @param eventType to unregister listener for
   */
  public abstract void removeListener(DemandsModifierListener listener, DemandsModifierEventType eventType);

  /**
   * Remove listener for all event types it is registered for
   *
   *  @param listener to remove
   */
  public abstract void removeListener(DemandsModifierListener listener);

  /**
   * Remove all registered listeners, apart from those internal to the owner of the producer
   */
  public abstract void removeAllNonInternalListeners();

}
