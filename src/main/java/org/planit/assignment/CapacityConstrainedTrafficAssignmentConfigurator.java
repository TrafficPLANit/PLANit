package org.planit.assignment;

import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.fundamentaldiagram.FundamentalDiagramConfigurator;
import org.planit.supply.fundamentaldiagram.FundamentalDiagramConfiguratorFactory;
import org.planit.supply.network.nodemodel.NodeModel;
import org.planit.supply.network.nodemodel.NodeModelConfigurator;
import org.planit.supply.network.nodemodel.NodeModelConfiguratorFactory;
import org.planit.utils.exceptions.PlanItException;

/**
 * Configurator for capacity constrained assignment
 * 
 * @author markr
 *
 */
public class CapacityConstrainedTrafficAssignmentConfigurator<T extends CapacityConstrainedAssignment> extends TrafficAssignmentConfigurator<T> {
  
  /**
   * Nested configurator for fundamental diagram within this assignment
   */
  private FundamentalDiagramConfigurator<? extends FundamentalDiagram> fundamentalDiagramConfigurator = null;
  
  /**
   * Nested configurator for node model within this assignment
   */
  private NodeModelConfigurator<? extends NodeModel> nodeModelConfigurator = null;   

  /**
   * Constructor 
   * 
   * @param instanceType the type we are configuring for
   * @throws PlanItException thrown if error
   */
  public CapacityConstrainedTrafficAssignmentConfigurator(Class<T> instanceType) throws PlanItException {
    super(instanceType);      
  }
  
  /**
   * choose a particular fundamental diagram implementation
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
   * @return configurator
   */
  public FundamentalDiagramConfigurator<? extends FundamentalDiagram> getFundamentalDiagram(){
    return fundamentalDiagramConfigurator;
  }
  
  /**
   * choose a particular node model implementation
   * @param nodeModelType type to choose
   * @return configurator
   * @throws PlanItException thrown if error
   */
  public NodeModelConfigurator<? extends NodeModel> createAndRegisterNodeModel(final String nodeModelType) throws PlanItException {
    nodeModelConfigurator = NodeModelConfiguratorFactory.createConfigurator(nodeModelType);
    return nodeModelConfigurator;
  }
  
  /**
   * Collect the node model configurator
   * @return configurator
   */
  public NodeModelConfigurator<? extends NodeModel> getNodeModel(){
    return nodeModelConfigurator;
  }  
   

}
