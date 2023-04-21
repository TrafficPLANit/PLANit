package org.goplanit.network;

import java.util.logging.Logger;

import org.goplanit.mode.ModesImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.Modes;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layers.NetworkLayers;

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
public abstract class LayeredNetwork<U extends NetworkLayer, T extends NetworkLayers<U>> extends Network {

  /** generated serial id */
  private static final long serialVersionUID = 2402806336978560448L;

  /** the logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(LayeredNetwork.class.getCanonicalName());

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
  public LayeredNetwork(IdGroupingToken tokenId) {
    super(tokenId);

    /* for mode management */
    this.modes = new ModesImpl(tokenId);

    /* until accessed remains null */
    this.transportLayers = null;
  }

  /**
   * Copy constructor.
   *
   * @param other                   to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param modeMapper to use for tracking mapping between original and copied modes
   * @param layerMapper to use for tracking mapping between original and copied layers
   */
  protected LayeredNetwork(final LayeredNetwork<U, T> other, boolean deepCopy, ManagedIdDeepCopyMapper<Mode> modeMapper, ManagedIdDeepCopyMapper<U> layerMapper) {
    super(other, deepCopy);

    // both are container wrappers, so requiring cloning also for shallow copy
    if(deepCopy){
      this.modes = other.modes.deepCloneWithMapping(modeMapper);
      this.transportLayers = (T) other.getTransportLayers().deepCloneWithMapping(layerMapper);
    }else{
      this.modes = other.modes.shallowClone();
      this.transportLayers = (T) other.getTransportLayers().shallowClone();
    }
    this.transportLayers = (T) (deepCopy ? other.getTransportLayers().deepClone() : other.getTransportLayers().shallowClone());
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
   * collect a layer by predefined mode type (we exclude custom mode types)
   *
   * @param predefinedModeType to collect layer for
   * @return corresponding layer, (null if not found)
   */
  public U getLayerByPredefinedModeType(PredefinedModeType predefinedModeType) {
    return transportLayers.get(predefinedModeType);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
    modes.reset();
    transportLayers.reset();
  }

  /**
   * Empty when all layers in the layers container are empty
   * @return true when empty, false otherwise
   */
  @Override
  public boolean isEmpty() {
    return getTransportLayers().isEachLayerEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract LayeredNetwork shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract LayeredNetwork deepClone();

}
