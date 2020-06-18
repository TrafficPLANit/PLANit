package org.planit.cost.physical;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.misc.IdGenerator;

/**
 * Class for dynamic cost functions, which calculate link segment costs for each iteration
 *
 * @author gman6028
 *
 */
public abstract class PhysicalCost extends TrafficAssignmentComponent<PhysicalCost> implements AbstractPhysicalCost {

  /** generated UID */
  private static final long serialVersionUID = 3657719270477537657L;

  /**
   * unique identifier for this demand set
   */
  protected final long id;

  protected PhysicalCost() {
    super();
    this.id = IdGenerator.generateId(PhysicalCost.class);
  }

  /**
   * Initialize the cost parameter values in the network
   *
   * @param physicalNetwork the physical network
   * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters
   *           have been set
   */
  public abstract void initialiseBeforeSimulation(PhysicalNetwork physicalNetwork) throws PlanItException;

  /**
   * {@inheritDoc}
   */
  @Override
  public long getId() {
    return this.id;
  }

}
