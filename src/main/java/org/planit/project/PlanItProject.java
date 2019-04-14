package org.planit.project;

import java.util.TreeMap;
import java.util.SortedMap;
import java.util.SortedSet;

import org.planit.demand.Demands;
import org.planit.dto.BprResultDto;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.userclass.Mode;
import org.planit.zoning.Zoning;
import org.planit.event.EventManager;
import org.planit.event.SimpleEventManager;
import org.planit.event.listener.InputBuilderListener;
import org.planit.event.listener.OutputBuilderListener;
import org.planit.exceptions.PlanItException;
import org.planit.exceptions.PlanItIncompatibilityException;
import org.planit.geo.utils.PlanitGeoUtils;

/**
 * The main class which hosts a single project that can consist of multiple networks, demands and traffic assignments all based on
 * a single configuration (user classes, modes etc.)
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
	
	/**
	 * Object Factory classes
	 */
	private TrafficAssignmentComponentFactory<PhysicalNetwork> physicalNetworkFactory;
	private TrafficAssignmentComponentFactory<Zoning> zoningFactory;
	private TrafficAssignmentComponentFactory<Demands> demandsFactory;
	private TrafficAssignmentComponentFactory<NetworkLoading> assignmentFactory;
	
	public PlanItProject(InputBuilderListener inputBuilderListener, OutputBuilderListener outputBuilderListener) {
		EventManager eventManager = new SimpleEventManager();
		eventManager.addEventListener(inputBuilderListener);
		eventManager.addEventListener(outputBuilderListener);
		
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
	
	/** Add a network to the project, if a network with the same id already exists the earlier network is replaced and returned (otherwise null)
	 * @param network, to register
	 * @return theCreatedNetwork
	 * @throws PlanItException 
	 */
	public PhysicalNetwork createAndRegisterPhysicalNetwork(String physicalNetworkType) throws PlanItException {
		PhysicalNetwork thePhysicalNetwork = physicalNetworkFactory.create(physicalNetworkType);
		physicalNetworks.put(thePhysicalNetwork.getId(), thePhysicalNetwork);
		return thePhysicalNetwork;
	}
	
	/** Create and register the zoning system on the network
	 * @param zoningType
	 * @return theCreatedZoning
	 * @throws PlanItException 
	 */
	public Zoning createAndRegisterZoning() throws PlanItException {
		Zoning theZoning = zoningFactory.create(Zoning.class.getCanonicalName());
		zonings.put(theZoning.getId(), theZoning);
		return theZoning;		
	}	
	
	/** create and register demands to the project
	 * @param demands type, to register
	 * @return theCreatedDemands
	 * @throws PlanItException 
	 */
	public Demands createAndRegisterDemands() throws PlanItException {
		Demands demands = demandsFactory.create(Demands.class.getCanonicalName());
		demandsMap.put(demands.getId(), demands);
		return demands;
	}	
	
	
	/** Factory method to create a deterministic traffic assignment instance of a given type
	 * @param trafficAssignmentType
	 * @return TrafficAssignment
	 * @throws PlanItException 
	 */
	public DeterministicTrafficAssignment  createAndRegisterDeterministicAssignment(String trafficAssignmentType) throws PlanItException {
		NetworkLoading networkLoadingAndAssignment = (NetworkLoading) assignmentFactory.create(trafficAssignmentType);
		if  (!(networkLoadingAndAssignment instanceof DeterministicTrafficAssignment))  {
			throw new PlanItException("Traffic assignment type is not a valid assignment type");
		}	
		DeterministicTrafficAssignment trafficAssignment = (DeterministicTrafficAssignment) networkLoadingAndAssignment;
		trafficAssignments.put(trafficAssignment.getId(), trafficAssignment);		
		return trafficAssignment;
	}	
	
	public Demands getDemands(long id) {
		return demandsMap.get(id);
	}
	
	public TrafficAssignment getTrafficAssignment(long id) {
		return trafficAssignments.get(id);
	}
	
	public Zoning getZoning(long id) {
		return zonings.get(id);
	}
	
	public PhysicalNetwork getPhysicalNetwork(long id) {
		return physicalNetworks.get(id);
	}
	
	/**
	 * Execute all registered traffic assignments
	 * @throws PlanItIncompatibilityException 
	 * @throws PlanItException 
	 */
	public SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> executeAllTrafficAssignments(PlanitGeoUtils planitGeoUtils) {
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap = new TreeMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>>();
		trafficAssignments.forEach( (id,ta) -> {
			try {
				SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> results = ta.execute(planitGeoUtils);
				resultsMap.put(id, results);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});		
		return resultsMap;
	}
	
	public SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> executeAllTrafficAssignments(double connectoidLength) {
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap = new TreeMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>>();
		trafficAssignments.forEach( (id,ta) -> {
			try {
				SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> results = ta.execute(connectoidLength);
				resultsMap.put(id, results);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});	
		return resultsMap;
	}
}
