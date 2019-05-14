package org.planit.zoning;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
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
 * @return             the zone added
 */
        protected Zone registerZone(@Nonnull Zone zone) {
            return zoneMap.put(zone.getCentroid().getOdPos(), zone);
        }
        
 /**
  * Returns a zone specified by its external Id
  * 
  * @param externalId      the external Id of the specified zone
  * @return                      the retrieved zone object
  */
        public Zone getZoneByExternalId(long externalId) {
            for (Zone zone : zoneMap.values()) {
                if (zone.getExternalId() == externalId) {
                    return zone;
                }
            }
            return null;
        }

/**
 * Returns iterator through the zones
 * 
 * @return          iterator through the zones
 */
        @Override
        public Iterator<Zone> iterator() {
            return zoneMap.values().iterator();
        }       
        
/** 
 * Create and register new zone to network identified via its id
 * 
 * @param odPos          row/column of the OD matrix this zone/centroid corresponds to
 * @param externalId     external Id of this zone
 * @return                     the new zone created
 */
        public Zone createAndRegisterNewZone(long odPos, long externalId) {
            Zone newZone = new Zone(odPos, externalId);
            registerZone(newZone);
            virtualNetwork.centroids.registerCentroid(newZone.getCentroid());
            return newZone;
        }  
        
/**
 * Retrieve zone by its position in the OD matrix 
 * 
 * @param odPos       the row/column in the OD matrix for the zone to be retrieved
 * @return zone         the zone retrieved
 */
        public Zone getZone(long odPos) {
            return zoneMap.get(odPos);
        }       
        
/** Collect number of zones on the zoning
 * 
 * @return      the number of zones in this zoning
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
     * Map storing all the zones by their row/column in the OD matrix
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
 * @return          id for this zoning
 */
    public long getId() {
        return this.id;
    }
    
/**
 * Get the virtual network for this zoning
 * 
 * @return          the virtual network for this zoning
 */
    public VirtualNetwork getVirtualNetwork() {
        return this.virtualNetwork;
    }

}