package org.goplanit.converter.zoning.connectoid;

import org.goplanit.utils.mode.Mode;

import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Settings for creating OD connectoids for the zones of a zoning that have none, see
 * {@link org.goplanit.zoning.ZoningUtils#createOdConnectoids}. Each setter refuses a value that cannot be used with an
 * {@link IllegalArgumentException} and keeps the value it had
 * <p>
 * A zoning read without connectoids, such as zones from a shapefile, cannot be reached from a network until each zone
 * has a way in. These settings say for which modes, how many ways in each zone gets, how far from the zone they may
 * lie, on which roads, and whether links may be broken to place them.
 * </p>
 *
 * @author markr
 */
public class OdConnectoidCreationSettings {

  /** modes to create connectoids for, none meaning every mode of the network that lies on a layer */
  private SortedSet<Mode> modes = Collections.emptySortedSet();

  /** connectoids per zone and layer, shared by the modes of the layer */
  private int numberOfConnectoidsPerZone = DEFAULT_NUMBER_OF_CONNECTOIDS_PER_ZONE;

  /** the preferred distance from a zone's point is this factor times the zone's radius */
  private double preferredDistanceZoneRadiusFactor = DEFAULT_PREFERRED_DISTANCE_ZONE_RADIUS_FACTOR;

  /** no connectoid lies further than this from its zone's point */
  private double maximumDistanceMetres = DEFAULT_MAXIMUM_DISTANCE_METRES;

  /** where a connectoid joins the network */
  private OdConnectoidAttachment attachment = DEFAULT_ATTACHMENT;

  /** when breaking links, a point closer to a node of its link than this factor times the preferred distance uses the
   * node instead */
  private double minimumDistanceToNodeFactor = DEFAULT_MINIMUM_DISTANCE_TO_NODE_FACTOR;

  /** a connectoid only joins link segments no faster than this for its mode */
  private double maximumLinkSpeedKmh = DEFAULT_MAXIMUM_LINK_SPEED_KMH;

  /** when true, a connectoid only joins the largest strongly connected part of the network for its mode */
  private boolean mainNetworkOnly = DEFAULT_MAIN_NETWORK_ONLY;

  /** when true, the zoning's own OD connectoids are replaced, otherwise zones that have them keep them */
  private boolean replaceExistingConnectoids = DEFAULT_REPLACE_EXISTING_CONNECTOIDS;

  /**
   * Copy constructor
   *
   * @param other to copy
   */
  private OdConnectoidCreationSettings(OdConnectoidCreationSettings other) {
    this.modes = other.modes;
    this.numberOfConnectoidsPerZone = other.numberOfConnectoidsPerZone;
    this.preferredDistanceZoneRadiusFactor = other.preferredDistanceZoneRadiusFactor;
    this.maximumDistanceMetres = other.maximumDistanceMetres;
    this.attachment = other.attachment;
    this.minimumDistanceToNodeFactor = other.minimumDistanceToNodeFactor;
    this.maximumLinkSpeedKmh = other.maximumLinkSpeedKmh;
    this.mainNetworkOnly = other.mainNetworkOnly;
    this.replaceExistingConnectoids = other.replaceExistingConnectoids;
  }

  /** one way in per zone, the plainest join */
  public static final int DEFAULT_NUMBER_OF_CONNECTOIDS_PER_ZONE = 1;

  /** within about the zone itself, whatever its size */
  public static final double DEFAULT_PREFERRED_DISTANCE_ZONE_RADIUS_FACTOR = 1.0;

  /** far enough for a large rural zone to reach a road */
  public static final double DEFAULT_MAXIMUM_DISTANCE_METRES = 5000;

  /** the network is left as it is */
  public static final OdConnectoidAttachment DEFAULT_ATTACHMENT = OdConnectoidAttachment.EXISTING_NODE;

  /** a tenth of the preferred distance, so breaking a link never leaves a stub next to one of its nodes */
  public static final double DEFAULT_MINIMUM_DISTANCE_TO_NODE_FACTOR = 0.1;

  /** local and collector roads only, never a motorway, ramp or fast arterial */
  public static final double DEFAULT_MAXIMUM_LINK_SPEED_KMH = 60;

  /** no zone is tied to an island of the network */
  public static final boolean DEFAULT_MAIN_NETWORK_ONLY = true;

  /** a zoning's own connectoids win */
  public static final boolean DEFAULT_REPLACE_EXISTING_CONNECTOIDS = false;

  /** Settings with every default */
  public OdConnectoidCreationSettings() {
  }

  /**
   * A copy that can be changed without changing these settings
   *
   * @return the copy
   */
  public OdConnectoidCreationSettings shallowClone() {
    return new OdConnectoidCreationSettings(this);
  }

  /**
   * The modes to create connectoids for
   *
   * @return the modes, empty meaning every mode of the network that lies on a layer
   */
  public SortedSet<Mode> getModes() {
    return Collections.unmodifiableSortedSet(modes);
  }

  /**
   * Set the modes to create connectoids for
   *
   * @param modes one or more
   */
  public void setModes(Mode... modes) {
    if (modes == null || modes.length == 0) {
      throw new IllegalArgumentException("Modes to create OD connectoids for must be one or more, none given");
    }
    this.modes = new TreeSet<>(Arrays.asList(modes));
  }

  /**
   * The number of connectoids per zone and layer, shared by the modes of the layer
   *
   * @return the number
   */
  public int getNumberOfConnectoidsPerZone() {
    return numberOfConnectoidsPerZone;
  }

  /**
   * Set the number of connectoids per zone and layer. Covering every mode can take more
   *
   * @param numberOfConnectoidsPerZone 1 or more
   */
  public void setNumberOfConnectoidsPerZone(int numberOfConnectoidsPerZone) {
    if (numberOfConnectoidsPerZone < 1) {
      throw new IllegalArgumentException(String.format(
          "Number of OD connectoids per zone must be 1 or more, was %d", numberOfConnectoidsPerZone));
    }
    this.numberOfConnectoidsPerZone = numberOfConnectoidsPerZone;
  }

  /**
   * The factor on a zone's radius giving the preferred distance of its connectoids from its point
   *
   * @return the factor
   */
  public double getPreferredDistanceZoneRadiusFactor() {
    return preferredDistanceZoneRadiusFactor;
  }

  /**
   * Set the factor on a zone's radius giving the preferred distance of its connectoids from its point
   *
   * @param preferredDistanceZoneRadiusFactor above 0
   */
  public void setPreferredDistanceZoneRadiusFactor(double preferredDistanceZoneRadiusFactor) {
    if (preferredDistanceZoneRadiusFactor <= 0) {
      throw new IllegalArgumentException(String.format(
          "Preferred distance factor on the zone radius must be above 0, was %f", preferredDistanceZoneRadiusFactor));
    }
    this.preferredDistanceZoneRadiusFactor = preferredDistanceZoneRadiusFactor;
  }

  /**
   * The distance from its zone's point no connectoid lies beyond
   *
   * @return the distance in metres
   */
  public double getMaximumDistanceMetres() {
    return maximumDistanceMetres;
  }

  /**
   * Set the distance from its zone's point no connectoid lies beyond
   *
   * @param maximumDistanceMetres above 0
   */
  public void setMaximumDistanceMetres(double maximumDistanceMetres) {
    if (maximumDistanceMetres <= 0) {
      throw new IllegalArgumentException(String.format(
          "Maximum distance of an OD connectoid must be above 0 m, was %f", maximumDistanceMetres));
    }
    this.maximumDistanceMetres = maximumDistanceMetres;
  }

  /**
   * Where a connectoid joins the network
   *
   * @return the attachment
   */
  public OdConnectoidAttachment getAttachment() {
    return attachment;
  }

  /**
   * Set where a connectoid joins the network
   *
   * @param attachment to use, not null
   */
  public void setAttachment(OdConnectoidAttachment attachment) {
    if (attachment == null) {
      throw new IllegalArgumentException("Attachment of OD connectoids must be given, was null");
    }
    this.attachment = attachment;
  }

  /**
   * When breaking links, the factor on the preferred distance within which a point of a link uses the link's node
   * instead
   *
   * @return the factor
   */
  public double getMinimumDistanceToNodeFactor() {
    return minimumDistanceToNodeFactor;
  }

  /**
   * Set, for breaking links, the factor on the preferred distance within which a point of a link uses the link's node
   * instead
   *
   * @param minimumDistanceToNodeFactor above 0
   */
  public void setMinimumDistanceToNodeFactor(double minimumDistanceToNodeFactor) {
    if (minimumDistanceToNodeFactor <= 0) {
      throw new IllegalArgumentException(String.format(
          "Minimum distance factor to a node must be above 0, was %f", minimumDistanceToNodeFactor));
    }
    this.minimumDistanceToNodeFactor = minimumDistanceToNodeFactor;
  }

  /**
   * The speed a link segment may have at most for a connectoid of a mode to join it
   *
   * @return the speed in km/h
   */
  public double getMaximumLinkSpeedKmh() {
    return maximumLinkSpeedKmh;
  }

  /**
   * Set the speed a link segment may have at most for a connectoid of a mode to join it
   *
   * @param maximumLinkSpeedKmh above 0
   */
  public void setMaximumLinkSpeedKmh(double maximumLinkSpeedKmh) {
    if (maximumLinkSpeedKmh <= 0) {
      throw new IllegalArgumentException(String.format(
          "Maximum link speed for OD connectoids must be above 0 km/h, was %f", maximumLinkSpeedKmh));
    }
    this.maximumLinkSpeedKmh = maximumLinkSpeedKmh;
  }

  /**
   * Whether a connectoid only joins the largest strongly connected part of the network for its mode
   *
   * @return true when so
   */
  public boolean isMainNetworkOnly() {
    return mainNetworkOnly;
  }

  /**
   * Set whether a connectoid only joins the largest strongly connected part of the network for its mode
   *
   * @param mainNetworkOnly true when so
   */
  public void setMainNetworkOnly(boolean mainNetworkOnly) {
    this.mainNetworkOnly = mainNetworkOnly;
  }

  /**
   * Whether the zoning's own OD connectoids are replaced
   *
   * @return true when replaced, false when zones that have them keep them
   */
  public boolean isReplaceExistingConnectoids() {
    return replaceExistingConnectoids;
  }

  /**
   * Set whether the zoning's own OD connectoids are replaced
   * <p>
   * Replacing removes every OD connectoid of the zoning, also those allowing only modes not asked for, which are not
   * made again; the result reports those modes. Without replacing, a zone that has OD connectoids keeps them all, even
   * when none allows a mode asked for, and the result reports the zone without a connectoid for that mode.
   * </p>
   *
   * @param replaceExistingConnectoids true to replace them, false for zones that have them to keep them
   */
  public void setReplaceExistingConnectoids(boolean replaceExistingConnectoids) {
    this.replaceExistingConnectoids = replaceExistingConnectoids;
  }
}
