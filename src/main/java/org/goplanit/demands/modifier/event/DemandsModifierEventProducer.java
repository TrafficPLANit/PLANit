package org.goplanit.demands.modifier.event;

import org.goplanit.utils.event.EventProducer;
import org.goplanit.utils.graph.modifier.GraphModifier;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;

/** Interface dedicated to the {@link GraphModifier} event related exposed methods. Specifies method signatures for registering
 * the appropriate listeners and event types that are supported on a demands modifier.
 *
 * @author markr
 *
 */
public interface DemandsModifierEventProducer extends EventProducer{

  /**
   * Recreate managed ids of all underlying managed entities id containers and fire a #RecreatedDemandsEntitiesManagedIdsEvent
   * for each container that was updated
   */
  public abstract void recreateManagedEntitiesIds();

  /**
   * Register listener for all its supported types fired by the graph modifier
   *
   *  @param listener to register
   */
  public abstract void addListener(GraphModifierListener listener);

  /**
   * Register listeners for events fired by the graph modifier
   *
   *  @param listener to register
   *  @param eventType to register listener for
   */
  public abstract void addListener(GraphModifierListener listener, GraphModifierEventType eventType);

  /**
   * Remove listener for given event type
   *
   *  @param listener to remove
   *  @param eventType to unregister listener for
   */
  public abstract void removeListener(GraphModifierListener listener, GraphModifierEventType eventType);

  /**
   * Remove listener for all event types it is registered for
   *
   *  @param listener to remove
   */
  public abstract void removeListener(GraphModifierListener listener);

  /**
   * Remove all registered listeners
   */
  public abstract void removeAllListeners();

}
