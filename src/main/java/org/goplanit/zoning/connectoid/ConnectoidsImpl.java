package org.goplanit.zoning.connectoid;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.goplanit.utils.containers.EntityIndex;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesWithForeignKeyIndicesImpl;
import org.goplanit.utils.zoning.connectoid.Connectoid;
import org.goplanit.utils.zoning.connectoid.Connectoids;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.utils.zoning.modifier.ZoningModifier;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;
import org.goplanit.utils.zoning.modifier.event.ModifiedZoneIdsEvent;

/**
 * Base implementation of Connectoids container and factory class
 * <p>
 * Connectoids join zones to a network that changes after they are made: nodes and link segments are removed or
 * replaced. The container follows those changes for the zoning holding it through its handler, which listens to the
 * modifier of each layer of the networks the zoning is attached to, so its connectoids never refer to a node that is
 * gone, see {@link UpdateConnectoidsOnNetworkChangeHandler}. For example, removing a node removes the connectoids at
 * it. The container indexes its connectoids by their access node for that. Every loss goes through the zoning modifier
 * and is reported once; a move or split is not a loss and changes the connectoids directly.
 * </p>
 *
 * @author markr
 * @param <T> type of connectoid
 */
public abstract class ConnectoidsImpl<T extends Connectoid> extends ManagedIdEntitiesWithForeignKeyIndicesImpl<T>
    implements Connectoids<T> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConnectoidsImpl.class.getCanonicalName());

  /** the connectoids by their access node, by identity */
  private final EntityIndex<DirectedVertex, T> byAccessNode =
      addForeignKeyIndex(connectoid -> Collections.singletonList(connectoid.getReferenceVertex()));

  /** reindexes a connectoid of this container whose access node changed; the same instance is added and removed */
  @SuppressWarnings("unchecked")
  private final Consumer<Connectoid> reindexOnAccessNodeChange = connectoid -> reindex((T) connectoid);

  /** the modifier of the zoning holding this container, through which it makes every change to its connectoids */
  private ZoningModifier zoningModifier;

  /** keeps the connectoids in step with the network, made when the container first listens to a layer modifier */
  private UpdateConnectoidsOnNetworkChangeHandler<T, ?> networkChangeHandler;

  /**
   * Be told when the access node of a connectoid this container holds changes so the index can stay up to date
   *
   * @param connectoid to be told about
   */
  private void listenToAccessNode(T connectoid) {
    ((ConnectoidImpl) connectoid).addAccessNodeChangeListener(reindexOnAccessNodeChange);
  }

  /**
   * Stop being told about a connectoid this container no longer holds
   *
   * @param connectoid to stop being told about, may be null
   */
  private void stopListeningToAccessNode(T connectoid) {
    if (connectoid != null) {
      ((ConnectoidImpl) connectoid).removeAccessNodeChangeListener(reindexOnAccessNodeChange);
    }
  }

  /**
   * update the references to all access zones for all connectoids
   */
  protected void updateConnectoidAccessZoneIdReferences() {
    forEach(Connectoid::recreateAccessZoneIdMapping);
  }

  /**
   * The modifier of the zoning holding this container, through which every change to its connectoids is made
   *
   * @return the zoning modifier
   */
  protected ZoningModifier getZoningModifier() {
    return zoningModifier;
  }

  /**
   * Make the handler that keeps the connectoids of this container in step with the network
   *
   * @return the handler
   */
  protected UpdateConnectoidsOnNetworkChangeHandler<T, ?> createNetworkChangeHandler() {
    return new UpdateConnectoidsOnNetworkChangeHandler<>(this);
  }

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ConnectoidsImpl(final IdGroupingToken groupId) {
    super(Connectoid::getId, Connectoid.CONNECTOID_ID_CLASS);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to use for tracking mapping between original and copied entity (may be null)
   */
  public ConnectoidsImpl(ConnectoidsImpl<T> other, boolean deepCopy, BiConsumer<T, T> mapper) {
    super(other, deepCopy, mapper);
    forEach(this::listenToAccessNode);
  }

  /**
   * Set the modifier of the zoning holding this container, through which it makes every change to its connectoids;
   * set by that modifier when it is made
   *
   * @param zoningModifier of the zoning holding this container
   */
  public void setZoningModifier(ZoningModifier zoningModifier) {
    this.zoningModifier = zoningModifier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T register(final T connectoid) {
    var previous = super.register(connectoid);
    if (previous != connectoid) {
      stopListeningToAccessNode(previous);
      listenToAccessNode(connectoid);
    }
    return previous;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T remove(final T connectoid) {
    var removed = super.remove(connectoid);
    stopListeningToAccessNode(removed);
    return removed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T remove(final long key) {
    var removed = super.remove(key);
    stopListeningToAccessNode(removed);
    return removed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    forEach(this::stopListeningToAccessNode);
    super.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { ModifiedZoneIdsEvent.EVENT_TYPE };
  }

  /**
   * Keep the connectoids in step with the changes made through a layer modifier, through the handler of this container;
   * for the zoning holding this container
   *
   * @param layerModifier to follow
   */
  public void listenTo(MacroscopicNetworkLayerModifier layerModifier) {
    if (networkChangeHandler == null) {
      networkChangeHandler = createNetworkChangeHandler();
    }
    networkChangeHandler.listenTo(layerModifier);
  }

  /**
   * Stop following a layer modifier; for the zoning holding this container
   *
   * @param layerModifier to stop following
   */
  public void stopListeningTo(MacroscopicNetworkLayerModifier layerModifier) {
    if (networkChangeHandler != null) {
      networkChangeHandler.stopListeningTo(layerModifier);
    }
  }

  /**
   * Support event callbacks whenever zones have been modified
   */
  @Override
  public void onZoningModifierEvent(ZoningModificationEvent event) {

    /* update connectoid zone id references when zone ids have changed */
    if (event.getType().equals(ModifiedZoneIdsEvent.EVENT_TYPE)) {
      updateConnectoidAccessZoneIdReferences();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Zone, Set<T>> createIndexByAccessZone() {
    HashMap<Zone,Set<T>> indexByAccessZone = new HashMap<>();
    for(var connectoid : this){
      for(var accessZoneEntry : connectoid){
        var accessZone = accessZoneEntry.getAccessZone();
        indexByAccessZone.putIfAbsent(accessZone,new HashSet<>());
        indexByAccessZone.get(accessZone).add((T) connectoid);
      }
    }
    return indexByAccessZone;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<T> getByAccessNode(DirectedVertex accessNode) {
    return byAccessNode.get(accessNode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ConnectoidsImpl<T> shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ConnectoidsImpl<T> deepClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ConnectoidsImpl<T> deepCloneWithMapping(BiConsumer<T, T> mapper);
}
