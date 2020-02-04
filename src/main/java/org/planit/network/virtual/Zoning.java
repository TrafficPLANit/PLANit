package org.planit.network.virtual;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.Zone;

/**
 * Zoning class which holds a particular zoning
 *
 * @author markr
 *
 */
public class Zoning extends TrafficAssignmentComponent<Zoning> implements Serializable {

    /** generated UID */
	private static final long serialVersionUID = -2986366471146628179L;

	// register to be eligible in PLANit
    static {
        try {
            TrafficAssignmentComponentFactory.registerTrafficAssignmentComponentType(Zoning.class);
        } catch (final PlanItException e) {
            e.printStackTrace();
        }
    }

	/**
     * Internal class for all zone specific code
     *
     */
	public class Zones {

        /**
         * Add zone to the internal container.
         *
         * @param zone
         *            the zone to be added to this Zoning
         * @return the zone added
         */
        protected Zone registerZone(@Nonnull final Zone zone) {
            return zoneMap.put(zone.getId(), zone);
        }

        /**
         * Returns a zone specified by its external Id
         *
         * @param externalId
         *            the external Id of the specified zone
         * @return the retrieved zone object
         */
        public Zone getZoneByExternalId(final long externalId) {
            for (final Zone zone : zoneMap.values()) {
                if (zone.getExternalId() == externalId) {
                    return zone;
                }
            }
            return null;
        }

        /**
         * Returns a List of registered Zones
         *
         * @return List of registered Zone objects
         */
        public List<Zone> toList() {
        	return new ArrayList<Zone>(zoneMap.values());
        }

        /**
         * Create and register new zone to network identified via its id
         *
         * @param externalId
         *            external Id of this zone
         * @return the new zone created
         */
        public Zone createAndRegisterNewZone(final long externalId) {
        	final ZoneImpl newZone = new ZoneImpl(externalId);
            final Centroid centroid = virtualNetwork.centroids.registerNewCentroid(newZone);
            newZone.setCentroid(centroid);
            registerZone(newZone);
            return newZone;
        }

        /**
         * Retrieve zone by its Id
         *
         * @param id
         *            the id of the zone
         * @return zone the zone retrieved
         */
        public Zone getZone(final long id) {
            return zoneMap.get(id);
        }

        /**
         * Collect number of zones on the zoning
         *
         * @return the number of zones in this zoning
         */
        public int getNumberOfZones() {
            return zoneMap.size();
        }
    }

    // Protected

    /**
     * unique identifier for this zoning
     */
    protected final long id;

    /**
     * Map storing all the zones by their row/column in the OD matrix
     */
    protected final Map<Long, Zone> zoneMap = new TreeMap<Long, Zone>();

    /**
     * Virtual network holds all the virtual connections to the physical network
     */
    protected final VirtualNetwork virtualNetwork = new VirtualNetwork();

    // Public

    /**
     * provide access to zones of this zoning
     */
    public final Zones zones = new Zones();

    /**
     * Constructor
     */
    public Zoning() {
        super();
        this.id = IdGenerator.generateId(Zoning.class);
    }

    // Public - getters - setters

   /**
    * #{@inheritDoc}
    */
    @Override
	public long getId() {
        return this.id;
    }

    /**
     * Get the virtual network for this zoning
     *
     * @return the virtual network for this zoning
     */
    public VirtualNetwork getVirtualNetwork() {
        return this.virtualNetwork;
    }

}