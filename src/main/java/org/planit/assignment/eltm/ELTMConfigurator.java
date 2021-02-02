package org.planit.assignment.eltm;

import org.planit.assignment.DynamicAssignmentConfigurator;
import org.planit.utils.exceptions.PlanItException;

/**
 * Configurator for eLTM
 * 
 * @author markr
 *
 */
public class ELTMConfigurator extends DynamicAssignmentConfigurator<ELTM> {

  /**
   * Constructor
   * 
   * @throws PlanItException thrown when error
   */
  public ELTMConfigurator() throws PlanItException {
    super(ELTM.class);
  }

}
