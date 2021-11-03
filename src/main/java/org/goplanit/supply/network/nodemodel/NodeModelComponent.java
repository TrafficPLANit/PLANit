package org.goplanit.supply.network.nodemodel;

import java.io.Serializable;

import org.goplanit.component.PlanitComponent;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Node model traffic component
 *
 * @author markr
 *
 */
public abstract class NodeModelComponent extends PlanitComponent<NodeModelComponent> implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = -6966680588075724261L;

  /**
   * Base constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected NodeModelComponent(final IdGroupingToken groupId) {
    super(groupId, NodeModelComponent.class);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public NodeModelComponent(final NodeModelComponent other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract NodeModelComponent clone();

}
