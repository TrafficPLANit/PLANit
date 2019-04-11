package org.planit.project;

import java.util.TreeMap;
import java.util.SortedMap;
import java.util.SortedSet;

import org.planit.demand.Demands;
import org.planit.dto.BprResultDto;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.transport.TransportNetwork;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.userclass.Mode;
import org.planit.time.TimePeriod;
import org.planit.zoning.Zoning;
import org.planit.event.EventManager;
import org.planit.event.InputBuilder;
import org.planit.event.SimpleEventManager;
import org.planit.exceptions.PlanItException;

/**
 * The top-level class which hosts a single project.
 * 
 *  A project can consist of multiple networks, demands and traffic assignments all based on a single configuration (user classes, modes etc.)
 * @author markr
 *
 */
public class PlanItProject {
	
	/**
	 * The physical networks registered on this project
	 */
	private TreeMap<Long, PhysicalNetwork> physicalNetworks;
	
	/**
	 * The zoning(s) registered on this project
	 */
	private TreeMap<Long, Zoning> zonings;

	/**
	 * The demands registered on this project
	 */
	private TreeMap<Long, Demands> demandsMap;
	
	/**
	 * The traffic assignment(s) registered on this project
	 */
	private TreeMap<Long, TrafficAssignment> trafficAssignments;
	
	private PhysicalNetwork physicalNetwork;
	
	private Zoning zoning;
	/**
	 * Object Factory classes
	 */
	private TrafficAssignmentComponentFactory<PhysicalNetwork> physicalNetworkFactory;
	private TrafficAssignmentComponentFactory<Zoning> zoningFactory;
	private TrafficAssignmentComponentFactory<Demands> demandsFactory;
	private TrafficAssignmentComponentFactory<NetworkLoading> assignmentFactory;
	private InputBuilder inputBuilder;
	
/**
 * Constructor which reads in the input builder listener and instantiates the object factory classes.
 * 
 * This constructor instantiates the EventManager, which must be a singleton class for the whole application.
 * 
 * At present the input builder listener must be either MetroScan or BasicCsvScan.
 * 
 * @param inputBuilderListener				InputBuilderListener used to read in data
 */
	public PlanItProject(InputBuilder inputBuilder) {
		this.inputBuilder = inputBuilder;
		EventManager eventManager = new SimpleEventManager();
		eventManager.addEventListener(inputBuilder);
		trafficAssignments = new TreeMap<Long,TrafficAssignment>();
		physicalNetworks = new TreeMap<Long,PhysicalNetwork>();
		zonings = new TreeMap<Long,Zoning>();
		demandsMap = new TreeMap<Long, Demands>();
		
		physicalNetworkFactory = new TrafficAssignmentComponentFactory<PhysicalNetwork>(PhysicalNetwork.class);		
		physicalNetworkFactory.setEventManager(eventManager);
		
		zoningFactory = new TrafficAssignmentComponentFactory<Zoning>(Zoning.class);
		zoningFactory.setEventManager(eventManager);
		
		demandsFactory = new TrafficAssignmentComponentFactory<Demands>(Demands.class);
		demandsFactory.setEventManager(eventManager);
		
		assignmentFactory = new TrafficAssignmentComponentFactory<NetworkLoading>(NetworkLoading.class);
		assignmentFactory.setEventManager(eventManager);
	}
	
/** 
 * Add a network to the project.  If a network with the same id already exists the earlier network is replaced and returned (otherwise null)
 * 
 * @param network					name of physical network class to register
 * @return 								the generated physical network
 * @throws PlanItException 	thrown if there is an error
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
 * @return 								the generated zoning object		
 * @throws PlanItException 	thrown if there is an error
 */
	public Zoning createAndRegisterZoning() throws PlanItException {
		zoning = zoningFactory.create(Zoning.class.getCanonicalName());
		zonings.put(zoning.getId(), zoning);
		return zoning;		
	}
	
/** 
 * Create and register demands to the project
 * 
 * There is only one Demands class, so no need to pass its name into this method.
 * 
 * @return 								the generated demands object
 * @throws PlanItException 	thrown if there is an error
 */
	public Demands createAndRegisterDemands() throws PlanItException {
		Demands demands = demandsFactory.create(Demands.class.getCanonicalName());
		demandsMap.put(demands.getId(), demands);
		return demands;
	}	
		
/** 
 * Create and register a deterministic traffic assignment instance of a given type
 * 
 * @param trafficAssignmentType		the class name of the traffic assignment type object to be created
 * @return											the generated traffic assignment object
 * @throws PlanItException 				thrown if there is an error
 */
	public DeterministicTrafficAssignment  createAndRegisterDeterministicAssignment(String trafficAssignmentType) throws PlanItException {
		NetworkLoading networkLoadingAndAssignment = (NetworkLoading) assignmentFactory.create(trafficAssignmentType);
		if  (!(networkLoadingAndAssignment instanceof DeterministicTrafficAssignment))  {
			throw new PlanItException("Traffic assignment type is not a valid assignment type");
		}	
		DeterministicTrafficAssignment trafficAssignment = (DeterministicTrafficAssignment) networkLoadingAndAssignment;
		TransportNetwork transportNetwork = inputBuilder.buildTransportNetwork(physicalNetwork, zoning);
		trafficAssignment.setTransportNetwork(transportNetwork);
		trafficAssignments.put(trafficAssignment.getId(), trafficAssignment);		
		return trafficAssignment;
	}

/**
 * Retrieve a Demands object given its id
 * 
 * @param id		the id of the Demands object
 * @return			the retrieved Demands object
 */
	public Demands getDemands(long id) {
		return demandsMap.get(id);
	}
	
/**
 * Retrieve a TrafficAssigment object given its id
 * 
 * @param id		the id of the TrafficAssignment object
 * @return			the retrieved TrafficAssignment object
 */
	public TrafficAssignment getTrafficAssignment(long id) {
		return trafficAssignments.get(id);
	}
	
/**
 * Retrieve a Zoning object given its id
 * 
 * @param id		the id of the the Zoning object
 * @return			the retrieved Zoning object
 */
	public Zoning getZoning(long id) {
		return zonings.get(id);
	}
	
/**
 * Retrieve a PhysicalNetwork object given its id
 * 
 * @param id		the id of the PhysicalNetwork object
 * @return			the retrieved PhysicalNetwork object
 */
	public PhysicalNetwork getPhysicalNetwork(long id) {
		return physicalNetworks.get(id);
	}
	
/**
 * Execute all registered traffic assignments 
 * 
 * Top-level error reporting is done in this class.  If several traffic assignments are registered and one fails, we report its error and continue with the next assignment. 
 * 
 * @return									map containing results categorized by run id, time period id and mode id
 * @throws PlanItException		thrown if the getPlanItGeoUtils() method in InputBuilderListener has not been overridden
 * 
 */
	public SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> executeAllTrafficAssignments() throws PlanItException {
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap = new TreeMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>>();
		trafficAssignments.forEach( (id,ta) -> {
			try {
				SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> results = ta.execute();
				resultsMap.put(id, results);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});	
		return resultsMap;
	}
	
}
