package org.planit.network.virtual;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.Node;

/**
 * Model free virtual network which is part of the zoning and holds all the virtual infrastructure connecting the zones
 * to the physical road network. 
 * 
 * @author markr
 */
public class VirtualNetwork {
	
	// INNER CLASSES
	
	/**
	 * Internal class for all Connectoid specific code
	 *
	 */
	public class Connectoids implements Iterable<Connectoid> {
		
		/** Add connectoid to the internal container
		 * @return connectoid, in case it overrides an existing connectoid, the removed connectoid is returned
		 */
		protected Connectoid registerConnectoid(@Nonnull Connectoid connectoid) {
			return connectoidMap.put(connectoid.getId(),connectoid);
		}
		
		@Override
		public Iterator<Connectoid> iterator() {
			return connectoidMap.values().iterator();
		}		
		
		/** create new connectoid to network identified via its id
		 * @return connectoid, new connectoid
		 * @throws PlanItException 
		 */
		public Connectoid registerNewConnectoid(Centroid centroid, Node node, double length) throws PlanItException {
			Connectoid newConnectoid = new Connectoid(centroid, node, length);
			registerConnectoid(newConnectoid);
			return newConnectoid;
		}
		
		/**
		 * Collect link by id 
		 * @param id
		 * @return link
		 */
		public Connectoid getConnectoid(long id) {
			return connectoidMap.get(id);
		}		
		
		/** Collect number of connectoids on the network
		 * @return numberOfConnectoids
		 */
		public int getNumberOfConnectoids() {
			return connectoidMap.size();			
		}		
	}	
		
	/**
	 * Internal class for non-physical LinkSegment specific code, i.e. connectoid segments 
	 * (physical link segments are placed in the network)
	 *
	 */
	public class ConnectoidSegments implements Iterable<ConnectoidSegment> {
		
		/** Register a link segment on the zoning
		 * @param connectoidSegment
		 */
		protected ConnectoidSegment registerConnectoidSegment(@Nonnull ConnectoidSegment connectoidSegment) {	
			return connectoidSegmentMap.put(connectoidSegment.getId(), connectoidSegment);
		}
		
		@Override
		public Iterator<ConnectoidSegment> iterator() {
			return connectoidSegmentMap.values().iterator();
		}		
		
		/** Create and register connectoid segment in AB direction on virtual network
		 * @param parentConnectoid
		 * @param directionAB
		 * @return created connectoid segment
		 * @throws PlanItException 
		 */
		public ConnectoidSegment createAndRegisterConnectoidSegment(@Nonnull Connectoid parentConnectoid, boolean directionAB) throws PlanItException {
			ConnectoidSegment connectoidSegment = new ConnectoidSegment(parentConnectoid, directionAB);
			parentConnectoid.registerConnectoidSegment(connectoidSegment, directionAB);
			registerConnectoidSegment(connectoidSegment);
			return connectoidSegment;
		}			
		
		
		/** Create and register connectoidSegment in AB direction on virtual network
		 * @param parentConnectoid
		 * @return created connectoid Segment
		 * @throws PlanItException 
		 */
		public ConnectoidSegment createAndRegisterConnectoidSegmentAB(@Nonnull Connectoid parentConnectoid) throws PlanItException {
			return createAndRegisterConnectoidSegment(parentConnectoid, true /*abDirection*/);
		}
		

		/** Create and register connectoidSegment in BA direction on zoning
		 * @param parentConnectoid
		 * @return connectoidSegment
		 * @throws PlanItException 
		 */
		public ConnectoidSegment createAndRegisterConnectoidSegmentBA(Connectoid parentConnectoid) throws PlanItException {
			return createAndRegisterConnectoidSegment(parentConnectoid, false /*baDirection*/);
		}		
		
		/**
		 * Collect connectoid segment by id 
		 * @param id
		 * @return connectoidSegment
		 */
		public ConnectoidSegment getConnectoidSegment(long id) {
			return connectoidSegmentMap.get(id);
		}			
		
		public int getNumberOfConnectoidSegments() {
			return connectoidSegmentMap.size();			
		}		
	}	
	
		
	/**
	 * Internal class for all Centroid specific code
	 *
	 */
	public class Centroids implements Iterable<Centroid> {
		
		/** Add centroid to the internal container
		 * @return centroid, in case it overrides an existing centroid, the removed centroid is returned
		 */	
		public Centroid registerCentroid(@Nonnull Centroid centroid) {
			return centroidMap.put(centroid.getId(),centroid);
		}		
		
		@Override
		public Iterator<Centroid> iterator() {
			return centroidMap.values().iterator();
		}
		
		public Centroid findCentroidByExternalId(long externalId) {
			for (Centroid centroid : centroidMap.values()) {
				if (centroid.getExternalId() == externalId) {
					return centroid;
				}
			}
			return null;
		}
		
		public int getNumberOfCentroids() {
			return centroidMap.size();
		}
	}	
	
	// Protected
	
	// for now use tree map to ensure non-duplicate keys until we add functionality to account for this (treemap is slower than hashmap)
	protected Map<Long, Connectoid> connectoidMap = new TreeMap<Long, Connectoid>();
	protected Map<Long, ConnectoidSegment> connectoidSegmentMap = new TreeMap<Long, ConnectoidSegment>();
	protected Map<Long, Centroid> centroidMap = new TreeMap<Long, Centroid>();
								
	// PUBLIC
	
	/**
	 * internal class instance containing all connectoid specific functionality
	 */
	public final Connectoids connectoids = new Connectoids();
	/**
	 * internal class instance containing all  connectoid segment specific functionality
	 */	
	public final ConnectoidSegments connectoidSegments = new ConnectoidSegments();
	/**
	 * internal class instance containing all centroid specific functionality
	 */		
	public final Centroids centroids = new Centroids();
	
}	

