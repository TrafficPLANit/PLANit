package org.goplanit.network.layers;

import java.util.logging.Logger;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.TransportLayer;
import org.goplanit.utils.network.layers.TransportLayers;

/**
 * Base implementation of the TransportLayer interface, without the createNew() method
 * 
 * @author markr
 *
 */
public abstract class TransportLayersImpl<T extends TransportLayer> extends ManagedIdEntitiesImpl<T> implements TransportLayers<T> {

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
    super(T::getId, TransportLayer.TRANSPORT_LAYER_ID_CLASS);
    this.idToken = idToken;
  }

  /**
   * Constructor
   * 
   * @param other to copy
   */
  public TransportLayersImpl(TransportLayersImpl<T> other) {
    super(other);
    this.idToken = other.idToken;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T get(final Mode mode) {
    return findFirst(layer -> layer.supports(mode));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getByXmlId(String xmlId) {
    return findFirst(layer -> layer.getXmlId().equals(xmlId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TransportLayersImpl<T> clone();

}
