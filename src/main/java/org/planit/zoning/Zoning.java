package org.planit.zoning;

import java.io.Serializable;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.demands.Demands;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.Modes;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.zoning.DirectedConnectoids;
import org.planit.utils.zoning.OdZones;
import org.planit.utils.zoning.TransferZoneGroups;
import org.planit.utils.zoning.TransferZones;
import org.planit.utils.zoning.UndirectedConnectoids;
import org.planit.utils.zoning.Zone;
import org.planit.utils.zoning.modifier.ZoningModifier;
import org.planit.zoning.modifier.ZoningModifierImpl;

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
  private static final Logger LOGGER = Logger.getLogger(Zoning.class.getCanonicalName());

  // Protected

  /**
   * Virtual network holds all the virtual connections to the physical network (layers)
   */
  protected final VirtualNetwork virtualNetwork;

  /**
   * modifier that can be used to perform modifications to the zoning that comprise more than a single element of the zoning, e.g. updating of ids. It is also used by listeners
   * that are needed to update the zoning components in case the referenced network gets modified
   */
  protected final ZoningModifier zoningModifier;

  // Public

  /**
   * provide access to undirected connectoids (of od zones)
   */
  public final UndirectedConnectoids odConnectoids;

  /**
   * provide access to directed connectoids (of transfer zones)
   */
  public final DirectedConnectoids transferConnectoids;

  /**
   * provide access to zones
   */
  public final OdZones odZones;

  /**
   * provide access to transfer zones (if any)
   */
  public final TransferZones transferZones;

  /**
   * provide access to transfer zone groups (if any)
   */
  public final TransferZoneGroups transferZoneGroups;

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

    odConnectoids = new UndirectedConnectoidsImpl(networkGroupId);
    transferConnectoids = new DirectedConnectoidsImpl(networkGroupId);
    odZones = new OdZonesImpl(networkGroupId);
    transferZones = new TransferZonesImpl(networkGroupId);
    transferZoneGroups = new TransferZoneGroupsImpl(networkGroupId);

    zoningModifier = new ZoningModifierImpl(this);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public Zoning(final Zoning other) {
    super(other);
    this.virtualNetwork = other.virtualNetwork;
    this.odConnectoids = other.odConnectoids.clone();
    this.transferConnectoids = other.transferConnectoids.clone();
    this.odZones = other.odZones.clone();
    this.transferZones = other.transferZones.clone();
    this.transferZoneGroups = other.transferZoneGroups.clone();

    this.zoningModifier = new ZoningModifierImpl(this);
  }

  // Public - getters - setters

  /**
   * Log general information on this zoning to the user
   * 
   * @param prefix to use
   */
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s#od zones: %d (#centroids: %d)", prefix, odZones.size(), odZones.getNumberOfCentroids()));
    LOGGER.info(String.format("%s#od connectoids: %d", prefix, odConnectoids.size()));
    if (!transferZones.isEmpty()) {
      LOGGER.info(String.format("%s#transfer connectoids: %d", prefix, transferConnectoids.size()));
      LOGGER.info(String.format("%s#transfer zones: %d (#centroids:", prefix, transferZones.size(), transferZones.getNumberOfCentroids()));
      LOGGER.info(String.format("%s#transfer zone groups: %d", prefix, transferZoneGroups.size()));
    }
  }

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
    final int nofZones = odZones.size();
    for (final Mode mode : modes) {
      for (TimePeriod timePeriod : demands.timePeriods) {
        final ODDemandMatrix odMatrix = demands.get(mode, timePeriod);
        if (odMatrix != null) {
          if (nofZones != odMatrix.getNumberOfTravelAnalysisZones()) {
            // inconsistent number of zones found
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * find a zone by over arching id regardless if it is a transfer or Od zone
   * 
   * @param id to find the zone by
   * @return zone found (if any)
   */
  public Zone getZone(long id) {
    Zone zone = odZones.get(id);
    if (zone == null) {
      zone = transferZones.get(id);
    }
    return zone;
  }

  /**
   * collect the number of centroids across all zones (od and transfer zones)
   * 
   * @return total number of centroids
   */
  public long getNumberOfCentroids() {
    return odZones.getNumberOfCentroids() + transferZones.getNumberOfCentroids();
  }

  /**
   * collect the number of connectoids (od and transfer)
   * 
   * @return total number of connectoids
   */
  public long getNumberOfConnectoids() {
    return odConnectoids.size() + transferConnectoids.size();
  }

  /**
   * The zoning's modifier instance
   * 
   * @return the zoning modifier
   */
  public ZoningModifier getZoningModifier() {
    return zoningModifier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Zoning clone() {
    return new Zoning(this);
  }

}
