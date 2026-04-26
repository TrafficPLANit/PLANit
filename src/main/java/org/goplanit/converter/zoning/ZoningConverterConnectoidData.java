package org.goplanit.converter.zoning;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.zoning.DirectedConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.TransferConnectoid;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.Point;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
    import java.util.logging.Logger;

/**
 * Zoning handler data specifically tailored towards connectoids
 * todo integrate with OSM version in OsmZoningReaderPlanitData
 *
 * @author markr
 */
public class ZoningConverterConnectoidData {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ZoningConverterConnectoidData.class.getCanonicalName());

  /** network to use */
  private final MacroscopicNetwork referenceNetwork;

  /** zoning to use */
  private final Zoning referenceZoning;

  /** track created connectoids by their location and layer they reside on, needed to avoid creating duplicates
   *  when dealing with multiple modes/layers */
  private final Map<MacroscopicNetworkLayer, Map<Point, List<TransferConnectoid>>> transferConnectoidsByLocation;

  /** track mapping from transfer zone to any connectoids that refer to it
   * track this because PLANit only tracks the other way around */
  private final Map<TransferZone, List<TransferConnectoid> > connectoidsByTransferZone = new HashMap<>();

  /**
   * Constructor
   *
   * @param referenceZoning to use
   * @param referenceNetwork to use
   */
  public ZoningConverterConnectoidData(Zoning referenceZoning, MacroscopicNetwork referenceNetwork){
    this(referenceZoning, referenceNetwork, new TreeMap<>());
  }

  /**
   * Constructor with initial map to use
   *
   * @param referenceZoning to use
   * @param referenceNetwork to use
   * @param directedConnectoidsByLocation to use
   */
  public ZoningConverterConnectoidData(
      Zoning referenceZoning,
      MacroscopicNetwork referenceNetwork,
      Map<MacroscopicNetworkLayer, Map<Point, List<TransferConnectoid>>> directedConnectoidsByLocation){
    this.transferConnectoidsByLocation = directedConnectoidsByLocation;
    this.referenceNetwork = referenceNetwork;
    this.referenceZoning = referenceZoning;
  }

  // CONNECTOIDS <-> LOCATION METHODS

  /** collect the registered connectoids indexed by their locations for a given network layer (unmodifiable)
   *
   * @param networkLayer to use
   * @return registered directed connectoids indexed by location
   */
  public Map<Point, List<TransferConnectoid>> getTransferConnectoidsByLocation(MacroscopicNetworkLayer networkLayer) {
    transferConnectoidsByLocation.putIfAbsent(networkLayer, new HashMap<>());
    return Collections.unmodifiableMap(transferConnectoidsByLocation.get(networkLayer));
  }

  /** Collect the registered connectoids by given locations and network layer (unmodifiable)
   *
   * @param nodeLocation to verify
   * @param networkLayer to extract from
   * @return found connectoids (if any), otherwise null or empty set
   */
  public List<TransferConnectoid> getTransferConnectoidsByLocation(
      Point nodeLocation, MacroscopicNetworkLayer networkLayer) {
    return getTransferConnectoidsByLocation(networkLayer).get(nodeLocation);
  }

  /** Add a connectoid to the registered connectoids indexed by their OSM id
   *
   * @param networkLayer to register for
   * @param connectoidLocation this connectoid relates to
   * @param connectoid to add
   * @return true when successful, false otherwise
   */
  public boolean addTransferConnectoidByLocation(
      MacroscopicNetworkLayer networkLayer, Point connectoidLocation , TransferConnectoid connectoid) {
    transferConnectoidsByLocation.putIfAbsent(networkLayer, new HashMap<>());
    Map<Point, List<TransferConnectoid>> connectoidsForLayer = transferConnectoidsByLocation.get(networkLayer);
    connectoidsForLayer.putIfAbsent(connectoidLocation, new ArrayList<>(1));
    List<TransferConnectoid> connectoids = connectoidsForLayer.get(connectoidLocation);
    if(!connectoids.contains(connectoid)) {
      return connectoids.add(connectoid);
    }
    return false;
  }

  /** Check if any connectoids have been registered for the given location on any layer
   *
   * @param location to verify
   * @return true when present, false otherwise
   */
  public boolean hasAnyTransferConnectoidsForLocation(Point location) {
    for( var entry : transferConnectoidsByLocation.entrySet()) {
      if(hasTransferConnectoidForLocation(entry.getKey(), location)) {
        return true;
      }
    }
    return false;
  }

  /** Check if any connectoid has been registered for the given location for this layer
   *
   * @param networkLayer to check for
   * @param point to use
   * @return true when present, false otherwise
   */
  public boolean hasTransferConnectoidForLocation(NetworkLayer networkLayer, Point point) {
    Map<Point, List<TransferConnectoid>>  connectoidsForLayer = transferConnectoidsByLocation.get(networkLayer);
    return connectoidsForLayer != null && connectoidsForLayer.get(point) != null &&
        !connectoidsForLayer.get(point).isEmpty();
  }

  /**
   * Helper for tracking update
   *
   * @param createdConnectoid to track
   * @param transferZone additional index
   * @param networkLayer additional index
   */
  public void registerNewConnectoidOnPlanitTrackingData(
      TransferConnectoid createdConnectoid, TransferZone transferZone, MacroscopicNetworkLayer networkLayer) {
    /* 1) index by access vertex node location */
    addTransferConnectoidByLocation(
        networkLayer, createdConnectoid.getReferenceVertex().getPosition() ,createdConnectoid);
    /* 2) index connectoids on transfer zone, so we can collect it by transfer zone as well */
    addConnectoidByTransferZone(transferZone, createdConnectoid);
  }

  // TRANSFERZONE <--> CONNECTOIDS METHODS

  /** Register a known mapping from transfer zone to connectoid
   *
   * @param transferZone to map to...
   * @param connectoid ...this connectoid
   */
  public void addConnectoidByTransferZone(TransferZone transferZone, TransferConnectoid connectoid) {
    connectoidsByTransferZone.putIfAbsent(transferZone, new ArrayList<>(1));
    var connectoids = connectoidsByTransferZone.get(transferZone);
    if(!connectoids.contains(connectoid)) {
      connectoids.add(connectoid);
    }
  }

  /** Verify if transfer zone has connectoids present
   *
   * @param transferZone to check for
   * @return true when there exist connectoids that reference this transfer zone, false otherwise
   */
  public boolean hasConnectoids(TransferZone transferZone) {
    return getConnectoidsByTransferZone(transferZone) != null && !getConnectoidsByTransferZone(transferZone).isEmpty();
  }

  /** Collect transfer zone's registered connectoids
   *
   * @param transferZone to map to...
   * @return connectoids found
   */
  public Collection<TransferConnectoid> getConnectoidsByTransferZone(TransferZone transferZone) {
    if(transferZone == null) {
      return null;
    }
    connectoidsByTransferZone.putIfAbsent(transferZone, new ArrayList<>(1));
    return connectoidsByTransferZone.get(transferZone);
  }


  // FUNCTIONALITY BEYOND GETTERS/SETTERS ON DATA


  /** create transfer connectoids with a single undirected zone entry for explicit access modes provided.
   *
   * @param connectoidExternalId external id (allowed to be null)
   * @param transferZone to relate connectoids to
   * @param networkLayer to use
   * @param accessNode of the connectoids
   * @param allowedModes used for each connectoid
   * @param type         the type of the zone connectoid combination reflecting how it is envisaged to be used
   * @return created connectoid
   */
  public TransferConnectoid createAndRegisterTransferConnectoidWithUndirectedZoneEntry(
      @Nullable String connectoidExternalId,
      final TransferZone transferZone,
      final MacroscopicNetworkLayer networkLayer,
      final DirectedVertex accessNode,
      final Set<Mode> allowedModes,
      ZoneConnectoidType type){

    var createdConnectoid = ZoningConverterUtils.createAndRegisterTransferConnectoidWithUndirectedAccessEntry(
        connectoidExternalId, referenceZoning, transferZone, accessNode, allowedModes, type);

    /* update PLANit data tracking information */
    registerNewConnectoidOnPlanitTrackingData(createdConnectoid, transferZone, networkLayer);

    return createdConnectoid;
  }


  /**
   * update an existing directed connectoid with new access zone, segment and allowed modes (for given type). In case
   * the link segment does not have any of the passed in modes listed as allowed, the connectoid is not updated
   * with these modes for the given access zone as it would not be possible to utilise it.
   *
   * @param connectoidToUpdate to connectoid to update
   * @param accessZone         to relate connectoids to
   * @param allowedModes       to add to the connectoid for the given access zone
   * @param type type to use
   */
  public void updateTransferConnectoidWithUndirectedAccess(
      @Nonnull TransferConnectoid connectoidToUpdate,
      @Nonnull TransferZone accessZone,
      @Nonnull Collection<Mode> allowedModes,
      ZoneConnectoidType type) {

    if(!connectoidToUpdate.hasAccessZoneEntry(accessZone, type)){
      connectoidToUpdate.createUndirectedAccessZoneEntry(accessZone, type);
    }
    var entry = connectoidToUpdate.getAccessZoneEntry(accessZone, type);
    if(entry instanceof DirectedConnectoidAccessZoneEntry){
      LOGGER.severe("Cannot add modes to connectoid entry that was expected to be undirected, " +
          "this should not happen");
      return;
    }
    // add if missing, if not directed already allowed
    entry.addAllowedModes(allowedModes);
  }

  /**
   * Create a connectoid or expand an existing connectoid with given modes if it exists for given transfer zone
   * and provided parameters.
   *
   * @param connectoidExternalId external id (allowed to be null)
   * @param transferZone to add connectoid for
   * @param undirectedModeAccess modes to support
   * @param accessVertex access node to use
   * @param type         the type of the zone connectoid combination reflecting how it is envisaged to be used
   * @return true if success, false otherwise
   */
  public boolean extractTransferConnectoidForUndirectedModeAccess(
      @Nullable String connectoidExternalId,
      TransferZone transferZone,
      DirectedVertex accessVertex,
      final Set<Mode> undirectedModeAccess,
      ZoneConnectoidType type) {

    if(referenceNetwork.getTransportLayers().size()>1) {
      throw new PlanItRunTimeException("Multiple layers in network not supported yet by " +
          "`extractTransferConnectoidForUndirectedModeAccess`");
    }
    var networkLayer = referenceNetwork.getLayerByMode(undirectedModeAccess.stream().findFirst().get());

    Point proposedLocation = accessVertex.getPosition();
    if(hasTransferConnectoidForLocation(networkLayer, proposedLocation)) {
      /* existing connectoid: update model eligibility */
      Collection<TransferConnectoid> connectoidsForNode = getTransferConnectoidsByLocation(
              proposedLocation, networkLayer);
      for(TransferConnectoid connectoid : connectoidsForNode) {
        /* update type-mode eligibility */
        updateTransferConnectoidWithUndirectedAccess(connectoid, transferZone, undirectedModeAccess, type);
        return true;
      }
    }

    var result = this.createAndRegisterTransferConnectoidWithUndirectedZoneEntry(
        connectoidExternalId, transferZone, networkLayer, accessVertex, undirectedModeAccess, type);
    return result !=null;
  }

  /**
   * Reset the PLANit data tracking containers
   */
  public void reset(){
    transferConnectoidsByLocation.clear();
    connectoidsByTransferZone.clear();
  }

}
