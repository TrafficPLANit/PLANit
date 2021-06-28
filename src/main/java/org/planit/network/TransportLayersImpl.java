package org.planit.network;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;

/**
 * Base implementation of the TransportLayer interface, without the createNew() method
 * 
 * @author markr
 *
 */
public abstract class TransportLayersImpl<T extends TransportLayer> implements TransportLayers<T> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(TransportLayersImpl.class.getCanonicalName());

  /** track the registered transport layers */
  protected final Map<Long, T> transportLayers = new TreeMap<Long, T>();

  /**
   * create id's for infrastructure layers based on this token
   */
  private final IdGroupingToken idToken;

  /**
   * Collect the token for id generation
   * 
   * @return id token
   */
  protected IdGroupingToken getIdToken() {
    return idToken;
  }

  /**
   * Constructor
   * 
   * @param idToken to generated id's for infrastructure layers
   */
  public TransportLayersImpl(IdGroupingToken idToken) {
    this.idToken = idToken;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T remove(T entity) {

    if (entity == null) {
      LOGGER.warning("cannot remove transport layer, null provided");
      return null;
    }

    return remove(entity.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T remove(long id) {
    return transportLayers.remove(id);
  }

  /**
   * return iterator over the available transport layers
   */
  @Override
  public Iterator<T> iterator() {
    return transportLayers.values().iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T register(T entity) {

    if (entity == null) {
      LOGGER.warning("cannot register transport layer, null provided");
      return null;
    }

    return transportLayers.put(entity.getId(), entity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T registerNew() {
    final T newLayer = createNew();
    register(newLayer);
    return newLayer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return transportLayers.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T get(long id) {
    return transportLayers.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T get(final Mode mode) {
    for (T layer : this) {
      if (layer.supports(mode)) {
        return layer;
      }
    }

    return null;
  }

}
