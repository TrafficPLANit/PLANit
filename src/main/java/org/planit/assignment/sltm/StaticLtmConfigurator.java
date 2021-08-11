package org.planit.assignment.sltm;

import org.planit.assignment.TrafficAssignmentConfigurator;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.fundamentaldiagram.FundamentalDiagramConfigurator;
import org.planit.supply.fundamentaldiagram.FundamentalDiagramConfiguratorFactory;
import org.planit.supply.network.nodemodel.NodeModel;
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
public class StaticLtmConfigurator extends TrafficAssignmentConfigurator<StaticLtm> {

  private static final String DISABLE_LINK_STORAGE_CONSTRAINTS = "disableLinkStorageConstraints";

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
   * @throws PlanItException thrown when error
   */
  public StaticLtmConfigurator() throws PlanItException {
    super(StaticLtm.class);
    createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);
    createAndRegisterNodeModel(NodeModel.TAMPERE);
  }

  /**
   * Disable enforcing any storage constraints on link(segments)
   * 
   */
  public void disableLinkStorageConstraints() {
    registerDelayedMethodCall(DISABLE_LINK_STORAGE_CONSTRAINTS);
  }

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
  public NodeModelConfigurator<? extends NodeModel> createAndRegisterNodeModel(final String nodeModelType) throws PlanItException {
    nodeModelConfigurator = NodeModelConfiguratorFactory.createConfigurator(nodeModelType);
    return nodeModelConfigurator;
  }

  /**
   * Collect the node model configurator
   * 
   * @return configurator
   */
  public NodeModelConfigurator<? extends NodeModel> getNodeModel() {
    return nodeModelConfigurator;
  }

}
