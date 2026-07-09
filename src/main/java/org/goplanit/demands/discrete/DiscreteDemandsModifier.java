package org.goplanit.demands.discrete;

import org.goplanit.demands.event.discrete.*;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.id.ManagedIdEntities;

import java.util.logging.Logger;

/**
 * Modifier for DiscreteDemands. To be used for complex changes that affect more than a single aspects of the
 * DiscreteDemands instance
 *
 */
public class DiscreteDemandsModifier extends EventProducerImpl implements DiscreteDemandsModifierEventProducer {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(DiscreteDemandsModifier.class.getCanonicalName());

  /** parent discreteDemands */
  private final DiscreteDemands discreteDemands;

  /**
   * Recreate managed ids and fire #RecreatedDiscreteDemandsEntitiesManagedIdsEvent event upon completion
   *
   * @param entities to recreate ids for
   * @param <T> type
   */
  private <T extends ManagedIdEntities<? extends ManagedId>> void recreateManagedEntitiesIdsFor(T entities) {
    entities.recreateIds(true);
    fireEvent(new RecreatedDiscreteDemandsEntitiesManagedIdsEvent(this, entities));
  }

  /**
   * {@inheritDoc}
   * <p>
   *   make public so derived classes can access it as well
   * </p>
   */
  @Override
  protected void fireEvent(EventListener eventListener, Event event) {
    ((DiscreteDemandsModifierListener) eventListener).onDiscreteDemandsModificationEvent(
        (DiscreteDemandsModificationEvent) event);
  }

  /**
   * Constructor
   * @param discreteDemands parent demands to perform modifications on
   */
  public DiscreteDemandsModifier(DiscreteDemands discreteDemands){
    this.discreteDemands = discreteDemands;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateManagedEntitiesIds() {
    recreateManagedEntitiesIdsFor(discreteDemands.getHouseholds());
    recreateManagedEntitiesIdsFor(discreteDemands.getPersons());
    recreateManagedEntitiesIdsFor(discreteDemands.getTours());
    recreateManagedEntitiesIdsFor(discreteDemands.getTrips());
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
  public void addListener(DiscreteDemandsModifierListener listener) {
    super.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(DiscreteDemandsModifierListener listener, DiscreteDemandsModifierEventType eventType) {
    super.addListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(DiscreteDemandsModifierListener listener, DiscreteDemandsModifierEventType eventType) {
    super.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(DiscreteDemandsModifierListener listener) {
    super.removeListener(listener);
  }
}
