package org.planit.trafficassignment.builder;

import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.route.RouteChoice;
import org.planit.route.RouteChoiceBuilder;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.trafficassignment.DynamicTrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

/**
 * A dynamic traffic assignment builder is assumed to only support capacity constrained traffic assignment
 * instances. It is used to build the traffic assignment instance with the proper configuration settings
 *
 * @author markr
 *
 */
public class DynamicTrafficAssignmentBuilder extends CapacityConstrainedTrafficAssignmentBuilder implements RouteChoiceBuilder {

	/** the route choice factory*/
	final protected TrafficAssignmentComponentFactory<RouteChoice> routeChoiceFactory;

	/** Constructor
	 *
	 * @param dynamicAssignment the dynamic assignment
	 * @param trafficComponentCreateListener the listener for further traffic components that are created by the builder
	 */
	public DynamicTrafficAssignmentBuilder(final DynamicTrafficAssignment dynamicTrafficAssignment,
			final InputBuilderListener trafficComponentCreateListener) {
		super(dynamicTrafficAssignment, trafficComponentCreateListener);
		routeChoiceFactory = new TrafficAssignmentComponentFactory<RouteChoice>(RouteChoice.class);
		// not registered on listener, since the route choice itself is only a wrapper around components but not a component itself
		routeChoiceFactory.addListener(trafficComponentCreateListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RouteChoice createAndRegisterRouteChoice(final String routeChoiceType) throws PlanItException {
        final RouteChoice routeChoice = routeChoiceFactory.create(routeChoiceType);
        ((DynamicTrafficAssignment)parentAssignment).setRouteChoice(routeChoice);
        return routeChoice;
	}


    // PUBLIC FACTORY METHODS

    /**
     * Create and Register smoothing component
     *
     * @param smoothingType
     *            the type of smoothing component to be created
     * @return Smoothing object created
     * @throws PlanItException
     *             thrown if there is an error
     */
    @Override
	public Smoothing createAndRegisterSmoothing(final String smoothingType) throws PlanItException {
        final Smoothing smoothing = smoothingFactory.create(smoothingType);
        parentAssignment.setSmoothing(smoothing);
        return smoothing;
    }

}
