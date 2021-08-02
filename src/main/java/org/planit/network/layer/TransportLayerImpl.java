package org.planit.network.layer;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.TransportLayer;

/**
 * Implementation of TransportLayer interface with only barebones functionality to support ids and modes. Only meant as starting point for actual implementations that built on it
 * 
 * @author markr
 *
 */
public abstract class TransportLayerImpl extends ExternalIdAbleImpl implements TransportLayer {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(TransportLayerImpl.class.getCanonicalName());

  /** the modes supported by this layer **/
  protected final Map<Long, Mode> supportedModes;

  /**
   * generate unique node id
   *
   * @param tokenId contiguous id generation within this group for instances of this class
   * @return nodeId
   */
  protected static long generateId(final IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, TransportLayer.TRANSPORT_LAYER_ID_CLASS);
  }

  /**
   * Constructor
   * 
   * @param tokenId to generate id for this instance for
   */
  protected TransportLayerImpl(IdGroupingToken tokenId) {
    super(generateId(tokenId));
    this.supportedModes = new TreeMap<Long, Mode>();
  }

  /**
   * Copy constructor
   * 
   * @param transportLayerImpl to copy
   */
  protected TransportLayerImpl(TransportLayerImpl transportLayerImpl) {
    super(transportLayerImpl);
    this.supportedModes = new TreeMap<Long, Mode>(transportLayerImpl.supportedModes);
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
    if (supportedModes != null && supportedModes.size() > 0) {
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
   * 
   */
  @Override
  public void logInfo(String prefix) {
    /* log supported modes */
    LOGGER.info(String.format("%s#supported modes: %s", prefix, getSupportedModes().stream().map((mode) -> {
      return mode.getXmlId();
    }).collect(Collectors.joining(", "))));
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
  public abstract TransportLayerImpl clone();

}
