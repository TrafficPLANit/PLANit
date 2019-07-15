package org.planit.trafficassignment.builder;

import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.demand.Demands;
import org.planit.event.management.EventHandler;
import org.planit.event.management.EventManager;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.output.formatter.OutputFormatter;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.zoning.Zoning;

/**
 * All traffic assignment instances require a network, demand, and (equilibrium)
 * smoothing procedure, all of which should be registered via this generic
 * builder. Specific traffic assignment methods might require special builders
 * derived from this builder
 * 
 * @author markr
 *
 */
public abstract class TrafficAssignmentBuilder implements EventHandler {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(TrafficAssignmentBuilder.class.getName());

    /**
     * The smoothing factory used in the assignment algorithm
     * 
     * NB: The smoothing factory is defined here because the same smoothing algorithm is used for all assignments.  If we later decide to use more than one smoothing
     * algorithm and allow different traffic assignments to use different smoothing algorithms, we would need to move this property and its handler methods to 
     * CustomPlanItProject and treat it like the factories for PhysicalNetwork, Demands and Zoning (and allow the different smoothing algorithms to be registered
     * on the project). 
     */
    protected final TrafficAssignmentComponentFactory<Smoothing> smoothingFactory;

    /**
     * The assignment all components will be registered on
     */
    protected final TrafficAssignment parentAssignment;

    // PUBLIC

    /**
     * Constructor
     * 
     * @param parentAssignment
     *            parent traffic assignment object for this builder
     */
    TrafficAssignmentBuilder(@Nonnull TrafficAssignment parentAssignment) {
        this.parentAssignment = parentAssignment;
        smoothingFactory = new TrafficAssignmentComponentFactory<Smoothing>(Smoothing.class);
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
    public Smoothing createAndRegisterSmoothing(String smoothingType) throws PlanItException {
        Smoothing smoothing = smoothingFactory.create(smoothingType);
        parentAssignment.setSmoothing(smoothing);
        return smoothing;
    }

    /**
     * Register physical network object
     * 
     * @param network
     *            PhysicalNetwork object be registered
     */
    public void registerPhysicalNetwork(PhysicalNetwork network) {
        parentAssignment.setPhysicalNetwork(network);
    }

    /**
     * Register demands object
     * 
     * @param demands
     *            Demands object to be registered
     */
    public void registerDemands(Demands demands) {
        parentAssignment.setDemands(demands);
    }

    /**
     * Register zoning object
     * 
     * @param zoning
     *            Zoning object to be registered
     */
    public void registerZoning(Zoning zoning) {
        parentAssignment.setZoning(zoning);
    }

    /**
     * Register the output formatter which dictates in which format our outputs will
     * be persisted
     * 
     * @param outputFormatter
     *            OutputFormatter being registered
     * @throws PlanItException thrown if there is an error or validation failure during setup of the output formatter
     */
    public void registerOutputFormatter(OutputFormatter outputFormatter) throws PlanItException {
         parentAssignment.registerOutputFormatter(outputFormatter);
    }

    /**
     * Register the initial link segment cost for the current assignment
     * 
     * @param initialLinkSegmentCost initial link segment cost for the current traffic assignment
     */
    public void registerInitialLinkSegmentCost(InitialLinkSegmentCost initialLinkSegmentCost) {
    	parentAssignment.setInitialLinkSegmentCost(initialLinkSegmentCost);
    }
    
    /**
     * Set the EventManager for this builder
     * 
     * The EventManager must be a singleton for each PlanItProject application.
     * 
     * @param eventManager
     *            EventManager to be used to create traffic assignment
     */
    public void setEventManager(EventManager eventManager) {
        smoothingFactory.setEventManager(eventManager);
    }

}
