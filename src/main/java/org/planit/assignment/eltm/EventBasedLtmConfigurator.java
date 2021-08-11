package org.planit.assignment.eltm;

import org.planit.assignment.DynamicAssignmentConfigurator;
import org.planit.utils.exceptions.PlanItException;

/**
 * Configurator for eLTM
 * 
 * @author markr
 *
 */
public class EventBasedLtmConfigurator extends DynamicAssignmentConfigurator<EventBasedLtm> {

  /**
   * Constructor
   * 
   * @throws PlanItException thrown when error
   */
  public EventBasedLtmConfigurator() throws PlanItException {
    super(EventBasedLtm.class);
  }

}
