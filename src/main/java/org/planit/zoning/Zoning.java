package org.planit.zoning;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

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
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(Zoning.class.getName());
        
	/**
	 * Internal class for all zone specific code
	 *
	 */
	public class Zones implements Iterable<Zone> {
		
        /** 
         * Add zone to the internal container.  
         * 
         * @param zone    the zone to be added to this Zoning
         * @return		       the zone added
         */
		protected Zone registerZone(@Nonnull Zone zone) {
		    return zoneMap.put(zone.getId(),zone);
		}

        /**
         * Returns iterator through the zones
         * 
         * @return			iterator through the zones
         */
		@Override
		public Iterator<Zone> iterator() {
			return zoneMap.values().iterator();
		}		
		
        /** 
         * Create and register new zone to network identified via its id
         * 
         * @param externalZoneId            the externalId of the zone (mandatory)
         * @return 							the new zone created
         */
		public Zone createAndRegisterNewZone(long externalZoneId) {
			Zone newZone = new Zone(externalZoneId);
			registerZone(newZone);
			virtualNetwork.centroids.registerCentroid(newZone.getCentroid());
			return newZone;
		}			
		
        /**
         * Retrieve zone by id 
         * 
         * @param id			the id for the zone to be retrieved
         * @return zone			the zone retrieved
         */
		public Zone getZone(long id) {
			return zoneMap.get(id);
		}		
		
        /** Collect number of zones on the zoning
         * 
         * @return		the number of zones in this zoning
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
	
    /**
     * Get the id for this zoning
     * 
     * @return			id for this zoning
     */
	public long getId() {
		return this.id;
	}
	
    /**
     * Get the virtual network for this zoning
     * 
     * @return			the virtual network for this zoning
     */
	public VirtualNetwork getVirtualNetwork() {
		return this.virtualNetwork;
	}

}