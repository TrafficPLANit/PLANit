package org.goplanit.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.wrapper.MapWrapper;
import org.goplanit.utils.wrapper.MapWrapperImpl;

/**
 * Abstract base class implementation for converter readers which has a mechanism to (optionally) keep track of entities by their source id as PLANit only indexes by internal id
 * 
 * @author markr
 *
 */
public abstract class BaseReaderImpl<T> implements ConverterReader<T> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(BaseReaderImpl.class.getCanonicalName());

  /** track PLANit entities by something else than their internal id via this map */
  protected final Map<Class<?>, MapWrapper<?, ?>> sourceIdTrackerMap;

  /**
   * Stores an object by its source Id, after checking whether the external Id is a duplicate
   * 
   * @param <V> type of object being stored
   * @param obj object being stored by its class signature, assuming ithat is the identifier it is registered under
   * @return true if successful, false otherwise
   * @throws PlanItException thrown if error
   */
  protected <V> void registerBySourceId(final V obj) throws PlanItException {
    registerBySourceId(obj.getClass(), obj);
  }

  /**
   * Stores an object by its source Id, after checking whether the external Id is a duplicate
   * 
   * @param <V>   type of object being stored
   * @param clazz class to identify the correct container
   * @param obj   object being stored
   * @return true if successful, false otherwise
   * @throws PlanItException thrown if dupliate or not possible to register due to lack of initialised container
   */
  protected <U, V> void registerBySourceId(Class<U> theClazz, final V obj) throws PlanItException {

    @SuppressWarnings("unchecked")
    MapWrapper<U, V> mapWrapper = (MapWrapper<U, V>) sourceIdTrackerMap.get(theClazz);
    if (mapWrapper == null) {
      throw new PlanItException("No source id container registered for PLANit entity of type %s, unable to register, perhaps consider registering via its superclass explicitly",
          obj.getClass().getName());
    }
    if (mapWrapper.contains(obj)) {
      throw new PlanItException("PLANit entity of type %s already registered by its source id %s, unable to register", obj.getClass().getName(),
          mapWrapper.getKeyByValue(obj).toString());
    }
    mapWrapper.register(obj);
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
    if (sourceIdTrackerMap.containsKey(clazz)) {
      LOGGER.warning(String.format("Unable to register PLANit entity sourceId tracker for %s, already present", clazz.getName()));
    }
    sourceIdTrackerMap.put(clazz, new MapWrapperImpl<K, V>(new HashMap<K, V>(), valueToKey));
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
    initialiseSourceIdMap(clazz, valueToKey);
    getSourceIdContainer(clazz).addAll(addToSourceIdMap);
  }

  /**
   * access to the container with sourceIds
   * 
   * @param clazz to collect container for
   * @return the source id map wrapper, null if not present
   */
  @SuppressWarnings("unchecked")
  protected <V> MapWrapper<?, V> getSourceIdContainer(Class<V> clazz) {
    return (MapWrapper<?, V>) sourceIdTrackerMap.get(clazz);
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
  @SuppressWarnings("unchecked")
  protected <V, K> V getBySourceId(Class<V> clazz, K key) {
    return ((MapWrapper<K, V>) sourceIdTrackerMap.get(clazz)).get(key);
  }

  /**
   * Constructor
   */
  protected BaseReaderImpl() {
    this.sourceIdTrackerMap = new HashMap<Class<?>, MapWrapper<?, ?>>();
  }
}
