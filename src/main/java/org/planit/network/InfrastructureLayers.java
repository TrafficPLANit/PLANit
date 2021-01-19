package org.planit.network;

import org.planit.network.macroscopic.physical.MacroscopicPhysicalNetwork;
import org.planit.utils.mode.Mode;

/**
 * interface to manage infrastructure layers. Currently we only support macroscopic infrastructure layers so every instance created through this class will return a
 * {@link MacroscopicPhysicalNetwork}. In future versions the user can choose which type is to be created.
 * 
 * @author markr
 *
 */
public interface InfrastructureLayers extends Iterable<InfrastructureLayer> {

  /**
   * Remove
   * 
   * @param instance to remove
   * @return removed instance (if any), otherwise null
   */
  public InfrastructureLayer remove(final InfrastructureLayer entity);

  /**
   * Remove by id
   * 
   * @param id to remove entity for
   * @return removed instance (if any), otherwise null
   */
  public InfrastructureLayer remove(long id);

  /**
   * Create a new infrastructure layer (without registering on this class). Currently we only support macroscopic infrastructure layers so the returned type is the derived class
   * {@link MacroscopicPhysicalNetwork}. In future versions the user can choose which type is to be created
   * 
   * @return created infrastructure layer
   */
  public MacroscopicPhysicalNetwork createNew();
  
  /**
   * Create a new infrastructure layer and registering on this class. Currently we only support macroscopic infrastructure layers so the returned type is the derived class
   * {@link MacroscopicPhysicalNetwork}. In future versions the user can choose which type is to be created
   * 
   * @return created infrastructure layer
   */
  public MacroscopicPhysicalNetwork registerNew();

  /**
   * Add to the container
   *
   * @param entity to be registered in this network
   * @return entity, in case it overrides an existing entry, the removed entry is returned
   */
  public InfrastructureLayer register(final InfrastructureLayer entity);

  /**
   * Return number of registered entity
   *
   * @return number of registered entity
   */
  public int size();

  /**
   * When there are no layers the instance is considered empty
   * 
   * @return true when no layers exist yet, false otherwise
   */
  default public boolean isNoLayers() {
    return !(size() > 0);
  }

  /**
   * check if each layer itself is empty
   * 
   * @return
   */
  default public boolean isEachLayerEmpty() {
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
   * Find a entity by its id
   *
   * @param id Id of entity
   * @return retrieved entity
   */
  public InfrastructureLayer get(final long id);

  /**
   * Find the layer that supports the passed in mode. Since a mode is only allowed to be supported by a single layer, this should yield the correct result. If multiple layers
   * support the same mode for some reason, this method returns the first layer that supports the mode
   *
   * @param mode to find the layer for
   * @return first matching layer
   */
  public InfrastructureLayer get(final Mode mode);

  /**
   * collect the first layer present based on the iterator
   * 
   * @return first available layer, null if no layers are present
   */
  public default InfrastructureLayer getFirst() {
    if (iterator().hasNext()) {
      return iterator().next();
    }
    return null;
  }

}
