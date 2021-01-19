package org.planit.assignment;

import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.network.nodemodel.NodeModel;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * When capacity constraints are in place we make a distinction between inflow and outflow and we assume the difference between the two is governed by a fundamental diagram.
 *
 * @author markr
 *
 */
public abstract class CapacityConstrainedTrafficAssignmentBuilder<T extends CapacityConstrainedAssignment> extends TrafficAssignmentBuilder<T> {

  /**
   * create a fundamental diagram instance based on configuration
   * 
   * @return fundamental diagram instance
   * @throws PlanItException thrown if error
   */
  protected FundamentalDiagram createFundamentalDiagramInstance(CapacityConstrainedTrafficAssignmentConfigurator<?> configurator) throws PlanItException {
    TrafficAssignmentComponentFactory<FundamentalDiagram> fundamentalDiagramFactory = new TrafficAssignmentComponentFactory<FundamentalDiagram>(FundamentalDiagram.class);
    fundamentalDiagramFactory.addListener(getInputBuilderListener(), TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    return fundamentalDiagramFactory.create(configurator.getFundamentalDiagram().getClassTypeToConfigure().getCanonicalName(),
        new Object[] { getGroupIdToken(), configurator.getInfrastructureNetwork() });
  }

  /**
   * create a node model instance based on configuration
   * 
   * @return node model instance
   * @throws PlanItException thrown if error
   */
  protected NodeModel createNodeModelInstance(CapacityConstrainedTrafficAssignmentConfigurator<?> configurator) throws PlanItException {
    TrafficAssignmentComponentFactory<NodeModel> nodeModelFactory = new TrafficAssignmentComponentFactory<NodeModel>(NodeModel.class);
    nodeModelFactory.addListener(getInputBuilderListener(), TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    return nodeModelFactory.create(configurator.getNodeModel().getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() });
  }

  /**
   * build subcomponents specific to capacity constrained assignment (if any)
   */
  @Override
  protected void buildSubComponents(T trafficAssignmentInstance) throws PlanItException {
    // delegate to super
    super.buildSubComponents(trafficAssignmentInstance);

    CapacityConstrainedTrafficAssignmentConfigurator<? extends CapacityConstrainedAssignment> configurator = (CapacityConstrainedTrafficAssignmentConfigurator<? extends CapacityConstrainedAssignment>) getConfigurator();

    /* build fundamental diagram sub component */
    if (configurator.getFundamentalDiagram() != null) {
      FundamentalDiagram fundamentalDiagram = createFundamentalDiagramInstance(configurator);
      configurator.getFundamentalDiagram().configure(fundamentalDiagram);
      trafficAssignmentInstance.setFundamentalDiagram(fundamentalDiagram);
    }

    /* build node model sub component */
    if (configurator.getNodeModel() != null) {
      NodeModel nodeModel = createNodeModelInstance(configurator);
      configurator.getNodeModel().configure(nodeModel);
      trafficAssignmentInstance.setNodeModel(nodeModel);
    }
  }

  /**
   * Constructor
   *
   * @param trafficAssignmentClass the traffic assignment class we are building
   * @param groupId                the id generation group this builder is part of
   * @param inputBuilderListener   the listener to be registered for any traffic components being created by this class
   * @param demands                the demands
   * @param zoning                 the zoning
   * @param network                the network
   * @throws PlanItException thrown if there is an error
   */
  protected CapacityConstrainedTrafficAssignmentBuilder(final Class<T> trafficAssignmentClass, final IdGroupingToken groupId, InputBuilderListener inputBuilderListener,
      final Demands demands, final Zoning zoning, final InfrastructureNetwork network) throws PlanItException {

    super(trafficAssignmentClass, groupId, inputBuilderListener, demands, zoning, network);

  }

}
