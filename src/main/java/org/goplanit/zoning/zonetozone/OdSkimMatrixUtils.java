package org.goplanit.zoning.zonetozone;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.transport.TransportModelNetworkImpl;
import org.goplanit.output.enums.SkimSubOutputType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.unit.DistanceUnit;
import org.goplanit.utils.unit.Unit;
import org.goplanit.utils.unit.UnitType;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.OdZones;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ToDoubleBiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utilities on OD skim matrices: creating the skims of several modes from a network, and inferring the cells of a skim
 * its search cannot reach
 * <p>
 * A skim made by a search holds no value, NaN, for a pair without a path, and other values come later by rules of
 * their own, such as the straight line between two zones or a distance within a zone. These read a skim's row and
 * fill its cells without a value, so a skim can be inferred whatever the rule, built from the network or made
 * elsewhere. A skim of a large network takes about a minute per mode, so the skims of several modes are built on one
 * join of the zoning to the network, once per proxy mode however many modes take it.
 * </p>
 *
 * @author markr
 */
public final class OdSkimMatrixUtils {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(OdSkimMatrixUtils.class.getCanonicalName());

  /** mean distance between two points drawn at random in a square of unit area, rounded */
  private static final double MEAN_DISTANCE_IN_UNIT_SQUARE = 0.52;

  /**
   * The skims of one type built from the network for the proxy mode of each given mode, searched from every OD zone,
   * or with trips given only from the zones some trip of a mode taking the skim leaves
   * <p>
   * The zoning is joined to the network once for all skims, as an assignment joins it, and the join undone once they
   * are built. The zones to search from are found before the join, which recreates the ids a hashed map finds the modes
   * by.
   * </p>
   *
   * @param network to skim, with a single layer
   * @param zoning whose OD zones are skimmed, with its crs set when cells are inferred
   * @param settings of the builder
   * @param skimType to build, of distance or of travel time
   * @param modes to create skims for
   * @param tripsByMode trips by pair of zones of each of the modes, null to search from every zone
   * @return the skim of each mode, modes taking one skim sharing its matrix
   */
  private static Map<Mode, OdSkimMatrix> createSkimsByMode(MacroscopicNetwork network, Zoning zoning,
      OdSkimBuilderSettings settings, SkimSubOutputType skimType, Collection<Mode> modes,
      Map<Mode, ? extends OdDemands> tripsByMode) {
    PlanItRunTimeException.throwIfNull(network, "Network to skim is null, not allowed");
    PlanItRunTimeException.throwIfNull(zoning, "Zoning to skim is null, not allowed");
    PlanItRunTimeException.throwIfNull(settings, "Settings of the skim are null, not allowed");
    PlanItRunTimeException.throwIfNull(modes, "Modes to skim for are null, not allowed");
    var modesByProxyMode = new LinkedHashMap<Mode, List<Mode>>();
    for (var mode : modes) {
      var proxyMode = settings.getProxyModeFor(mode);
      if (proxyMode != mode) {
        LOGGER.info(String.format("Mode %s takes the skims of mode %s", mode.getXmlId(), proxyMode.getXmlId()));
      }
      modesByProxyMode.computeIfAbsent(proxyMode, newProxyMode -> new ArrayList<>()).add(mode);
    }

    var odZones = zoning.getOdZones();
    var originsByProxyMode = new IdentityHashMap<Mode, Collection<OdZone>>();
    for (var modesOfProxyMode : modesByProxyMode.entrySet()) {
      Collection<OdZone> origins = odZones.toCollection();
      if (tripsByMode != null) {
        var searched = new boolean[odZones.size()];
        modesOfProxyMode.getValue().forEach(mode -> tripsByMode.get(mode).forEachNonZeroOdDemand(
            odZones, (origin, destination, trips) -> searched[(int) origin.getId()] = true));
        origins = odZones.stream().filter(zone -> searched[(int) zone.getId()]).collect(Collectors.toList());
      }
      originsByProxyMode.put(modesOfProxyMode.getKey(), origins);
    }

    var skimByProxyMode = new IdentityHashMap<Mode, OdSkimMatrix>();
    var transportNetwork = new TransportModelNetworkImpl(network, zoning);
    transportNetwork.integrateTransportNetworkViaConnectoids(true);
    try {
      var builder = new OdSkimBuilder(transportNetwork, settings, skimType);
      for (var proxyMode : modesByProxyMode.keySet()) {
        skimByProxyMode.put(proxyMode, builder.build(proxyMode, originsByProxyMode.get(proxyMode)).get(skimType));
      }
    } finally {
      transportNetwork.removeVirtualNetworkFromPhysicalNetwork(false);
    }
    var skimsByMode = new HashMap<Mode, OdSkimMatrix>();
    modesByProxyMode.forEach((proxyMode, modesOfProxyMode) -> modesOfProxyMode.forEach(
        mode -> skimsByMode.put(mode, skimByProxyMode.get(proxyMode))));
    return skimsByMode;
  }

  /** utilities, not to be instantiated */
  private OdSkimMatrixUtils() {
  }

  /**
   * A skim without a value in any cell, as a search leaves the skim of a mode without a path anywhere
   *
   * @param zones of the skim
   * @param skimType of the skim
   * @param unit of the skim's values, one that fits its type
   * @return the skim, NaN in every cell
   */
  public static OdSkimMatrix createSkimWithoutValues(OdZones zones, SkimSubOutputType skimType, Unit unit) {
    var skim = new OdSkimMatrix(zones, skimType, unit);
    zones.forEachOriginDestination((origin, destination) -> skim.setValue(origin, destination, Double.NaN));
    return skim;
  }

  /**
   * A skim without a value in any cell, in the default unit of its type, km for a distance and hours for a time
   *
   * @param zones of the skim
   * @param skimType of the skim
   * @return the skim, NaN in every cell
   */
  public static OdSkimMatrix createSkimWithoutValues(OdZones zones, SkimSubOutputType skimType) {
    return createSkimWithoutValues(zones, skimType, skimType.getDefaultUnit());
  }

  /**
   * Whether every skim holds its values in the given unit, checked once so a loop over many trips can read the skims
   * as they are rather than converting each value
   *
   * @param skims to check
   * @param unit to check for
   * @return true when every skim is in the unit
   */
  public static boolean areInUnit(Collection<? extends OdSkimMatrix> skims, Unit unit) {
    return skims.stream().allMatch(skim -> skim.isInUnit(unit));
  }

  /**
   * Mean of the smallest values in a zone's row of a skim, leaving out the zone's own cell and the cells without a value
   * <p>
   * The smallest few are kept in order as the row is read, so one pass over the row is enough: with values of 10, 2, 6
   * and 4 and three asked for, the mean is that of 2, 4 and 6, 4. Fewer than asked for with a value gives the mean of
   * those there are. For a distance skim, a zone's nearest zones by the network give the scale of a trip within it.
   * </p>
   *
   * @param skim to read
   * @param zone whose row to read
   * @param numberOfSmallest to take at most
   * @return the mean, NaN when no other cell of the row has a value
   */
  public static double findMeanOfSmallestInRow(OdSkimMatrix skim, Zone zone, int numberOfSmallest) {
    double[] smallest = new double[numberOfSmallest];
    int found = 0;
    for (var other : skim.getZones()) {
      if (other == zone) {
        continue;
      }
      double value = skim.getValue(zone, other);
      if (Double.isNaN(value) || (found == numberOfSmallest && value >= smallest[found - 1])) {
        continue;
      }
      int position = found < numberOfSmallest ? found++ : found - 1;
      while (position > 0 && smallest[position - 1] > value) {
        smallest[position] = smallest[position - 1];
        --position;
      }
      smallest[position] = value;
    }
    if (found == 0) {
      return Double.NaN;
    }
    double sum = 0;
    for (int index = 0; index < found; ++index) {
      sum += smallest[index];
    }
    return sum / found;
  }

  /**
   * Set every cell of a skim without a value to the value given for its pair of zones
   *
   * @param skim to fill
   * @param valueOfPair the value of a cell by its origin and destination zone, NaN to leave it without one
   */
  public static void fillCellsWithoutValue(OdSkimMatrix skim, ToDoubleBiFunction<Zone, Zone> valueOfPair) {
    for (var origin : skim.getZones()) {
      for (var destination : skim.getZones()) {
        if (Double.isNaN(skim.getValue(origin, destination))) {
          skim.setValue(origin, destination, valueOfPair.applyAsDouble(origin, destination));
        }
      }
    }
  }

  /**
   * Refuse a skim whose cells cannot be inferred: one of a type other than distance and travel time, or of travel time
   * for a mode without a straight line speed
   *
   * @param skimType of the skim
   * @param mode of the skim
   * @param settings for the straight line speeds
   */
  public static void verifyCellsInferable(SkimSubOutputType skimType, Mode mode, OdSkimBuilderSettings settings) {
    PlanItRunTimeException.throwIf(
        skimType != SkimSubOutputType.DISTANCE && skimType != SkimSubOutputType.TRAVEL_TIME,
        "Cells of a skim of type %s cannot be inferred, only of %s and %s", skimType, SkimSubOutputType.DISTANCE,
        SkimSubOutputType.TRAVEL_TIME);
    PlanItRunTimeException.throwIf(skimType == SkimSubOutputType.TRAVEL_TIME
            && Double.isNaN(settings.getStraightLineSpeedKmhFor(mode)),
        "Mode %s has no straight line speed, unable to infer the times its search cannot reach", mode.getXmlId());
  }

  /**
   * Infer the value of each cell of a skim its search cannot reach, and of each zone to itself, from the straight line
   * between the zones and the intrazonal rule of the settings
   * <p>
   * Every trip needs a length and a time, also those the network cannot measure: a pair of zones on parts of the
   * network not joined, a zone without a connectoid for the mode, a mode without a layer, and a trip within one zone,
   * which the search only gives the way out to the network and back for. A cell without a value gets the straight line
   * between the zones' points times the detour factor, a zone's point being the centre of its polygon, or its
   * centroid's position when it has no polygon. Within a zone, by the nearest neighbours rule, it gets a factor times
   * the mean of the zone's smallest values to other zones, after the common practice in four-step models; by the area
   * rule, and where nearest neighbours has nothing to go by, 0.52 times the square root of the zone's area, the mean
   * distance between two points drawn at random in a square of that area, or 0 for a zone without a polygon. A time
   * skim takes these distances at the mode's straight line speed, while the nearest neighbours rule reads its own
   * times. That rule reads the zone's row as the search left it, so every zone's value within it is set before any
   * other cell is filled.
   * </p>
   *
   * @param skim of distance or of travel time, inferred in its own unit, NaN where its search has no path, inferred in
   *             place
   * @param mode of the skim, whose straight line speed a time skim takes
   * @param zoning whose OD zones the skim is of, with its crs set
   * @param settings for the detour factor, the intrazonal rule and the straight line speeds
   */
  public static void inferUnreachableCells(OdSkimMatrix skim, Mode mode, Zoning zoning,
      OdSkimBuilderSettings settings) {
    PlanItRunTimeException.throwIfNull(skim, "Skim to infer is null, not allowed");
    PlanItRunTimeException.throwIfNull(mode, "Mode of the skim to infer is null, not allowed");
    PlanItRunTimeException.throwIfNull(zoning, "Zoning of the skim to infer is null, not allowed");
    PlanItRunTimeException.throwIfNull(settings, "Settings of the skim to infer are null, not allowed");
    var skimType = skim.getOdSkimOutputType();
    verifyCellsInferable(skimType, mode, settings);
    PlanItRunTimeException.throwIfNull(zoning.getCoordinateReferenceSystem(),
        "Zoning of the skim has no coordinate reference system, unable to measure straight lines");
    /* the straight line and the area rule give kilometres: a distance skim holds them in its own unit, a time skim the
     * time they take at the mode's straight line speed in km/h, in its own unit; both are a factor on the kilometres */
    double skimValuePerKm = skimType == SkimSubOutputType.TRAVEL_TIME
        ? Unit.HOUR.convertTo(skim.getUnit(), 1 / settings.getStraightLineSpeedKmhFor(mode))
        : Unit.KM.convertTo(skim.getUnit(), 1);
    DoubleUnaryOperator skimValueOfKm = km -> km * skimValuePerKm;
    var geoUtils = new PlanitJtsCrsUtils(zoning.getCoordinateReferenceSystem());
    var zones = zoning.getOdZones();
    var pointByZoneId = new Point[zones.size()];
    var withinZoneByZoneId = new double[zones.size()];
    for (var zone : zones) {
      pointByZoneId[(int) zone.getId()] = zone.getGeometryCentreOrCentroidPosition();
      double areaSquareMetres = zone.hasGeometry() ? geoUtils.getAreaInSquareMetres(zone.getGeometry()) : 0;
      double areaRuleKm =
          DistanceUnit.convertMeterTo(UnitType.KM, MEAN_DISTANCE_IN_UNIT_SQUARE * Math.sqrt(areaSquareMetres));
      withinZoneByZoneId[(int) zone.getId()] = skimValueOfKm.applyAsDouble(areaRuleKm);
    }
    if (settings.getIntrazonalDistanceMethod() == IntrazonalDistanceMethod.NEAREST_NEIGHBOURS) {
      for (var zone : zones) {
        double mean = findMeanOfSmallestInRow(skim, zone, settings.getNumberOfNearestZones());
        if (!Double.isNaN(mean)) {
          withinZoneByZoneId[(int) zone.getId()] = settings.getNearestZonesFactor() * mean;
        }
      }
    }
    for (var zone : zones) {
      skim.setValue(zone, zone, withinZoneByZoneId[(int) zone.getId()]);
    }
    fillCellsWithoutValue(skim, (origin, destination) -> {
      var from = pointByZoneId[(int) origin.getId()];
      var to = pointByZoneId[(int) destination.getId()];
      return from == null || to == null ? Double.NaN :
          skimValueOfKm.applyAsDouble(settings.getDetourFactor() * geoUtils.getDistanceInKilometres(from, to));
    });
  }

  /**
   * The skims of one type built from the network for the proxy mode of each given mode, searched from every OD zone,
   * once per proxy mode, on one join of the zoning to the network
   *
   * @param network to skim, with a single layer
   * @param zoning whose OD zones are skimmed, with its crs set when cells are inferred
   * @param settings of the builder, whose proxy modes decide the skim each mode takes
   * @param skimType to build, of distance or of travel time
   * @param modes to create skims for
   * @return the skim of each mode, modes taking one skim sharing its matrix
   */
  public static Map<Mode, OdSkimMatrix> createSkimsByMode(MacroscopicNetwork network, Zoning zoning,
      OdSkimBuilderSettings settings, SkimSubOutputType skimType, Collection<Mode> modes) {
    return createSkimsByMode(network, zoning, settings, skimType, modes, null);
  }

  /**
   * The skims as {@link #createSkimsByMode(MacroscopicNetwork, Zoning, OdSkimBuilderSettings, SkimSubOutputType,
   * Collection)} for the modes of the trips given, each skim searched only from the zones some trip of a mode taking
   * it leaves, as searching from the others costs time and finds pairs that do not matter
   *
   * @param network to skim, with a single layer
   * @param zoning whose OD zones are skimmed, with its crs set when cells are inferred
   * @param settings of the builder, whose proxy modes decide the skim each mode takes
   * @param skimType to build, of distance or of travel time
   * @param tripsByMode trips by pair of zones of each mode to create a skim for
   * @return the skim of each mode, modes taking one skim sharing its matrix
   */
  public static Map<Mode, OdSkimMatrix> createSkimsByMode(MacroscopicNetwork network, Zoning zoning,
      OdSkimBuilderSettings settings, SkimSubOutputType skimType, Map<Mode, ? extends OdDemands> tripsByMode) {
    PlanItRunTimeException.throwIfNull(tripsByMode, "Trips by mode to skim for are null, not allowed");
    return createSkimsByMode(network, zoning, settings, skimType, tripsByMode.keySet(), tripsByMode);
  }
}
