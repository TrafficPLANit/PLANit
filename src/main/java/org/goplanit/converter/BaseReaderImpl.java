package org.goplanit.converter;

import org.goplanit.utils.misc.CustomIndexTracker;
import org.goplanit.utils.wrapper.MapWrapper;

import java.util.function.Function;

/**
 * Abstract base class implementation for converter readers which has a mechanism to (optionally) keep track of entities by their source id as PLANit only indexes by internal id
 * 
 * @author markr
 *
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
   * register a new source id tracker (empty) where a function is used to extract the source id from the entity and the class is used unique identifier for the underlying tracking
   * container
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
   * register a new source id tracker (empty) where a function is used to extract the source id from the entity and the class is used unique identifier for the underlying tracking
   * container
   *
   * @param <K>              key type used
   * @param <V>              value type used
   * @param clazz            identifier in container of containers
   * @param valueToKey       function mapping value to key
   * @param addToSourceIdMap add all entities in iterable to the newly created source id map upon creation
   */
  protected <K, V> void initialiseSourceIdMap(Class<V> clazz, final Function<V, K> valueToKey, Iterable<V> addToSourceIdMap) {
    sourceIdTracker.initialiseEntityContainer(clazz, valueToKey, addToSourceIdMap);
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
