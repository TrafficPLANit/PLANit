package org.planit.trafficassignment.builder;

import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.route.choice.RouteChoice;
import org.planit.route.choice.RouteChoiceBuilder;
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

	// needed to allow route choice to register inputbuilder listener on its traffic components
	private final InputBuilderListener trafficComponentCreateListener;

	/** the route choice factory*/
	final protected TrafficAssignmentComponentFactory<RouteChoice> routeChoiceFactory;

	/** Constructor
	 *
	 * @param dynamicAssignment the dynamic assignment
	 * @param trafficComponentCreateListener the listener for further traffic components that are created by the builder
	 */
	public DynamicTrafficAssignmentBuilder(final DynamicTrafficAssignment assignment, final InputBuilderListener trafficComponentCreateListener) {
		super(assignment, trafficComponentCreateListener);
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
