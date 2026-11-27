package org.goplanit.converter;

import org.goplanit.utils.misc.CharacterUtils;
import org.goplanit.utils.misc.CustomIndexTracker;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.wrapper.MapWrapper;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.zoning.Zoning;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Abstract base class implementation for converter readers which has a mechanism to (optionally) keep track of
 * entities by their source id as PLANit only indexes by internal id
 * 
 * @author markr
 * @param <T> type of top level entity reader
 */
public abstract class BaseReaderImpl<T> implements ConverterReader<T> {

  /** track PLANit entities by something else than their internal id via this map */
  protected final CustomIndexTracker sourceIdTracker;

  /**
   * Stores an object by its source Id, after checking whether the external Id is a duplicate
   *
   * @param <V> type of object being stored
   * @param obj object being stored by its class signature, assuming it hat is the identifier it is registered under
   */
  protected <V> void registerBySourceId(final V obj) {
    registerBySourceId(obj.getClass(), obj);
  }

  /**
   * Stores an object by its source Id, after checking whether the external Id is a duplicate
   *
   * @param <U>      type of object being stored
   * @param <V>      value to store
   * @param theClazz class to identify the correct container
   * @param obj      object being stored
   */
  protected <U, V> void registerBySourceId(Class<U> theClazz, final V obj) {
    sourceIdTracker.register(theClazz, obj);
  }

  /**
   * register a new source id tracker (empty) where a function is used to extract the source id from the entity and
   * the class is used unique identifier for the underlying tracking container
   *
   * @param <K>        key type used
   * @param <V>        value type used
   * @param clazz      identifier in container of containers
   * @param valueToKey function mapping value to key
   */
  protected <K, V> void initialiseSourceIdMap(Class<V> clazz, final Function<V, K> valueToKey) {
    sourceIdTracker.initialiseEntityContainer(clazz, valueToKey);
  }

  /**
   * register a new source id tracker (empty) where a function is used to extract the source id from the entity
   * and the class is used unique identifier for the underlying tracking container
   *
   * @param <K>              key type used
   * @param <V>              value type used
   * @param clazz            identifier in container of containers
   * @param valueToKey       function mapping value to key
   * @param addToSourceIdMap add all entities in iterable to the newly created source id map upon creation
   */
  protected <K, V> void initialiseSourceIdMap(
      Class<V> clazz, final Function<V, K> valueToKey, Iterable<? extends V> addToSourceIdMap) {
    sourceIdTracker.initialiseEntityContainer(clazz, valueToKey, addToSourceIdMap);
  }

  /**
   * Register a new source id tracker holding the entities of each of several containers, such as the link segments of
   * every layer of a network a reader refers to, so a reference by source id finds an entity whichever layer holds it
   *
   * @param <K>        key type used
   * @param <V>        value type used
   * @param <C>        type of the containers, such as a network layer
   * @param clazz      identifier in container of containers
   * @param valueToKey function mapping value to key, such as its XML id
   * @param containers whose entities to add, such as a network's layers
   * @param entitiesOf the entities of one container to add, such as a layer's link segments
   */
  protected <K, V, C> void initialiseSourceIdMap(
      Class<V> clazz,
      final Function<V, K> valueToKey,
      Iterable<? extends C> containers,
      Function<? super C, ? extends Iterable<? extends V>> entitiesOf) {
    sourceIdTracker.initialiseEntityContainer(clazz, valueToKey);
    containers.forEach(container -> getSourceIdContainer(clazz).addAll(entitiesOf.apply(container)));
  }

  /**
   * Register the source id trackers of the zones of a zoning a reader refers to, its OD zones and its transfer zones,
   * each by its XML id, such as for demands naming their origins and destinations
   *
   * @param zoning whose zones to track
   */
  protected void initialiseZoneSourceIdMaps(Zoning zoning) {
    initialiseSourceIdMap(OdZone.getOdZoneIdClass(), Zone::getXmlId, zoning.getOdZones());
    initialiseSourceIdMap(TransferZone.getTransferZoneIdClass(), Zone::getXmlId, zoning.getTransferZones());
  }

  /**
   * access to the container with sourceIds
   *
   * @param <V>   value of the container
   * @param clazz to collect container for
   * @return the source id map wrapper, null if not present
   */
  @SuppressWarnings("unchecked")
  protected <V> MapWrapper<?, V> getSourceIdContainer(Class<V> clazz) {
    return sourceIdTracker.getEntityContainer(clazz);
  }

  /**
   * Get an entry by its source id
   *
   * @param <V>   return type
   * @param <K>   key to find it by
   * @param clazz class identifier for selecting the correct container tracker
   * @param key   the actual key to use
   * @return value found, null if not present
   */
  protected <V, K> V getBySourceId(Class<V> clazz, K key) {
    return sourceIdTracker.get(clazz, key);
  }

  /**
   * Find the entities a comma-separated list of source ids refers to, telling the caller of each id that is unknown, so
   * it can skip it, or the whole list, as it needs
   *
   * @param <T>        type of entity
   * @param type       of entity, the source id map to look in
   * @param refs       to parse, may be null or blank
   * @param unknownRef told each id that is unknown
   * @return the entities found, in the order listed, empty when none are listed
   */
  protected <T> List<T> findBySourceIds(Class<T> type, String refs, Consumer<String> unknownRef) {
    if (StringUtils.isNullOrBlank(refs)) {
      return List.of();
    }
    var found = new ArrayList<T>();
    for (var ref : refs.split(CharacterUtils.COMMA.toString())) {
      var entity = getBySourceId(type, ref.trim());
      if (entity == null) {
        unknownRef.accept(ref.trim());
      } else {
        found.add(entity);
      }
    }
    return found;
  }

  /**
   * Provides a reference to internal overall tracker. Only to be used if tracking has to happen outside of derived
   * reader and access is otherwise cumbersome. Use with caution.
   *
   * @return internal tracker instance
   */
  protected CustomIndexTracker getSourceIdTrackerRaw(){
    return sourceIdTracker;
  }

  /**
   * Constructor
   */
  protected BaseReaderImpl() {
    this.sourceIdTracker = new CustomIndexTracker();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    sourceIdTracker.reset();
  }
}
