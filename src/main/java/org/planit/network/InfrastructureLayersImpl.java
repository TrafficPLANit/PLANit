package org.planit.network;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;

/**
 * Base implementation of the InfrastructureLayers interface, without the createNew() method
 * 
 * @author markr
 *
 */
public abstract class InfrastructureLayersImpl<T extends InfrastructureLayer> implements InfrastructureLayers<T> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(InfrastructureLayersImpl.class.getCanonicalName());

  /** track the registered infrastructure layers */
  protected final Map<Long, T> infrastructureLayers = new TreeMap<Long, T>();

  /**
   * create id's for infrastructure layers based on this token
   */
  private final IdGroupingToken idToken;
  
  /** collect the token for id generation
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
  public InfrastructureLayersImpl(IdGroupingToken idToken) {
    this.idToken = idToken;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T remove(T entity) {

    if (entity == null) {
      LOGGER.warning("cannot remove infrastructure layer, null provided");
      return null;
    }

    return remove(entity.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T remove(long id) {
    return infrastructureLayers.remove(id);
  }


  /**
   * return iterator over the available infrastructure layers
   */
  @Override
  public Iterator<T> iterator() {
    return infrastructureLayers.values().iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T register(T entity) {

    if (entity == null) {
      LOGGER.warning("cannot register infrastructure layer, null provided");
      return null;
    }

    return infrastructureLayers.put(entity.getId(), entity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T registerNew() {
    final T newInfrastructureLayer = createNew();
    register(newInfrastructureLayer);
    return newInfrastructureLayer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return infrastructureLayers.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T get(long id) {
    return infrastructureLayers.get(id);
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
