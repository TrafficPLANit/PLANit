package org.planit.trafficassignment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

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
import org.planit.logging.PlanItLogger;
import org.planit.network.physical.PhysicalNetwork;
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
import org.planit.utils.misc.IdGenerator;
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
	 * Unique id
	 */
	protected final long id;

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
	 * The physical link cost function
	 */
	protected PhysicalCost physicalCost;
	
	/**
	 * Map storing InitialLinkSegmentCost objects for each time period
	 */
	protected Map<Long, InitialLinkSegmentCost> initialLinkSegmentCostByTimePeriod;

	/**
	 * Create the gap function which is to be implemented by a derived class of
	 * TrafficAssignment
	 * 
	 * @return gapFunction
	 */
	protected abstract GapFunction createGapFunction();

	// Protected methods

	/**
	 * Check if any components are undefined, if so throw exception
	 * 
	 * @throws PlanItException thrown if any components are undefined
	 */
	protected void checkForEmptyComponents() throws PlanItException {
		if (demands == null) {
			throw new PlanItException("Demand is null");
		}
		if (physicalNetwork == null) {
			throw new PlanItException("Network is null");
		}
		if (smoothing == null) {
			throw new PlanItException("Smoothing is null");
		}
		if (zoning == null) {
			throw new PlanItException("Zoning is null");
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
	 * Initialise the transport network by combining the physical and virtual components
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
	 * Initialise all relevant traffic assignment components before execution of the assignment commences
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
	 * Finalise all relevant traffic assignment components after execution of the assignment has ended
	 * @throws PlanItException thrown if there is an error
	 */
	protected void finalizeAfterExecution() throws PlanItException {
	    
	    disbandTransportNetwork();
	    
        // Finalise traffic assignment components including the traffic assignment itself
        outputManager.finaliseAfterSimulation();    
        
	    PlanItLogger.info("Finished simulation");        
	}

	// Public

	/**
	 * Constructor. Note that defaults that partly depend on derived classes are
	 * assumed to be invoked by the calling method via this.initialiseDefaults()
	 */
	public TrafficAssignment() {
		this.id = IdGenerator.generateId(TrafficAssignment.class);
		outputManager = new OutputManager(this);
		initialLinkSegmentCostByTimePeriod = new HashMap<Long, InitialLinkSegmentCost>();
	}

	// Public abstract methods

	/**
	 * Each traffic assignment class can have its own builder which reveals what
	 * components need to be registered on the traffic assignment instance in order
	 * to function properly.
	 * 
	 * @param trafficComponentCreateListener, the listener should be registered on all traffic component factories the traffic assignment utilises
	 * @return trafficAssignmentBuilder to use
	 */
	public abstract TrafficAssignmentBuilder collectBuilder(final InputBuilderListener trafficComponentCreateListener);

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
	public OutputTypeConfiguration activateOutput(OutputType outputType) throws PlanItException {
	    OutputTypeConfiguration theOutputTypeConfiguration = null;
		if (!outputManager.isOutputTypeActive(outputType)) {
			PlanItLogger.info("Registering Output Type " + outputType);
			OutputTypeAdapter outputTypeAdapter = createOutputTypeAdapter(outputType);
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
																	
		PlanItLogger.info("Finished execution");
	}

	// Getters - Setters

	/**
	 * collect traffic assignment id
	 * 
	 * @return id
	 */
	public long getId() {
		return id;
	}

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
	public void setSmoothing(@Nonnull Smoothing smoothing) {
		this.smoothing = smoothing;
	}

	/**
	 * Set the PhysicalNetwork for the current assignment
	 * 
	 * @param physicalNetwork the PhysicalNetwork object for the current assignment
	 */
	public void setPhysicalNetwork(@Nonnull PhysicalNetwork physicalNetwork) {
		this.physicalNetwork = physicalNetwork;
	}

	/**
	 * Set the Demands object for the current assignment
	 * 
	 * @param demands the Demands object for the current assignment
	 */
	public void setDemands(@Nonnull Demands demands) {
		this.demands = demands;
	}

	/**
	 * Set the zoning object for the current assignment
	 * 
	 * @param zoning the Zoning object for the current assignment
	 */
	public void setZoning(@Nonnull Zoning zoning) {
		this.zoning = zoning;
	}
	
	/**
	 * Set the initial link segment cost
	 * 
	 * @param initialLinkSegmentCost the initial link segment cost
	 */
	public void setInitialLinkSegmentCost(InitialLinkSegmentCost initialLinkSegmentCost) {
		this.initialLinkSegmentCost = initialLinkSegmentCost;
	}
	
	/**
	 * Set the initial link segment cost for a specified time period
	 * 
	 * @param timePeriod the specified time period
	 * @param initialLinkSegmentCost the initial link segment cost
	 */
	public void setInitialLinkSegmentCost(TimePeriod timePeriod, InitialLinkSegmentCost initialLinkSegmentCost) {
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
	public void setPhysicalCost(PhysicalCost physicalCost) throws PlanItException {
		this.physicalCost = physicalCost;
		if (this.physicalCost instanceof InteractorAccessor) {
			// request an accessee instance that we can use to collect the relevant information for the cost
			fireEvent(new org.djutils.event.Event(
					((InteractorAccessor)physicalCost).getRequestedAccesseeEventType(), this, new Object[] {this.physicalCost}));
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
	public void setVirtualCost(VirtualCost virtualCost) throws PlanItException {
		this.virtualCost = virtualCost;
		if (this.virtualCost instanceof InteractorAccessor) {
			// request an accessee instance that we can use to collect the relevant information for the virtual cost
			fireEvent(new org.djutils.event.Event(
					((InteractorAccessor)virtualCost).getRequestedAccesseeEventType(), this, new Object[] {this.virtualCost}));
		}
	}

	/**
	 * Register the output formatter on the assignment
	 *
	 * @param outputFormatter OutputFormatter to be registered
	 */
	public void registerOutputFormatter(OutputFormatter outputFormatter) {
		outputManager.registerOutputFormatter(outputFormatter);
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