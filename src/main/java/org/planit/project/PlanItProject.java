package org.planit.project;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.SortedSet;

import org.planit.demand.Demands;
import org.planit.dto.BprResultDto;
import org.planit.network.physical.Node;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Centroid;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.userclass.Mode;
import org.planit.zoning.Zoning;
import org.planit.event.EventHandler;
import org.planit.event.EventManager;
import org.planit.exceptions.PlanItException;
import org.planit.geo.utils.PlanitGeoUtils;

/**
 * Main class that hosts a single project that can consist of multiple networks, demands and traffic assignments all based on
 * a single configuration (user classes, modes etc.)
 * @author markr
 *
 */

public class PlanItProject implements EventHandler {
	
	/**
	 * The physical networks registered on this project
	 */
	private TreeMap<Long, PhysicalNetwork> physicalNetworks;
	
	/**
	 * The zoning(s) registered on this project
	 */
	private TreeMap<Long, Zoning> zonings;
	
	/**
	 * The traffic assignment(s) registered on this project
	 */
	private TreeMap<Long, TrafficAssignment> trafficAssignments;
	private TrafficAssignmentComponentFactory<PhysicalNetwork> physicalNetworkFactory;
	private TrafficAssignmentComponentFactory<Zoning> zoningFactory;
	private TrafficAssignmentComponentFactory<Demands> demandsFactory;
	private TrafficAssignmentComponentFactory<NetworkLoading> assignmentFactory;

	public void setEventManager(EventManager eventManager) {
		trafficAssignments = new TreeMap<Long,TrafficAssignment>();
		physicalNetworks = new TreeMap<Long,PhysicalNetwork>();
		zonings = new TreeMap<Long,Zoning>();
		
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
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public PhysicalNetwork createAndRegisterPhysicalNetwork(String physicalNetworkType) throws InstantiationException, 
																				                                                                                  IllegalAccessException, 
																				                                                                                  IllegalArgumentException, 
																				                                                                                  InvocationTargetException, 
																				                                                                                  NoSuchMethodException, 
																				                                                                                  SecurityException, 
																				                                                                                  ClassNotFoundException, 
																				                                                                                  PlanItException,
	                                                                                                                                                              IOException {
		PhysicalNetwork thePhysicalNetwork = physicalNetworkFactory.create(physicalNetworkType);
		physicalNetworks.put(thePhysicalNetwork.getId(), thePhysicalNetwork);
		return thePhysicalNetwork;
	}
	
	/** Create and register the zoning system on the network
	 * @param zoningType
	 * @return theCreatedZoning
	 * @throws PlanItException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public Zoning createAndRegisterZoning() throws InstantiationException, 
												                                     IllegalAccessException, 
												                                     IllegalArgumentException, 
												                                     InvocationTargetException, 
												                                     NoSuchMethodException, 
												                                     SecurityException, 
												                                     ClassNotFoundException, 
												                                     PlanItException,
	                                                                                 IOException {
		Zoning theZoning = zoningFactory.create(Zoning.class.getCanonicalName());
		zonings.put(theZoning.getId(), theZoning);
		return theZoning;		
	}	
	
	/** create and register demands to the project
	 * @param demands type, to register
	 * @return theCreatedDemands
	 * @throws PlanItException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public Demands createAndRegisterDemands() throws InstantiationException, 
														                                        IllegalAccessException, 
														                                        IllegalArgumentException, 
														                                        InvocationTargetException, 
														                                        NoSuchMethodException, 
														                                        SecurityException, 
														                                        ClassNotFoundException, 
														                                        PlanItException,
	                                                                                            IOException {
		return demandsFactory.create(Demands.class.getCanonicalName());
	}	
	
	
	/** Factory method to create a deterministic traffic assignment instance of a given type
	 * @param trafficAssignmentType
	 * @return TrafficAssignment
	 * @throws PlanItException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public DeterministicTrafficAssignment  createAndRegisterDeterministicAssignment(String trafficAssignmentType) throws PlanItException, 
																												                                                                                           InstantiationException, 
																												                                                                                           IllegalAccessException, 
																												                                                                                           IllegalArgumentException, 
																												                                                                                           InvocationTargetException, 
																												                                                                                           NoSuchMethodException, 
																												                                                                                           SecurityException, 
																												                                                                                           ClassNotFoundException,
							                                                                                                                                                                               IOException {
		NetworkLoading networkLoadingAndAssignment = (NetworkLoading) assignmentFactory.create(trafficAssignmentType);
		if  (!(networkLoadingAndAssignment instanceof DeterministicTrafficAssignment))  {
			throw new PlanItException("Traffic assignment type is not a valid assignment type");
		}	
		DeterministicTrafficAssignment trafficAssignment = (DeterministicTrafficAssignment) networkLoadingAndAssignment;
		trafficAssignments.put(trafficAssignment.getId(), trafficAssignment);		
		return trafficAssignment;
	}	
	
	/**
	 * Execute all registered traffic assignments
	 */
	public SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> executeAllTrafficAssignmentsUsingGeometryId(PlanitGeoUtils planitGeoUtils) {
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap = new TreeMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>>();
		trafficAssignments.forEach( (id,ta) -> {
			try {
				SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> results = ta.executeUsingGeometryId(planitGeoUtils);
				resultsMap.put(id, results);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});		
		return resultsMap;
	}
	
	public SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> executeAllTrafficAssignmentsUsingGeometryId(double connectoidLength) {
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap = new TreeMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>>();
		trafficAssignments.forEach( (id,ta) -> {
			try {
				SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> results = ta.executeUsingGeometryId(connectoidLength);
				resultsMap.put(id, results);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});	
		return resultsMap;
	}
	
	public SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> executeAllTrafficAssignmentsUsingCentroidMap(SortedMap<Centroid, Node> centroidMap, double connectoidLength) {
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap = new TreeMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>>();
		trafficAssignments.forEach( (id,ta) -> {
			try {
				SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> results = ta.executeUsingCentroidMap(centroidMap, connectoidLength);
				resultsMap.put(id, results);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});		
		return resultsMap;
	}
	
	public SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> executeAllTrafficAssignmentsUsingGeometryId(SortedMap<Long, TrafficAssignment> trafficAssignments, PlanitGeoUtils planitGeoUtils) {
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap = new TreeMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>>();
		trafficAssignments.forEach( (id,ta) -> {
			try {
				SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> results = ta.executeUsingGeometryId(planitGeoUtils);
				resultsMap.put(id, results);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});		
		return resultsMap;
	}

	public SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> executeAllTrafficAssignmentsUsingGeometryId(SortedMap<Long, TrafficAssignment> trafficAssignments, double connectoidLength) {
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap = new TreeMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>>();
		trafficAssignments.forEach( (id,ta) -> {
			try {
				SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> results = ta.executeUsingGeometryId(connectoidLength);
				resultsMap.put(id, results);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});		
		return resultsMap;
	}

	public SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> executeAllTrafficAssignmentsUsingCentroidMap(SortedMap<Long, TrafficAssignment> trafficAssignments, SortedMap<Centroid, Node> centroidMap, double connectoidLength) {
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap = new TreeMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>>();
		trafficAssignments.forEach( (id,ta) -> {
			try {
				SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> results = ta.executeUsingCentroidMap(centroidMap, connectoidLength);
				resultsMap.put(id, results);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});		
		return resultsMap;
	}

}
