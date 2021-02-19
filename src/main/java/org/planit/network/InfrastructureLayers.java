package org.planit.network;

import java.util.ArrayList;
import java.util.Collection;

import org.planit.network.macroscopic.physical.MacroscopicPhysicalNetwork;
import org.planit.utils.mode.Mode;

/**
 * interface to manage infrastructure layers. Currently we only support macroscopic infrastructure layers so every instance created through this class will return a
 * {@link MacroscopicPhysicalNetwork}. In future versions the user can choose which type is to be created.
 * 
 * @author markr
 *
 */
public interface InfrastructureLayers<T extends InfrastructureLayer> extends Iterable<T> {

  /**
   * Remove
   * 
   * @param entity to remove
   * @return removed instance (if any), otherwise null
   */
  public abstract T remove(final T entity);

  /**
   * Remove by id
   * 
   * @param id to remove entity for
   * @return removed instance (if any), otherwise null
   */
  public abstract T remove(long id);

  /**
   * Create a new infrastructure layer (without registering on this class). Currently we only support macroscopic infrastructure layers so the returned type is the derived class
   * {@link MacroscopicPhysicalNetwork}. In future versions the user can choose which type is to be created
   * 
   * @return created infrastructure layer
   */
  public abstract T createNew();

  /**
   * Create a new infrastructure layer and registering on this class. Currently we only support macroscopic infrastructure layers so the returned type is the derived class
   * {@link MacroscopicPhysicalNetwork}. In future versions the user can choose which type is to be created
   * 
   * @return created infrastructure layer
   */
  public abstract T registerNew();

  /**
   * Add to the container
   *
   * @param entity to be registered in this network
   * @return entity, in case it overrides an existing entry, the removed entry is returned
   */
  public abstract T register(final T entity);

  /**
   * Return number of registered entity
   *
   * @return number of registered entity
   */
  public abstract int size();
  
  /**
   * Find a entity by its id
   *
   * @param id Id of entity
   * @return retrieved entity
   */
  public abstract T get(final long id);

  /**
   * Find the layer that supports the passed in mode. Since a mode is only allowed to be supported by a single layer, this should yield the correct result. If multiple layers
   * support the same mode for some reason, this method returns the first layer that supports the mode
   *
   * @param mode to find the layer for
   * @return first matching layer
   */
  public abstract T get(final Mode mode);  

  /**
   * When there are no layers the instance is considered empty
   * 
   * @return true when no layers exist yet, false otherwise
   */
  public default boolean isNoLayers() {
    return !(size() > 0);
  }

  /**
   * check if each layer itself is empty
   * 
   * @return true when all empty false otherwise
   */
  public default boolean isEachLayerEmpty() {
    boolean eachLayerEmpty = true;
    for (InfrastructureLayer layer : this) {
      if (!layer.isEmpty()) {
        eachLayerEmpty = false;
        break;
      }
    }
    return eachLayerEmpty;
  }

  /**
   * collect the first layer present based on the iterator
   * 
   * @return first available layer, null if no layers are present
   */
  public default T getFirst() {
    if (iterator().hasNext()) {
      return iterator().next();
    }
    return null;
  }
  
  /** allows you to collect all registered layers of a specific derived infrastructure layer type
   * @param <U> derived type of type T
   * @return list of layers of desired type, empty list when none exist
   */
  @SuppressWarnings("unchecked")
  public default <U extends InfrastructureLayer> Collection<U> getLayersOfType(){
    ArrayList<U> layerList = new ArrayList<U>();
    for (T layer : this) {
      try {
        U castLayer = (U) layer;
        layerList.add(castLayer);
      } catch (ClassCastException e) {
        /* wrong type ignore */
      }
    }
    return layerList;
  }  

}
