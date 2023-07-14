package org.goplanit.demands.modifier;

import org.goplanit.demands.Demands;
import org.goplanit.demands.modifier.event.DemandsModificationEvent;
import org.goplanit.demands.modifier.event.DemandsModifierEventProducer;
import org.goplanit.demands.modifier.event.DemandsModifierListener;
import org.goplanit.demands.modifier.event.RecreatedDemandsEntitiesManagedIdsEvent;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.id.ManagedIdEntities;

import java.util.logging.Logger;

/**
 * Modifier for Demands. To be used for complex changes that affect more than a single aspects of the Demands instance
 *
 */
public class DemandsModifier extends EventProducerImpl implements DemandsModifierEventProducer {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(DemandsModifier.class.getCanonicalName());

  /** parent demands */
  private final Demands demands;

  /**
   * Recreate managed ids and fire #RecreatedDemandsEntitiesManagedIdsEvent event upon completion
   *
   * @param entities to recreate ids for
   * @param <T> type
   */
  private <T extends ManagedIdEntities<? extends ManagedId>> void recreateManagedEntitiesIdsFor(T entities) {
    entities.recreateIds(true);
    fireEvent(new RecreatedDemandsEntitiesManagedIdsEvent(this, entities));
  }

  /**
   * {@inheritDoc}
   * <p>
   *   make public so derived classes can access it as well
   * </p>
   */
  @Override
  protected void fireEvent(EventListener eventListener, Event event) {
    DemandsModifierListener.class.cast(eventListener).onDemandsModificationEvent(DemandsModificationEvent.class.cast(event));
  }

  /**
   * Constructor
   * @param demands parent demands to perform modifications on
   */
  public DemandsModifier(Demands demands){
    this.demands = demands;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateManagedEntitiesIds() {
    recreateManagedEntitiesIdsFor(demands.timePeriods);
    recreateManagedEntitiesIdsFor(demands.travelerTypes);
    recreateManagedEntitiesIdsFor(demands.userClasses);
  }

  /**
   * Reset modifier
   */
  public void reset() {
    super.removeAllListeners();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(GraphModifierListener listener) {
    super.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(GraphModifierListener listener, GraphModifierEventType eventType) {
    super.addListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(GraphModifierListener listener, GraphModifierEventType eventType) {
    super.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(GraphModifierListener listener) {
    super.removeListener(listener);
  }
}
