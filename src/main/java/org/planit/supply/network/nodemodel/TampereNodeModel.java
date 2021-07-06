package org.planit.supply.network.nodemodel;

import org.planit.utils.id.IdGroupingToken;

/**
 * Tampere node model traffic component
 *
 * @author markr
 *
 */
public class TampereNodeModel extends NodeModel {

  /** generated UID */
  private static final long serialVersionUID = 624108273657030487L;

  /**
   * Base Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public TampereNodeModel(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public TampereNodeModel(final TampereNodeModel other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TampereNodeModel clone() {
    return new TampereNodeModel(this);
  }

}
