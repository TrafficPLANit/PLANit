package org.goplanit.assignment.ltm.eltm;

import org.goplanit.algorithms.nodemodel.NodeModel;
import org.goplanit.assignment.ltm.LtmConfigurator;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
import org.goplanit.utils.exceptions.PlanItException;

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
   */
  public EventBasedLtmConfigurator(){
    super(EventBasedLtm.class);
    createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);
    createAndRegisterNodeModel(NodeModel.TAMPERE);
  }

}
