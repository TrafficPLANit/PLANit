package org.planit.assignment.ltm.eltm;

import org.planit.algorithms.nodemodel.NodeModel;
import org.planit.assignment.ltm.LtmConfigurator;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.utils.exceptions.PlanItException;

/**
 * Configurator for eLTM
 * 
 * <ul>
 * <li>Fundamental diagram: NEWELL</li>
 * <li>Node Model: TAMPERE</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class EventBasedLtmConfigurator extends LtmConfigurator<EventBasedLtm> {

  /**
   * Constructor
   * 
   * @throws PlanItException thrown when error
   */
  public EventBasedLtmConfigurator() throws PlanItException {
    super(EventBasedLtm.class);
    createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);
    createAndRegisterNodeModel(NodeModel.TAMPERE);
  }

}
