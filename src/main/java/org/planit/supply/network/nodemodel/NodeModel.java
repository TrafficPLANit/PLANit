package org.planit.supply.network.nodemodel;

import java.io.Serializable;

import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.misc.IdGenerator;

/**
 * Node model traffic component
 *
 * @author markr
 *
 */
public abstract class NodeModel extends TrafficAssignmentComponent<NodeModel> implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = -6966680588075724261L;

  /**
   * unique identifier
   */
  protected final long id;

  /**
   * Base constructor
   * 
   * @param parent for id generation
   */
  public NodeModel(Object parent) {
    super();
    this.id = IdGenerator.generateId(parent, NodeModel.class);
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public long getId() {
    return this.id;
  }

}
