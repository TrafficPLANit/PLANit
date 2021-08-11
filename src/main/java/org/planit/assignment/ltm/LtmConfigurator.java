package org.planit.assignment.ltm;

import org.planit.algorithms.nodemodel.NodeModel;
import org.planit.assignment.TrafficAssignmentConfigurator;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.fundamentaldiagram.FundamentalDiagramConfigurator;
import org.planit.supply.fundamentaldiagram.FundamentalDiagramConfiguratorFactory;
import org.planit.supply.network.nodemodel.NodeModelComponent;
import org.planit.supply.network.nodemodel.NodeModelConfigurator;
import org.planit.supply.network.nodemodel.NodeModelConfiguratorFactory;
import org.planit.utils.exceptions.PlanItException;

/**
 * Configurator for sLTM. Adopting the following defaults:
 * 
 * <ul>
 * <li>Fundamental diagram: NEWELL</li>
 * <li>Node Model: TAMPERE</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class LtmConfigurator<T extends LtmAssignment> extends TrafficAssignmentConfigurator<T> {

  /**
   * Nested configurator for fundamental diagram within this assignment
   */
  private FundamentalDiagramConfigurator<? extends FundamentalDiagram> fundamentalDiagramConfigurator = null;

  /**
   * Nested configurator for node model within this assignment
   */
  private NodeModelConfigurator<? extends NodeModelComponent> nodeModelConfigurator = null;

  /**
   * Constructor
   * 
   * @param ltmClass used
   * @throws PlanItException thrown when error
   */
  public LtmConfigurator(Class<T> ltmClass) throws PlanItException {
    super(ltmClass);
    createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);
    createAndRegisterNodeModel(NodeModel.TAMPERE);
  }

  //
  // Child component options
  //

  /**
   * choose a particular fundamental diagram implementation
   * 
   * @param fundamentalDiagramType type to choose
   * @return configurator
   * @throws PlanItException thrown if error
   */
  public FundamentalDiagramConfigurator<? extends FundamentalDiagram> createAndRegisterFundamentalDiagram(final String fundamentalDiagramType) throws PlanItException {
    fundamentalDiagramConfigurator = FundamentalDiagramConfiguratorFactory.createConfigurator(fundamentalDiagramType);
    return fundamentalDiagramConfigurator;
  }

  /**
   * Collect the fundamental diagram configurator
   * 
   * @return configurator
   */
  public FundamentalDiagramConfigurator<? extends FundamentalDiagram> getFundamentalDiagram() {
    return fundamentalDiagramConfigurator;
  }

  /**
   * choose a particular node model implementation
   * 
   * @param nodeModelType type to choose
   * @return configurator
   * @throws PlanItException thrown if error
   */
  public NodeModelConfigurator<? extends NodeModelComponent> createAndRegisterNodeModel(final String nodeModelType) throws PlanItException {
    nodeModelConfigurator = NodeModelConfiguratorFactory.createConfigurator(nodeModelType);
    return nodeModelConfigurator;
  }

  /**
   * Collect the node model configurator
   * 
   * @return configurator
   */
  public NodeModelConfigurator<? extends NodeModelComponent> getNodeModel() {
    return nodeModelConfigurator;
  }

}
