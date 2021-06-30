package org.planit.network;

import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.TransportLayers;
import org.planit.utils.network.layer.TransportLayer;
import org.planit.utils.wrapper.LongMapWrapperImpl;

/**
 * Base implementation of the TransportLayer interface, without the createNew() method
 * 
 * @author markr
 *
 */
public abstract class TransportLayersImpl<T extends TransportLayer> extends LongMapWrapperImpl<T> implements TransportLayers<T> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(TransportLayersImpl.class.getCanonicalName());

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
    super(new TreeMap<Long, T>(), T::getId);
    this.idToken = idToken;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T registerNew() {
    final T newLayer = createAndRegisterNew();
    register(newLayer);
    return newLayer;
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
