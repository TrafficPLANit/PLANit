package org.planit.project;

import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.demand.Demands;
import org.planit.event.listener.InputBuilderListener;
import org.planit.event.management.EventManager;
import org.planit.event.management.SimpleEventManager;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.formatter.OutputFormatterFactory;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.supply.networkloading.NetworkLoading;
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
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(CustomPlanItProject.class.getName());

    /**
     * The physical networks registered on this project
     */
    protected TreeMap<Long, PhysicalNetwork> physicalNetworks;

    /**
     * The zoning(s) registered on this project
     */
    protected TreeMap<Long, Zoning> zonings;

    /**
     * The demands registered on this project
     */
    protected TreeMap<Long, Demands> demandsMap;

    /**
     * The output formatter(s) registered on this project
     */
    protected TreeMap<Long, OutputFormatter> outputFormatters;

    /**
     * The physical network used for this project
     */
    protected PhysicalNetwork physicalNetwork;

    /**
     * The zoning used for this project.
     */
    protected Zoning zoning;
    
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

    // Protected methods

    /**
     * Instantiate the factories and register the event manager on them
     * 
     * @param eventManager
     *            the EventManager for this project
     */
    protected void initialiseFactories(EventManager eventManager) {
    	physicalNetworkFactory = new TrafficAssignmentComponentFactory<PhysicalNetwork>(PhysicalNetwork.class);
    	zoningFactory = new TrafficAssignmentComponentFactory<Zoning>(Zoning.class);
    	demandsFactory = new TrafficAssignmentComponentFactory<Demands>(Demands.class);
    	assignmentFactory = new TrafficAssignmentComponentFactory<NetworkLoading>(NetworkLoading.class);
        physicalNetworkFactory.setEventManager(eventManager);
        zoningFactory.setEventManager(eventManager);
        demandsFactory.setEventManager(eventManager);
        assignmentFactory.setEventManager(eventManager);
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
        	e.printStackTrace();
            LOGGER.severe(e.getMessage());
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
     * @param inputBuilderListener
     *            InputBuilderListener used to read in data
     */
    public CustomPlanItProject(InputBuilderListener inputBuilderListener) {
        eventManager.addEventListener(inputBuilderListener);
        trafficAssignments = new TreeMap<Long, TrafficAssignment>();
        physicalNetworks = new TreeMap<Long, PhysicalNetwork>();
        zonings = new TreeMap<Long, Zoning>();
        demandsMap = new TreeMap<Long, Demands>();
        outputFormatters = new TreeMap<Long, OutputFormatter>();
        initialiseFactories(eventManager);
    }

    /**
     * Add a network to the project. If a network with the same id already exists
     * the earlier network is replaced and returned (otherwise null)
     * 
     * @param physicalNetworkType
     *            name of physical network class to register
     * @return the generated physical network
     * @throws PlanItException
     *             thrown if there is an error
     */
    public PhysicalNetwork createAndRegisterPhysicalNetwork(String physicalNetworkType) throws PlanItException {
        physicalNetwork = physicalNetworkFactory.create(physicalNetworkType);
        physicalNetworks.put(physicalNetwork.getId(), physicalNetwork);
        return physicalNetwork;
    }

    /**
     * Create and register the zoning system on the network
     * 
     * There is only one Zoning class, so no need to pass its name into this method.
     * 
     * @return the generated zoning object
     * @throws PlanItException
     *             thrown if there is an error
     */
    public Zoning createAndRegisterZoning() throws PlanItException {
        zoning = zoningFactory.create(Zoning.class.getCanonicalName());
        zonings.put(zoning.getId(), zoning);
        return zoning;
    }

    /**
     * Create and register demands to the project
     * 
     * There is only one Demands class, so no need to pass its name into this
     * method.
     * 
     * @return the generated demands object
     * @throws PlanItException
     *             thrown if there is an error
     */
    public Demands createAndRegisterDemands() throws PlanItException {
        Demands demands = demandsFactory.create(Demands.class.getCanonicalName());
        demandsMap.put(demands.getId(), demands);
        return demands;
    }

    /**
     * Create and register a deterministic traffic assignment instance of a given
     * type
     * 
     * @param trafficAssignmentType
     *            the class name of the traffic assignment type object to be created
     * @return the generated traffic assignment object
     * @throws PlanItException
     *             thrown if there is an error
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
     * @param outputFormatterType
     *            the class name of the output formatter type object to be created
     * @return the generated output formatter object
     * @throws PlanItException
     *             thrown if there is an error
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
     * @param id
     *            the id of the Demands object
     * @return the retrieved Demands object
     */
    public Demands getDemands(long id) {
        return demandsMap.get(id);
    }

    /**
     * Retrieve a TrafficAssigment object given its id
     * 
     * @param id
     *            the id of the TrafficAssignment object
     * @return the retrieved TrafficAssignment object
     */
    public TrafficAssignment getTrafficAssignment(long id) {
        return trafficAssignments.get(id);
    }

    /**
     * Retrieve an output formatter object given its id
     * 
     * @param id
     *            the id of the output formatter object
     * @return the retrieved output formatter object
     */
    public OutputFormatter getOutputFormatter(long id) {
        return outputFormatters.get(id);
    }

    /**
     * Retrieve a Zoning object given its id
     * 
     * @param id
     *            the id of the the Zoning object
     * @return the retrieved Zoning object
     */
    public Zoning getZoning(long id) {
        return zonings.get(id);
    }

    /**
     * Retrieve a PhysicalNetwork object given its id
     * 
     * @param id
     *            the id of the PhysicalNetwork object
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
     * @throws PlanItException
     *             thrown if there is an error
     * 
     */
    public void executeAllTrafficAssignments() throws PlanItException {
        trafficAssignments.forEach((id, ta) -> {
            executeTrafficAssignment(ta);
        });
    }

}