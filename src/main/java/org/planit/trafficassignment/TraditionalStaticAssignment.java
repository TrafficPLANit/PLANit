package org.planit.trafficassignment;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.planit.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.ShortestPathAlgorithm;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.demand.ODDemand;
import org.planit.demand.ODDemandIterator;
import org.planit.dto.BprResultDto;
import org.planit.event.RequestAccesseeEvent;
import org.planit.event.listener.InteractorListener;
import org.planit.exceptions.PlanItException;
import org.planit.gap.GapFunction;
import org.planit.gap.LinkBasedRelativeDualityGapFunction;
import org.planit.gap.StopCriterion;
import org.planit.interactor.LinkVolumeAccessee;
import org.planit.network.EdgeSegment;
import org.planit.network.Vertex;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.network.transport.TransportNetwork;
import org.planit.network.virtual.Centroid;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.utils.ArrayOperations;
import org.planit.utils.DefaultValues;
import org.planit.utils.Pair;
import org.planit.zoning.Zone;

/**
 * Traditional static assignment traffic component
 * 
 * @author gman6028
 *
 */
public class TraditionalStaticAssignment extends CapacityRestrainedAssignment implements LinkVolumeAccessee, InteractorListener {
		
	/**
	 * Mode specific data
	 */
	private class ModeData{
		
		public double[] currentNetworkSegmentFlows 	= null;
		public double[] nextNetworkSegmentFlows 	= null;
		
		ModeData(int numberOfNetworkSegments){
			this.currentNetworkSegmentFlows = (double[]) emptySegmentArray.clone();
			this.nextNetworkSegmentFlows 	= (double[]) emptySegmentArray.clone();			
		}
	}
	
	/**
	 * empty array to quickly initialize new arrays when needed 
	 */
	private double[] emptySegmentArray;
	
	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(TraditionalStaticAssignment.class.getName());
		
	/**
	 * network wide segment flows
	 */
	private double[] totalNetworkSegmentFlows = null;
	
	/**
	 * Store the mode specific data required during assignment
	 */
	private final Map<Mode,ModeData> modeSpecificData = new TreeMap<Mode,ModeData>();
	
	/** 
	 * Iteration index, tracking the iteration during execution
	 */ 
	private int iterationIndex;	
	
	/**
	 * holds the count of segments in the transport network
	 */
	private int numberOfNetworkSegments;
	/**
	 * holds the count of all vertices in the transport network
	 */
	private int numberOfNetworkVertices;
	
	private LinkBasedRelativeDualityGapFunction dualityGapFunction;
	
	public TraditionalStaticAssignment() {
		super();
	}
	
/**
 * Initialize a time period by creating the mode specific data
 * 
 * @param modes                         Set of modes to consider in the given time period 
 */
	private void initialiseTimePeriodModeData(Set<Mode> modes) {
		modeSpecificData.clear();
		for(Mode mode : modes) {
			modeSpecificData.put(mode, new ModeData(numberOfNetworkSegments));
		}
	}
	
/** 
 * Collect the updated edge segment costs for the given mode
 * 
 * @param mode                            the current mode
 * @return                                      array of updated edge segment costs
 * @throws PlanItException           thrown if there is an error
 */
	private double[] collectUpdatedCosts(Mode mode) throws PlanItException {
		double[] currentSegmentCosts = new double[transportNetwork.getTotalNumberOfEdgeSegments()];
		Iterator<ConnectoidSegment> connectoidSegmentIter = transportNetwork.connectoidSegments.iterator();			
		while(connectoidSegmentIter.hasNext()){
			ConnectoidSegment currentSegment = connectoidSegmentIter.next();
			currentSegmentCosts[(int) currentSegment.getId()] = virtualCost.calculateSegmentCost(mode, currentSegment);
		}		
		Iterator<LinkSegment> linkSegmentIter = transportNetwork.linkSegments.iterator();			
		while (linkSegmentIter.hasNext()) {
			LinkSegment currentSegment = linkSegmentIter.next();
			currentSegmentCosts[(int) currentSegment.getId()] = physicalCost.calculateSegmentCost(mode, currentSegment);
		}		
		return currentSegmentCosts;
	}
	
/**
 * Apply smoothing based on current and previous flows and the adopted smoothing method.  The smoothed results are registered as the current segment flows while the current segment flows are assigned to the previous segment flows (which are discarded).
 * 
 * @param modeData      data for the current mode
 */
	private void applySmoothing(ModeData modeData) {
		double[] smoothedSegmentFlows = smoothing.applySmoothing(modeData.currentNetworkSegmentFlows,  modeData.nextNetworkSegmentFlows, numberOfNetworkSegments);
		// update flow arrays for next iteration
		modeData.currentNetworkSegmentFlows = smoothedSegmentFlows;
	}	

/** 
 * Perform assignment for a given time period, mode and costs imposed on Dijkstra shortest path
 * 
 * @param odDemands                    origin-demand store
 * @param currentModeData            data for the current mode
 * @param networkSegmentCosts   segment costs for the network
 * @param shortestPathAlgorithm    shortest path algorithm to be used
 * @throws PlanItException              thrown if there is an error
 */
	private void executeModeTimePeriod(ODDemand odDemands, ModeData currentModeData, double[] networkSegmentCosts, ShortestPathAlgorithm shortestPathAlgorithm) throws PlanItException {
		ODDemandIterator odDemandIter = odDemands.iterator();
		
		// loop over all available OD demands
		while(odDemandIter.hasNext()) {
			double odDemand = odDemandIter.next();
			int originZoneId =  odDemandIter.getCurrentOriginId();
			int destinationZoneId =  odDemandIter.getCurrentDestinationId();
			//System.out.println("Calculating flow from origin zone " + originZoneId + " to " + destinationZoneId + " which has demand " + odDemand);
			int previousOriginZoneId = 0;
			if  (((odDemand - DefaultValues.DEFAULT_EPSILON )> 0.0) && (originZoneId != destinationZoneId)) {
				Zone currentOriginZone = null;
				Pair<Double, EdgeSegment>[] vertexPathCost = null;
				// UPDATE ORIGIN BASED: SHORTEST PATHS - ONE-TO-ALL
				TransportNetwork network  = getTransportNetwork();
				if (previousOriginZoneId != originZoneId) {
					currentOriginZone = network.zones.getZone(originZoneId-1);
					Centroid originCentroid = currentOriginZone.getCentroid();
					if (originCentroid.exitEdgeSegments.isEmpty()) {
						throw new PlanItException("Edge segments have not been assigned to Centroid " + originCentroid.getCentroidId());
					}
					vertexPathCost = shortestPathAlgorithm.executeOneToAll(originCentroid);
				}	
				// UPDATE DESTINATION ZONE
				//TODO: Costly to lookup destination zone via map whereas we know it is the next (non-zero demand) id compared to the previous)
				
				Zone currentDestinationZone = network.zones.getZone(destinationZoneId-1);
				// OD-SHORTEST PATH LOADING
				double shortestPathCost = 0;
				if (currentDestinationZone == null) {
					throw new PlanItException("currentDestinationZone is null for destinationZoneId = " + destinationZoneId);
				}
				Vertex currentPathStartVertex = currentDestinationZone.getCentroid();
				//System.out.println("currentPathStartVertex.getExternalId() = " + currentPathStartVertex.getExternalId() + " currentPathStartVertex.getId() = " + currentPathStartVertex.getId() + " currentPathStartVertex.getCentroidId() = " +  currentDestinationZone.getCentroid().getCentroidId());
				while (currentPathStartVertex.getId() != currentOriginZone.getCentroid().getId()) {	
					int startVertexId = (int) currentPathStartVertex.getId();
//TODO - commented-out lines below would be needed if vertexPathCost array is sorted by node external Id
					//int startVertexId = 0;
					//while ((vertexPathCost[startVertexId].getSecond() != null) &&   (vertexPathCost[startVertexId].getSecond().getDownstreamVertex().getId() != currentPathStartVertex.getId())) {
					//	startVertexId++;
					//}
					if (vertexPathCost[startVertexId].getSecond() == null) {
						throw new PlanItException("The solution could not find an Edge Segment for vertex " + startVertexId + " which has external reference " + currentPathStartVertex.getExternalId());
					}
					EdgeSegment currentEdgeSegment = vertexPathCost[startVertexId].getSecond();
					double edgeSegmentCost = networkSegmentCosts[(int) currentEdgeSegment.getId()];
					shortestPathCost += edgeSegmentCost;
					currentModeData.nextNetworkSegmentFlows[(int) currentEdgeSegment.getId()] += odDemand;											
					currentPathStartVertex = currentEdgeSegment.getUpstreamVertex();	
				}
				dualityGapFunction.increaseConvexityBound(odDemand*shortestPathCost);	
				previousOriginZoneId = originZoneId;
			}
		}
	}	
	
	private double[] getTotalNetworkSegmentCosts(Set<Mode> modes) throws PlanItException {
		double[] totalNetworkSegmentCosts = new double[numberOfNetworkSegments];
		for(Mode mode : modes) {
			double[] networkSegmentCostPerMode = collectUpdatedCosts(mode);
			for (int i=0; i<numberOfNetworkSegments; i++) {
				totalNetworkSegmentCosts[i] += networkSegmentCostPerMode[i];
			}			
		}
		return totalNetworkSegmentCosts;
	}
			
/** Perform assignment for a given time period using Dijkstra's algorithm
 * 
 * @param timePeriod            the time period for the current assignment
 * @param modes                  the modes for the current assignment
 * @throws PlanItException   thrown if there is an error
 */
	private void executeTimePeriod(TimePeriod timePeriod, Set<Mode> modes) throws PlanItException {
		initialiseTimePeriodModeData(modes);	
		boolean hasConverged = false;
		totalNetworkSegmentFlows = (double[])emptySegmentArray.clone();		
		
		double[] totalNetworkSegmentCosts = getTotalNetworkSegmentCosts(modes);
		
		while (!hasConverged) {
			dualityGapFunction.reset();
			smoothing.update(iterationIndex);			
			
			// NETWORK LOADING - PER MODE
			totalNetworkSegmentFlows = (double[])emptySegmentArray.clone();
			for(Mode mode : modes) {
				// mode specific data
				ModeData currentModeData = modeSpecificData.get(mode);		
				currentModeData.nextNetworkSegmentFlows = (double[])emptySegmentArray.clone();		
				// AON based network loading
				ShortestPathAlgorithm shortestPathAlgorithm = new DijkstraShortestPathAlgorithm(totalNetworkSegmentCosts, numberOfNetworkSegments, numberOfNetworkVertices);
				ODDemand odDemands = demands.get(mode, timePeriod);
				executeModeTimePeriod(odDemands, currentModeData, totalNetworkSegmentCosts, shortestPathAlgorithm);
				
				double sumProduct = ArrayOperations.dotProduct(currentModeData.currentNetworkSegmentFlows, totalNetworkSegmentCosts, numberOfNetworkSegments);
				dualityGapFunction.increaseActualSystemTravelTime(sumProduct);
				applySmoothing(currentModeData);
				// aggregate smoothed mode specific flows - for cost computation				
				ArrayOperations.addTo(totalNetworkSegmentFlows, currentModeData.currentNetworkSegmentFlows, numberOfNetworkSegments);
				modeSpecificData.put(mode, currentModeData);
			}				

			totalNetworkSegmentCosts = getTotalNetworkSegmentCosts(modes);
			dualityGapFunction.computeGap();
		    iterationIndex++;			
		    LOGGER.info("Iteration " + iterationIndex + ": duality gap = " + dualityGapFunction.getGap());
		    hasConverged = dualityGapFunction.hasConverged(iterationIndex);
		} 		
	}
	
	/* (non-Javadoc)
	 * @see org.planit.trafficassignment.TrafficAssignment#initialiseBeforeEquilibration()
	 */
	
	@Override
	public void initialiseBeforeEquilibration() {
		// initialize members that are used throughout the assignment
		iterationIndex = 0;
		this.numberOfNetworkSegments = getTransportNetwork().getTotalNumberOfEdgeSegments();
		this.numberOfNetworkVertices = getTransportNetwork().getTotalNumberOfVertices();
		this.emptySegmentArray = new double[numberOfNetworkSegments];
	}	
	
	/**
	 * Execute assignment 
	 * 
	 * @throws PlanItException     thrown if there is an error
	 */
	@Override
	public SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> executeEquilibration() throws PlanItException {
	SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> results = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>();
		// perform assignment per period - per mode
		Set<TimePeriod> timePeriods = demands.getRegisteredTimePeriods();
		System.out.println("There are " + timePeriods.size() + " time periods to loop through.");
		BPRLinkTravelTimeCost bprLinkTravelTimeCost = (BPRLinkTravelTimeCost) physicalCost;
		for(TimePeriod timePeriod : timePeriods) {
			SortedMap<Mode, SortedSet<BprResultDto>> resultsForCurrentTimePeriod = new TreeMap<Mode, SortedSet<BprResultDto>>();
			LOGGER.info("Equilibrating time period "+ timePeriod.toString());
			Set<Mode> modes = demands.getRegisteredModesForTimePeriod(timePeriod);
			executeTimePeriod(timePeriod,modes);			
			double[] totalNetworkSegmentCosts = getTotalNetworkSegmentCosts(modes);
			Iterator<LinkSegment> linkSegmentIter = getTransportNetwork().linkSegments.iterator();	
			for (Mode mode : modes) {
				SortedSet<BprResultDto> resultsForCurrentModeAndTimePeriod = new TreeSet<BprResultDto>();  //TreeSet implements SortedSet so stores results in order
				double totalCost = 0.0;
				while (linkSegmentIter.hasNext()) {
					MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) linkSegmentIter.next();
					int id = (int) linkSegment.getId();
					if (totalNetworkSegmentFlows[id] > 0.0) {
						double cost = totalNetworkSegmentCosts[id];
						totalCost += totalNetworkSegmentFlows[id] * cost;
						BprResultDto bprResultDto = new BprResultDto(linkSegment.getUpstreamVertex().getExternalId(), 
								                                                                        linkSegment.getDownstreamVertex().getExternalId(), 
								                                                                        totalNetworkSegmentFlows[id], 
								                                                                        cost, 
								                                                                        totalCost,
								                                                                        linkSegment.getLinkSegmentType().getCapacityPerLane() * linkSegment.getNumberOfLanes(),
								                                                                        linkSegment.getParentLink().getLength(),
								                                                                        linkSegment.getMaximumSpeed(),
								                                                                        bprLinkTravelTimeCost.getAlpha(linkSegment),
								                                                                        bprLinkTravelTimeCost.getBeta(linkSegment));
						resultsForCurrentModeAndTimePeriod.add(bprResultDto);
					}
				}
				if (!resultsForCurrentModeAndTimePeriod.isEmpty()) {
					resultsForCurrentTimePeriod.put(mode, resultsForCurrentModeAndTimePeriod);
				}
			}
			if (!resultsForCurrentTimePeriod.isEmpty()) {
				results.put(timePeriod, resultsForCurrentTimePeriod);
			}
		}
		return results;
	}
	
	/* (non-Javadoc)
	 * @see org.planit.interactor.LinkVolumeInteractor.LinkVolumeAccessee#getLinkSegmentFlows()
	 */
	@Override
	public double[] getLinkSegmentFlows() {
		return totalNetworkSegmentFlows;
	}
	
	/* (non-Javadoc)
	 * @see org.planit.interactor.LinkVolumeInteractor.LinkVolumeAccessee#getNumberOfLinkSegments()
	 */
	@Override
	public int getNumberOfLinkSegments() {
		return getTransportNetwork().getTotalNumberOfLinkSegments();
	}	
		
	@Override
	public void onRequestInteractorEvent(RequestAccesseeEvent event) {
		if (event.getSourceAccessor().getRequestedAccessee().equals(LinkVolumeAccessee.class)) {
			event.getSourceAccessor().setAccessee(this);
		}
	}
	
	protected GapFunction createGapFunction() {
		dualityGapFunction = new LinkBasedRelativeDualityGapFunction(new StopCriterion());
		return dualityGapFunction;
	}
	
	public GapFunction getGapFunction() {
		return dualityGapFunction;
	}

}