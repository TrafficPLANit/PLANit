package org.planit.trafficassignment.builder;

import java.util.List;

import javax.annotation.Nonnull;

import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.event.management.EventHandler;
import org.planit.event.management.EventManager;
import org.planit.exceptions.PlanItException;
import org.planit.gap.GapFunction;
import org.planit.network.physical.ModeImpl;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.demands.Demands;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.OutputFormatter;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.utils.network.physical.Mode;
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
     * Register the demands zoning and network objects
     * 
     * @param demands Demands object to be registered
     * @param zoning Zoning object to be registered
     * @throws PlanItException thrown if the number of zones in the Zoning and Demand objects is inconsistent
     */
    public void registerDemandZoningAndNetwork(Demands demands, Zoning zoning, PhysicalNetwork network) throws PlanItException {
    	parentAssignment.setPhysicalNetwork(network);
    	int noZonesInZoning = zoning.zones.getNumberOfZones();
    	for (Mode mode : network.modes.toList()) {
    		for (TimePeriod timePeriod : TimePeriod.getAllTimePeriods()) {
    			ODDemandMatrix odMatrix = demands.get(mode, timePeriod);
    			if (odMatrix == null) {
    				throw new PlanItException("No demands matrix defined for Mode " + mode.getExternalId() + " and Time Period " + timePeriod.getExternalId());
    			}
    			int noZonesInDemands = odMatrix.getNumberOfTravelAnalysisZones();
    			if (noZonesInZoning != noZonesInDemands) {
    				throw new PlanItException("Zoning object has " + noZonesInZoning + " zones, this is inconsistent with Demands object which has " + noZonesInDemands + " zones for Mode " + mode.getExternalId() + " and Time Period " + timePeriod.getExternalId());
    			}
    		}
    	}
        parentAssignment.setZoning(zoning);
        parentAssignment.setDemands(demands);
   }

    /**
     * Register an output formatter
     * 
     * @param outputFormatter  OutputFormatter being registered
     * @throws PlanItException thrown if there is an error or validation failure during setup of the output formatter
     */
    public void registerOutputFormatter(OutputFormatter outputFormatter) throws PlanItException {
    	parentAssignment.registerOutputFormatter(outputFormatter);
    }
    
    /**
     * Returns a list of output formatters registered on this assignment
     * 
     * @return List of OutputFormatter objects registered on this assignment
     */
    public List<OutputFormatter> getOutputFormatters() {
        return parentAssignment.getOutputFormatters();
    }    

    /**
     * Register the initial link segment cost 
     * 
     * @param initialLinkSegmentCost initial link segment cost for the current traffic assignment
     */
    public void registerInitialLinkSegmentCost(InitialLinkSegmentCost initialLinkSegmentCost) {
    	parentAssignment.setInitialLinkSegmentCost(initialLinkSegmentCost);
    }
    
    /**
     * Register the initial link segment cost for a specified time period
     * 
     * @param timePeriod the specified time period
     * @param initialLinkSegmentCost initial link segment cost for the current traffic assignment
     */
    public void registerInitialLinkSegmentCost(TimePeriod timePeriod, InitialLinkSegmentCost initialLinkSegmentCost) {
    	initialLinkSegmentCost.setTimePeriod(timePeriod);
    	parentAssignment.setInitialLinkSegmentCost(timePeriod, initialLinkSegmentCost);
    }     
    
    /**
     * Method that allows one to activate specific output types for persistence on the traffic assignment instance
     * 
     * @param outputType OutputType object to be used
     * @return outputTypeConfiguration the output type configuration that is now active
     * @throws PlanItException thrown if there is an error activating the output
     */
    public OutputTypeConfiguration activateOutput(OutputType outputType) throws PlanItException {        
        return parentAssignment.activateOutput(outputType);
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
    
    /**
     * Provide the output configuration for user access
     * 
     * @return outputConfiguration for this traffic assignment
     */
    public OutputConfiguration getOutputConfiguration() {
        return parentAssignment.getOutputConfiguration();
    }    
    
    /**
     * Collect the gap function of the trafficAssignment instance
     * 
     * @return gapFunction
     */
    public GapFunction getGapFunction() {
        return parentAssignment.getGapFunction();
    }     

}
