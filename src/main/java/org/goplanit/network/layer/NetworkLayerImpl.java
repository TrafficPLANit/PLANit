package org.goplanit.network.layer;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedMode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.NetworkLayer;

/**
 * Implementation of NetworkLayer interface with only bare-bones functionality to support ids and modes. Only meant as starting point for actual implementations that built on it
 * 
 * @author markr
 *
 */
public abstract class NetworkLayerImpl extends ExternalIdAbleImpl implements NetworkLayer {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(NetworkLayerImpl.class.getCanonicalName());

  /** the modes supported by this layer (not owned) **/
  protected final Map<Long, Mode> supportedModes;

  /**
   * generate unique node id
   *
   * @param tokenId contiguous id generation within this group for instances of this class
   * @return nodeId
   */
  protected static long generateId(final IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, NetworkLayer.NETWORK_LAYER_ID_CLASS);
  }

  /**
   * Constructor
   * 
   * @param tokenId to generate id for this instance for
   */
  protected NetworkLayerImpl(IdGroupingToken tokenId) {
    super(generateId(tokenId));
    this.supportedModes = new TreeMap<>();
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected NetworkLayerImpl(NetworkLayerImpl other, boolean deepCopy /* no impact yet */) {
    super(other);
    this.supportedModes = new TreeMap<>(other.supportedModes);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public boolean registerSupportedMode(Mode supportedMode) {
    if (supportedMode != null) {
      supportedModes.put(supportedMode.getId(), supportedMode);
      return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public boolean registerSupportedModes(Collection<Mode> supportedModes) {
    boolean success = false;
    if (supportedModes != null && !supportedModes.isEmpty()) {
      success = true;
      for (Mode mode : supportedModes) {
        if (!registerSupportedMode(mode)) {
          success = false;
        }
      }
    }
    return success;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public Collection<Mode> getSupportedModes() {
    return supportedModes.values();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean supportsPredefinedMode(PredefinedModeType predefinedModeType) {
    return supportedModes.values().stream().anyMatch(m -> m.isPredefinedModeType() && ((PredefinedMode)m).getPredefinedModeType() == predefinedModeType);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void logInfo(String prefix) {
    /* log supported modes */
    LOGGER.info(String.format("%ssupported modes: %s", prefix, getSupportedModes().stream().map(ExternalIdAble::getXmlId).collect(Collectors.joining(", "))));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    this.supportedModes.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract NetworkLayerImpl shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract NetworkLayerImpl deepClone();

}
