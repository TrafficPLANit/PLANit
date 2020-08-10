package org.planit.assignment;

import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.network.nodemodel.NodeModel;
import org.planit.trafficassignment.builder.CapacityConstrainedTrafficAssignmentBuilder;
import org.planit.utils.id.IdGroupingToken;

/**
 * Capacity constrained traffic assignment component
 *
 * @author markr
 *
 */
public abstract class CapacityConstrainedAssignment extends TrafficAssignment {

  // PROTECTED

  /** generated UID */
  private static final long serialVersionUID = 2568729148299613377L;

  /**
   * The builder for all capacity constrained traffic assignment instances
   */
  protected CapacityConstrainedTrafficAssignmentBuilder capacityConstrainedBuilder;

  /**
   * Fundamental diagram to use
   */
  protected FundamentalDiagram fundamentalDiagram = null;

  /**
   * Node model to use
   */
  protected NodeModel nodeModel = null;

  // PUBLIC

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected CapacityConstrainedAssignment(IdGroupingToken groupId) {
    super(groupId);
  }

  // Getters - Setters

  /**
   * Set the fundamental diagram
   * 
   * @param fundamentalDiagram the fundamental diagram
   */
  public void setFundamentalDiagram(final FundamentalDiagram fundamentalDiagram) {
    this.fundamentalDiagram = fundamentalDiagram;
  }

  /**
   * The node model to use
   * 
   * @param nodeModel to use
   */
  public void setNodeModel(final NodeModel nodeModel) {
    this.nodeModel = nodeModel;
  }

}
