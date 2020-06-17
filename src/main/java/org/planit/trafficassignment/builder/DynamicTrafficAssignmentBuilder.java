package org.planit.trafficassignment.builder;

import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.route.choice.RouteChoice;
import org.planit.route.choice.RouteChoiceBuilder;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.trafficassignment.DynamicTrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

/**
 * A dynamic traffic assignment builder is assumed to only support capacity constrained traffic
 * assignment
 * instances. It is used to build the traffic assignment instance with the proper configuration
 * settings
 *
 * @author markr
 *
 */
public class DynamicTrafficAssignmentBuilder extends CapacityConstrainedTrafficAssignmentBuilder implements
    RouteChoiceBuilder {

  // needed to allow route choice to register inputbuilder listener on its traffic components
  private final InputBuilderListener trafficComponentCreateListener;

  /** the route choice factory */
  final protected TrafficAssignmentComponentFactory<RouteChoice> routeChoiceFactory;

  /**
   * Constructor
   *
   * @param assignment the dynamic assignment
   * @param trafficComponentCreateListener the listener for further traffic components that are
   *          created by the builder
   * @param demands the demands
   * @param zoning the zoning
   * @param physicalNetwork the physical network
   * @throws PlanItException thrown if there is an exception
   */
  public DynamicTrafficAssignmentBuilder(
      final DynamicTrafficAssignment assignment,
      final InputBuilderListener trafficComponentCreateListener,
      final Demands demands,
      final Zoning zoning,
      final PhysicalNetwork physicalNetwork) throws PlanItException {
    super(assignment, trafficComponentCreateListener, demands, zoning, physicalNetwork);
    this.trafficComponentCreateListener = trafficComponentCreateListener;
    routeChoiceFactory = new TrafficAssignmentComponentFactory<RouteChoice>(RouteChoice.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RouteChoice createAndRegisterRouteChoice(final String routeChoiceType) throws PlanItException {
    final RouteChoice routeChoice =
        routeChoiceFactory.createWithConstructorArguments(routeChoiceType, trafficComponentCreateListener);
    ((DynamicTrafficAssignment) parentAssignment).setRouteChoice(routeChoice);
    return routeChoice;
  }

  // PUBLIC FACTORY METHODS

  /**
   * Create and Register smoothing component
   *
   * @param smoothingType
   *          the type of smoothing component to be created
   * @return Smoothing object created
   * @throws PlanItException
   *           thrown if there is an error
   */
  @Override
  public Smoothing createAndRegisterSmoothing(final String smoothingType) throws PlanItException {
    final Smoothing smoothing = smoothingFactory.create(smoothingType);
    parentAssignment.setSmoothing(smoothing);
    return smoothing;
  }

}
