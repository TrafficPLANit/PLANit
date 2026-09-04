package org.goplanit.converter.zoning;

import org.goplanit.converter.utils.ProjectedBoundingAreaHelper;
import org.goplanit.utils.containers.MapUtils;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedMode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.mode.TrackModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Executor able to facilitate injection of access egress of modes other than the PT transfer zone vehicle modes.
 * Depending on the user configuration of the converter wanting to use this. This may be a no-op but if any
 * true multi-modal trips are to be modeled this is a vital final step in completing the pt network.
 * this is especially useful when the importer (OSM/GTFS) has no well-defined connections directly. In that
 * case this functionality is capable of adding in additional infrastructure and/or connectoids to allow
 * for these connections to the relevant physical network nearby that is available in OSM. Mode compatibility is
 * tested for to ensure the access/egress points are valid.
 *
 * @author markr
 */
public class TransferZoningInjectAccessEgressExecutor {

  /**
   * The logger for this class
   */
  private static final Logger LOGGER = Logger.getLogger(
      TransferZoningInjectAccessEgressExecutor.class.getCanonicalName());

  /** no direct external entity for connectoid, indicate it was inferred upon parsing*/
  private static final String CONNECTOID_INFERRED = "inferred";

  private final ProjectedBoundingAreaHelper boundingAreaHelper;
  private final ZoningConverterCommonData zoningConverterData;

  /**
   * Connect the provided transfer zone to the nearest network point via a connectoid with the appropriate
   * modes given it falls within max distance. Since transfer connectoids can be directed and we are not dealing
   * with fixed routes but access for let's say pedestrians, we must create a connectoid/entry for each
   * access/egress mode supported at the chosen network access node.
   *
   * @param transferZone        to connect
   * @param tzConnectoids existing connectoids of zone
   * @param bannedModesOnAccessEgress     existing modes of the transfer connectoid we are considered banned for
   *                                      any eligible points of access (attached segments) for the modes to add
   * @param modesToAdd          the modes to add access/egress entries for
   * @param maxSearchRadius to apply
   * @param suppressSpatialWarnings when true suppress warnings
   * @return true when for all non ferry land modes a connectoid is now available, false otherwise
   */
  private boolean connectPtStopTransferZoneToNetworkWithAccessEgressModes(
      TransferZone transferZone,
      Set<TransferConnectoid> tzConnectoids,
      Set<Mode> bannedModesOnAccessEgress,
      Set<Mode> modesToAdd,
      double maxSearchRadius,
      boolean suppressSpatialWarnings) {

    var connectoidDataHelper = this.zoningConverterData.getConnectoidData();
    var accessModesToAdd = new TreeSet<>(modesToAdd);
    var egressModesToAdd = new TreeSet<>(modesToAdd);

    // if we're lucky the transfer zone's existing connectoids are already connected to the land network in which
    // case we check if any of the other segments coming into the access nodes (that do not support ferry) are eligible.
    // if so, expand the connectoid and add the modes that we need and cross them of the onFerryNonFerryModes list
    var addedModesResult = ZoningConverterUtils.expandTransferConnectoidsWithEligibleUndirectedAccessEgressEntries(
        transferZone,
        tzConnectoids,
        bannedModesOnAccessEgress,
        modesToAdd);
    // remainder to deal with
    accessModesToAdd.removeAll(addedModesResult.first());
    egressModesToAdd.removeAll(addedModesResult.second());
    if(accessModesToAdd.isEmpty() && egressModesToAdd.isEmpty()){
      return true;
    }

    var combinedRemainingModesToAdd =
        Stream.concat(accessModesToAdd.stream(), egressModesToAdd.stream()).collect(Collectors.toSet());
    var spatiallyMatchedLinks = zoningConverterData.findNearbyModeCompatibleLinks(
        transferZone.getGeometry(true),
        bannedModesOnAccessEgress,
        combinedRemainingModesToAdd,
        maxSearchRadius);
    if(spatiallyMatchedLinks.isEmpty()){
      if(!suppressSpatialWarnings) {
        LOGGER.warning(String.format("DISCARD: waiting area %s, no mode compatible OSM ways within %.2fm " +
                "found (for modes: [%s]) to attach to network",
            transferZone.getIdsAsString(), maxSearchRadius,
            modesToAdd.stream().map(Mode::getName).collect(Collectors.joining(","))));
      }
      return false;
    }

    // attach closest node to each mode provided given options for link segments when function is applied below
    BiFunction<Mode, Boolean, Map.Entry<Mode, DirectedVertex>> findClosestModeCompatibleNodeFunction =
        (mode, isNodeUpstreamOfSegments) -> {
          var closest = ZoningConverterUtils.findClosestModeCompatibleNode(
              transferZone.getGeometry(true),
              mode,
              (Stream<MacroscopicLinkSegment>) spatiallyMatchedLinks.stream().flatMap(DirectedEdge::streamEdgeSegments),
              isNodeUpstreamOfSegments,
              this.zoningConverterData.getGeoUtils(),
              maxSearchRadius,
              suppressSpatialWarnings);
          if(closest == null) {
            if (!suppressSpatialWarnings) {
              LOGGER.warning(String.format("Mode [%s] for waiting area (%s) no compatible OSM ways within %.2fm found",
                  mode.getName(), transferZone.getIdsAsString(), maxSearchRadius));
            }
            return null;
          }
          return Map.entry(mode, closest.first());
        };

    // access modes nodes
    var modesWithClosestAccessNode = accessModesToAdd.stream().map( accessMode ->
        findClosestModeCompatibleNodeFunction.apply(accessMode, false)).filter(Objects::nonNull).collect(
        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    // egress modes nodes
    var modesWithClosestEgressNode = egressModesToAdd.stream().map( egressMode ->
        findClosestModeCompatibleNodeFunction.apply(egressMode, true)).filter(Objects::nonNull).collect(
        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // combined access/egress + remove combined from access/egress only
    Map<Mode, DirectedVertex> modesWithClosestAccessEgressNode = modesWithClosestAccessNode.entrySet().stream()
        .filter(entry -> modesWithClosestEgressNode.containsKey(entry.getKey()) &&
            Objects.equals(entry.getValue(), modesWithClosestEgressNode.get(entry.getKey())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    modesWithClosestAccessEgressNode.forEach( (k,v) -> {
      modesWithClosestAccessNode.remove(k); modesWithClosestEgressNode.remove(k);});

    // access egress combined
    MapUtils.invertMap(modesWithClosestAccessEgressNode).forEach( (node, modes) ->{
      // now create/update transfer connectoids for each one
      connectoidDataHelper.extractTransferConnectoidForUndirectedModeAccess(
          CONNECTOID_INFERRED, transferZone, node, modes, ZoneConnectoidType.ZONE_ACCESS_EGRESS);
    });

    // access only
    MapUtils.invertMap(modesWithClosestAccessNode).forEach( (node, modes) ->{
      connectoidDataHelper.extractTransferConnectoidForUndirectedModeAccess(
          CONNECTOID_INFERRED, transferZone, node, modes, ZoneConnectoidType.ZONE_ACCESS);
    });

    // egress only
    MapUtils.invertMap(modesWithClosestEgressNode).forEach( (node, modes) ->{
      // now create new transfer connectoids for each
      connectoidDataHelper.extractTransferConnectoidForUndirectedModeAccess(
          CONNECTOID_INFERRED, transferZone, node, modes, ZoneConnectoidType.ZONE_EGRESS);
    });
    return true;
  }

  /**
   * Connect the provided transfer zone to the nearest land network via a connectoid with the appropriate (inferred)
   * land modes given it falls within max distance. Since transfer connectoids can be directed and we are not dealing
   * with fixed routes but access for example pedestrians, we must create a connectoid/entry for each
   * access/egress mode supported  into the chosen land network access node.
   *
   * @param transferZone                                   to connect
   * @param connectoids                                    existing connectoids of zone
   * @param ferryMode                                      PLANit ferry mode
   * @param ferryStopToNearbyLandNetworkSearchRadiusMeters to use
   * @return true when for all non ferry land modes a connectoid is now available, false otherwise
   */
  private boolean connectFerryTransferZoneToLandNetwork(
      TransferZone transferZone,
      Set<TransferConnectoid> connectoids,
      PredefinedMode ferryMode, double ferryStopToNearbyLandNetworkSearchRadiusMeters) {

    final boolean suppressSpatialWarnings =
        !boundingAreaHelper.fallsWithinSpatiallyEligibleBoundingArea(transferZone, false);

    var onFerryNonFerryModes = ZoningConverterUtils.extractExplicitAllowedModesFromDirectedAccessEntries(connectoids);
    boolean removed = onFerryNonFerryModes.remove(ferryMode);
    if(!removed){
      LOGGER.warning(String.format("Expected support for ferry on for ferry transfer zone (%s), but it is not",
          transferZone.getIdsAsString()));
    }
    if(onFerryNonFerryModes.isEmpty()){
      LOGGER.warning(String.format("Expected at least one on-ferry mode for ferry access link segments for " +
              "transfer zone (%s), but none found, unable to connect to land network without land modes",
          transferZone.getIdsAsString()));
      return false;
    }

    // with on ferry modes identified create the access egress connectoids
    connectPtStopTransferZoneToNetworkWithAccessEgressModes(
        transferZone, connectoids, Collections.singleton(ferryMode) , onFerryNonFerryModes,
        ferryStopToNearbyLandNetworkSearchRadiusMeters, suppressSpatialWarnings);

    return true;
  }

  /**
   * Nearly identical to connectFerryTransferZoneToLandNetwork except this now deals with rail-based modes and
   * active modes only (as we do not allow cars on trains, not car as access/egress mode yet)
   *
   * @param transferZone                                       to connected
   * @param transferConnectoids                                of the transfer zone
   * @param railBasedModes                                     accepted rail based modes
   * @param allSupportedActiveModes                            supported active modes to supplement
   * @param railBasedStopToNearbyRoadNetworkSearchRadiusMeters to apply
   * @return true when success, false otherwise
   */
  private boolean connectRailBasedTransferZoneToRoadNetwork(
      @Nonnull TransferZone transferZone,
      @Nonnull Set<TransferConnectoid> transferConnectoids,
      @Nonnull Set<Mode> railBasedModes,
      @Nonnull Set<Mode> allSupportedActiveModes, double railBasedStopToNearbyRoadNetworkSearchRadiusMeters) {
    final boolean suppressSpatialWarnings =
        !boundingAreaHelper.fallsWithinSpatiallyEligibleBoundingArea(transferZone, false);
    // with rail access egress modes known create the access egress connectoids
    return connectPtStopTransferZoneToNetworkWithAccessEgressModes(
        transferZone, transferConnectoids, railBasedModes , allSupportedActiveModes,
        railBasedStopToNearbyRoadNetworkSearchRadiusMeters, suppressSpatialWarnings);
  }

  /**
   * If configured as such connect all ferries to land network if not connected yet based on search radius and eligible
   * modes for ferry as it is assumed they can access those stops with those modes from the land network but may not
   * be tagged as such in OSM.
   *
   * @param searchRadius to apply
   */
  private void connectFerriesToLandNetwork(double searchRadius) {
    var referenceNetwork = zoningConverterData.getReferenceNetwork();
    var referenceZoning = zoningConverterData.getReferenceZoning();

    var ferryMode = referenceNetwork.getModes().get(PredefinedModeType.FERRY);
    if(ferryMode == null){
      return;
    }
    // implicitly allowed modes in case no explicit modes are set on connectoid
    var allAvailableModes = referenceNetwork.getModes();
    var nonFerryModes = allAvailableModes.stream().filter(
        m -> !m.getPredefinedModeType().equals(PredefinedModeType.FERRY)).collect(Collectors.toSet());

    // create index from transfer zone to transfer connectoid for efficient checking
    var connectoidsByTransferZoneMapping = referenceZoning.getTransferConnectoids().createIndexByAccessZone();

    // todo: if one or more nonFerry modes are connected but others are not, t is still classified as diconnected
    //  technically we should run this per mode and supplement per missing mode
    var result = ZoningConverterUtils.findPtStopModeTransferZonesWithoutAccessEgressModeSupport(
        connectoidsByTransferZoneMapping,
        Collections.singleton(ferryMode),
        nonFerryModes);

    var ferryTransferZones = result.first();
    if(ferryTransferZones.isEmpty()){
      LOGGER.info("Connecting ferry waiting areas to land network");
    }
    var ferryTransferZonesDisconnectedFromLandNetwork = result.second();
    LOGGER.info(String.format("Identified %d (%.2f%%) disconnected ferry stops from land network",
        ferryTransferZonesDisconnectedFromLandNetwork.size(),
        (double)ferryTransferZonesDisconnectedFromLandNetwork.size()*100/ferryTransferZones.size()));

    // for each identified connect it to the land network
    var numSuccess = ferryTransferZonesDisconnectedFromLandNetwork.stream().map( tz ->
            connectFerryTransferZoneToLandNetwork(
                tz,
                connectoidsByTransferZoneMapping.get(tz),
                ferryMode,
                searchRadius) ).map(b -> b ? 1 : 0).
        reduce(0, Integer::sum);
    LOGGER.info(String.format("Connected %d (%.2f%%) disconnected ferry stops to land network",
        numSuccess, (double)numSuccess*100/ferryTransferZonesDisconnectedFromLandNetwork.size()));
  }

  /**
   * If configured as such connect all rail based stops to land network if not connected yet based on search
   * radius and eligible modes as it is assumed they can access those stops with those modes from the active mode
   * network but may not be tagged as such in OSM.
   *
   * @param searchRadius  to apply
   * @param settings to apply
   */
  private void connectRailBasedStopsToActiveModeNetwork(
      double searchRadius, AccessEgressInjectionSettings settings) {
    var referenceNetwork = zoningConverterData.getReferenceNetwork();
    var referenceZoning = zoningConverterData.getReferenceZoning();

    var railBasedModes = referenceNetwork.getModes().stream().filter(
        m -> m.getPhysicalFeatures().getTrackType().equals(TrackModeType.RAIL)).collect(Collectors.toSet());
    if(railBasedModes.isEmpty()){
      return;
    }

    // access/egress modes we'll support
    var allSupportedActiveModes = collectAccessEgressModes(referenceNetwork.getModes().stream().filter(
        m -> m.getPredefinedModeType().isActiveModeType()).collect(Collectors.toSet()), settings);
    if(allSupportedActiveModes.isEmpty()){
      return;
    }
    var connectoidsByTransferZoneMapping = referenceZoning.getTransferConnectoids().createIndexByAccessZone();

    // disconnected rail stops without access egress modes
    var result = ZoningConverterUtils.findPtStopModeTransferZonesWithoutAccessEgressModeSupport(
        connectoidsByTransferZoneMapping, railBasedModes, allSupportedActiveModes);
    var railBasedTransferZones = result.first();
    if(railBasedTransferZones.isEmpty()){
      return;
    }
    LOGGER.info("Connecting rail-based waiting areas to active transport network");

    var railBasedTransferZonesDisconnectedFromActiveModesNetwork = result.second();
    LOGGER.info(String.format("Identified %d (%.2f%%) disconnected rail-based stops/stations from active road network",
        railBasedTransferZonesDisconnectedFromActiveModesNetwork.size(),
        (double)railBasedTransferZonesDisconnectedFromActiveModesNetwork.size()*100/railBasedTransferZones.size()));

    // connect those to the road network - note we do not ban usage of rail supporting links for active modes in
    // case this is for on-street tram in which case we would limit valid access/egress links/nodes.
    var numSuccess = railBasedTransferZonesDisconnectedFromActiveModesNetwork.stream().map( tz ->
            connectPtStopTransferZoneToNetworkWithAccessEgressModes(
                tz, connectoidsByTransferZoneMapping.get(tz), Collections.emptySet()  , allSupportedActiveModes,
                searchRadius, !boundingAreaHelper.fallsWithinSpatiallyEligibleBoundingArea(tz, false))
        ).map(b -> b ? 1 : 0).
        reduce(0, Integer::sum);
    LOGGER.info(String.format("Connected %d (%.2f%%) disconnected rail-based stops/stations to land network",
        numSuccess, (double)numSuccess*100/railBasedTransferZonesDisconnectedFromActiveModesNetwork.size()));
  }

  /**
   * Connect all bus stops to nearby network supporting pedestrian access/egress if not already present
   *
   * @param searchRadius max search radius acceptable between stop and road network
   * @param settings to apply
   */
  private void connectBusStopsToPedestrianNetwork(
      double searchRadius, AccessEgressInjectionSettings settings) {
    var referenceNetwork = zoningConverterData.getReferenceNetwork();
    var referenceZoning = zoningConverterData.getReferenceZoning();
    if(!referenceNetwork.getModes().containsPredefinedMode(PredefinedModeType.BUS)){
      return;
    }

    var busBasedModes = referenceNetwork.getModes().stream().filter(
        m -> m.getPredefinedModeType().equals(PredefinedModeType.BUS)).collect(Collectors.toSet());
    // access/egress modes we'll support (for bus we assume bicycles are not used (yet))
    var allSupportedActiveModes = collectAccessEgressModes(referenceNetwork.getModes().stream().filter(
        m -> m.getPredefinedModeType().equals(PredefinedModeType.PEDESTRIAN)).collect(Collectors.toSet()), settings);
    if(allSupportedActiveModes.isEmpty()){
      return;
    }

    var connectoidsByTransferZoneMapping = referenceZoning.getTransferConnectoids().createIndexByAccessZone();

    // relevant transfer zones
    var result = ZoningConverterUtils.findPtStopModeTransferZonesWithoutAccessEgressModeSupport(
        connectoidsByTransferZoneMapping, busBasedModes, allSupportedActiveModes);
    var busBasedTransferZones = result.first();
    if(busBasedTransferZones.isEmpty()){
      return;
    }
    LOGGER.info("Connecting bus-based waiting areas to active transport network");

    var busBasedTransferZonesDisconnectedFromActiveModesNetwork = result.second();
    LOGGER.info(String.format("Identified %d (%.2f%%) disconnected bus-based stops/stations from pedestrian network",
        busBasedTransferZonesDisconnectedFromActiveModesNetwork.size(),
        (double)busBasedTransferZonesDisconnectedFromActiveModesNetwork.size()*100/busBasedTransferZones.size()));

    // connect those to the network + for buses we do not consider links supporting buses attached to nodes as banned
    // for our access/egress active modes (unlike rail/feries)
    var numSuccess = busBasedTransferZonesDisconnectedFromActiveModesNetwork.stream().map( tz ->
        connectPtStopTransferZoneToNetworkWithAccessEgressModes(
            tz, connectoidsByTransferZoneMapping.get(tz), Collections.emptySet() , allSupportedActiveModes,
            searchRadius, !boundingAreaHelper.fallsWithinSpatiallyEligibleBoundingArea(tz, false))
        ).map(b -> b ? 1 : 0).
        reduce(0, Integer::sum);
    LOGGER.info(String.format("Connected %d (%.2f%%) disconnected bus-based stops to pedestrian network",
        numSuccess, (double)numSuccess*100/busBasedTransferZonesDisconnectedFromActiveModesNetwork.size()));
  }


  /**
   * The modes to create access/egress entries with: the preferred ones when available, otherwise the road based
   * modes if the settings allow falling back on those.
   * <p>
   * A scenario modelling only motorised modes has no active modes, in which case stops would not be connected to the
   * road network at all and nothing would say so. Finding where a stop meets that network does not require an active
   * mode to exist, the modes only decide which links are eligible to attach to.
   * </p>
   * <p>
   * The fallback modes do end up recorded on the created entry, so a consumer should read those as identifying the
   * attachment point rather than as stating who may travel there.
   * </p>
   *
   * @param preferredModes to use when available
   * @param settings to apply
   * @return modes to create the entries with, empty when none are available
   */
  private Set<Mode> collectAccessEgressModes(Set<Mode> preferredModes, AccessEgressInjectionSettings settings) {
    if(!preferredModes.isEmpty() || !settings.isFallbackOnRoadModesWithoutActiveModes()){
      return preferredModes;
    }

    var roadModes = zoningConverterData.getReferenceNetwork().getModes().stream().filter(
        m -> m.getPhysicalFeatures().getTrackType().equals(TrackModeType.ROAD)).collect(Collectors.toSet());
    if(!roadModes.isEmpty()){
      LOGGER.info(String.format("No active modes present to connect pt stops with, falling back on road modes [%s]",
          roadModes.stream().map(Mode::getName).collect(Collectors.joining(","))));
    }
    return roadModes;
  }

  /**
   * Constructor
   *
   * @param boundingAreaHelper to use
   * @param zoningConverterData to use
   */
  public TransferZoningInjectAccessEgressExecutor(
      final ProjectedBoundingAreaHelper boundingAreaHelper,
      final ZoningConverterCommonData zoningConverterData){

    this.boundingAreaHelper = boundingAreaHelper;
    this.zoningConverterData = zoningConverterData;
  }

  /**
   * Execute
   *
   * @param settings to apply
   */
  public void execute(AccessEgressInjectionSettings settings){

    /* both indexes are populated by readers as they parse, but are empty when working from a zoning parsed earlier.
     * Establishing them here keeps that distinction out of the caller: indexing an already indexed connectoid is
     * ignored, and the spatial index is only built when absent since recreating it is not free */
    zoningConverterData.getConnectoidData().indexExistingTransferConnectoids();
    if(!zoningConverterData.hasSpatiallyIndexedLinks()){
      zoningConverterData.recreateSpatiallyIndexedLinks();
    }

    // if configured connect all ferry stops to land network (for modes allowed on each ferry at waiting area)
    if(settings.isConnectFerryStopsToNearbyLandNetwork()) {
      connectFerriesToLandNetwork(settings.getFerryStopToNearbyLandNetworkSearchRadiusMeters());
    }

    // if configured connect all rail stops to active mode (pedestrian, bicycle) network
    if(settings.isConnectRailBasedStopsToPassengerNetwork()) {
      connectRailBasedStopsToActiveModeNetwork(
          settings.getRailBasedStopToPassengerNetworkSearchRadiusMeters(), settings);
    }

    // if configured connect all bus stops to pedestrian network
    if(settings.isConnectBusBasedStopsToPassengerNetwork()) {
      connectBusStopsToPedestrianNetwork(
          settings.getBusBasedStopToPassengerNetworkSearchRadiusMeters(), settings);
    }

  }

}
