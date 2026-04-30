package org.goplanit.converter.zoning;

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
   * When a rail based stop is disconnected from the land network it cannot be used for transfers, the provided distance
   * is the maximum distance it will use when creating links based on the accessible modes of the rail
   * to nearby road network
   */
  private double searchRadiusRailBasedStopToPassengerNetworkMeters = DEFAULT_SEARCH_RADIUS_RAIL_STOP_TO_ROAD_NETWORK_M;

  private double searchRadiusBusBasedStopToPassengerNetworkMeters = DEFAULT_SEARCH_RADIUS_BUS_STOP_TO_ROAD_NETWORK_M;

  /** by default, we connect dangling ferry stops to the nearest ferry route */
  public static boolean DEFAULT_CONNECT_FERRY_STOP_TO_PASSENGER_LAND_NETWORK = true;

  /** by default, we connect dangling rail based stops to the nearest road network link with eligible access modes */
  public static boolean DEFAULT_CONNECT_RAIL_STOP_TO_PASSENGER_NETWORK = true;

  /** by default, we connect bus based stops to the nearest road network link with eligible access modes */
  public static boolean DEFAULT_CONNECT_BUS_STOP_TO_PASSENGER_NETWORK = true;

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
   * flag for connecting ferry stops to nearby land network if not already connected
   * @return true when active, false otherwise
   */
  public boolean isConnectFerryStopsToNearbyLandNetwork() {
    return connectFerryStopToNearbyLandNetwork;
  }

  /** Decide whether to connect ferry stops to nearby land network if not already connected
   *
   * @param connectFerryStopToNearbyLandNetwork when true do this, when false do not
   */
  public void setConnectFerryStopsToNearbyLandNetwork(boolean connectFerryStopToNearbyLandNetwork) {
    this.connectFerryStopToNearbyLandNetwork = connectFerryStopToNearbyLandNetwork;
  }

  /**
   * Access to search radius for ferry stop to ferry route
   * @return search radius
   */
  public double getFerryStopToNearbyLandNetworkSearchRadiusMeters() {
    return searchRadiusFerryStopToLandNetworkMeters;
  }

  /**
   * flag for connecting rail based stops to nearby road network if not already connected
   * @return true when active, false otherwise
   */
  public boolean isConnectRailBasedStopsToPassengerNetwork() {
    return connectRailBasedStopToPassengerNetwork;
  }

  /** Decide whether to connect ferry stops to nearby land network if not already connected
   *
   * @param connectRailBasedStopToPassengerNetwork when true do this, when false do not
   */
  public void setConnectRailBasedStopsToPassengerNetwork(boolean connectRailBasedStopToPassengerNetwork) {
    this.connectRailBasedStopToPassengerNetwork = connectRailBasedStopToPassengerNetwork;
  }

  /**
   * Access to search radius for rail based stop to road network
   * @return search radius
   */
  public double getRailBasedStopToPassengerNetworkSearchRadiusMeters() {
    return searchRadiusRailBasedStopToPassengerNetworkMeters;
  }

  /**
   * Set  search radius for ferry stop to land network
   * @param searchRadiusFerryStopToLandNetworkMeters  search radius
   */
  public void setFerryStopToLandNetworkSearchRadiusMeters(double searchRadiusFerryStopToLandNetworkMeters) {
    this.searchRadiusFerryStopToLandNetworkMeters = searchRadiusFerryStopToLandNetworkMeters;
  }

  /**
   * flag for connecting bus based stops to nearby passenger network if not already connected
   * @return true when active, false otherwise
   */
  public boolean isConnectBusBasedStopsToPassengerNetwork() {
    return connectBusBasedStopToPassengerNetwork;
  }

  /** Decide whether to connect bus based stops to nearby road network if not already connected for access egress modes
   *
   * @param connectBusBasedStopToPassengerNetwork when true do this, when false do not
   */
  public void setConnectBusBasedStopsToPassengerNetwork(boolean connectBusBasedStopToPassengerNetwork) {
    this.connectBusBasedStopToPassengerNetwork = connectBusBasedStopToPassengerNetwork;
  }

  /**
   * Access to search radius for bus based stop to road network
   * @return search radius
   */
  public double getBusBasedStopToPassengerNetworkSearchRadiusMeters() {
    return searchRadiusBusBasedStopToPassengerNetworkMeters;
  }
}
