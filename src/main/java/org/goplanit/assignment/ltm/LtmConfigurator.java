package org.goplanit.assignment.ltm;

import org.goplanit.algorithms.nodemodel.NodeModel;
import org.goplanit.assignment.TrafficAssignmentConfigurator;
import org.goplanit.path.choice.PathChoice;
import org.goplanit.path.choice.PathChoiceConfigurator;
import org.goplanit.path.choice.PathChoiceConfiguratorFactory;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagramComponent;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagramConfigurator;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagramConfiguratorFactory;
import org.goplanit.supply.network.nodemodel.NodeModelComponent;
import org.goplanit.supply.network.nodemodel.NodeModelConfigurator;
import org.goplanit.supply.network.nodemodel.NodeModelConfiguratorFactory;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Configurator for sLTM. Adopting the following defaults:
 * 
 * <ul>
 * <li>Fundamental diagram: NEWELL</li>
 * <li>Node Model: TAMPERE</li>
 * <li>Path Choice: not implemented yet</li>
 * </ul>
 * 
 * @author markr
 * @param <T> type of assignment
 */
public class LtmConfigurator<T extends LtmAssignment> extends TrafficAssignmentConfigurator<T> {

  /**
   * Nested configurator for fundamental diagram within this assignment
   */
  private FundamentalDiagramConfigurator<? extends FundamentalDiagramComponent> fundamentalDiagramConfigurator = null;

  /**
   * Nested configurator for node model within this assignment
   */
  private NodeModelConfigurator<? extends NodeModelComponent> nodeModelConfigurator = null;

  /**
   * Nested configurator for path choice within this assignment
   */
  private PathChoiceConfigurator<? extends PathChoice> pathChoiceConfigurator = null;

  /**
   * Remove any previously registered path choice from the configurator
   */
  protected void unRegisterPathChoice() {
    pathChoiceConfigurator = null;
  }

  /**
   * Constructor
   * 
   * @param ltmClass used
   */
  public LtmConfigurator(Class<T> ltmClass){
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
   */
  public FundamentalDiagramConfigurator<? extends FundamentalDiagramComponent> createAndRegisterFundamentalDiagram(
          final String fundamentalDiagramType){
    fundamentalDiagramConfigurator = FundamentalDiagramConfiguratorFactory.createConfigurator(fundamentalDiagramType);
    return fundamentalDiagramConfigurator;
  }

  /**
   * Collect the fundamental diagram configurator
   * 
   * @return configurator
   */
  public FundamentalDiagramConfigurator<? extends FundamentalDiagramComponent> getFundamentalDiagram() {
    return fundamentalDiagramConfigurator;
  }

  /**
   * choose a particular node model implementation
   * 
   * @param nodeModelType type to choose
   * @return configurator
   */
  public NodeModelConfigurator<? extends NodeModelComponent> createAndRegisterNodeModel(final String nodeModelType){
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

  /**
   * choose a particular path choice implementation
   * 
   * @param pathChoiceType type to choose
   * @return path choice configurator
   */
  public PathChoiceConfigurator<? extends PathChoice> createAndRegisterPathChoice(final String pathChoiceType){
    pathChoiceConfigurator = PathChoiceConfiguratorFactory.createConfigurator(pathChoiceType);
    return pathChoiceConfigurator;
  }

  /**
   * Collect the path choice configurator
   * 
   * @return path choice configurator
   */
  public PathChoiceConfigurator<? extends PathChoice> getPathChoice() {
    return pathChoiceConfigurator;
  }

}
