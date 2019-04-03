package org.planit.zoning;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.planit.network.virtual.Centroid;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.IdGenerator;

/**
 * Zoning class which holds a particular zoning
 * 
 * @author markr
 *
 */
public class Zoning extends TrafficAssignmentComponent<Zoning> {
	
	/**
	 * Internal class for all zone specific code
	 *
	 */
	public class Zones implements Iterable<Zone> {
		
		/** Add zone to the internal container
		 * @return zone, in case it overrides an existing zone, the removed zone is returned
		 */
		protected Zone registerZone(@Nonnull Zone zone) {
			return zoneMap.put(zone.getId(),zone);
		}
		
		@Override
		public Iterator<Zone> iterator() {
			return zoneMap.values().iterator();
		}		
		
		/** create new zone to network identified via its id
		 * @param centroid, of the zone
		 * @return zone, new zone
		 */
		public Zone registerNewZone(Centroid centroid) {
			Zone newZone = new Zone(centroid);
			registerZone(newZone);
			return newZone;
		}			
		
		/**
		 * Collect zone by id 
		 * @param id
		 * @return zone
		 */
		public Zone getZone(long id) {
			return zoneMap.get(id);
		}		
		
		/** Collect number of zones on the zoning
		 * @return
		 */
		public int getNumberOfZones() {
			return zoneMap.size();			
		}
	}	
			

	// Protected
	
	/**
	 * unique identifier for this zoning
	 */
	protected long id;
	
	/**
	 * Holds all the zones
	 */
	protected Map<Long, Zone> zoneMap = new TreeMap<Long, Zone>();

	/**
	 * Virtual network holds all the virtual connections to the physical network
	 */
	protected final VirtualNetwork virtualNetwork = new VirtualNetwork();
			
	// Public
	
	/**
	 * provide access to zones of this zoning
	 */
	public Zones zones = new Zones();
	
	/**
	 * Constructor
	 */
	public Zoning() {
		super();
		this.id = IdGenerator.generateId(Zoning.class);
	}
		

	// Public - getters - setters
	
	public long getId() {
		return this.id;
	}
	
	public VirtualNetwork getVirtualNetwork() {
		return this.virtualNetwork;
	}

}
