package org.goplanit.supply.network.nodemodel;

import java.util.Map;

import org.goplanit.utils.id.IdGroupingToken;

/**
 * Tampere node model traffic component signalling that the Tampere node model algorithm is to be used
 * 
 *
 * @author markr
 *
 */
public class TampereNodeModelComponent extends NodeModelComponent {

  /** generated UID */
  private static final long serialVersionUID = 624108273657030487L;

  /**
   * Base Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public TampereNodeModelComponent(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public TampereNodeModelComponent(final TampereNodeModelComponent other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TampereNodeModelComponent clone() {
    return new TampereNodeModelComponent(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // nothing to reset
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return null;
  }

}
