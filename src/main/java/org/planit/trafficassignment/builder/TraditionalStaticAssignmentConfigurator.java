package org.planit.trafficassignment.builder;

import org.planit.assignment.traditionalstatic.TraditionalStaticAssignment;

/**
 * Configurator for traditional static assignment
 * 
 * @author markr
 *
 */
public class TraditionalStaticAssignmentConfigurator extends TrafficAssignmentConfigurator<TraditionalStaticAssignment> {

  /**
   * Constructor 
   * 
   * @param instanceType the type we are configuring for
   */
  public TraditionalStaticAssignmentConfigurator(Class<TraditionalStaticAssignment> instanceType) {
    super(instanceType);
  }

}
