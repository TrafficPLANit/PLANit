package org.goplanit.zoning;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.component.PlanitComponent;
import org.goplanit.demands.Demands;
import org.goplanit.network.virtual.VirtualNetworkImpl;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.Modes;
import org.goplanit.utils.network.virtual.*;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.zoning.*;
import org.goplanit.utils.zoning.modifier.ZoningModifier;
import org.goplanit.zoning.modifier.ZoningModifierImpl;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Zoning class which holds a particular zoning
 *
 * @author markr
 *
 */
public class Zoning extends PlanitComponent<Zoning> implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = -2986366471146628179L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(Zoning.class.getCanonicalName());

  // Protected

  /** the CRS of this zoning */
  protected CoordinateReferenceSystem crs;

  /**
   * Virtual network holds all the virtual connections to the physical network (layers)
   * todo: we should have potentially multiple virtual networks per zoning, one for each physical network the zoning is used on!
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
  protected final UndirectedConnectoids odConnectoids;

  /**
   * provide access to directed connectoids (of transfer zones)
   */
  protected final DirectedConnectoids transferConnectoids;

  /**
   * provide access to zones
   */
  protected final OdZones odZones;

  /**
   * provide access to transfer zones (if any)
   */
  protected final TransferZones transferZones;

  /**
   * provide access to transfer zone groups (if any)
   */
  protected final TransferZoneGroups transferZoneGroups;

  /**
   * Constructor
   * 
   * The second id generation token should be the token used by the physical network to create physical network entities such as links, nodes, etc. The virtual network should
   * register connectoids, centroids, etc. with ids compatible under this same network. For example, a centroid is a vertex, like a node, so the vertex ids should be contiguous and
   * unique throughout the combination of the virtual and physical network. Hence, they should use the same network id token
   * 
   * @param groupId        contiguous id generation within this group for instances of this class
   * @param virtualNetworkGroupId contiguous id generation for all instances created by the virtual network
   */
  public Zoning(IdGroupingToken groupId, IdGroupingToken virtualNetworkGroupId) {
    super(groupId, Zoning.class);
    virtualNetwork = new VirtualNetworkImpl(virtualNetworkGroupId);

    odConnectoids = new UndirectedConnectoidsImpl(virtualNetworkGroupId);
    transferConnectoids = new DirectedConnectoidsImpl(virtualNetworkGroupId);
    odZones = new OdZonesImpl(virtualNetworkGroupId);
    transferZones = new TransferZonesImpl(virtualNetworkGroupId);
    transferZoneGroups = new TransferZoneGroupsImpl(virtualNetworkGroupId);

    zoningModifier = new ZoningModifierImpl(this);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param undirConnectoidMapper to use for tracking mapping between original and copied entity (may be null)
   * @param dirConnectoidMapper to use for tracking mapping between original and copied entity (may be null)
   * @param odZoneMapper to use for tracking mapping between original and copied entity (may be null)
   * @param transferZoneMapper to use for tracking mapping between original and copied entity (may be null)
   * @param transferZoneGroupMapper to use for tracking mapping between original and copied entity (may be null)
   */
  public Zoning(
      final Zoning other,
      boolean deepCopy,
      ManagedIdDeepCopyMapper<UndirectedConnectoid> undirConnectoidMapper,
      ManagedIdDeepCopyMapper<DirectedConnectoid> dirConnectoidMapper,
      ManagedIdDeepCopyMapper<OdZone> odZoneMapper,
      ManagedIdDeepCopyMapper<TransferZone> transferZoneMapper,
      ManagedIdDeepCopyMapper<TransferZoneGroup> transferZoneGroupMapper) {
    super(other, deepCopy);

    // extension of class, with reference to this, so copy always required
    this.zoningModifier = new ZoningModifierImpl(this);

    // These are all container wrappers as well, so always require a clone
    if(deepCopy){
      this.odConnectoids =        other.odConnectoids.deepCloneWithMapping(undirConnectoidMapper);
      this.transferConnectoids =  other.transferConnectoids.deepCloneWithMapping(dirConnectoidMapper);
      this.odZones =              other.odZones.deepCloneWithMapping(odZoneMapper);
      this.transferZones =        other.transferZones.deepCloneWithMapping(transferZoneMapper);
      this.transferZoneGroups =   other.transferZoneGroups.deepCloneWithMapping(transferZoneGroupMapper);

      if(odZoneMapper != null) ConnectoidUtils.updateAccessZoneMapping(odConnectoids, (OdZone z) -> odZoneMapper.getMapping(z), true);
      if(transferZoneMapper != null){
        ConnectoidUtils.updateAccessZoneMapping(transferConnectoids, (TransferZone z) -> transferZoneMapper.getMapping(z), true);
        TransferZoneGroupUtils.updateTransferZoneMapping(transferZoneGroups, (TransferZone z) -> transferZoneMapper.getMapping(z), true);
      }

      var connectoidEdgeMapper = new GraphEntityDeepCopyMapper<ConnectoidEdge>();
      var connectoidEdgeSegmentMapper = new GraphEntityDeepCopyMapper<ConnectoidSegment>();
      var centroidVertexMapper = new GraphEntityDeepCopyMapper<CentroidVertex>();
      this.virtualNetwork = other.virtualNetwork.deepCloneWithMapping(connectoidEdgeMapper, connectoidEdgeSegmentMapper, centroidVertexMapper);

      // make sure centroid vertex's parent centroid is updated properly (this goes across zones that own  zone and centroids and virtual network, so done here)
      var centroidMapper = centroidVertexMapper.stream().collect(Collectors.toMap(entry -> entry.getKey().getParent(), entry -> entry.getValue().getParent()));
      CentroidVertexUtils.updateCentroidVertexCentroidMapping(virtualNetwork.getCentroidVertices(), (Centroid originalCentroid) -> centroidMapper.get(originalCentroid), true);

    }else{
      this.odConnectoids =        other.odConnectoids.shallowClone();
      this.transferConnectoids =  other.transferConnectoids.shallowClone();
      this.odZones =              other.odZones.shallowClone();
      this.transferZones =        other.transferZones.shallowClone();
      this.transferZoneGroups =   other.transferZoneGroups.shallowClone();

      this.virtualNetwork =       other.virtualNetwork.shallowClone();
    }
  }

  // Public - getters - setters

  /**
   * Log general information on this zoning to the user
   * 
   * @param prefix to use
   */
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s XML id %s (external id: %s) ", prefix, getXmlId(), getExternalId()));
    LOGGER.info(String.format("%s #od zones: %d (#centroids: %d)", prefix, odZones.size(), odZones.getNumberOfCentroids()));
    LOGGER.info(String.format("%s #od connectoids: %d", prefix, odConnectoids.size()));
    if (!transferZones.isEmpty()) {
      LOGGER.info(String.format("%s #transfer connectoids: %d", prefix, transferConnectoids.size()));
      LOGGER.info(String.format("%s #transfer zones: %d", prefix, transferZones.size(), transferZones.getNumberOfCentroids()));
      LOGGER.info(String.format("%s #transfer zone groups: %d", prefix, transferZoneGroups.size()));
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
        final OdDemands odDemandsForModeTime = demands.get(mode, timePeriod);
        if (odDemandsForModeTime != null) {
          if (nofZones != odDemandsForModeTime.getNumberOfOdZones()) {
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
   * Access to the odZones container
   * 
   * @return odZones
   */
  public OdZones getOdZones() {
    return odZones;
  }

  /**
   * Access to the transferZones container
   * 
   * @return transferZones
   */
  public TransferZones getTransferZones() {
    return transferZones;
  }

  /**
   * Access to the transferZoneGroups container
   * 
   * @return TranferZoneGroups
   */
  public TransferZoneGroups getTransferZoneGroups() {
    return transferZoneGroups;
  }

  /**
   * Access to the origin-destination connectoids container
   * 
   * @return od connectoids container
   */
  public UndirectedConnectoids getOdConnectoids() {
    return this.odConnectoids;
  }

  /**
   * Access to the transfer connectoids container
   * 
   * @return transfer connectoids container
   */
  public DirectedConnectoids getTransferConnectoids() {
    return this.transferConnectoids;
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
  public Zoning shallowClone() {
    return new Zoning(this, false, null, null, null, null, null);
  }

  /**
   * {@inheritDoc}
   *
   */
  @Override
  public Zoning deepClone() {
    return new Zoning(
        this,
        true,
        new ManagedIdDeepCopyMapper<>(),
        new ManagedIdDeepCopyMapper<>(),
        new ManagedIdDeepCopyMapper<>(),
        new ManagedIdDeepCopyMapper<>(),
        new ManagedIdDeepCopyMapper<>());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    this.virtualNetwork.reset();
    this.odConnectoids.reset();
    this.odZones.reset();
    this.transferConnectoids.reset();
    this.odConnectoids.reset();
    this.transferZoneGroups.reset();
    this.transferZones.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return null;
  }

  /**
   * Verify if od zones are present
   *
   * @return true when present, false otherwise
   */
  public boolean hasOdZones() {
    return !getOdZones().isEmpty();
  }

  /**
   * Verify if transfer zones are present
   *
   * @return true when present, false otherwise
   */
  public boolean hasTransferZones() {
    return !getTransferZones().isEmpty();
  }

  /**
   * Verify if transfer connectoids are present
   *
   * @return true when present, false otherwise
   */
  public boolean hasTransferConnectoids() {
    return !getTransferConnectoids().isEmpty();
  }

  /**
   * Verify if transfer connectoids are present
   *
   * @return true when present, false otherwise
   */
  public boolean hasOdConnectoids() {
    return !getOdConnectoids().isEmpty();
  }

  /**
   * The crs used by the zoning
   *
   * @return crs
   */
  public CoordinateReferenceSystem getCoordinateReferenceSystem(){
    return crs;
  }

  /**
   * Set the crs to use by the zoning
   *
   * @param crs to use
   */
  public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs){
    this.crs = crs;
  }
}
