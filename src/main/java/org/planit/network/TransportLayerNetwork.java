package org.planit.network;

import java.util.logging.Logger;

import org.planit.mode.ModesImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.Modes;
import org.planit.utils.network.layer.TransportLayer;
import org.planit.utils.network.layers.TransportLayers;

/**
 * A transport network with one or more layers. One can choose the container for the different layers as a generic type that defines the container level operations available. Each
 * container has a certain base class for the TransportLayer entities which is the second generic type. This allows one to have a base class for each layer, while the layer itself
 * can derive from this base level. This way the user has maximum flexibility regarding what the functionality and properties of each layer are and how they are exposed via the
 * container
 * 
 * @author markr
 *
 * @param <U> transport layer base class
 * @param <T> transport layer container class where each layer extends {@code <U>}
 */
public abstract class TransportLayerNetwork<U extends TransportLayer, T extends TransportLayers<U>> extends Network {

  /** generated serial id */
  private static final long serialVersionUID = 2402806336978560448L;

  /** the logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(TransportLayerNetwork.class.getCanonicalName());

  /**
   * class instance containing all modes specific functionality across the layers
   */
  private final Modes modes;

  /** stores the various layers grouped by their supported modes of transport */
  private T transportLayers;

  // Protected

  /**
   * Derived type is to provide the actual layer implementations
   * 
   * @param networkIdToken to use
   * @return infrastructure layers container
   */
  protected abstract T createLayersContainer(IdGroupingToken networkIdToken);

  /**
   * Default constructor
   * 
   * @param tokenId to use for id generation
   */
  public TransportLayerNetwork(IdGroupingToken tokenId) {
    super(tokenId);

    /* for mode management */
    this.modes = new ModesImpl(tokenId);

    /* until accessed remaines null */
    this.transportLayers = null;

  }

  /**
   * collect a layer by mode
   * 
   * @param mode to collect layer for
   * @return corresponding layer, (null if not found)
   */
  public U getLayerByMode(Mode mode) {
    return transportLayers.get(mode);
  }

  /**
   * Collect the modes
   * 
   * @return modes container
   */
  public Modes getModes() {
    return modes;
  }

  /**
   * Collect the transport layers
   * 
   * @return transport layers container
   */
  public T getTransportLayers() {
    if (transportLayers == null) {
      /* delegate to let implementing class generate the correct instance for layer management */
      this.transportLayers = createLayersContainer(getNetworkGroupingTokenId());
    }
    return transportLayers;
  }

}