package org.planit.network.macroscopic;

import org.planit.utils.mode.Mode;

/**
 * @author markr
 *
 */
public interface InfrastructureLayers {

  /** Remove
   * 
   * @param instance to remove
   */
  public void remove(InfrastructureLayer entity);
  
  /** Remove by id
   * 
   * @param vertexId to remove vertex for
   */  
  public void remove(long id);  

  
  /** Create a new vertex (without registering on this class)
   * 
   * @return created vertex
   */
  public InfrastructureLayer createNew();
  
  /**
   * Add to the container
   *
   * @param entity to be registered in this network
   * @return entity, in case it overrides an existing entry, the removed entry is returned
   */
  public InfrastructureLayer register(InfrastructureLayer entity);  

  /**
   * Create and register new entity
   *
   * @return new layer created
   */
  public InfrastructureLayer registerNew();

  /**
   * Return number of registered entity
   *
   * @return number of registered entity
   */
  public int size();
  
  /** When there are no layers the instance isconsidered empty
   * 
   * @return true when no layers exist yet, false otherwise
   */
  default public boolean isEmpty() {
    return !(size() > 0);
  }

  /**
   * Find a entity by its id
   *
   * @param id Id of entity
   * @return retrieved entity
   */
  public InfrastructureLayer get(final long id);
  
  /**
   * Find the layer by the mode since a mode is only allowed to be supported by a single layer
   *
   * @param mode to find the layer for
   * @return retrieved entity
   */
  public InfrastructureLayer get(Mode mode);  
  
}
