package org.planit.network;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;

/**
 * Implementation of infrastructure layer interface with only barebones functionality to support ids and modes. Only meant as starting point for actual implementations
 * 
 * @author markr
 *
 */
public abstract class InfrastructureLayerImpl implements InfrastructureLayer {

  /** unique id for this infrastructure layer */
  protected long id;

  /** xml id for this infrastructure layer */
  protected String xmlId;

  /** external id for this infrastructure layer (if any) */
  protected String externalId;

  /** the modes supported by this layer **/
  protected final Map<Long, Mode> supportedModes = new TreeMap<Long, Mode>();

  /**
   * generate unique node id
   *
   * @param tokenId contiguous id generation within this group for instances of this class
   * @return nodeId
   */
  protected static long generateId(final IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, InfrastructureLayerImpl.class);
  }

  /**
   * Set id on vertex
   * 
   * @param id to set
   */
  protected void setId(Long id) {
    this.id = id;
  }

  /**
   * Constructor
   * 
   * @param tokenId to generate id for this instance for
   */
  public InfrastructureLayerImpl(IdGroupingToken tokenId) {
    setId(generateId(tokenId));

    this.xmlId = null;
    this.externalId = null;
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
  public long getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public String getExternalId() {
    return externalId;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public String getXmlId() {
    return xmlId;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void setXmlId(String xmlId) {
    this.xmlId = xmlId;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public final Collection<Mode> getSupportedModes() {
    return supportedModes.values();
  }

}
