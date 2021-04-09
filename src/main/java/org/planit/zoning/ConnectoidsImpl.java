package org.planit.zoning;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.Connectoids;

/**
 * Base implementation of Connectoids container and factory class
 * 
 * @author markr
 *
 */
public abstract class ConnectoidsImpl<T extends Connectoid> implements Connectoids<T> {

  /**
   * connectoids container
   */
  protected Map<Long, T> connectoidMap = new TreeMap<Long, T>();
  
  /**
   * recreate the mapping such that all the keys used for each connectoid reflect their internal id.
   * To be called whenever the ids of connectoids are changed
   */
  protected void updateIdMapping() {
    Map<Long, T> updatedMap = new HashMap<Long, T>(connectoidMap.size());
    connectoidMap.forEach((oldId, connectoid) -> updatedMap.put(connectoid.getId(), connectoid));
    connectoidMap.clear();
    connectoidMap = updatedMap;
  }  

  /**
   * Register on container
   * 
   * @param idToUse    to use
   * @param connectoid to register
   * @return result of put on map
   */
  protected T register(long idToUse, T connectoid) {
    return connectoidMap.put(idToUse, connectoid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connectoid remove(T connectoid) {
    return remove(connectoid.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connectoid remove(long connectoidId) {
    return connectoidMap.remove(connectoidId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<T> iterator() {
    return connectoidMap.values().iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T get(long id) {
    return connectoidMap.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return connectoidMap.size();
  }

}
