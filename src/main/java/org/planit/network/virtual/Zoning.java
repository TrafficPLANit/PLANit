package org.planit.network.virtual;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.demands.Demands;
import org.planit.network.physical.PhysicalNetwork.Modes;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.time.TimePeriod;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Mode;
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

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(Zoning.class.getCanonicalName());

  /**
   * Internal class for all zone specific code
   *
   */
  public class Zones implements Iterable<Zone> {

    /**
     * Add zone to the internal container.
     *
     * @param zone the zone to be added to this Zoning
     * @return the zone added
     */
    protected Zone registerZone(final Zone zone) {
      return zoneMap.put(zone.getId(), zone);
    }

    /**
     * Create and register new zone to network identified via its id
     *
     * @param externalId external Id of this zone
     * @return the new zone created
     */
    public Zone createAndRegisterNewZone(final Object externalId) {
      final ZoneImpl newZone = new ZoneImpl(groupId, externalId);
      final Centroid centroid = virtualNetwork.centroids.registerNewCentroid(newZone);
      newZone.setCentroid(centroid);
      registerZone(newZone);
      return newZone;
    }

    /**
     * Retrieve zone by its Id
     *
     * @param id the id of the zone
     * @return zone the zone retrieved
     */
    public Zone getZoneById(final long id) {
      return zoneMap.get(id);
    }

    /**
     * Collect iterator for all registered zones (non-ordered)
     * 
     * @return iterator
     */
    @Override
    public Iterator<Zone> iterator() {
      return zoneMap.values().iterator();
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
   * Map storing all the zones by their row/column in the OD matrix
   */
  protected final Map<Long, Zone> zoneMap = new TreeMap<Long, Zone>();

  /**
   * Virtual network holds all the virtual connections to the physical network
   */
  protected final VirtualNetwork virtualNetwork;

  // Public

  /**
   * provide access to zones of this zoning
   */
  public final Zones zones = new Zones();

  /**
   * Constructor
   * 
   * The second id generation token should be the token used by the physical network to create physical network entities such as links, nodes, etc. The virtual network should
   * register connectoids, centroids, etc. with ids compatible under this same network. For example, a centroid is a vertex, like a node, so the vertex ids should be contiguous and
   * unique throughout the combination of the virtual and physical network. Hence, they should use the same network id token
   * 
   * @param groupId        contiguous id generation within this group for instances of this class
   * @param networkGroupId contiguous id generation for all instances created by the virtual network
   */
  public Zoning(IdGroupingToken groupId, IdGroupingToken networkGroupId) {
    super(groupId, Zoning.class);
    virtualNetwork = new VirtualNetwork(networkGroupId);
  }

  // Public - getters - setters

  /**
   * Get the virtual network for this zoning
   *
   * @return the virtual network for this zoning
   */
  public VirtualNetwork getVirtualNetwork() {
    return this.virtualNetwork;
  }

  /**
   * Verify if passed in demands are compatible with the zoning structure. Compatibility is ensured when the number of zones matches the number of origins/destinations in the
   * demands.
   * 
   * @param demands to verify against
   * @param modes   to check
   * @return true when compatible, false otherwise
   */
  public boolean isCompatibleWithDemands(Demands demands, Modes modes) {
    final int noZonesInZoning = zones.getNumberOfZones();
    for (final Mode mode : modes) {
      for (TimePeriod timePeriod : demands.timePeriods) {
        final ODDemandMatrix odMatrix = demands.get(mode, timePeriod);
        if (odMatrix != null) {
          if (noZonesInZoning != odMatrix.getNumberOfTravelAnalysisZones()) {
            // inconsistent number of zones found
            return false;
          }
        }
      }
    }
    return true;
  }

}
