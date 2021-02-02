package org.planit.assignment;

import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.InfrastructureNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.zoning.Zoning;

/**
 * 
 * builder for capacity constrained traffic assignment
 * 
 * @author markr
 *
 * @param <T> capacity constrained assignment type
 */
public abstract class CapacityConstrainedTrafficAssignmentBuilder<T extends CapacityConstrainedAssignment> extends TrafficAssignmentBuilder<T> {

  /**
   * Constructor
   * 
   * @param capacityconstrainedAssignmentClass assignment class
   * @param tokenId                            for id generation
   * @param inputBuilderListener               input builder
   * @param demands                            to use
   * @param zoning                             to use
   * @param network                            to use
   * @throws PlanItException thrown if error
   */
  protected CapacityConstrainedTrafficAssignmentBuilder(Class<T> capacityconstrainedAssignmentClass, IdGroupingToken tokenId, InputBuilderListener inputBuilderListener,
      Demands demands, Zoning zoning, InfrastructureNetwork network) throws PlanItException {
    super(capacityconstrainedAssignmentClass, tokenId, inputBuilderListener, demands, zoning, network);
  }

}
