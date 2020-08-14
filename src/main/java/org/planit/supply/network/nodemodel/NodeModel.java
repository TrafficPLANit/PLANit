package org.planit.supply.network.nodemodel;

import java.io.Serializable;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.utils.id.IdGroupingToken;

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
   * Short hand for Tampere node model calss type
   */
  public static final String TAMPERE = TampereNodeModel.class.getCanonicalName();

  /**
   * Base constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public NodeModel(final IdGroupingToken groupId) {
    super(groupId, NodeModel.class);
  }

}
