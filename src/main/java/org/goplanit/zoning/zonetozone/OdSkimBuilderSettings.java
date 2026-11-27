package org.goplanit.zoning.zonetozone;

import org.goplanit.algorithms.parallel.GenericBatchExecutorService;
import org.goplanit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.goplanit.utils.mode.Mode;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Settings of an {@link OdSkimBuilder}: how a connectoid is costed in the search, how many threads the searches run
 * on, which modes take the skims of another mode, and whether and how the cells the search cannot reach are inferred.
 * Each setter refuses a value that cannot be used with an {@link IllegalArgumentException} and keeps the value it had
 * <p>
 * The search costs a link segment by the cost the builder is given, but a connectoid has no travel time of its own,
 * so it is costed as its length at a speed, as the assignments cost it. The searches from each zone do not depend on
 * one another, so they run in parallel, and the skims are the same on any number of threads. A search leaves a cell
 * without a value where it has no path, which suits a caller that wants to see where the network has none; a caller
 * that needs a value in every cell, such as a length for every trip of a demand, has them inferred from the straight
 * line between the zones and a rule within a zone, see {@link OdSkimMatrixUtils#inferUnreachableCells}.
 * </p>
 *
 * @author markr
 */
public class OdSkimBuilderSettings {

  /** speed a connectoid is costed at in the search */
  private double connectoidSpeedKmh = DEFAULT_CONNECTOID_SPEED_KMH;

  /** threads the searches run on */
  private int numberOfThreads = GenericBatchExecutorService.getDefaultNumberOfThreads();

  /**
   * the mode whose skims a mode takes, for the modes that take another's, by the modes themselves, as joining a zoning
   * to a network recreates the ids a hashed map would find them by
   */
  private final Map<Mode, Mode> proxyModeByMode = new IdentityHashMap<>();

  /** true to infer the cells the search cannot reach, false to leave them without a value */
  private boolean inferUnreachableCells = DEFAULT_INFER_UNREACHABLE_CELLS;

  /** factor on the straight line between two zones, for a cell the search cannot reach */
  private double detourFactor = DEFAULT_DETOUR_FACTOR;

  /** how the value of a trip within one zone is inferred */
  private IntrazonalDistanceMethod intrazonalDistanceMethod = DEFAULT_INTRAZONAL_DISTANCE_METHOD;

  /** number of nearest other zones a trip within a zone is inferred from, for the nearest neighbours rule */
  private int numberOfNearestZones = DEFAULT_NUMBER_OF_NEAREST_ZONES;

  /** factor on the mean value to the nearest other zones, for the nearest neighbours rule */
  private double nearestZonesFactor = DEFAULT_NEAREST_ZONES_FACTOR;

  /**
   * the speed each mode with one travels a straight line at, for an inferred time, by the modes themselves, as joining
   * a zoning to a network recreates the ids a hashed map would find them by
   */
  private final Map<Mode, Double> straightLineSpeedKmhByMode = new IdentityHashMap<>();

  /**
   * Copy constructor
   *
   * @param other to copy
   */
  private OdSkimBuilderSettings(OdSkimBuilderSettings other) {
    this.connectoidSpeedKmh = other.connectoidSpeedKmh;
    this.numberOfThreads = other.numberOfThreads;
    this.proxyModeByMode.putAll(other.proxyModeByMode);
    this.inferUnreachableCells = other.inferUnreachableCells;
    this.detourFactor = other.detourFactor;
    this.intrazonalDistanceMethod = other.intrazonalDistanceMethod;
    this.numberOfNearestZones = other.numberOfNearestZones;
    this.nearestZonesFactor = other.nearestZonesFactor;
    this.straightLineSpeedKmhByMode.putAll(other.straightLineSpeedKmhByMode);
  }

  /** the speed the assignments cost a connectoid at */
  public static final double DEFAULT_CONNECTOID_SPEED_KMH = SpeedConnectoidTravelTimeCost.DEFAULT_CONNECTOID_SPEED_KPH;

  /** a caller sees where the network has no path, unless it asks for more */
  public static final boolean DEFAULT_INFER_UNREACHABLE_CELLS = false;

  /** roads run about a third longer than the straight line */
  public static final double DEFAULT_DETOUR_FACTOR = 1.3;

  /** needs no polygon, and follows the network's own values */
  public static final IntrazonalDistanceMethod DEFAULT_INTRAZONAL_DISTANCE_METHOD =
      IntrazonalDistanceMethod.NEAREST_NEIGHBOURS;

  /** the common choice for the nearest neighbours rule */
  public static final int DEFAULT_NUMBER_OF_NEAREST_ZONES = 3;

  /** a trip within a zone is about half as long as one to a neighbour */
  public static final double DEFAULT_NEAREST_ZONES_FACTOR = 0.5;

  /** Settings with every default */
  public OdSkimBuilderSettings() {
  }

  /**
   * A copy that can be changed without changing these settings
   *
   * @return the copy
   */
  public OdSkimBuilderSettings shallowClone() {
    return new OdSkimBuilderSettings(this);
  }

  /**
   * The speed a connectoid is costed at in the search
   *
   * @return the speed in km/h
   */
  public double getConnectoidSpeedKmh() {
    return connectoidSpeedKmh;
  }

  /**
   * Set the speed a connectoid is costed at in the search
   *
   * @param connectoidSpeedKmh above 0
   */
  public void setConnectoidSpeedKmh(double connectoidSpeedKmh) {
    if (connectoidSpeedKmh <= 0) {
      throw new IllegalArgumentException(String.format(
          "Connectoid speed must be above 0 km/h, was %f", connectoidSpeedKmh));
    }
    this.connectoidSpeedKmh = connectoidSpeedKmh;
  }

  /**
   * The number of threads the searches run on
   *
   * @return the number
   */
  public int getNumberOfThreads() {
    return numberOfThreads;
  }

  /**
   * Set the number of threads the searches run on. The skim is the same on any number
   *
   * @param numberOfThreads from 1 up to {@link GenericBatchExecutorService#getDefaultNumberOfThreads()}
   */
  public void setNumberOfThreads(int numberOfThreads) {
    GenericBatchExecutorService.verifyNumberOfThreads(numberOfThreads);
    this.numberOfThreads = numberOfThreads;
  }

  /**
   * The mode whose skims a mode takes
   *
   * @param mode to look up
   * @return the proxy mode set for it, or the mode itself when none is
   */
  public Mode getProxyModeFor(Mode mode) {
    return proxyModeByMode.getOrDefault(mode, mode);
  }

  /**
   * Set the mode whose skims a mode takes, its distances and times between and within zones, instead of skims of its
   * own. Naming the mode itself removes its proxy mode
   * <p>
   * Modes that go where another goes, such as a taxi where a car goes, would each repeat that mode's skims, so one
   * skim serves them all, the search and the inferred cells alike. A proxy mode is always one step away: a mode whose
   * skims another takes cannot take another's itself, and a mode that takes another's cannot lend its own.
   * </p>
   *
   * @param mode to set the proxy mode for, not null
   * @param proxyMode whose skims it takes, not null
   */
  public void setProxyModeFor(Mode mode, Mode proxyMode) {
    if (mode == null || proxyMode == null) {
      throw new IllegalArgumentException("Mode and its proxy mode must both be given, one was null");
    }
    if (mode == proxyMode) {
      proxyModeByMode.remove(mode);
      return;
    }
    if (proxyModeByMode.containsKey(proxyMode)) {
      throw new IllegalArgumentException(String.format(
          "Mode %s takes the skims of mode %s itself, unable to lend its skims to mode %s",
          proxyMode.getXmlId(), proxyModeByMode.get(proxyMode).getXmlId(), mode.getXmlId()));
    }
    if (proxyModeByMode.containsValue(mode)) {
      throw new IllegalArgumentException(String.format(
          "Mode %s lends its skims to another mode, unable to take the skims of mode %s",
          mode.getXmlId(), proxyMode.getXmlId()));
    }
    proxyModeByMode.put(mode, proxyMode);
  }

  /**
   * Whether the cells the search cannot reach are inferred
   *
   * @return true when inferred, false when left without a value
   */
  public boolean isInferUnreachableCells() {
    return inferUnreachableCells;
  }

  /**
   * Set whether the cells the search cannot reach, and each zone to itself, are inferred by the rules of these
   * settings, or left as the search leaves them, without a value where it has no path
   *
   * @param inferUnreachableCells true to infer them
   */
  public void setInferUnreachableCells(boolean inferUnreachableCells) {
    this.inferUnreachableCells = inferUnreachableCells;
  }

  /**
   * The factor on the straight line between two zones, for a cell the search cannot reach
   *
   * @return the factor
   */
  public double getDetourFactor() {
    return detourFactor;
  }

  /**
   * Set the factor on the straight line between two zones, for a cell the search cannot reach
   *
   * @param detourFactor 1 or more
   */
  public void setDetourFactor(double detourFactor) {
    if (detourFactor < 1) {
      throw new IllegalArgumentException(String.format("Detour factor must be 1 or more, was %f", detourFactor));
    }
    this.detourFactor = detourFactor;
  }

  /**
   * How the value of a trip within one zone is inferred
   *
   * @return the method
   */
  public IntrazonalDistanceMethod getIntrazonalDistanceMethod() {
    return intrazonalDistanceMethod;
  }

  /**
   * Set how the value of a trip within one zone is inferred
   *
   * @param intrazonalDistanceMethod to use, not null
   */
  public void setIntrazonalDistanceMethod(IntrazonalDistanceMethod intrazonalDistanceMethod) {
    if (intrazonalDistanceMethod == null) {
      throw new IllegalArgumentException("Intrazonal distance method must be given, was null");
    }
    this.intrazonalDistanceMethod = intrazonalDistanceMethod;
  }

  /**
   * The number of nearest other zones a trip within a zone is inferred from, for the nearest neighbours rule
   *
   * @return the number
   */
  public int getNumberOfNearestZones() {
    return numberOfNearestZones;
  }

  /**
   * Set the number of nearest other zones a trip within a zone is inferred from, for the nearest neighbours rule
   *
   * @param numberOfNearestZones 1 or more
   */
  public void setNumberOfNearestZones(int numberOfNearestZones) {
    if (numberOfNearestZones < 1) {
      throw new IllegalArgumentException(String.format(
          "Number of nearest zones must be 1 or more, was %d", numberOfNearestZones));
    }
    this.numberOfNearestZones = numberOfNearestZones;
  }

  /**
   * The factor on the mean value to the nearest other zones, for the nearest neighbours rule
   *
   * @return the factor
   */
  public double getNearestZonesFactor() {
    return nearestZonesFactor;
  }

  /**
   * Set the factor on the mean value to the nearest other zones, for the nearest neighbours rule
   *
   * @param nearestZonesFactor above 0
   */
  public void setNearestZonesFactor(double nearestZonesFactor) {
    if (nearestZonesFactor <= 0) {
      throw new IllegalArgumentException(String.format(
          "Nearest zones factor must be above 0, was %f", nearestZonesFactor));
    }
    this.nearestZonesFactor = nearestZonesFactor;
  }

  /**
   * The speed a mode travels a straight line at, for an inferred time
   *
   * @param mode to look up
   * @return the speed in km/h, NaN when none is set
   */
  public double getStraightLineSpeedKmhFor(Mode mode) {
    return straightLineSpeedKmhByMode.getOrDefault(mode, Double.NaN);
  }

  /**
   * Set the speed a mode travels a straight line at, for an inferred time: a straight line between two zones, and the
   * area rule within a zone, are distances, which a time skim takes at this speed
   * <p>
   * The only speed a mode has of its own is its maximum, which would make an inferred time far too short, so there is
   * no default, and times are only inferred for a mode with a speed set. A mode taking the skims of a proxy mode takes
   * the proxy mode's speed with them.
   * </p>
   *
   * @param mode to set the speed for, not null
   * @param straightLineSpeedKmh above 0
   */
  public void setStraightLineSpeedKmhFor(Mode mode, double straightLineSpeedKmh) {
    if (mode == null) {
      throw new IllegalArgumentException("Mode of a straight line speed must be given, was null");
    }
    if (!(straightLineSpeedKmh > 0)) {
      throw new IllegalArgumentException(String.format(
          "Straight line speed of mode %s must be above 0 km/h, was %f", mode.getXmlId(), straightLineSpeedKmh));
    }
    straightLineSpeedKmhByMode.put(mode, straightLineSpeedKmh);
  }
}
