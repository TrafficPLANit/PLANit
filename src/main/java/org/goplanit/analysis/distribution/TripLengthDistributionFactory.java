package org.goplanit.analysis.distribution;

import org.goplanit.demands.Demands;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.trip.TripUtils;
import org.goplanit.userclass.UserClass;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.binning.BinningConfiguration;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.unit.DistanceUnit;
import org.goplanit.utils.unit.Unit;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;
import org.goplanit.zoning.zonetozone.OdSkimMatrixUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Builds trip length distributions from a demand, measuring each trip by the complete distance skim of its mode
 * <p>
 * A discrete demand and OD demand hold their trips differently, one trip per tour leg shared by its participants, or
 * a number per pair of zones, so each has its own way in; both end in the same distribution, so a sample can be
 * compared with its full demand, or a modelled demand with an observed one, bin by bin.
 * </p>
 *
 * @author markr
 */
public final class TripLengthDistributionFactory {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(TripLengthDistributionFactory.class.getCanonicalName());

  /** static factory, not to be instantiated */
  private TripLengthDistributionFactory() {
  }

  /**
   * The distribution of a discrete demand's trips per mode, in km, as
   * {@link #create(DiscreteDemands, Map, BinningConfiguration, DistanceUnit)}
   *
   * @param discreteDemands whose trips to count
   * @param distances complete distance skim of each mode to measure the trips by, in any distance unit
   * @param bins distance bins in km
   * @return the distribution per mode
   */
  public static TripLengthDistribution<Mode> create(
      DiscreteDemands discreteDemands, Map<Mode, OdSkimMatrix> distances, BinningConfiguration<Double> bins) {
    return create(discreteDemands, distances, bins, Unit.KM);
  }

  /**
   * The distribution of a discrete demand's trips per mode: each person taking part in a trip counts once, as the
   * trip of a tour shared by two persons is travelled twice, with the trip's distance between its origin and
   * destination zone, its tour's where the trip has none of its own
   * <p>
   * A trip without a mode is left out, and one without both zones, or whose mode has no skim, counts as out of range
   * without distance; how many trips without a mode or zones is logged. The skims' units are checked once: skims in
   * the distribution's unit are read as they are, others converted trip by trip.
   * </p>
   *
   * @param discreteDemands whose trips to count
   * @param distances complete distance skim of each mode to measure the trips by, in any distance unit
   * @param bins distance bins in the distance unit
   * @param distanceUnit of the distribution
   * @return the distribution per mode
   */
  public static TripLengthDistribution<Mode> create(DiscreteDemands discreteDemands,
      Map<Mode, OdSkimMatrix> distances, BinningConfiguration<Double> bins, DistanceUnit distanceUnit) {
    PlanItRunTimeException.throwIfNull(discreteDemands, "Discrete demands to measure are null, not allowed");
    PlanItRunTimeException.throwIfNull(distances, "Distances to measure trips by are null, not allowed");
    var distribution = new TripLengthDistribution<Mode>(bins, distanceUnit);
    boolean distancesInUnit = OdSkimMatrixUtils.areInUnit(distances.values(), distanceUnit);
    long withoutMode = 0;
    long withoutZones = 0;
    for (var trip : discreteDemands.getTrips()) {
      var mode = trip.getMode();
      if (mode == null) {
        ++withoutMode;
        continue;
      }
      if (trip.getOrigin(true) == null || trip.getDestination(true) == null) {
        ++withoutZones;
      }
      double length = distancesInUnit ? TripUtils.getDistance(trip, distances) :
          TripUtils.getDistance(trip, distances, distanceUnit);
      distribution.add(mode, length, trip.getNumberOfParticipants());
    }
    if (withoutMode > 0 || withoutZones > 0) {
      LOGGER.warning(String.format(
          "Trip length distribution: %d trips without a mode left out, %d without an origin or destination zone "
              + "counted out of range", withoutMode, withoutZones));
    }
    return distribution;
  }

  /**
   * The distribution of the OD demand of a time period per user class, in km, as
   * {@link #create(Demands, TimePeriod, Map, BinningConfiguration, DistanceUnit)}
   *
   * @param demands whose OD demand to count
   * @param timePeriod of the OD demand
   * @param distances complete distance skim of each mode to measure the pairs of zones by, in any distance unit, a
   *                  mode without one counting its trips out of range
   * @param bins distance bins in km
   * @return the distribution per user class
   */
  public static TripLengthDistribution<UserClass> create(
      Demands demands, TimePeriod timePeriod, Map<Mode, OdSkimMatrix> distances, BinningConfiguration<Double> bins) {
    return create(demands, timePeriod, distances, bins, Unit.KM);
  }

  /**
   * The distribution of the OD demand of a time period per user class: each cell's demand counts as that many trips,
   * fractions included, of the distance between its zones by the user class's mode
   * <p>
   * The OD demand of a time period is held per mode, in pcu per hour, so a user class's demand is that of its mode,
   * divided by the mode's pcu to count trips rather than pcus. Two user classes of one mode with demand in the period
   * cannot be told apart and are refused. Each skim's unit is checked once: a skim in the distribution's unit is read
   * as it is, another converted cell by cell.
   * </p>
   *
   * @param demands whose OD demand to count
   * @param timePeriod of the OD demand
   * @param distances complete distance skim of each mode to measure the pairs of zones by, in any distance unit, a
   *                  mode without one counting its trips out of range
   * @param bins distance bins in the distance unit
   * @param distanceUnit of the distribution
   * @return the distribution per user class
   */
  public static TripLengthDistribution<UserClass> create(Demands demands, TimePeriod timePeriod,
      Map<Mode, OdSkimMatrix> distances, BinningConfiguration<Double> bins, DistanceUnit distanceUnit) {
    PlanItRunTimeException.throwIfNull(demands, "Demands to measure are null, not allowed");
    PlanItRunTimeException.throwIfNull(timePeriod, "Time period to measure demands for is null, not allowed");
    PlanItRunTimeException.throwIfNull(distances, "Distances to measure trips by are null, not allowed");
    var distribution = new TripLengthDistribution<UserClass>(bins, distanceUnit);
    var userClassByMode = new HashMap<Mode, UserClass>();
    for (var userClass : demands.userClasses) {
      var mode = userClass.getMode();
      var odDemands = demands.get(mode, timePeriod);
      if (odDemands == null) {
        continue;
      }
      var otherUserClass = userClassByMode.put(mode, userClass);
      PlanItRunTimeException.throwIf(otherUserClass != null,
          "User classes %s and %s share the OD demand of mode %s, unable to tell their trips apart",
          otherUserClass == null ? null : otherUserClass.getXmlId(), userClass.getXmlId(), mode.getXmlId());

      var skim = distances.get(mode);
      boolean skimInUnit = skim != null && skim.isInUnit(distanceUnit);
      for (var cells = odDemands.iterator(); cells.hasNext(); ) {
        Double demandPcuHour = cells.next();
        if (demandPcuHour == null || !(demandPcuHour > 0)) {
          continue;
        }
        var origin = cells.getCurrentFromZone();
        var destination = cells.getCurrentToZone();
        double length = skim == null ? Double.NaN : skimInUnit ? skim.getValue(origin, destination) :
            skim.getValueConvertedTo(origin, destination, distanceUnit);
        distribution.add(userClass, length, demandPcuHour / mode.getPcu());
      }
    }
    return distribution;
  }
}
