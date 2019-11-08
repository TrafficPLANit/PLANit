package org.planit.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.input.InputBuilderListener;
import org.planit.logging.PlanItLogger;
import org.planit.event.management.EventManager;
import org.planit.event.management.SimpleEventManager;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.demands.Demands;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.formatter.OutputFormatterFactory;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.zoning.Zoning;
import org.planit.exceptions.PlanItException;

/**
 * The top-level class which hosts a single project.
 * 
 * A project can consist of multiple networks, demands and traffic assignments
 * all based on a single configuration (user classes, modes etc.)
 * 
 * @author markr
 *
 */
public class CustomPlanItProject {

    /**
     * The physical networks registered on this project
     */
    protected TreeMap<Long, PhysicalNetwork> physicalNetworks;

    /**
     * The demands registered on this project
     */
    protected TreeMap<Long, Demands> demandsMap;

    /**
     * The output formatter(s) registered on this project
     */
    protected TreeMap<Long, OutputFormatter> outputFormatters;

    /**
     * Object Factory for physical network object
     */
    protected TrafficAssignmentComponentFactory<PhysicalNetwork> physicalNetworkFactory;

    /**
     * Object factory for zoning objects
     */
    protected TrafficAssignmentComponentFactory<Zoning> zoningFactory;
    
    /**
     * Object factory for demands object
     */
    protected TrafficAssignmentComponentFactory<Demands> demandsFactory;

    /**
     * Object factory for network loading object
     */
    protected TrafficAssignmentComponentFactory<NetworkLoading> assignmentFactory;

   /**
     * Event manager used by all components
     */
    protected EventManager eventManager = new SimpleEventManager();

    /**
     * The traffic assignment(s) registered on this project
     */
    protected TreeMap<Long, TrafficAssignment> trafficAssignments;
    
    /**
     * Object factory for physical costs
     */
    protected TrafficAssignmentComponentFactory<InitialPhysicalCost> initialPhysicalCostFactory;

    /**
     * Map to store all InitialLinkSegmentCost objects for each physical network
     */
    protected Map<PhysicalNetwork, List<InitialLinkSegmentCost>> initialLinkSegmentCosts;
    
    // Protected methods

    /**
     * Instantiate the factories and register the event manager on them
     * 
     * @param eventManager the EventManager for this project
     */
    protected void initialiseFactories(EventManager eventManager) {
    	physicalNetworkFactory = new TrafficAssignmentComponentFactory<PhysicalNetwork>(PhysicalNetwork.class);
    	zoningFactory = new TrafficAssignmentComponentFactory<Zoning>(Zoning.class);
    	demandsFactory = new TrafficAssignmentComponentFactory<Demands>(Demands.class);
    	assignmentFactory = new TrafficAssignmentComponentFactory<NetworkLoading>(NetworkLoading.class);
		initialPhysicalCostFactory = new TrafficAssignmentComponentFactory<InitialPhysicalCost>(InitialPhysicalCost.class);
        physicalNetworkFactory.setEventManager(eventManager);
        zoningFactory.setEventManager(eventManager);
        demandsFactory.setEventManager(eventManager);
        assignmentFactory.setEventManager(eventManager);
		initialPhysicalCostFactory.setEventManager(eventManager);
		initialLinkSegmentCosts = new HashMap<PhysicalNetwork, List<InitialLinkSegmentCost>>();
    }

    /**
     * Execute a particular traffic assignment
     * 
     * @param ta
     *            TrafficAssignment to be run
     */
    protected void executeTrafficAssignment(TrafficAssignment ta) {
        try {
            ta.execute();
        } catch (Exception e) {
             PlanItLogger.severe(e.getMessage());
        }
    }

    // Public methods

    /**
     * Constructor which reads in the input builder listener and instantiates the
     * object factory classes.
     * 
     * This constructor instantiates the EventManager, which must be a singleton
     * class for the whole application.
     * 
     * @param inputBuilderListener InputBuilderListener used to read in data
     */
    public CustomPlanItProject(InputBuilderListener inputBuilderListener) {
        eventManager.addEventListener(inputBuilderListener);
        trafficAssignments = new TreeMap<Long, TrafficAssignment>();
        physicalNetworks = new TreeMap<Long, PhysicalNetwork>();
        demandsMap = new TreeMap<Long, Demands>();
        outputFormatters = new TreeMap<Long, OutputFormatter>();
        initialiseFactories(eventManager);
    }

    /**
     * Create and register a physical network on the project
     * 
     * @param physicalNetworkType name of physical network class to register
     * @return the generated physical network
     * @throws PlanItException  thrown if there is an error
     */
    public PhysicalNetwork createAndRegisterPhysicalNetwork(String physicalNetworkType) throws PlanItException {
    	PhysicalNetwork physicalNetwork = physicalNetworkFactory.create(physicalNetworkType);
        physicalNetworks.put(physicalNetwork.getId(), physicalNetwork);
        return physicalNetwork;
    }

    /**
     * Create and register the zoning system on the network
     * 
     * @param physicalNetwork the physical network on which the zoning will be based
     * @return the generated zoning object
     * @throws PlanItException thrown if there is an error
     */
    public Zoning createAndRegisterZoning(PhysicalNetwork physicalNetwork) throws PlanItException {
    	if (physicalNetwork == null) {
    		PlanItLogger.severe("The physical network must be defined before definition of zones can begin");
    		throw new PlanItException("Tried to define zones before the physical network was defined.");
    	}
        Zoning zoning = zoningFactory.create(Zoning.class.getCanonicalName(), physicalNetwork);
        return zoning;
    }

    /**
     * Create and register demands to the project
     * 
     * @param zoning Zoning object which defines the zones which will be used in the demand matrix to be created
     * @return the generated demands object
     * @throws PlanItException thrown if there is an error
     */
    public Demands createAndRegisterDemands(Zoning zoning) throws PlanItException {
    	if (zoning == null) {
    		PlanItLogger.severe("Zones must be defined before definition of demands can begin");
    		throw new PlanItException("Tried to define demands before zones were defined.");
    	}
        Demands demands = demandsFactory.create(Demands.class.getCanonicalName(), zoning);
        demandsMap.put(demands.getId(), demands);
        return demands;
    }

    /**
     * Create and register a deterministic traffic assignment instance of a given
     * type
     * 
     * @param trafficAssignmentType the class name of the traffic assignment type object to be created
     * @return the generated traffic assignment object
     * @throws PlanItException thrown if there is an error
     */
    public DeterministicTrafficAssignment createAndRegisterDeterministicAssignment(String trafficAssignmentType)
            throws PlanItException {
        NetworkLoading networkLoadingAndAssignment = (NetworkLoading) assignmentFactory.create(trafficAssignmentType);
        if (!(networkLoadingAndAssignment instanceof DeterministicTrafficAssignment)) {
            throw new PlanItException("Traffic assignment type is not a valid assignment type");
        }
        DeterministicTrafficAssignment trafficAssignment = (DeterministicTrafficAssignment) networkLoadingAndAssignment;
        // now initialize it, since initialization depends on the concrete class we
        // cannot do this on the constructor of the superclass nor
        // can we do it in the derived constructors as some components are the same
        // across assignments and we want to avoid duplicate code
        trafficAssignment.initialiseDefaults();
        trafficAssignments.put(trafficAssignment.getId(), trafficAssignment);
        return trafficAssignment;
    }
	
	/**
	 * Create and register initial link segment costs from a (single) file which we assume are available in the native xml/csv output format
     * as provided in this project
	 * 
	 * @param network physical network the InitialLinkSegmentCost object will be registered for
	 * @param fileName file containing the initial link segment cost values
	 * @return the InitialLinkSegmentCost object
	 * @throws PlanItException thrown if there is an error
	 */
	public InitialLinkSegmentCost createAndRegisterInitialLinkSegmentCost(PhysicalNetwork network, String fileName) throws PlanItException {
		if (network == null ) {
			PlanItLogger.severe("Physical network must be read in before initial costs can be read.");
			throw new PlanItException("Attempted to read in initial costs before the physical network was defined");
		}
		if (!initialLinkSegmentCosts.containsKey(network)) {
			initialLinkSegmentCosts.put(network, new ArrayList<InitialLinkSegmentCost>());
		}
		InitialLinkSegmentCost initialLinkSegmentCost = (InitialLinkSegmentCost) initialPhysicalCostFactory.create(InitialLinkSegmentCost.class.getCanonicalName(), network, fileName);
		initialLinkSegmentCosts.get(network).add(initialLinkSegmentCost);
        return initialLinkSegmentCost;
	}

	/**
	 * Create and register initial link segment costs from a (single) file for each time period
	 * 
	 * @param network physical network the InitialLinkSegmentCost object will be registered for
	 * @param fileName location of file containing the initial link segment cost values
	 * @param timePeriod the current time period
	 * @return the InitialLinkSegmentCost object
	 * @throws PlanItException thrown if there is an error
	 */
	public InitialLinkSegmentCost createAndRegisterInitialLinkSegmentCost(PhysicalNetwork network, String fileName, TimePeriod timePeriod) throws PlanItException {
		if (network == null ) {
			PlanItLogger.severe("Physical network must be read in before initial costs can be read.");
			throw new PlanItException("Attempted to read in initial costs before the physical network was defined");
		}
		if (!initialLinkSegmentCosts.containsKey(network)) {
			initialLinkSegmentCosts.put(network, new ArrayList<InitialLinkSegmentCost>());
		}
		InitialLinkSegmentCost initialLinkSegmentCost = (InitialLinkSegmentCost) initialPhysicalCostFactory.create(InitialLinkSegmentCost.class.getCanonicalName(), network, fileName, timePeriod);
		initialLinkSegmentCosts.get(network).add(initialLinkSegmentCost);
        return initialLinkSegmentCost;
	}
	
	/**
	 * Create and register initial link segment costs from a (single) file for all time periods in Demands object 
	 * 
	 * @param network physical network the InitialLinkSegmentCost object will be registered for
	 * @param fileName location of file containing the initial link segment cost values
	 * @param demands the Demands object
	 * @return the InitialLinkSegmentCost object
	 * @throws PlanItException thrown if there is an error
	 */
	public Map<TimePeriod, InitialLinkSegmentCost> createAndRegisterInitialLinkSegmentCost(PhysicalNetwork network, String fileName, Demands demands) throws PlanItException {
		if (network == null ) {
			PlanItLogger.severe("Physical network must be read in before initial costs can be read.");
			throw new PlanItException("Attempted to read in initial costs before the physical network was defined");
		}
		Map<TimePeriod, InitialLinkSegmentCost> initialCostsMap = new HashMap<TimePeriod, InitialLinkSegmentCost>();
		for (TimePeriod timePeriod : demands.getRegisteredTimePeriods()) {
			PlanItLogger.info("Registering Initial Link Segment Costs for Time Period " + timePeriod.getId());
			InitialLinkSegmentCost initialLinkSegmentCost = createAndRegisterInitialLinkSegmentCost(network, fileName, timePeriod);
			initialCostsMap.put(timePeriod, initialLinkSegmentCost);
		}
		return initialCostsMap;
	}
	
	/**
	 * Return the initial link segment costs for a network
	 * 
	 * @param network the specified physical network
	 * @return the initial link segment costs for the specified physical network
	 */
	public List<InitialLinkSegmentCost> getInitialLinkSegmentCost(PhysicalNetwork network) {
		return initialLinkSegmentCosts.get(network);
	}
	
    /**
     * Check if assignments have already been registered
     * 
     * @return true if registered assignments exist, false otherwise
     */
    public boolean hasRegisteredAssignments() {
        return !trafficAssignments.isEmpty();
    }

    /**
     * Create and register an output formatter instance of a given type
     * 
     * @param outputFormatterType  the class name of the output formatter type object to be created
     * @return the generated output formatter object
     * @throws PlanItException thrown if there is an error
     */
    public OutputFormatter createAndRegisterOutputFormatter(String outputFormatterType) throws PlanItException {
        OutputFormatter outputFormatter = OutputFormatterFactory.createOutputFormatter(outputFormatterType);
        if (outputFormatter == null) {
            throw new PlanItException("Output writer of type " + outputFormatterType + " could not be created");
        }
        outputFormatters.put(outputFormatter.getId(), outputFormatter);
        return outputFormatter;
    }

    /**
     * Retrieve a Demands object given its id
     * 
     * @param id the id of the Demands object
     * @return the retrieved Demands object
     */
    public Demands getDemands(long id) {
        return demandsMap.get(id);
    }

    /**
     * Retrieve a TrafficAssigment object given its id
     * 
     * @param id the id of the TrafficAssignment object
     * @return the retrieved TrafficAssignment object
     */
    public TrafficAssignment getTrafficAssignment(long id) {
        return trafficAssignments.get(id);
    }

    /**
     * Retrieve an output formatter object given its id
     * 
     * @param id the id of the output formatter object
     * @return the retrieved output formatter object
     */
    public OutputFormatter getOutputFormatter(long id) {
        return outputFormatters.get(id);
    }

    /**
     * Retrieve a PhysicalNetwork object given its id
     * 
     * @param id the id of the PhysicalNetwork object
     * @return the retrieved PhysicalNetwork object
     */
    public PhysicalNetwork getPhysicalNetwork(long id) {
        return physicalNetworks.get(id);
    }

    /**
     * Execute all registered traffic assignments
     * 
     * Top-level error reporting is done in this class. If several traffic
     * assignments are registered and one fails, we report its error and continue
     * with the next assignment.
     * 
     * @throws PlanItException thrown if there is an error
     * 
     */
    public void executeAllTrafficAssignments() throws PlanItException {
        trafficAssignments.forEach((id, ta) -> {
            executeTrafficAssignment(ta);
        });
    }
    
    /**
     * Returns a set of all traffic assignments registered for this project
     * 
     * @return Set of registered traffic assignments
     */
    public List<TrafficAssignment> getAllAssignments() {
    	return new ArrayList<TrafficAssignment>(trafficAssignments.values());
    }

}