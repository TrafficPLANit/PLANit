package org.planit.trafficassignment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.djutils.event.Event;
import org.djutils.event.EventType;
import org.planit.cost.Cost;
import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.data.SimulationData;
import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.gap.GapFunction;
import org.planit.input.InputBuilderListener;
import org.planit.interactor.InteractorAccessor;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentImpl;
import org.planit.network.transport.TransportNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.OutputManager;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.OutputFormatter;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 * Traffic assignment class which simultaneously is responsible for the loading
 * hence it is also considered as a traffic assignment component of this type
 *
 * @author markr
 *
 */
public abstract class TrafficAssignment extends NetworkLoading {

	// Private

	/** generated UID */
	private static final long serialVersionUID = 801775330292422910L;
	
  /** the logger */
  private static final Logger LOGGER =  Logger.getLogger(MacroscopicLinkSegmentImpl.class.getCanonicalName());	

	/**
	 * The zoning to use
	 */
	private Zoning zoning;

	/**
	 * Gap function instance containing functionality to compute the gap between
	 * equilibrium and the current state gap
	 */
	private GapFunction gapFunction;

	// Protected

	/**
     * The builder for all traffic assignment instances
     */
    protected TrafficAssignmentBuilder trafficAssignmentBuilder;

	/**
	 * Physical network to use
	 */
	protected PhysicalNetwork physicalNetwork;

	/**
	 * Output manager deals with all the output configurations for the registered
	 * traffic assignments
	 */
	protected OutputManager outputManager;

	/**
	 * The transport network to use which is an adaptor around the physical network
	 * and the zoning
	 */
	protected TransportNetwork transportNetwork = null;

	/**
	 * The virtual cost function
	 */
	protected VirtualCost virtualCost;

	/**
	 * holds the count of all link segments in the transport network
	 */
	protected int numberOfNetworkSegments;

    /**
     * holds the count of all vertices in the transport network
     */
    protected int numberOfNetworkVertices;

	/**
	 * the smoothing to use
	 */
	protected Smoothing smoothing = null;

	/**
	 * The demand to use
	 */
	protected Demands demands = null;

	/**
	 * The initial link segment cost
	 */
	protected InitialLinkSegmentCost initialLinkSegmentCost;

	/**
	 * The physical generalized cost approach
	 */
	protected PhysicalCost physicalCost;

	/**
	 * Map storing InitialLinkSegmentCost objects for each time period
	 */
	protected Map<Long, InitialLinkSegmentCost> initialLinkSegmentCostByTimePeriod;

	/** create the traffic assignment builder for this traffic assignment
	 * @param trafficComponentCreateListener listener to register on all traffic assignment components that this builder can build
	 * @param physicalNetwork the physical network this assignment works on
	 * @param zoning the zoning this assignment works on
	 * @param demands the demands this assignment works on
	 * @return created traffic assignment builder
	 */
	protected abstract TrafficAssignmentBuilder createTrafficAssignmentBuilder(
			InputBuilderListener trafficComponentCreateListener,
			Demands demands,
			Zoning zoning,
			PhysicalNetwork physicalNetwork) throws PlanItException;

	/**
	 * Create the gap function which is to be implemented by a derived class of
	 * TrafficAssignment
	 *
	 * @return gapFunction
	 */
	protected abstract GapFunction createGapFunction();

	// Protected methods

	/** register all the known listeners for the passed in eventType on this producer for this event type
	 * @param eventType
	 */
	protected abstract void addRegisteredEventTypeListeners(EventType eventType);

	/**
	 * Check if any components are undefined, if so throw exception
	 *
	 * @throws PlanItException thrown if any components are undefined
	 */
	protected void checkForEmptyComponents() throws PlanItException {
		if (demands == null) {
			String errorMessage = "Demand is null";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
		}
		if (physicalNetwork == null) {
			String errorMessage = "Network is null";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
		}
		if (smoothing == null) {
			String errorMessage = "Smoothing is null";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
		}
		if (zoning == null) {
			String errorMessage = "Zoning is null";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
		}
	}

    /**
     * Verify if the traffic assignment components are compatible and nonnull
     *
     * @throws PlanItException thrown if the components are not compatible
     */
    protected void verifyComponentCompatibility() throws PlanItException {
        // TODO
    }

	/**
	 * Initialize the transport network by combining the physical and virtual components
	 * @throws PlanItException thrown if there is an error
	 */
	protected void createTransportNetwork() throws PlanItException {
        transportNetwork = new TransportNetwork(physicalNetwork, zoning);
        transportNetwork.integrateConnectoidsAndLinks();
        this.numberOfNetworkSegments = getTransportNetwork().getTotalNumberOfEdgeSegments();
        this.numberOfNetworkVertices = getTransportNetwork().getTotalNumberOfVertices();
	}

	/**
	 * detach the virtual and physical transport network again
	 * @throws PlanItException thrown if there is an error
	 */
	protected void disbandTransportNetwork() throws PlanItException {
        // Disconnect here since the physical network might be reused in a different assignment
        transportNetwork.removeVirtualNetworkFromPhysicalNetwork();
	}

	/**
	 * Initialize all relevant traffic assignment components before execution of the assignment commences
	 *
	 * @throws PlanItException thrown if there is an error
	 */
	protected void initialiseBeforeExecution() throws PlanItException{
	    // verify validity
        checkForEmptyComponents();
        verifyComponentCompatibility();
        createTransportNetwork();
        outputManager.initialiseBeforeSimulation(getId());
        physicalCost.initialiseBeforeSimulation(physicalNetwork);
         virtualCost.initialiseBeforeSimulation(zoning.getVirtualNetwork());
	}

	/**
	 * Finalize all relevant traffic assignment components after execution of the assignment has ended
	 * @throws PlanItException thrown if there is an error
	 */
	protected void finalizeAfterExecution() throws PlanItException {

	    disbandTransportNetwork();

        // Finalize traffic assignment components including the traffic assignment itself
        outputManager.finaliseAfterSimulation();

	    LOGGER.info("Finished simulation");
	}

	// Public

	/**
	 * Constructor. Note that defaults that partly depend on derived classes are
	 * assumed to be invoked by the calling method via this.initialiseDefaults()
	 */
	public TrafficAssignment() {
		super();
		outputManager = new OutputManager(this);
		initialLinkSegmentCostByTimePeriod = new HashMap<Long, InitialLinkSegmentCost>();
	}

	// Public abstract methods

	/**
	 * Each traffic assignment class can have its own builder which reveals what
	 * components need to be registered on the traffic assignment instance in order
	 * to function properly.
	 * @param physicalNetwork this assignment works on
	 * @param zoning this assignment works on
	 * @param demands this assignment works on
	 *
	 * @param trafficComponentCreateListener, the listener should be registered on all traffic component factories the traffic assignment utilises
	 * @return trafficAssignmentBuilder to use
	 * @throws PlanItException
	 */
	public TrafficAssignmentBuilder collectBuilder(
			final InputBuilderListener trafficComponentCreateListener,
			final Demands theDemands,
			final Zoning theZoning,
			final PhysicalNetwork thePhysicalNetwork) throws PlanItException {
		if(this.trafficAssignmentBuilder == null) {
			this.trafficAssignmentBuilder =
					createTrafficAssignmentBuilder(
							trafficComponentCreateListener, theDemands, theZoning, thePhysicalNetwork);
		}
		return this.trafficAssignmentBuilder;
	}

	/**
	 * Run equilibration after resources initialized, including saving results
	 *
	 * @throws PlanItException thrown if there is an error
	 */
	public abstract void executeEquilibration() throws PlanItException;

	/**
	 * Create the output type adapter for the current output type
	 *
	 * @param outputType the current output type
	 * @return the output type adapter corresponding to the current traffic assignment and output type
	 */
	public abstract OutputTypeAdapter createOutputTypeAdapter(OutputType outputType);

	/**
	 * Collect the gap function which is to be set by a derived class of
	 * TrafficAssignment via the initialiseDefaults() right after construction
	 *
	 * @return gapFunction
	 */
	public GapFunction getGapFunction() {
		return gapFunction;
	}

	// Public methods

	/**
	 * Initialize the traffic assignment defaults: (i) activate link output
	 *
	 * @throws PlanItException thrown when there is an error
	 */
	public void initialiseDefaults() throws PlanItException {
		// general defaults
		this.gapFunction = createGapFunction();
	}

	/**
	 * Method that allows one to activate specific output types for persistence
	 * which is passed on to the output manager
	 *
	 * @param outputType OutputType object to be used
	 * @return outputTypeConfiguration the output type configuration that is now active
	 * @throws PlanItException thrown if there is an error activating the output
	 */
	public OutputTypeConfiguration activateOutput(final OutputType outputType) throws PlanItException {
	    OutputTypeConfiguration theOutputTypeConfiguration = null;
		if (!outputManager.isOutputTypeActive(outputType)) {
			LOGGER.info("Registering Output Type " + outputType);
			final OutputTypeAdapter outputTypeAdapter = createOutputTypeAdapter(outputType);
			outputManager.registerOutputTypeAdapter(outputType, outputTypeAdapter);
	        theOutputTypeConfiguration = outputManager.createAndRegisterOutputTypeConfiguration(outputType, this);
		} else {
		    theOutputTypeConfiguration = outputManager.getOutputConfiguration().getOutputTypeConfiguration(outputType);
		}
		return theOutputTypeConfiguration;
	}

	/**
	 * Execute assignment, including initializing resources, running equilibration
	 * and then closing resources
	 *
	 * @throws PlanItException thrown if there is an error
	 */
	public void execute() throws PlanItException {

		initialiseBeforeExecution();

		executeEquilibration();

		finalizeAfterExecution();

		LOGGER.info("Finished execution");
	}

	// Getters - Setters

	/**
	 * Get the TransportNetwork used in the current assignment
	 *
	 * @return TransportNetwork used in current assignment
	 */
	public TransportNetwork getTransportNetwork() {
		return transportNetwork;
	}

	/**
	 * Provide the output configuration for user access (via the output manager)
	 *
	 * @return outputConfiguration for this traffic assignment
	 */
	public OutputConfiguration getOutputConfiguration() {
		return outputManager.getOutputConfiguration();
	}

	/**
	 * Set the Smoothing object for the current assignment
	 *
	 * @param smoothing Smoothing object for the current assignment
	 */
	public void setSmoothing(@Nonnull final Smoothing smoothing) {
		this.smoothing = smoothing;
	}

	/**
	 * Set the PhysicalNetwork for the current assignment
	 *
	 * @param physicalNetwork the PhysicalNetwork object for the current assignment
	 */
	public void setPhysicalNetwork(@Nonnull final PhysicalNetwork physicalNetwork) {
		this.physicalNetwork = physicalNetwork;
	}

	/**
	 * Set the Demands object for the current assignment
	 *
	 * @param demands the Demands object for the current assignment
	 */
	public void setDemands(@Nonnull final Demands demands) {
		this.demands = demands;
	}

	/**
	 * Set the zoning object for the current assignment
	 *
	 * @param zoning the Zoning object for the current assignment
	 */
	public void setZoning(@Nonnull final Zoning zoning) {
		this.zoning = zoning;
	}

	/**
	 * Set the initial link segment cost
	 *
	 * @param initialLinkSegmentCost the initial link segment cost
	 */
	public void setInitialLinkSegmentCost(final InitialLinkSegmentCost initialLinkSegmentCost) {
		this.initialLinkSegmentCost = initialLinkSegmentCost;
	}

	/**
	 * Set the initial link segment cost for a specified time period
	 *
	 * @param timePeriod the specified time period
	 * @param initialLinkSegmentCost the initial link segment cost
	 */
	public void setInitialLinkSegmentCost(final TimePeriod timePeriod, final InitialLinkSegmentCost initialLinkSegmentCost) {
		initialLinkSegmentCostByTimePeriod.put(timePeriod.getId(), initialLinkSegmentCost);
	}

	/**
	 * Get the dynamic physical cost object for the current assignment
	 *
	 * @return the physical cost object for the current assignment
	 */
	public Cost<LinkSegment> getPhysicalCost() {
		return physicalCost;
	}

	/**
	 * Set the physical cost where in case the cost is an InteractorAccessor will
	 * trigger an event to get access to the required data via requesting an
	 * InteractorAccessee
	 *
	 * @param physicalCost the physical cost object for the current assignment
	 * @throws PlanItException thrown if there is an error
	 */
	public void setPhysicalCost(final PhysicalCost physicalCost) throws PlanItException {
		this.physicalCost = physicalCost;
		if (this.physicalCost instanceof InteractorAccessor) {
			// request an accessee instance that we can use to collect the relevant information for the cost
			final EventType requestAccessee = ((InteractorAccessor)physicalCost).getRequestedAccesseeEventType();
			addRegisteredEventTypeListeners(requestAccessee);
			fireEvent(new Event(requestAccessee, this, this.physicalCost));
			if (!listeners.containsKey(requestAccessee)) {
				String errorMessage = "Error during setPhysicalCost";
	      LOGGER.severe(errorMessage);
	      throw new PlanItException(errorMessage);
			}
		}
	}

	/**
	 * Returns the virtual cost object for the current assignment
	 *
	 * @return the virtual cost object for the current assignments
	 */
	public Cost<ConnectoidSegment> getVirtualCost() {
		return virtualCost;
	}

	/**
	 * Set the virtual cost where in case the cost is an InteractorAccessor will
	 * trigger an event to get access to the required data via requesting an
	 * InteractorAccessee
	 *
	 * @param virtualCost the virtual cost object to be assigned
	 * @throws PlanItException thrown if there is an error
	 */
	public void setVirtualCost(final VirtualCost virtualCost) throws PlanItException {
		this.virtualCost = virtualCost;
		if (this.virtualCost instanceof InteractorAccessor) {
			// request an accessee instance that we can use to collect the relevant information for the virtual cost
			final EventType requestAccesseeType = ((InteractorAccessor)virtualCost).getRequestedAccesseeEventType();
			addRegisteredEventTypeListeners(requestAccesseeType);
			fireEvent(new Event(requestAccesseeType, this, this.virtualCost));
			if (!listeners.containsKey(requestAccesseeType)) {
				String errorMessage = "Error during setVirtualCost";
	      LOGGER.severe(errorMessage);
	      throw new PlanItException(errorMessage);
			}
		}
	}

	/**
	 * Register the output formatter on the assignment
	 *
	 * @param outputFormatter OutputFormatter to be registered
	 */
	public void registerOutputFormatter(final OutputFormatter outputFormatter) {
		outputManager.registerOutputFormatter(outputFormatter);
	}
	
	/**
	 * Unregister an output formatter
	 * 
	 * @param outputFormatter the output formatter to be removed
	 */
	public void unregisterOutputFormatter(final OutputFormatter outputFormatter) {
	  outputManager.unregisterOutputFormatter(outputFormatter);
	}

	/**
	 * Returns a list of output formatters registered on this assignment
	 *
	 * @return List of OutputFormatter objects registered on this assignment
	 */
	public List<OutputFormatter> getOutputFormatters() {
		return outputManager.getOutputFormatters();
	}

	/**
	 * Return the simulation data for the current iteration
	 *
	 * @return the simulation data for the current iteration
	 */
	public abstract SimulationData getSimulationData();

}