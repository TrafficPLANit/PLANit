package org.planit.trafficassignment.builder;

import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.network.nodemodel.NodeModel;
import org.planit.trafficassignment.CapacityConstrainedAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

/**
 * When capacity constraints are in place we make a distinction between inflow
 * and outflow and we assume the difference between the two is governed by a
 * fundamental diagram.
 *
 * @author markr
 *
 */
public class CapacityConstrainedTrafficAssignmentBuilder extends TrafficAssignmentBuilder {

  // FACTORIES

  /**
   * fundamental diagram factory to create fundamental diagrams used to populate
   * the link model
   */
  protected final TrafficAssignmentComponentFactory<FundamentalDiagram> fundamentalDiagramFactory;

  /**
   * fundamental diagram factory to create fundamental diagrams used to populate
   * the link model
   */
  protected final TrafficAssignmentComponentFactory<NodeModel> nodeModelFactory;

  /**
   * Constructor
   *
   * @param capacityConstrainedAssignment CapacityConstrainedAssignment object to be built
   * @param trafficComponentCreateListener the listener to be registered for any traffic components
   *          being created by this class
   * @param demands the demands
   * @param zoning the zoning
   * @param physicalNetwork the physical network
   * @throws PlanItException thrown if there is an error
   */
  protected CapacityConstrainedTrafficAssignmentBuilder(
      final CapacityConstrainedAssignment capacityConstrainedAssignment,
      final InputBuilderListener trafficComponentCreateListener,
      final Demands demands,
      final Zoning zoning,
      final PhysicalNetwork physicalNetwork) throws PlanItException {
    super(capacityConstrainedAssignment, trafficComponentCreateListener, demands, zoning, physicalNetwork);
    fundamentalDiagramFactory = new TrafficAssignmentComponentFactory<FundamentalDiagram>(FundamentalDiagram.class);
    nodeModelFactory = new TrafficAssignmentComponentFactory<NodeModel>(NodeModel.class);

    // register the listener on create events of the factory
    fundamentalDiagramFactory.addListener(trafficComponentCreateListener,
        TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    nodeModelFactory.addListener(trafficComponentCreateListener,
        TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
  }

  // FACTORY METHODS

  /**
   * Create and register FundamentalDiagram on assignment
   *
   * @param fundamentalDiagramType the type of Fundamental Diagrams to be created
   * @param physicalNetwork the network for which link segments the fundamental diagram parameters
   *          are to be provided
   * @return FundamentalDiagram created
   * @throws PlanItException thrown if there is an error
   */
  public FundamentalDiagram createAndRegisterFundamentalDiagram(final String fundamentalDiagramType,
      final PhysicalNetwork physicalNetwork) throws PlanItException {
    final FundamentalDiagram createdFundamentalDiagram = fundamentalDiagramFactory.create(fundamentalDiagramType,
        physicalNetwork);
    ((CapacityConstrainedAssignment) parentAssignment).setFundamentalDiagram(createdFundamentalDiagram);
    return createdFundamentalDiagram;
  }

  /**
   * Create and register NodeMode on assignment
   *
   * @param nodeModelType
   *          the type of Node Model to be built
   * @return NodeModel created
   * @throws PlanItException
   *           thrown if there is an error
   */
  public NodeModel createAndRegisterNodeModel(final String nodeModelType) throws PlanItException {
    final NodeModel createdNodeModel = nodeModelFactory.create(nodeModelType);
    ((CapacityConstrainedAssignment) parentAssignment).setNodeModel(createdNodeModel);
    return createdNodeModel;
  }

}
