package org.planit.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.logging.PlanItLogger;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.formatter.OutputFormatterFactory;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;

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

	// INNER CLASSES

	/**
	 * Internal class for registered physical networks
	 *
	 */
	public class ProjectNetworks {

		/**
		 * Returns a List of Links
		 *
		 * @return List of Links
		 */
		public List<PhysicalNetwork> toList() {
			return new ArrayList<PhysicalNetwork>(physicalNetworkMap.values());
		}

		/**
		 * Get physical network by id
		 *
		 * @param id the id of the link
		 * @return the retrieved link
		 */
		public PhysicalNetwork getPhysicalNetwork(final long id) {
			return physicalNetworkMap.get(id);
		}

		/**
		 * Get the number of networks
		 *
		 * @return the number of networks in the project
		 */
		public int getNumberOfPhysicalNetworks() {
			return physicalNetworkMap.size();
		}

	    /**
	     * Check if assignments have already been registered
	     *
	     * @return true if registered assignments exist, false otherwise
	     */
	    public boolean hasRegisteredNetworks() {
	        return !physicalNetworkMap.isEmpty();
	    }

	    /**
	     * Collect the first network that is registered (if any). Otherwise return null
	     * @return first network that is registered if none return null
	     */
	    public PhysicalNetwork getFirstNetwork()
	    {
	        return hasRegisteredNetworks() ? physicalNetworkMap.firstEntry().getValue() : null;
	    }
	}

	/**
	 * Internal class for registered demands
	 *
	 */
	public class ProjectDemands {

		/**
		 * Returns a List of demands
		 *
		 * @return List of demands
		 */
		public List<Demands> toList() {
			return new ArrayList<Demands>(demandsMap.values());
		}

		/**
		 * Get demands by id
		 *
		 * @param id the id of the demands
		 * @return the retrieved demands
		 */
		public Demands getDemands(final long id) {
			return demandsMap.get(id);
		}

		/**
		 * Get the number of demands
		 *
		 * @return the number of demands in the project
		 */
		public int getNumberOfDemands() {
			return demandsMap.size();
		}
	}

	/**
	 * Internal class for registered zonings
	 *
	 */
	public class ProjectZonings {

		/**
		 * Returns a List of zoning
		 *
		 * @return List of zoning
		 */
		public List<Zoning> toList() {
			return new ArrayList<Zoning>(zoningsMap.values());
		}

		/**
		 * Get zoning by id
		 *
		 * @param id the id of the zoning
		 * @return the retrieved zoning
		 */
		public Zoning getZoning(final long id) {
			return zoningsMap.get(id);
		}

		/**
		 * Get the number of zonings
		 *
		 * @return the number of zonings in the project
		 */
		public int getNumberOfZonings() {
			return zoningsMap.size();
		}
	}

	/**
	 * Internal class for registered traffic assignments
	 *
	 */
	public class ProjectAssignments {

		/**
		 * Returns a List of traffic assignments
		 *
		 * @return List of traffic assignments
		 */
		public List<TrafficAssignment> toList() {
			return new ArrayList<TrafficAssignment>(trafficAssignmentsMap.values());
		}

		/**
		 * Get traffic assignment by id
		 *
		 * @param id the id of the traffic assignment
		 * @return the retrieved assignment
		 */
		public TrafficAssignment getTrafficAssignment(final long id) {
			return trafficAssignmentsMap.get(id);
		}

		/**
		 * Get the number of traffic assignment
		 *
		 * @return the number of traffic assignment in the project
		 */
		public int getNumberOfTrafficAssignments() {
			return trafficAssignmentsMap.size();
		}

	    /**
	     * Check if assignments have already been registered
	     *
	     * @return true if registered assignments exist, false otherwise
	     */
	    public boolean hasRegisteredAssignments() {
	        return !trafficAssignmentsMap.isEmpty();
	    }

	    /**
	     * Collect the first traffic assignment that is registered (if any). Otherwise return null
	     * @return first traffic assignment that is registeredm if none return null
	     */
	    public TrafficAssignment getFirstTrafficAssignment()
	    {
	        return hasRegisteredAssignments() ? trafficAssignmentsMap.firstEntry().getValue() : null;
	    }
	}

	/** the listener that we register on each traffic assignment component creation event for external initialisation */
	protected final InputBuilderListener inputBuilderListener;

    /**
     * The physical networks registered on this project
     */
    protected final TreeMap<Long, PhysicalNetwork> physicalNetworkMap;

    /**
     * The demands registered on this project
     */
    protected final TreeMap<Long, Demands> demandsMap;

    /**
     * The zonings registered on this project
     */
    protected final TreeMap<Long, Zoning> zoningsMap;

    /**
     * The traffic assignment(s) registered on this project
     */
    protected final TreeMap<Long, TrafficAssignment> trafficAssignmentsMap;

    /**
     * Object factory for zoning objects
     */
    protected TrafficAssignmentComponentFactory<Zoning> zoningFactory;

    /**
     * The output formatter(s) registered on this project
     */
    protected final TreeMap<Long, OutputFormatter> outputFormatters;

    /**
     * Object Factory for physical network object
     */
    protected TrafficAssignmentComponentFactory<PhysicalNetwork> physicalNetworkFactory;

    /**
     * Object factory for demands object
     */
    protected TrafficAssignmentComponentFactory<Demands> demandsFactory;

    /**
     * Object factory for network loading object
     */
    protected TrafficAssignmentComponentFactory<NetworkLoading> assignmentFactory;

    /**
     * Object factory for physical costs
     */
    protected TrafficAssignmentComponentFactory<InitialPhysicalCost> initialPhysicalCostFactory;

    /**
     * Map to store all InitialLinkSegmentCost objects for each physical network
     */
    protected final Map<PhysicalNetwork, List<InitialLinkSegmentCost>> initialLinkSegmentCosts = new HashMap<PhysicalNetwork, List<InitialLinkSegmentCost>>();

    // Protected methods

    /**
     * Instantiate the factories and register the event manager on them
     *
     */
    protected void initialiseFactories() {
    	physicalNetworkFactory = new TrafficAssignmentComponentFactory<PhysicalNetwork>(PhysicalNetwork.class);
    	zoningFactory = new TrafficAssignmentComponentFactory<Zoning>(Zoning.class);
    	demandsFactory = new TrafficAssignmentComponentFactory<Demands>(Demands.class);
    	assignmentFactory = new TrafficAssignmentComponentFactory<NetworkLoading>(NetworkLoading.class);
		initialPhysicalCostFactory = new TrafficAssignmentComponentFactory<InitialPhysicalCost>(InitialPhysicalCost.class);

		physicalNetworkFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
		zoningFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
		demandsFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
		assignmentFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
		initialPhysicalCostFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    }

    /**
     * Execute a particular traffic assignment
     *
     * @param ta
     *            TrafficAssignment to be run
     */
    protected void executeTrafficAssignment(final TrafficAssignment ta) {
        try {
            ta.execute();
        } catch (final Exception e) {
             PlanItLogger.severe(e.getMessage());
             e.printStackTrace();
        }
    }

    /**
     * The registered physical networks
     */
    public final ProjectNetworks physicalNetworks = new ProjectNetworks();

    /**
     * The registered demands
     */
    public final ProjectDemands demands = new ProjectDemands();

    /**
     * The registered zonings
     */
    public final ProjectZonings zonings = new ProjectZonings();

    /**
     * The registered assignments
     */
    public final ProjectAssignments trafficAssignments = new ProjectAssignments();

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
    public CustomPlanItProject(final InputBuilderListener inputBuilderListener) {
    	this.inputBuilderListener = inputBuilderListener;
        initialiseFactories();

        trafficAssignmentsMap = new TreeMap<Long, TrafficAssignment>();
        physicalNetworkMap = new TreeMap<Long, PhysicalNetwork>();
        demandsMap = new TreeMap<Long, Demands>();
        zoningsMap = new TreeMap<Long, Zoning>();
        outputFormatters = new TreeMap<Long, OutputFormatter>();
    }

    /**
     * Create and register a physical network on the project
     *
     * @param physicalNetworkType name of physical network class to register
     * @return the generated physical network
     * @throws PlanItException  thrown if there is an error
     */
    public PhysicalNetwork createAndRegisterPhysicalNetwork(final String physicalNetworkType) throws PlanItException {
    	final PhysicalNetwork physicalNetwork = physicalNetworkFactory.create(physicalNetworkType);
        physicalNetworkMap.put(physicalNetwork.getId(), physicalNetwork);
        return physicalNetwork;
    }

    /**
     * Create and register the zoning system on the network
     *
     * @param physicalNetwork the physical network on which the zoning will be based
     * @return the generated zoning object
     * @throws PlanItException thrown if there is an error
     */
    public Zoning createAndRegisterZoning(final PhysicalNetwork physicalNetwork) throws PlanItException {
    	if (physicalNetwork == null) {
    		PlanItLogger.severe("The physical network must be defined before definition of zones can begin");
    		throw new PlanItException("Tried to define zones before the physical network was defined.");
    	}
        final Zoning zoning = zoningFactory.create(Zoning.class.getCanonicalName(), new Object[] {physicalNetwork});
        zoningsMap.put(zoning.getId(), zoning);
        return zoning;
    }

    /**
     * Create and register demands to the project
     *
     * @param zoning Zoning object which defines the zones which will be used in the demand matrix to be created
     * @return the generated demands object
     * @throws PlanItException thrown if there is an error
     */
    public Demands createAndRegisterDemands(final Zoning zoning) throws PlanItException {
    	if (zoning == null) {
    		PlanItLogger.severe("Zones must be defined before definition of demands can begin");
    		throw new PlanItException("Tried to define demands before zones were defined.");
    	}
        final Demands demands = demandsFactory.create(Demands.class.getCanonicalName(), new Object[] {zoning});
        demandsMap.put(demands.getId(), demands);
        return demands;
    }

    /**
     * Create and register a deterministic traffic assignment instance of a given
     * type
     *
     * @param trafficAssignmentType the class name of the traffic assignment type object to be created
     * @return the traffic assignment builder object
     * @throws PlanItException thrown if there is an error
     */
    public TrafficAssignmentBuilder createAndRegisterTrafficAssignment(final String trafficAssignmentType)
            throws PlanItException {
        final NetworkLoading networkLoadingAndAssignment = assignmentFactory.create(trafficAssignmentType);
        if (!(networkLoadingAndAssignment instanceof TrafficAssignment)) {
            throw new PlanItException("not a valid traffic assignment type");
        }
        final TrafficAssignment trafficAssignment = (TrafficAssignment) networkLoadingAndAssignment;
        final TrafficAssignmentBuilder trafficAssignmentBuilder = trafficAssignment.collectBuilder(inputBuilderListener);
        // now initialize it, since initialization depends on the concrete class we
        // cannot do this on the constructor of the superclass nor
        // can we do it in the derived constructors as some components are the same
        // across assignments and we want to avoid duplicate code
        trafficAssignment.initialiseDefaults();
        trafficAssignmentsMap.put(trafficAssignment.getId(), trafficAssignment);
        // do not allow direct access to the traffic assignment component. Instead, provide the traffic assignment
        // builder object which is dedicated to providing all the configuration options relevant to the end user while
        // hiding any internals of the traffic assignment concrete class instance
        return trafficAssignmentBuilder;
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
	public InitialLinkSegmentCost createAndRegisterInitialLinkSegmentCost(final PhysicalNetwork network, final String fileName) throws PlanItException {
		if (network == null ) {
			PlanItLogger.severe("Physical network must be read in before initial costs can be read.");
			throw new PlanItException("Attempted to read in initial costs before the physical network was defined");
		}
		if (!initialLinkSegmentCosts.containsKey(network)) {
			initialLinkSegmentCosts.put(network, new ArrayList<InitialLinkSegmentCost>());
		}
		final InitialLinkSegmentCost initialLinkSegmentCost =
				(InitialLinkSegmentCost) initialPhysicalCostFactory.create(InitialLinkSegmentCost.class.getCanonicalName(), new Object[] {network, fileName});
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
	public InitialLinkSegmentCost createAndRegisterInitialLinkSegmentCost(final PhysicalNetwork network, final String fileName, final TimePeriod timePeriod) throws PlanItException {
		if (network == null ) {
			PlanItLogger.severe("Physical network must be read in before initial costs can be read.");
			throw new PlanItException("Attempted to read in initial costs before the physical network was defined");
		}
		if (!initialLinkSegmentCosts.containsKey(network)) {
			initialLinkSegmentCosts.put(network, new ArrayList<InitialLinkSegmentCost>());
		}
		final InitialLinkSegmentCost initialLinkSegmentCost =
				(InitialLinkSegmentCost) initialPhysicalCostFactory.create(InitialLinkSegmentCost.class.getCanonicalName(), new Object[] {network, fileName, timePeriod});
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
	public Map<TimePeriod, InitialLinkSegmentCost> createAndRegisterInitialLinkSegmentCost(final PhysicalNetwork network, final String fileName, final Demands demands) throws PlanItException {
		if (network == null ) {
			PlanItLogger.severe("Physical network must be read in before initial costs can be read.");
			throw new PlanItException("Attempted to read in initial costs before the physical network was defined");
		}
		final Map<TimePeriod, InitialLinkSegmentCost> initialCostsMap = new HashMap<TimePeriod, InitialLinkSegmentCost>();
		for (final TimePeriod timePeriod : demands.getRegisteredTimePeriods()) {
			PlanItLogger.info("Registering Initial Link Segment Costs for Time Period " + timePeriod.getId());
			final InitialLinkSegmentCost initialLinkSegmentCost = createAndRegisterInitialLinkSegmentCost(network, fileName, timePeriod);
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
	public List<InitialLinkSegmentCost> getInitialLinkSegmentCost(final PhysicalNetwork network) {
		return initialLinkSegmentCosts.get(network);
	}

    /**
     * Create and register an output formatter instance of a given type
     *
     * @param outputFormatterType  the class name of the output formatter type object to be created
     * @return the generated output formatter object
     * @throws PlanItException thrown if there is an error
     */
    public OutputFormatter createAndRegisterOutputFormatter(final String outputFormatterType) throws PlanItException {
        final OutputFormatter outputFormatter = OutputFormatterFactory.createOutputFormatter(outputFormatterType);
        if (outputFormatter == null) {
            throw new PlanItException("Output writer of type " + outputFormatterType + " could not be created");
        }
        outputFormatters.put(outputFormatter.getId(), outputFormatter);
        return outputFormatter;
    }

    /**
     * Retrieve an output formatter object given its id
     *
     * @param id the id of the output formatter object
     * @return the retrieved output formatter object
     */
    public OutputFormatter getOutputFormatter(final long id) {
        return outputFormatters.get(id);
    }

    /**
     * Execute all registered traffic assignments
     *
     * Top-level error recording is done in this class. If several traffic
     * assignments are registered and one fails, we record its error and continue
     * with the next assignment.
     *
     * @return Map of ids of failed runs (key) together with their exceptions (value).  Empty if all runs succeed
     * @throws PlanItException required for subclasses which override this method and generate an exception before the runs start
      */
    public Map<Long, PlanItException> executeAllTrafficAssignments() throws PlanItException {
    	final Map<Long, PlanItException> exceptionMap = new HashMap<Long, PlanItException>();
        for (final long id : trafficAssignmentsMap.keySet()) {
        	try {
        		trafficAssignmentsMap.get(id).execute();
        	} catch (final PlanItException pe) {
        		exceptionMap.put(id,  pe);
        	}
        }
        return exceptionMap;
    }

    /**
     * Returns a set of all traffic assignments registered for this project
     *
     * @return Set of registered traffic assignments
     */
    public List<TrafficAssignment> getAllAssignments() {
    	return new ArrayList<TrafficAssignment>(trafficAssignmentsMap.values());
    }

}