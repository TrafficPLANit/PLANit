package org.goplanit.converter.zoning;

import org.goplanit.utils.exceptions.ExceptionUtils;
import org.goplanit.utils.settings.annotations.SettingsKeyLevel;
import org.goplanit.utils.settings.annotations.SettingsRead;
import org.goplanit.utils.settings.annotations.SettingsWrite;

/**
 * Settings of connecting public transport stops to the network for access and egress: for ferry, rail-based and bus
 * stops, whether a stop off the network its passengers walk or cycle on is connected to it, and how far is searched.
 * Each setting has a default; a setter refuses a value that makes no sense and leaves the setting as it was
 * <p>
 * A stop often lies just beside the roads and paths its passengers use, such as a station platform mapped apart from
 * the street, so without a connection no trip could reach it. A converter holds these settings as a section of its
 * own, so each converter connects its stops the same way
 * </p>
 *
 * @author markr
 */
public class AccessEgressInjectionSettings {

  /**
   * When a ferry stop is disconnected from to the land network it cannot be used for transfers, this
   * option will generate simple link connectoid based on the accessible modes of the ferry to nearby land network
   */
  private boolean connectFerryStopToNearbyLandNetwork = DEFAULT_CONNECT_FERRY_STOP_TO_PASSENGER_LAND_NETWORK;

  /**
   * When a ferry stop is disconnected from the land network it cannot be used for transfers, the provided distance
   * is the maximum distance it will use when creating links based on the accessible modes of the ferry
   * to nearby land network
   */
  private double  searchRadiusFerryStopToLandNetworkMeters = DEFAULT_SEARCH_RADIUS_FERRY_STOP_TO_LAND_NETWORK_M;

  /**
   * When a rail based stop is disconnected from the road network it cannot be used for transfers, this
   * option will generate connectoids based on the accessible modes of the rail (e.g., pedestrian, bicycle) to nearby
   * road network
   */
  private boolean connectRailBasedStopToPassengerNetwork = DEFAULT_CONNECT_RAIL_STOP_TO_PASSENGER_NETWORK;

  /**
   * When a bus based stop is disconnected from the road network it cannot be used for transfers, this
   * option will generate connectoids based on the accessible modes of the bus (e.g., pedestrian) to nearby
   * road network
   */
  private boolean connectBusBasedStopToPassengerNetwork = DEFAULT_CONNECT_BUS_STOP_TO_PASSENGER_NETWORK;

  /**
   * Stops are connected using active modes, which are absent when only motorised modes are modelled, for example a
   * scenario with just pt and car. This option then falls back on the road based modes to make the connection, since
   * finding where a stop meets the road network does not require an active mode to exist
   */
  private boolean fallbackOnRoadModesWithoutActiveModes = DEFAULT_FALLBACK_ON_ROAD_MODES_WITHOUT_ACTIVE_MODES;

  /**
   * When a rail based stop is disconnected from the land network it cannot be used for transfers, the provided distance
   * is the maximum distance it will use when creating links based on the accessible modes of the rail
   * to nearby road network
   */
  private double searchRadiusRailBasedStopToPassengerNetworkMeters = DEFAULT_SEARCH_RADIUS_RAIL_STOP_TO_ROAD_NETWORK_M;

  /**
   * When a bus based stop is disconnected from the road network it cannot be used for transfers, the provided distance
   * is the maximum distance it will use when creating links based on the accessible modes of the bus to nearby road
   * network
   */
  private double searchRadiusBusBasedStopToPassengerNetworkMeters = DEFAULT_SEARCH_RADIUS_BUS_STOP_TO_ROAD_NETWORK_M;

  /** by default, we connect dangling ferry stops to the nearest ferry route */
  public static boolean DEFAULT_CONNECT_FERRY_STOP_TO_PASSENGER_LAND_NETWORK = true;

  /** by default, we connect dangling rail based stops to the nearest road network link with eligible access modes */
  public static boolean DEFAULT_CONNECT_RAIL_STOP_TO_PASSENGER_NETWORK = true;

  /** by default, we connect bus based stops to the nearest road network link with eligible access modes */
  public static boolean DEFAULT_CONNECT_BUS_STOP_TO_PASSENGER_NETWORK = true;

  /** by default, we fall back on road based modes to connect stops when no active modes are modelled at all */
  public static boolean DEFAULT_FALLBACK_ON_ROAD_MODES_WITHOUT_ACTIVE_MODES = true;

  /**
   * default search radius in meters for mapping ferry stops to land network. When found and
   * {@link #isConnectFerryStopsToNearbyLandNetwork()} is true then a new connectoid to the nearest road
   * supporting non-ferry modes is created to avoid the ferry stop to be dangling
   */
  public static double DEFAULT_SEARCH_RADIUS_FERRY_STOP_TO_LAND_NETWORK_M = 500;

  /**
   * default search radius in meters for mapping rail based stops to road network. When found and
   * {@link #isConnectRailBasedStopsToPassengerNetwork()} is true then a new connectoid to the nearest road
   * supporting access mode is created to avoid the stop to be dangling
   */
  public static double DEFAULT_SEARCH_RADIUS_RAIL_STOP_TO_ROAD_NETWORK_M = 200;

  /**
   * default search radius in meters for mapping bus based stops to road network. When found and
   * {@link #isConnectBusBasedStopsToPassengerNetwork()} is true then a new connectoid to the nearest road
   * supporting configured access mode is created to avoid the stop to be dangling
   */
  public static double DEFAULT_SEARCH_RADIUS_BUS_STOP_TO_ROAD_NETWORK_M = 25;

  /**
   * Default constructor
   */
  public AccessEgressInjectionSettings(){}

  /**
   * flag for connecting ferry stops to nearby land network if not already connected
   * @return true when active, false otherwise
   */
  @SettingsRead(key = "connectFerryStopsToNearbyLandNetwork")
  public boolean isConnectFerryStopsToNearbyLandNetwork() {
    return connectFerryStopToNearbyLandNetwork;
  }

  /** Decide whether to connect ferry stops to nearby land network if not already connected
   *
   * @param connectFerryStopToNearbyLandNetwork when true do this, when false do not
   */
  @SettingsWrite(key = "connectFerryStopsToNearbyLandNetwork", level = SettingsKeyLevel.COMMON,
      description = "Connect a ferry stop off the land network to the nearest road its passengers can use")
  public void setConnectFerryStopsToNearbyLandNetwork(boolean connectFerryStopToNearbyLandNetwork) {
    this.connectFerryStopToNearbyLandNetwork = connectFerryStopToNearbyLandNetwork;
  }

  /**
   * Access to search radius for ferry stop to land network
   * @return search radius
   */
  @SettingsRead(key = "ferryStopToLandNetworkSearchRadiusMeters")
  public double getFerryStopToLandNetworkSearchRadiusMeters() {
    return searchRadiusFerryStopToLandNetworkMeters;
  }

  /**
   * Set  search radius for ferry stop to land network
   * @param searchRadiusFerryStopToLandNetworkMeters  search radius, above 0
   * @throws IllegalArgumentException naming the value, the setting unchanged, when it is not above 0
   */
  @SettingsWrite(key = "ferryStopToLandNetworkSearchRadiusMeters", level = SettingsKeyLevel.UNCOMMON,
      description = "Furthest the land network may be from a ferry stop connected to it, in metres")
  public void setFerryStopToLandNetworkSearchRadiusMeters(double searchRadiusFerryStopToLandNetworkMeters) {
    ExceptionUtils.throwIllegalSettingValueIf(!(searchRadiusFerryStopToLandNetworkMeters > 0),
        "Ferry stop to land network search radius", searchRadiusFerryStopToLandNetworkMeters, "above 0 m");
    this.searchRadiusFerryStopToLandNetworkMeters = searchRadiusFerryStopToLandNetworkMeters;
  }

  /**
   * flag for connecting rail based stops to nearby road network if not already connected
   * @return true when active, false otherwise
   */
  @SettingsRead(key = "connectRailBasedStopsToPassengerNetwork")
  public boolean isConnectRailBasedStopsToPassengerNetwork() {
    return connectRailBasedStopToPassengerNetwork;
  }

  /** Decide whether to connect rail based stops to nearby road network if not already connected
   *
   * @param connectRailBasedStopToPassengerNetwork when true do this, when false do not
   */
  @SettingsWrite(key = "connectRailBasedStopsToPassengerNetwork", level = SettingsKeyLevel.COMMON,
      description = "Connect a rail-based stop off the road network to the nearest road its passengers can walk or " +
          "cycle on")
  public void setConnectRailBasedStopsToPassengerNetwork(boolean connectRailBasedStopToPassengerNetwork) {
    this.connectRailBasedStopToPassengerNetwork = connectRailBasedStopToPassengerNetwork;
  }

  /**
   * Access to search radius for rail based stop to road network
   * @return search radius
   */
  @SettingsRead(key = "railBasedStopToPassengerNetworkSearchRadiusMeters")
  public double getRailBasedStopToPassengerNetworkSearchRadiusMeters() {
    return searchRadiusRailBasedStopToPassengerNetworkMeters;
  }

  /**
   * Set search radius for rail based stop to road network
   * @param searchRadiusRailBasedStopToPassengerNetworkMeters search radius, above 0
   * @throws IllegalArgumentException naming the value, the setting unchanged, when it is not above 0
   */
  @SettingsWrite(key = "railBasedStopToPassengerNetworkSearchRadiusMeters", level = SettingsKeyLevel.UNCOMMON,
      description = "Furthest the road network may be from a rail-based stop connected to it, in metres")
  public void setRailBasedStopToPassengerNetworkSearchRadiusMeters(
      double searchRadiusRailBasedStopToPassengerNetworkMeters) {
    ExceptionUtils.throwIllegalSettingValueIf(!(searchRadiusRailBasedStopToPassengerNetworkMeters > 0),
        "Rail-based stop to passenger network search radius", searchRadiusRailBasedStopToPassengerNetworkMeters,
        "above 0 m");
    this.searchRadiusRailBasedStopToPassengerNetworkMeters = searchRadiusRailBasedStopToPassengerNetworkMeters;
  }

  /**
   * flag for connecting bus based stops to nearby passenger network if not already connected
   * @return true when active, false otherwise
   */
  @SettingsRead(key = "connectBusBasedStopsToPassengerNetwork")
  public boolean isConnectBusBasedStopsToPassengerNetwork() {
    return connectBusBasedStopToPassengerNetwork;
  }

  /** Decide whether to connect bus based stops to nearby road network if not already connected for access egress modes
   *
   * @param connectBusBasedStopToPassengerNetwork when true do this, when false do not
   */
  @SettingsWrite(key = "connectBusBasedStopsToPassengerNetwork", level = SettingsKeyLevel.COMMON,
      description = "Connect a bus stop off the road network to the nearest road its passengers can walk on")
  public void setConnectBusBasedStopsToPassengerNetwork(boolean connectBusBasedStopToPassengerNetwork) {
    this.connectBusBasedStopToPassengerNetwork = connectBusBasedStopToPassengerNetwork;
  }

  /**
   * Access to search radius for bus based stop to road network
   * @return search radius
   */
  @SettingsRead(key = "busBasedStopToPassengerNetworkSearchRadiusMeters")
  public double getBusBasedStopToPassengerNetworkSearchRadiusMeters() {
    return searchRadiusBusBasedStopToPassengerNetworkMeters;
  }

  /**
   * Set search radius for bus based stop to road network
   * @param searchRadiusBusBasedStopToPassengerNetworkMeters search radius, above 0
   * @throws IllegalArgumentException naming the value, the setting unchanged, when it is not above 0
   */
  @SettingsWrite(key = "busBasedStopToPassengerNetworkSearchRadiusMeters", level = SettingsKeyLevel.UNCOMMON,
      description = "Furthest the road network may be from a bus stop connected to it, in metres")
  public void setBusBasedStopToPassengerNetworkSearchRadiusMeters(
      double searchRadiusBusBasedStopToPassengerNetworkMeters) {
    ExceptionUtils.throwIllegalSettingValueIf(!(searchRadiusBusBasedStopToPassengerNetworkMeters > 0),
        "Bus-based stop to passenger network search radius", searchRadiusBusBasedStopToPassengerNetworkMeters,
        "above 0 m");
    this.searchRadiusBusBasedStopToPassengerNetworkMeters = searchRadiusBusBasedStopToPassengerNetworkMeters;
  }

  /**
   * flag for falling back on road based modes to connect stops when no active modes are modelled
   * @return true when active, false otherwise
   */
  @SettingsRead(key = "fallbackOnRoadModesWithoutActiveModes")
  public boolean isFallbackOnRoadModesWithoutActiveModes() {
    return fallbackOnRoadModesWithoutActiveModes;
  }

  /** Decide whether to fall back on road based modes when no active modes are available to connect stops with. Without
   * this a scenario modelling only motorised modes leaves its stops disconnected from the road network entirely
   *
   * @param fallbackOnRoadModesWithoutActiveModes when true do this, when false do not
   */
  @SettingsWrite(key = "fallbackOnRoadModesWithoutActiveModes", level = SettingsKeyLevel.UNCOMMON,
      description = "Connect stops by the road modes when no walking or cycling mode is modelled")
  public void setFallbackOnRoadModesWithoutActiveModes(boolean fallbackOnRoadModesWithoutActiveModes) {
    this.fallbackOnRoadModesWithoutActiveModes = fallbackOnRoadModesWithoutActiveModes;
  }
}
