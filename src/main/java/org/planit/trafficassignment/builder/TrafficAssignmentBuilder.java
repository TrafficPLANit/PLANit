package org.planit.trafficassignment.builder;

import java.util.List;
import java.util.logging.Logger;

import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.gap.GapFunction;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.OutputFormatter;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.utils.network.physical.Mode;

/**
 * All traffic assignment instances require a network, demand, and (equilibrium)
 * smoothing procedure, all of which should be registered via this generic
 * builder. Specific traffic assignment methods might require special builders
 * derived from this builder
 *
 * @author markr
 *
 */
public abstract class TrafficAssignmentBuilder {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(TrafficAssignmentBuilder.class.getCanonicalName());   

    /**
     * Register the demands zoning and network objects
     *
     * @param demands Demands object to be registered
     * @param zoning Zoning object to be registered
     * @throws PlanItException thrown if the number of zones in the Zoning and Demand objects is inconsistent
     */
    private void registerDemandZoningAndNetwork(final Demands demands, final Zoning zoning, final PhysicalNetwork network) throws PlanItException {
    	parentAssignment.setPhysicalNetwork(network);
    	final int noZonesInZoning = zoning.zones.getNumberOfZones();
    	for (final Mode mode : network.modes.toList()) {
     		for (TimePeriod timePeriod : demands.timePeriods.getRegisteredTimePeriods()) {
    			final ODDemandMatrix odMatrix = demands.get(mode, timePeriod);
    			if (odMatrix == null) {
    				String errorMessage = "No demands matrix defined for Mode " + mode.getExternalId() + " and Time Period " + timePeriod.getExternalId();
    	      LOGGER.severe(errorMessage);
    	      throw new PlanItException(errorMessage);
    			}
    			final int noZonesInDemands = odMatrix.getNumberOfTravelAnalysisZones();
    			if (noZonesInZoning != noZonesInDemands) {
    				String errorMessage = "Zoning object has " + noZonesInZoning + " zones, this is inconsistent with Demands object which has " + noZonesInDemands + " zones for Mode " + mode.getExternalId() + " and Time Period " + timePeriod.getExternalId();
    	      LOGGER.severe(errorMessage);
    	      throw new PlanItException(errorMessage);
    			}
    		}
    	}
        parentAssignment.setZoning(zoning);
        parentAssignment.setDemands(demands);
   }

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
	 * Cost factory to create physical costs to register on the generalized cost.
	 */
	protected final TrafficAssignmentComponentFactory<PhysicalCost> physicalCostFactory;

	/**
	 * Cost factory to create physical costs to register on the generalized cost.
	 */
	protected final TrafficAssignmentComponentFactory<VirtualCost> virtualCostFactory;

    /**
     * The assignment all components will be registered on
     */
    protected final TrafficAssignment parentAssignment;

    // PUBLIC

    /**
     * Constructor
     * @param parentAssignment
     * @param trafficComponentCreateListener
     * @param demands
     * @param zoning
     * @param physicalNetwork
     * @throws PlanItException if registration of demands, zoning, or network does not work
     */
    TrafficAssignmentBuilder(
    		final TrafficAssignment parentAssignment,
    		final InputBuilderListener trafficComponentCreateListener,
    		final Demands demands,
			final Zoning zoning,
			final PhysicalNetwork physicalNetwork) throws PlanItException {
        this.parentAssignment = parentAssignment;
        registerDemandZoningAndNetwork(demands, zoning, physicalNetwork);

        smoothingFactory = new TrafficAssignmentComponentFactory<Smoothing>(Smoothing.class);
		physicalCostFactory = new TrafficAssignmentComponentFactory<PhysicalCost>(PhysicalCost.class);
		virtualCostFactory = new TrafficAssignmentComponentFactory<VirtualCost>(VirtualCost.class);
		// register listener on factories
		smoothingFactory.addListener(trafficComponentCreateListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
		physicalCostFactory.addListener(trafficComponentCreateListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
		virtualCostFactory.addListener(trafficComponentCreateListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
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
    public Smoothing createAndRegisterSmoothing(final String smoothingType) throws PlanItException {
        final Smoothing smoothing = smoothingFactory.create(smoothingType);
        parentAssignment.setSmoothing(smoothing);
        return smoothing;
    }

	/**
	 * Create and register physical link cost function to determine travel time
	 *
	 * @param physicalTraveltimeCostFunctionType the type of cost function to be
	 *                                           created
	 * @return the physical cost created
	 * @throws PlanItException thrown if there is an error
	 */
	public PhysicalCost createAndRegisterPhysicalCost(final String physicalTraveltimeCostFunctionType)
			throws PlanItException {
		final PhysicalCost physicalCost = physicalCostFactory.create(physicalTraveltimeCostFunctionType);
		if (parentAssignment.getPhysicalCost() == null) {
			parentAssignment.setPhysicalCost(physicalCost);
		}
		return physicalCost;
	}

	/**
	 * Create and Register virtual link cost function to determine travel time
	 *
	 * @param virtualTraveltimeCostFunctionType the type of cost function to be
	 *                                          created
	 * @return the cost function created
	 * @throws PlanItException thrown if there is an error
	 */
	public VirtualCost createAndRegisterVirtualCost(final String virtualTraveltimeCostFunctionType)
			throws PlanItException {
		final VirtualCost createdCost = virtualCostFactory.create(virtualTraveltimeCostFunctionType);
		if (parentAssignment.getVirtualCost() == null) {
			parentAssignment.setVirtualCost(createdCost);
		}
		return createdCost;
	}

    /**
     * Register an output formatter
     *
     * @param outputFormatter  OutputFormatter being registered
     * @throws PlanItException thrown if there is an error or validation failure during setup of the output formatter
     */
    public void registerOutputFormatter(final OutputFormatter outputFormatter) throws PlanItException {
    	parentAssignment.registerOutputFormatter(outputFormatter);
    }
    
    /**
     * Remove an output formatter which has already been registered
     * 
     * This is used by the Python interface, which registers the PlanItIO formatter by default
     * 
     * @param outputFormatter the output formatter to be removed
     * @throws PlanItException thrown if there is an error during removal of the output formatter
     */
    public void unregisterOutputFormatter(final OutputFormatter outputFormatter) throws PlanItException {
      parentAssignment.unregisterOutputFormatter(outputFormatter);
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
    public void registerInitialLinkSegmentCost(final InitialLinkSegmentCost initialLinkSegmentCost) {
    	parentAssignment.setInitialLinkSegmentCost(initialLinkSegmentCost);
    }

    /**
     * Register the initial link segment cost for a specified time period
     *
     * @param timePeriod the specified time period
     * @param initialLinkSegmentCost initial link segment cost for the current traffic assignment
     */
    public void registerInitialLinkSegmentCost(final TimePeriod timePeriod, final InitialLinkSegmentCost initialLinkSegmentCost) {
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
    public OutputTypeConfiguration activateOutput(final OutputType outputType) throws PlanItException {
        return parentAssignment.activateOutput(outputType);
    }
    
    /**
     * Deactivate an output type
     * 
     * @param outputType OutputType to be deactivated
     */
    public void deactivateOutput(final OutputType outputType) {
      parentAssignment.deactivateOutput(outputType);
    }
    
	/**
	 * Verify if a given output type is active
	 * @param outputType
	 * @return true if active, false otherwise
	 */
	public boolean isOutputTypeActive(final OutputType outputType)
	{
		return parentAssignment.isOutputTypeActive(outputType);
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
