package org.planit.trafficassignment.builder;

import org.planit.assignment.TrafficAssignment;
import org.planit.demands.Demands;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * Traffic assignment builder factory for the assignment types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class TrafficAssignmentBuilderFactory {

  /** Create a builder for given assignment type
   * @param trafficAssignmentType type of assignment the builder is created for
   * @param projectToken id group this builder is created for
   * @param thePhysicalNetwork  network to register
   * @param theZoning  zoning to register
   * @param theDemands  demands to register
   * @return the created builder
   * @throws PlanItException 
   */
  public static TrafficAssignmentBuilder<?> createBuilder(final String trafficAssignmentType, IdGroupingToken projectToken, Demands theDemands, Zoning theZoning, PhysicalNetwork<?, ?, ?> thePhysicalNetwork) throws PlanItException{
    
    if(trafficAssignmentType.equals(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT)){
      return new TraditionalStaticAssignmentBuilder(projectToken, theDemands, theZoning, thePhysicalNetwork);
    }else {
      throw new PlanItException(String.format("Unable to construct builder for given trafficAssignmentType %s",trafficAssignmentType));
    }
  }
}
