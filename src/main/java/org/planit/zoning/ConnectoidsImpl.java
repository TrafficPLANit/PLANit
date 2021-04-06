package org.planit.zoning;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.Connectoids;

/**
 * Base implementation of Connectoids container and factory class
 * 
 * @author markr
 *
 */
public abstract class ConnectoidsImpl<T extends Connectoid> implements Connectoids<T> {

  /** id generation token */
  protected IdGroupingToken idToken;

  /**
   * connectoids container
   */
  protected Map<Long, T> connectoidMap = new TreeMap<Long, T>();

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
   * Constructor
   * 
   * @param idToken to use
   */
  public ConnectoidsImpl(IdGroupingToken idToken) {
    this.idToken = idToken;
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
