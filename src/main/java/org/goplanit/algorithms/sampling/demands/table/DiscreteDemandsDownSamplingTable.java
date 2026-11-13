package org.goplanit.algorithms.sampling.demands.table;

import org.goplanit.algorithms.sampling.demands.DiscreteDemandsDownSamplingSettings;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingCarAvailability;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingCheckedTable;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingModeGroup;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.household.Household;
import org.goplanit.demands.discrete.person.EmploymentStatus;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.person.Sex;
import org.goplanit.demands.discrete.person.StudyStatus;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.trip.TripUtils;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.HilbertCurveUtils;
import org.goplanit.utils.math.SparseIntRowMatrix;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.misc.binning.BinBuilder;
import org.goplanit.utils.misc.binning.BinningConfiguration;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.LocalTimeUtils;
import org.goplanit.utils.unit.DistanceUnit;
import org.goplanit.utils.unit.Unit;
import org.goplanit.utils.unit.UnitType;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;
import org.goplanit.zoning.zonetozone.OdSkimMatrixUtils;

import java.time.LocalTime;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.IntToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * What a down-sampling draws from: the demand as a table, with a row for each sampling unit and a column for each
 * category of every {@link DownSamplingBalanceGroup}, with the full demand's total of each category. Survey sampling
 * calls such a list of what a sample is drawn from a sampling frame. All terms are listed in
 * {@link org.goplanit.algorithms.sampling.demands}
 * <p>
 * A down-sampling keeps or drops whole sampling units, so it needs the counts of each rather than the demand's
 * objects. A sampling unit is a household with its persons, tours and trips; households whose persons share a tour
 * are one sampling unit, and a person without a household is one of their own. Each row holds the sampling unit's
 * zone, the ids of its households or person in the demand, and its values. The down-sampling table keeps counts only,
 * never the demand's objects, so it is compact, can be drawn from as often as needed without changing the demand, and
 * gives the statistics of any draw. The sampling units are ordered by their zone along a Hilbert curve and within a
 * zone in the order given, see {@link #createUnitOrder()}, and the categories that need the whole demand, the income
 * bands and the areas of the tour destinations, are binned once every sampling unit is counted.
 * </p>
 * <p>
 * Each person taking part in a trip makes a person trip, so a trip on a tour of two persons counts twice. A trip by a
 * mode its travellers drive, such as a car, is driven by the participant assumed to drive, see
 * {@link org.goplanit.demands.discrete.tour.ParticipantTour#isAssumedAsDriver()}, and ridden by the others; driven, it
 * is a car driven trip, whose kilometres are the vehicle kilometres. A trip without a tour is the trip of the person
 * whose schedule holds it, who drives it alone. A tour counts once, whoever takes part, and only a top-level tour
 * counts for purposes and destinations, a sub tour being part of its parent's day. Kilometres are counted in whole
 * metres, so every value is a count. A value not known is a category of its own, {@link #NOT_KNOWN}, and a category
 * no sampling unit has is left out.
 * </p>
 * <p>
 * The same pass counts what the statistics check but the draw does not balance: the cells of each
 * {@link DownSamplingCheckedTable}, and each sampling unit's car driven trips by the zone they leave from. The trips
 * between areas wait for the areas, so each person trip keeps its zones until every sampling unit is counted, see
 * {@link DownSamplingTripZones}.
 * </p>
 *
 * @author markr
 */
public class DiscreteDemandsDownSamplingTable {

  /** groups whose categories are the values found in the demand, sorted by label */
  private static final Set<DownSamplingBalanceGroup> GROUPS_SORTED_BY_LABEL =
      EnumSet.of(DownSamplingBalanceGroup.PERSON_TRIPS_BY_MODE, DownSamplingBalanceGroup.TOURS_BY_PURPOSE);

  /** sampling units kept an origin area needs at least, per total balanced over all sampling units */
  private static final int UNITS_KEPT_PER_TOTAL_OF_AN_ORIGIN_AREA = 4;

  /** hours of a day on the clock, for the person trips by mode and hour */
  private static final int HOURS_OF_THE_DAY = 24;

  /** label of the one category of the persons per zone */
  private static final String PERSONS = "persons";

  /** labels of a person known to hold a car licence, and known not to */
  private static final List<String> CAR_LICENCE_LABELS = List.of("yes", "no");

  /** end of a mode's label for a trip by a mode its travellers drive, made by the person driving */
  private static final String DRIVEN = " driven";

  /** end of a mode's label for a trip by a mode its travellers drive, made by a person riding along */
  private static final String RIDDEN = " ridden";

  /**
   * The key of a value's category from keys whose last is that of a value not known
   *
   * @param keysNotKnownLast the keys of a group's categories, the last for a value not known
   * @param index of the value's category, below 0 when not known
   * @return the key
   */
  private static int selectKey(int[] keysNotKnownLast, int index) {
    return keysNotKnownLast[index < 0 ? keysNotKnownLast.length - 1 : index];
  }

  /**
   * The key of a label of a group or checked table, registered on first use, the keys of all groups or of all checked
   * tables numbered on from each other so one sparse matrix holds them
   *
   * @param keyByLabel the key of each label, per group or checked table
   * @param labelOfKey the label of each key
   * @param group or checked table of the label
   * @param label to look up
   * @param <G> type of the group
   * @return the key
   */
  private static <G> int findKeyRegisteringOnFirstUse(
      Map<G, Map<String, Integer>> keyByLabel, List<String> labelOfKey, G group, String label) {
    return keyByLabel.computeIfAbsent(group, newGroup -> new LinkedHashMap<>()).computeIfAbsent(label, newLabel -> {
      labelOfKey.add(newLabel);
      return labelOfKey.size() - 1;
    });
  }

  /** the settings, a copy of those given */
  private final DiscreteDemandsDownSamplingSettings settings;

  /** distance skim of each mode each trip's distance comes from, dropped once the demand is counted */
  private Map<Mode, OdSkimMatrix> distances;

  /** true when every distance skim is in km, as they usually are, so each trip's distance is read as it is */
  private final boolean areAllDistancesInKm;

  /** position of each OD zone of the zoning along the Hilbert curve, -1 for a zone without a place */
  private final Map<OdZone, Integer> positionOfZone = new IdentityHashMap<>();

  /** number of OD zones with a place along the curve */
  private final int numberOfPlacedZones;

  /** the key of each category label, per group */
  private final Map<DownSamplingBalanceGroup, Map<String, Integer>> keyByLabel =
      new EnumMap<>(DownSamplingBalanceGroup.class);

  /** the category label of each key */
  private final List<String> labelOfKey = new ArrayList<>();

  /** the keys of the person kilometres, by mode group */
  private final int[] kilometreKeys;

  /** the keys of the person trips by distance band, by mode group, by band and the band not known last */
  private final int[][] distanceBandKeys;

  /** keys of the households by car availability, not known last */
  private final int[] carAvailabilityKeys;

  /** keys of the households by number of workers */
  private final int[] workerKeys;

  /** keys of the households by number of cars, not known last */
  private final int[] carKeys;

  /** keys of the households by size */
  private final int[] sizeKeys;

  /** keys of the persons by age band, not known last */
  private final int[] ageKeys;

  /** keys of the persons by employment status, not known last */
  private final int[] employmentKeys;

  /** keys of the persons by study status, not known last */
  private final int[] studyKeys;

  /** keys of the persons by car licence, not known last */
  private final int[] carLicenceKeys;

  /** keys of the persons by sex, not known last */
  private final int[] sexKeys;

  /** key of the tour destinations in each zone, by zone position, -1 for a zone not a destination yet */
  private final int[] destinationKeyByZonePosition;

  /** key of the tour destinations without a zone or in a zone without a place */
  private final int destinationNotKnownKey;

  /** the keys of each mode's person trips, driven then ridden for a mode its travellers drive */
  private final Map<Mode, int[]> modeKeys = new IdentityHashMap<>();

  /** the time the demand's day starts at */
  private LocalTime dayStart;

  /** the departure periods of the demand's day */
  private BinningConfiguration<Integer> departurePeriods;

  /** keys of the person trips by departure period, not known last */
  private int[] departurePeriodKeys;

  /** keys of the car driven trips by departure period, not known last */
  private int[] carDrivenPeriodKeys;

  /** the values of the sampling units, a row each, by key, a column each */
  private final SparseIntRowMatrix values = new SparseIntRowMatrix();

  /** the values of the sampling units in the checked tables, a row each, by checked key, a column each */
  private final SparseIntRowMatrix checkedValues = new SparseIntRowMatrix();

  /** the key of each checked cell label, per checked table */
  private final Map<DownSamplingCheckedTable, Map<String, Integer>> checkedKeyByLabel =
      new EnumMap<>(DownSamplingCheckedTable.class);

  /** the checked cell label of each checked key */
  private final List<String> labelOfCheckedKey = new ArrayList<>();

  /** by key of trips by mode, the checked keys of its person trips by hour, the hour not known last, -1 until used */
  private final List<int[]> hourKeysOfModeKey = new ArrayList<>();

  /** by key of trips by mode, the checked keys of its person trips by distance band, not known last, -1 until used */
  private final List<int[]> bandKeysOfModeKey = new ArrayList<>();

  /** the checked keys of the persons by age band and then sex, not known last for both, -1 until used */
  private final int[][] ageSexKeys;

  /** the car driven trips of the sampling units, a row each, by origin zone position, a column each */
  private final SparseIntRowMatrix carDrivenTripsByOrigin = new SparseIntRowMatrix();

  /** the zones of each person trip of the sampling units, for the trips between areas */
  private final DownSamplingTripZones tripZones = new DownSamplingTripZones();

  /** the zones with a place, in their order along the curve */
  private final List<OdZone> zonesAlongCurve;

  /** number of sampling units */
  private int numberOfUnits;

  /** zone position of each sampling unit, -1 for one without a zone */
  private int[] zonePositionOfUnit = new int[0];

  /** persons of each sampling unit */
  private int[] personsOfUnit = new int[0];

  /** first member of each sampling unit, and one past the last member */
  private int[] unitMemberStarts = new int[]{0};

  /** number of members, households and persons without a household */
  private int numberOfMembers;

  /** id of each member in the demand */
  private long[] memberIds = new long[0];

  /** true for a member that is a person without a household */
  private boolean[] memberIsPerson = new boolean[0];

  /** yearly income of each member, NaN when not known or for a person without a household */
  private double[] memberIncomes = new double[0];

  /** true once a sampling unit without a zone is added */
  private boolean anyUnitWithoutZone;

  /** the categories of each group by its ordinal */
  private List<List<String>> categoriesByGroup;

  /** the full totals of each group's categories by its ordinal */
  private List<long[]> fullTotalsByGroup;

  /** the first column of each group by its ordinal, its categories numbered on from it */
  private int[] firstColumnOfGroup;

  /** number of columns, the categories of all groups */
  private int numberOfColumns;

  /** the column of each key, -1 for a key in no category */
  private int[] columnOfKey;

  /** the income bands, null when no income is known */
  private BinningConfiguration<Double> incomeBands;

  /** the income category of a household whose income is not known */
  private int incomeNotKnownCategory;

  /** the area of each zone position along the curve */
  private int[] areaOfPosition;

  /** number of destination areas, and one more for an area not known, the last */
  private int areaSlots;

  /** the column of the origin area of each zone position along the curve, null without origin areas */
  private int[] originColumnOfPosition;

  /** the cells of each checked table by its ordinal */
  private List<List<String>> cellsByCheckedTable;

  /** per checked table by its ordinal, the cell of each checked key, -1 for a key in no cell of the table */
  private int[][] cellOfCheckedKey;

  /** the cell of each area pair and mode group in the trips between areas, -1 for one no person trip is in */
  private int[] cellOfAreaCell;

  /**
   * The key of a category, registered on first use
   *
   * @param group of the category
   * @param label of the category
   * @return the key
   */
  private int findKey(DownSamplingBalanceGroup group, String label) {
    return findKeyRegisteringOnFirstUse(keyByLabel, labelOfKey, group, label);
  }

  /**
   * The key of a checked cell, registered on first use
   *
   * @param checkedTable of the cell
   * @param label of the cell
   * @return the checked key
   */
  private int findCheckedKey(DownSamplingCheckedTable checkedTable, String label) {
    return findKeyRegisteringOnFirstUse(checkedKeyByLabel, labelOfCheckedKey, checkedTable, label);
  }

  /**
   * The checked key of a person trip's cell in a checked table crossing the mode of trips by mode with one more way
   * of counting, such as the hour, remembered per mode so a label is only made the first time
   *
   * @param keysOfModeKey the checked keys remembered, by key of trips by mode
   * @param modeKey of the person trip's mode in trips by mode
   * @param index of the other way of counting, the last for not known
   * @param slots of the other way of counting, not known included
   * @param checkedTable of the cell
   * @param labelOfIndex label of each index of the other way of counting
   * @return the checked key
   */
  private int findCheckedKeyOfModeKey(List<int[]> keysOfModeKey, int modeKey, int index, int slots,
      DownSamplingCheckedTable checkedTable, IntFunction<String> labelOfIndex) {
    while (keysOfModeKey.size() <= modeKey) {
      keysOfModeKey.add(null);
    }
    var keys = keysOfModeKey.get(modeKey);
    if (keys == null) {
      keys = new int[slots];
      Arrays.fill(keys, -1);
      keysOfModeKey.set(modeKey, keys);
    }
    if (keys[index] < 0) {
      keys[index] = findCheckedKey(checkedTable, labelOfKey.get(modeKey) + " " + labelOfIndex.apply(index));
    }
    return keys[index];
  }

  /**
   * The position of a zone along the curve
   *
   * @param zone to look up, null for none
   * @return the position, -1 for no zone or a zone without a place
   */
  private int findPosition(OdZone zone) {
    return zone == null ? -1 : positionOfZone.getOrDefault(zone, -1);
  }

  /**
   * Register the keys of a group's categories, in their order
   *
   * @param group of the categories
   * @param labels of the categories
   * @param withNotKnown when true a category for a value not known follows them
   * @return the keys, in the order of the labels
   */
  private int[] registerKeys(DownSamplingBalanceGroup group, List<String> labels, boolean withNotKnown) {
    var keys = labels.stream().mapToInt(label -> findKey(group, label));
    return (withNotKnown ? IntStream.concat(keys, IntStream.of(findKey(group, NOT_KNOWN))) : keys).toArray();
  }

  /**
   * Register the departure periods of the demand's day
   *
   * @param dayStartOfDemand the demand's day starts at
   */
  private void registerDeparturePeriods(LocalTime dayStartOfDemand) {
    dayStart = dayStartOfDemand;
    departurePeriods = settings.createDeparturePeriods(dayStart);
    departurePeriodKeys = registerKeys(
        DownSamplingBalanceGroup.PERSON_TRIPS_BY_DEPARTURE_PERIOD, departurePeriods.getLabels(), true);
    carDrivenPeriodKeys = registerKeys(
        DownSamplingBalanceGroup.CAR_DRIVEN_TRIPS_BY_DEPARTURE_PERIOD, departurePeriods.getLabels(), true);
  }

  /**
   * Verify a zone is one of the zoning's OD zones, and when asked that it has a place along the curve
   *
   * @param zone to verify, null for none, which passes
   * @param placeNeeded when true a zone without polygon or centroid position is refused
   * @param ofWhat the zone belongs to, for the message
   * @throws IllegalArgumentException when refused
   */
  private void verifyZone(OdZone zone, boolean placeNeeded, String ofWhat) {
    if (zone == null) {
      return;
    }
    var position = positionOfZone.get(zone);
    if (position == null) {
      throw new IllegalArgumentException(String.format(
          "Zone %s of %s is not an OD zone of the down-sampling's zoning", zone.getIdsAsString(), ofWhat));
    }
    if (placeNeeded && position < 0) {
      throw new IllegalArgumentException(String.format(
          "Zone %s of %s has neither a polygon nor a centroid position, so it has no place in the order of the zones",
          zone.getIdsAsString(), ofWhat));
    }
  }

  /**
   * Verify the demand can be counted, before any of it is
   * <p>
   * Everything the demand could be refused for is checked first, so a refusal comes before the work of counting. A
   * zone must be the zoning's, as the order of the zones and each trip's distance come from it; a household's zone, and
   * the zone of each sampling unit, must have a place, as the zone's id in the table is its place along the curve.
   * </p>
   *
   * @param demands to verify
   * @param demandUnits the demand's sampling units
   * @throws IllegalArgumentException when a zone is not of the zoning or a sampling unit's zone has no place
   */
  private void verifyDemand(DiscreteDemands demands, DownSamplingDemandUnits demandUnits) {
    for (var household : demands.getHouseholds()) {
      verifyZone(household.getZone(), true, "household " + household.getIdsAsString());
    }
    for (var unit : demandUnits.getUnits()) {
      verifyZone(unit.findZone(), true, "a sampling unit");
    }
    for (var tour : demands.getTours()) {
      verifyZone(tour.getOrigin(), false, "tour " + tour.getIdsAsString());
      verifyZone(tour.getDestination(), false, "tour " + tour.getIdsAsString());
    }
    for (var trip : demands.getTrips()) {
      verifyZone(trip.getOrigin(true), false, "trip " + trip.getIdsAsString());
      verifyZone(trip.getDestination(true), false, "trip " + trip.getIdsAsString());
    }
  }

  /**
   * Make room for the given numbers of sampling units and members, exactly, as the demand adds them all at once
   *
   * @param units to make room for
   * @param members to make room for
   */
  private void makeRoomForUnitsAndMembers(int units, int members) {
    zonePositionOfUnit = new int[units];
    personsOfUnit = new int[units];
    unitMemberStarts = new int[units + 1];
    memberIds = new long[members];
    memberIsPerson = new boolean[members];
    memberIncomes = new double[members];
  }

  /**
   * The key of a mode's person trips
   *
   * @param mode of the trips, null when not known
   * @param drives true when the person drives them
   * @return the key
   */
  private int findModeKey(Mode mode, boolean drives) {
    var group = DownSamplingBalanceGroup.PERSON_TRIPS_BY_MODE;
    if (mode == null) {
      return findKey(group, NOT_KNOWN);
    }
    var keys = modeKeys.computeIfAbsent(mode, newMode -> newMode.isSelfDrivenMotorisedMode() ?
        new int[]{findKey(group, newMode.getXmlId() + DRIVEN), findKey(group, newMode.getXmlId() + RIDDEN)} :
        new int[]{findKey(group, newMode.getXmlId())});
    return drives || keys.length == 1 ? keys[0] : keys[1];
  }

  /**
   * Count one person's trip in the sampling unit being filled: in the groups, in the checked tables by mode and hour
   * and by mode and distance band, its zones for the trips between areas, and when driven by car its origin zone
   *
   * @param trip made
   * @param distanceKm of the trip, NaN when not known
   * @param assumedAsDriver true when the person is assumed to drive the trip if its mode needs one of its travellers to
   */
  private void addPersonTrip(Trip trip, double distanceKm, boolean assumedAsDriver) {
    var mode = trip.getMode();
    boolean drives = assumedAsDriver && mode != null && mode.isSelfDrivenMotorisedMode();
    int modeKey = findModeKey(mode, drives);
    values.add(modeKey, 1);

    var modeGroup = DownSamplingModeGroup.of(mode, drives);
    boolean distanceKnown = !Double.isNaN(distanceKm);
    var bands = settings.getDistanceBands();
    int band = distanceKnown ? bands.binIndexOf(distanceKm) : -1;
    values.add(kilometreKeys[modeGroup.ordinal()],
        distanceKnown ? (int) Math.round(DistanceUnit.convertKmTo(UnitType.METER, distanceKm)) : 0);
    values.add(selectKey(distanceBandKeys[modeGroup.ordinal()], band), 1);

    int period = trip.getStartTime() == null ? -1 : departurePeriods.binIndexOf(
        (int) LocalTimeUtils.secondsFromWrapAroundDayAnchor(dayStart, trip.getStartTime()));
    values.add(selectKey(departurePeriodKeys, period), 1);
    if (drives) {
      values.add(selectKey(carDrivenPeriodKeys, period), 1);
    }

    int hour = trip.getStartTime() == null ? HOURS_OF_THE_DAY : trip.getStartTime().getHour();
    checkedValues.add(findCheckedKeyOfModeKey(hourKeysOfModeKey, modeKey, hour, HOURS_OF_THE_DAY + 1,
        DownSamplingCheckedTable.PERSON_TRIPS_BY_MODE_AND_HOUR,
        index -> index == HOURS_OF_THE_DAY ? NOT_KNOWN : StringUtils.zeroPaddedStringOf(index, 2)), 1);
    checkedValues.add(findCheckedKeyOfModeKey(bandKeysOfModeKey, modeKey, band < 0 ? bands.size() : band,
        bands.size() + 1, DownSamplingCheckedTable.PERSON_TRIPS_BY_MODE_AND_DISTANCE_BAND,
        index -> index == bands.size() ? NOT_KNOWN : bands.getLabels().get(index)), 1);

    int origin = findPosition(trip.getOrigin(true));
    tripZones.add(origin, findPosition(trip.getDestination(true)), modeGroup);
    if (drives && origin >= 0) {
      carDrivenTripsByOrigin.add(origin, 1);
    }
  }

  /**
   * Count a household in the sampling unit being filled
   *
   * @param household to count
   * @param persons of the household
   */
  private void addHousehold(Household household, List<Person> persons) {
    int drivers = (int) persons.stream().filter(person -> person.mayDrive(settings.getMinimumDrivingAge())).count();
    int workers = (int) persons.stream().filter(Person::isKnownToBeEmployed).count();
    var cars = household.getNumberOfCars();
    values.add(selectKey(carAvailabilityKeys,
        cars == null ? -1 : DownSamplingCarAvailability.of(cars, drivers).ordinal()), 1);
    values.add(selectKey(carKeys, cars == null ? -1 : settings.getCarsPerHouseholdBands().binIndexOf(cars)), 1);
    values.add(workerKeys[settings.getWorkersPerHouseholdBands().binIndexOf(workers)], 1);
    values.add(sizeKeys[settings.getHouseholdSizeBands().binIndexOf(persons.size())], 1);
  }

  /**
   * Count a person in the sampling unit being filled, with their trips without a tour
   *
   * @param person to count
   */
  private void addPerson(Person person) {
    var ageBands = settings.getAgeBands();
    int ageBand = person.getAge() == null ? -1 : ageBands.binIndexOf(person.getAge());
    values.add(selectKey(ageKeys, ageBand), 1);
    EmploymentStatus employment = person.getEmploymentStatus();
    values.add(selectKey(employmentKeys, employment == null ? -1 : employment.ordinal()), 1);
    StudyStatus study = person.getStudyStatus();
    values.add(selectKey(studyKeys, study == null ? -1 : study.ordinal()), 1);
    Boolean carLicence = person.getCarLicence();
    values.add(selectKey(carLicenceKeys, carLicence == null ? -1 : carLicence ? 0 : 1), 1);
    Sex sex = person.getSex();
    values.add(selectKey(sexKeys, sex == null ? -1 : sex.ordinal()), 1);

    int ageIndex = ageBand < 0 ? ageBands.size() : ageBand;
    int sexIndex = sex == null ? Sex.values().length : sex.ordinal();
    if (ageSexKeys[ageIndex][sexIndex] < 0) {
      ageSexKeys[ageIndex][sexIndex] = findCheckedKey(DownSamplingCheckedTable.PERSONS_BY_AGE_BAND_AND_SEX,
          (ageBand < 0 ? NOT_KNOWN : ageBands.getLabels().get(ageBand)) + " " +
              (sex == null ? NOT_KNOWN : sex.name()));
    }
    checkedValues.add(ageSexKeys[ageIndex][sexIndex], 1);

    for (var element : person.getSchedule()) {
      if (element instanceof Trip && ((Trip) element).getTour() == null) {
        var trip = (Trip) element;
        addPersonTrip(trip, findDistanceKmOf(trip), true);
      }
    }
  }

  /**
   * Count a top-level tour in the sampling unit being filled: its purpose, with the mode of its first trip in the
   * checked table, and where it goes
   *
   * @param tour to count
   */
  private void addTour(Tour tour) {
    var purpose = tour.getPurpose() == null ? NOT_KNOWN : tour.getPurpose();
    values.add(findKey(DownSamplingBalanceGroup.TOURS_BY_PURPOSE, purpose), 1);
    var mode = tour.getOutboundMode();
    checkedValues.add(findCheckedKey(DownSamplingCheckedTable.TOURS_BY_PURPOSE_AND_MODE,
        purpose + " " + (mode == null ? NOT_KNOWN : mode.getXmlId())), 1);
    int position = findPosition(tour.getDestination());
    if (position < 0) {
      values.add(destinationNotKnownKey, 1);
      return;
    }
    if (destinationKeyByZonePosition[position] < 0) {
      destinationKeyByZonePosition[position] =
          findKey(DownSamplingBalanceGroup.TOUR_DESTINATIONS_BY_AREA, String.valueOf(position));
    }
    values.add(destinationKeyByZonePosition[position], 1);
  }

  /**
   * The distance of a trip in km, the unit of the distance bands and the kilometres, read as its skim holds it when
   * every skim is in km, and converted otherwise
   *
   * @param trip to measure
   * @return the distance in km, NaN when it cannot be measured
   */
  private double findDistanceKmOf(Trip trip) {
    return areAllDistancesInKm ? TripUtils.getDistance(trip, distances) : TripUtils.getDistance(trip, distances, Unit.KM);
  }

  /**
   * Add a member of the sampling unit being added, a household or a person without a household
   *
   * @param id of the household or person in the demand
   * @param isPerson true for a person without a household
   * @param income the household's yearly income, NaN when not known or for a person without a household
   */
  private void addMember(long id, boolean isPerson, double income) {
    memberIds[numberOfMembers] = id;
    memberIsPerson[numberOfMembers] = isPerson;
    memberIncomes[numberOfMembers] = income;
    ++numberOfMembers;
  }

  /**
   * Add a sampling unit of the demand: its members, zone and persons, and its values
   * <p>
   * The sampling unit's values come from its households and their persons, its persons without a household, its
   * top-level tours, and the trips on its tours, each trip once per person taking part. A household of two adults
   * whose joint tour by car has a trip there and one back adds two car driven trips for the one assumed to drive and
   * two car ridden trips for the other, one tour of its purpose, and a household of size 2.
   * </p>
   *
   * @param unit the sampling unit
   */
  private void addUnit(DownSamplingUnit unit) {
    int persons = 0;
    for (var household : unit.getHouseholds()) {
      addMember(household.getId(), false,
          household.getYearlyIncome() == null ? Double.NaN : household.getYearlyIncome());
      var householdPersons = unit.getPersonsOf(household);
      persons += householdPersons.size();
      addHousehold(household, householdPersons);
      householdPersons.forEach(this::addPerson);
    }
    for (var person : unit.getPersonsWithoutHousehold()) {
      addMember(person.getId(), true, Double.NaN);
      ++persons;
      addPerson(person);
    }
    unit.getTours().stream().filter(tour -> !tour.hasParentTour()).forEach(this::addTour);
    for (var trip : unit.getTrips()) {
      double distanceKm = findDistanceKmOf(trip);
      trip.getTour().getParticipantTours().forEach(
          participation -> addPersonTrip(trip, distanceKm, participation.isAssumedAsDriver()));
    }
    values.finishRow();
    checkedValues.finishRow();
    carDrivenTripsByOrigin.finishRow();
    tripZones.finishRow();

    int position = findPosition(unit.findZone());
    anyUnitWithoutZone |= position < 0;
    zonePositionOfUnit[numberOfUnits] = position;
    personsOfUnit[numberOfUnits] = persons;
    unitMemberStarts[++numberOfUnits] = numberOfMembers;
  }

  /**
   * The members of a sampling unit that are households, or that are persons without a household
   *
   * @param unit sampling unit, from 0
   * @param persons true for the persons without a household, false for the households
   * @return the members
   */
  private IntStream streamMembers(int unit, boolean persons) {
    return IntStream.range(unitMemberStarts[unit], unitMemberStarts[unit + 1])
        .filter(member -> memberIsPerson[member] == persons);
  }

  /**
   * The income category of a household member
   *
   * @param member a household
   * @return the category
   */
  private int findIncomeCategory(int member) {
    return Double.isNaN(memberIncomes[member]) ? incomeNotKnownCategory : incomeBands.binIndexOf(memberIncomes[member]);
  }

  /**
   * The categories of a group of keys: those with a value in a sampling unit, in the order registered, or by label
   * for a group whose values are those found in the demand, the value not known last either way
   *
   * @param group whose categories to collect
   * @param firstColumn of the group
   */
  private void collectCategoriesOfKeys(DownSamplingBalanceGroup group, int firstColumn) {
    var keys = keyByLabel.getOrDefault(group, Map.of()).values().stream().filter(key -> values.getColumnTotal(key) > 0);
    if (GROUPS_SORTED_BY_LABEL.contains(group)) {
      keys = keys.sorted(Comparator.comparing((Integer key) -> NOT_KNOWN.equals(labelOfKey.get(key)))
          .thenComparing(labelOfKey::get));
    }
    var orderedKeys = keys.collect(Collectors.toList());
    for (int category = 0; category < orderedKeys.size(); ++category) {
      columnOfKey[orderedKeys.get(category)] = firstColumn + category;
    }
    categoriesByGroup.add(orderedKeys.stream().map(labelOfKey::get).collect(Collectors.toUnmodifiableList()));
    fullTotalsByGroup.add(orderedKeys.stream().mapToLong(values::getColumnTotal).toArray());
  }

  /**
   * The income bands: as many as the settings say, each holding about as many of the known incomes of all households,
   * the fifths by default, and their income not known
   */
  private void binHouseholdIncomes() {
    var knownIncomes = IntStream.range(0, numberOfMembers)
        .filter(member -> !memberIsPerson[member] && !Double.isNaN(memberIncomes[member]))
        .mapToDouble(member -> memberIncomes[member]).sorted().toArray();
    incomeBands = knownIncomes.length == 0 ?
        null : BinBuilder.ofEqualCountDoubleBins(knownIncomes, settings.getNumberOfIncomeBands());
    incomeNotKnownCategory = incomeBands == null ? 0 : incomeBands.size();

    var totals = new long[incomeNotKnownCategory + 1];
    IntStream.range(0, numberOfMembers).filter(member -> !memberIsPerson[member])
        .forEach(member -> ++totals[findIncomeCategory(member)]);
    var categories = new ArrayList<>(incomeBands == null ? List.of() : incomeBands.getLabels());
    if (totals[incomeNotKnownCategory] > 0) {
      categories.add(NOT_KNOWN);
    }
    categoriesByGroup.add(List.copyOf(categories));
    fullTotalsByGroup.add(Arrays.copyOf(totals, categories.size()));
  }

  /**
   * Bin the zones in their order along the curve, each bin a run of neighbouring zones with about equal counts, each
   * zone's count standing for as many values at its position, see
   * {@link BinBuilder#ofEqualCountDoubleBins(double[], int)}
   *
   * @param countAtPosition the count of each zone position
   * @param numberOfBins to make, at least one; fewer come out when one zone holds more than a bin's share
   * @return the bins of zone positions, null when no zone has a count
   */
  private BinningConfiguration<Double> binAlongCurve(IntToLongFunction countAtPosition, int numberOfBins) {
    var positions = IntStream.range(0, numberOfPlacedZones)
        .flatMap(position -> IntStream.generate(() -> position).limit(countAtPosition.applyAsLong(position)))
        .asDoubleStream().toArray();
    return positions.length == 0 ? null : BinBuilder.ofEqualCountDoubleBins(positions, numberOfBins);
  }

  /**
   * The destination areas: the zones in their order along the curve binned into runs with about equal numbers of tour
   * destinations, as many as the settings say, numbered from 1, and the destinations not known. Every zone with a place
   * gets its area, for the trips between areas; a zone before the first area's first destination is in the first area
   *
   * @param firstColumn of the group
   */
  private void binTourDestinationsIntoAreas(int firstColumn) {
    var areas = binAlongCurve(position -> destinationKeyByZonePosition[position] < 0 ?
        0 : values.getColumnTotal(destinationKeyByZonePosition[position]), settings.getNumberOfDestinationAreas());
    int numberOfAreas = areas == null ? 0 : areas.size();
    areaSlots = numberOfAreas + 1;
    areaOfPosition = IntStream.range(0, numberOfPlacedZones)
        .map(position -> areas == null ? numberOfAreas : Math.max(0, areas.binIndexOf((double) position))).toArray();

    var totals = new long[numberOfAreas + 1];
    for (int position = 0; position < numberOfPlacedZones; ++position) {
      int key = destinationKeyByZonePosition[position];
      if (key >= 0) {
        int area = areas.binIndexOf((double) position);
        columnOfKey[key] = firstColumn + area;
        totals[area] += values.getColumnTotal(key);
      }
    }
    var categories = IntStream.rangeClosed(1, numberOfAreas).mapToObj(String::valueOf)
        .collect(Collectors.toCollection(ArrayList::new));
    if (values.getColumnTotal(destinationNotKnownKey) > 0) {
      columnOfKey[destinationNotKnownKey] = firstColumn + numberOfAreas;
      totals[numberOfAreas] = values.getColumnTotal(destinationNotKnownKey);
      categories.add(NOT_KNOWN);
    }
    categoriesByGroup.add(List.copyOf(categories));
    fullTotalsByGroup.add(Arrays.copyOf(totals, categories.size()));
  }

  /**
   * The origin areas: the zones in their order along the curve binned into runs with about equal numbers of car driven
   * trips leaving them, numbered from 1, as many as the settings and the sampling units kept allow, none below 2
   * <p>
   * The car driven trips of all zones are matched as a whole, which leaves a region with more of them than it should
   * have and another with fewer. Matched per area as well, each region keeps its share, but every total balanced costs
   * the draw some freedom, and too many leave it unable to meet them, so the floor gives each area at least 4 sampling
   * units kept per total balanced over all zones: at a rate of 0.1 of 2.6 million sampling units, with 134 such totals,
   * at most 485 areas. Persons per zone are left out of that count, being held per zone rather than over all zones.
   * The areas are binned last, after every other group, so the count is known. One area would only repeat the car
   * driven trips of all zones, so below 2 there are none. A zone before the first area's first car driven trip is in
   * the first area.
   * </p>
   *
   * @param firstColumn of the group
   */
  private void binCarDrivenTripsIntoOriginAreas(int firstColumn) {
    int totalsBalanced = settings.getBalanceOrder().stream()
        .filter(group -> group != DownSamplingBalanceGroup.PERSONS_PER_ZONE
            && group != DownSamplingBalanceGroup.CAR_DRIVEN_TRIPS_BY_ORIGIN_AREA)
        .mapToInt(group -> categoriesByGroup.get(group.ordinal()).size()).sum();
    int allowed = Math.min(settings.getMaximumNumberOfOriginAreas(), (int) (settings.getDownSampleRate() * numberOfUnits
        / (UNITS_KEPT_PER_TOTAL_OF_AN_ORIGIN_AREA * (double) totalsBalanced)));
    var areas = allowed < 2 ? null : binAlongCurve(carDrivenTripsByOrigin::getColumnTotal, allowed);
    if (areas == null) {
      categoriesByGroup.add(List.of());
      fullTotalsByGroup.add(new long[0]);
      return;
    }
    originColumnOfPosition = IntStream.range(0, numberOfPlacedZones)
        .map(position -> firstColumn + Math.max(0, areas.binIndexOf((double) position))).toArray();
    var totals = new long[areas.size()];
    for (int position = 0; position < numberOfPlacedZones; ++position) {
      totals[originColumnOfPosition[position] - firstColumn] += carDrivenTripsByOrigin.getColumnTotal(position);
    }
    categoriesByGroup.add(IntStream.rangeClosed(1, areas.size()).mapToObj(String::valueOf)
        .collect(Collectors.toUnmodifiableList()));
    fullTotalsByGroup.add(totals);
  }

  /**
   * The cells of the trips between areas: those some person trip falls in, by mode group, then origin area, then
   * destination area, labelled such as "CAR_DRIVER 1 to 2", an area not known as {@link #NOT_KNOWN}
   *
   * @return the cell labels
   */
  private List<String> collectAreaCells() {
    var totals = tripZones.collectCellTotals(areaOfPosition, areaSlots);
    cellOfAreaCell = new int[totals.length];
    Arrays.fill(cellOfAreaCell, -1);
    IntFunction<String> areaLabel = area -> area == areaSlots - 1 ? NOT_KNOWN : String.valueOf(area + 1);
    var cells = new ArrayList<String>();
    for (int cell = 0; cell < totals.length; ++cell) {
      if (totals[cell] > 0) {
        cellOfAreaCell[cell] = cells.size();
        var modeGroup = DownSamplingModeGroup.values()[cell / (areaSlots * areaSlots)];
        cells.add(modeGroup + " " + areaLabel.apply((cell / areaSlots) % areaSlots) + " to "
            + areaLabel.apply(cell % areaSlots));
      }
    }
    return List.copyOf(cells);
  }

  /**
   * Collect the cells of every checked table: those some sampling unit has, sorted by label, and for the trips between
   * areas in the order of their mode groups and areas
   * <p>
   * A cell is registered the first time a sampling unit has a value for it, so the cells are only known once every
   * sampling unit is counted. The trips between areas wait for the destination areas as well, which only the whole
   * demand's tour destinations decide: until then each person trip keeps its zones, see {@link DownSamplingTripZones}.
   * </p>
   */
  private void collectCellsOfCheckedTables() {
    var checkedTables = DownSamplingCheckedTable.values();
    cellsByCheckedTable = new ArrayList<>(checkedTables.length);
    cellOfCheckedKey = new int[checkedTables.length][];
    for (var checkedTable : checkedTables) {
      if (checkedTable == DownSamplingCheckedTable.PERSON_TRIPS_BY_AREAS_AND_MODE_GROUP) {
        cellsByCheckedTable.add(collectAreaCells());
        continue;
      }
      var keys = checkedKeyByLabel.getOrDefault(checkedTable, Map.of()).values().stream()
          .filter(key -> checkedValues.getColumnTotal(key) > 0)
          .sorted(Comparator.comparing(labelOfCheckedKey::get)).collect(Collectors.toList());
      var cellOfKey = new int[labelOfCheckedKey.size()];
      Arrays.fill(cellOfKey, -1);
      for (int cell = 0; cell < keys.size(); ++cell) {
        cellOfKey[keys.get(cell)] = cell;
      }
      cellOfCheckedKey[checkedTable.ordinal()] = cellOfKey;
      cellsByCheckedTable.add(keys.stream().map(labelOfCheckedKey::get).collect(Collectors.toUnmodifiableList()));
    }
  }

  /**
   * Collect the categories of every group and the cells of every checked table, once every sampling unit is counted,
   * as some need the whole demand
   * <p>
   * A category is registered when a sampling unit first has a value for it, and the income bands and the areas are
   * binned from every sampling unit's values, so nothing can be numbered before. The groups then take their columns
   * in the order of {@link DownSamplingBalanceGroup}, each a run of its categories. The origin areas come last because
   * how many there may be depends on the number of categories of every other group balanced. The distance skims are
   * dropped here, every trip having been counted.
   * </p>
   */
  private void collectGroupCategoriesAndCells() {
    var groups = DownSamplingBalanceGroup.values();
    categoriesByGroup = new ArrayList<>(groups.length);
    fullTotalsByGroup = new ArrayList<>(groups.length);
    firstColumnOfGroup = new int[groups.length];
    columnOfKey = new int[labelOfKey.size()];
    Arrays.fill(columnOfKey, -1);
    int column = 0;
    for (var group : groups) {
      firstColumnOfGroup[group.ordinal()] = column;
      switch (group) {
        case PERSONS_PER_ZONE:
          categoriesByGroup.add(List.of(PERSONS));
          fullTotalsByGroup.add(new long[]{Arrays.stream(personsOfUnit, 0, numberOfUnits).asLongStream().sum()});
          break;
        case HOUSEHOLDS_BY_INCOME_BAND:
          binHouseholdIncomes();
          break;
        case TOUR_DESTINATIONS_BY_AREA:
          binTourDestinationsIntoAreas(column);
          break;
        case CAR_DRIVEN_TRIPS_BY_ORIGIN_AREA:
          binCarDrivenTripsIntoOriginAreas(column);
          break;
        default:
          collectCategoriesOfKeys(group, column);
      }
      column += categoriesByGroup.get(group.ordinal()).size();
    }
    numberOfColumns = column;
    collectCellsOfCheckedTables();
    distances = null;
  }

  /** label of a category whose value is not known */
  public static final String NOT_KNOWN = "not known";

  /**
   * Constructor, putting the zoning's OD zones in their order along a Hilbert curve and counting the demand, which is
   * checked in full first and not changed
   * <p>
   * A zone's place is the centre of its polygon, or its centroid's position when it has no polygon; a zone with
   * neither has no place, and a household in it is refused. The curve runs over the zones of the whole zoning, not
   * only those of the demand, so the zones of one zoning come in the same order whatever demand is down-sampled.
   * </p>
   *
   * @param settings of the down-sampling, copied
   * @param zoning whose OD zones the demand's zones are
   * @param distances complete distance skim of each mode, in any distance unit, each trip's distance coming from its
   *                  mode's, used only while counting
   * @param demands to count, with its households, persons, tours and trips, and its persons without a household
   * @throws IllegalArgumentException when a zone of the demand is not of the zoning, a sampling unit's zone has no
   *                                  place, or a person is not of the demand
   */
  public DiscreteDemandsDownSamplingTable(DiscreteDemandsDownSamplingSettings settings, Zoning zoning,
      Map<Mode, OdSkimMatrix> distances, DiscreteDemands demands) {
    PlanItRunTimeException.throwIfNull(settings, "Settings of the down-sampling table are null, not allowed");
    PlanItRunTimeException.throwIfNull(zoning, "Zoning of the down-sampling table is null, not allowed");
    PlanItRunTimeException.throwIfNull(distances, "Distances of the down-sampling table are null, not allowed");
    PlanItRunTimeException.throwIfNull(demands, "Demand of the down-sampling table is null, not allowed");
    this.settings = settings.shallowClone();
    this.distances = distances;
    this.areAllDistancesInKm = OdSkimMatrixUtils.areInUnit(distances.values(), Unit.KM);

    var placedZones = new ArrayList<OdZone>();
    for (var zone : zoning.getOdZones()) {
      if (zone.getGeometryCentreOrCentroidPosition() == null) {
        positionOfZone.put(zone, -1);
      } else {
        placedZones.add(zone);
      }
    }
    var curveOrder = HilbertCurveUtils.sortAlongCurve(
        placedZones, zone -> zone.getGeometryCentreOrCentroidPosition().getCoordinate());
    for (int position = 0; position < curveOrder.size(); ++position) {
      positionOfZone.put(curveOrder.get(position), position);
    }
    numberOfPlacedZones = curveOrder.size();
    zonesAlongCurve = Collections.unmodifiableList(curveOrder);
    destinationKeyByZonePosition = new int[numberOfPlacedZones];
    Arrays.fill(destinationKeyByZonePosition, -1);
    ageSexKeys = new int[this.settings.getAgeBands().size() + 1][Sex.values().length + 1];
    for (var keysOfAgeBand : ageSexKeys) {
      Arrays.fill(keysOfAgeBand, -1);
    }

    kilometreKeys = registerKeys(DownSamplingBalanceGroup.PERSON_KILOMETRES_BY_MODE_GROUP,
        StringUtils.toStrings(DownSamplingModeGroup.values()), false);
    distanceBandKeys = new int[DownSamplingModeGroup.values().length][];
    for (var modeGroup : DownSamplingModeGroup.values()) {
      var labels = this.settings.getDistanceBands().getLabels().stream()
          .map(band -> modeGroup + " " + band).collect(Collectors.toList());
      labels.add(modeGroup + " " + NOT_KNOWN);
      distanceBandKeys[modeGroup.ordinal()] =
          registerKeys(DownSamplingBalanceGroup.PERSON_TRIPS_BY_DISTANCE_BAND_AND_MODE_GROUP, labels, false);
    }
    destinationNotKnownKey = findKey(DownSamplingBalanceGroup.TOUR_DESTINATIONS_BY_AREA, NOT_KNOWN);
    carAvailabilityKeys = registerKeys(DownSamplingBalanceGroup.HOUSEHOLDS_BY_CAR_AVAILABILITY,
        StringUtils.toStrings(DownSamplingCarAvailability.values()), true);
    workerKeys = registerKeys(DownSamplingBalanceGroup.HOUSEHOLDS_BY_NUMBER_OF_WORKERS,
        this.settings.getWorkersPerHouseholdBands().getLabels(), false);
    carKeys = registerKeys(DownSamplingBalanceGroup.HOUSEHOLDS_BY_NUMBER_OF_CARS,
        this.settings.getCarsPerHouseholdBands().getLabels(), true);
    sizeKeys = registerKeys(
        DownSamplingBalanceGroup.HOUSEHOLDS_BY_SIZE, this.settings.getHouseholdSizeBands().getLabels(), false);
    ageKeys = registerKeys(DownSamplingBalanceGroup.PERSONS_BY_AGE_BAND, this.settings.getAgeBands().getLabels(), true);
    employmentKeys = registerKeys(
        DownSamplingBalanceGroup.PERSONS_BY_EMPLOYMENT_STATUS, StringUtils.toStrings(EmploymentStatus.values()), true);
    studyKeys = registerKeys(
        DownSamplingBalanceGroup.PERSONS_BY_STUDY_STATUS, StringUtils.toStrings(StudyStatus.values()), true);
    carLicenceKeys = registerKeys(DownSamplingBalanceGroup.PERSONS_BY_CAR_LICENCE, CAR_LICENCE_LABELS, true);
    sexKeys = registerKeys(DownSamplingBalanceGroup.PERSONS_BY_SEX, StringUtils.toStrings(Sex.values()), true);

    var demandUnits = new DownSamplingDemandUnits(demands);
    verifyDemand(demands, demandUnits);
    registerDeparturePeriods(demands.getDayStartTime());
    makeRoomForUnitsAndMembers(demandUnits.getUnits().size(), demandUnits.getNumberOfMembers());
    demandUnits.getUnits().forEach(this::addUnit);
    collectGroupCategoriesAndCells();
  }

  /**
   * Number of sampling units, the rows of the down-sampling table
   *
   * @return the number
   */
  public int size() {
    return numberOfUnits;
  }

  /**
   * Number of zones in the table: the zoning's OD zones with a place along the curve, and one more when a sampling
   * unit has no zone
   *
   * @return the number
   */
  public int getNumberOfZonesInTable() {
    return numberOfPlacedZones + (anyUnitWithoutZone ? 1 : 0);
  }

  /**
   * The zone id in the table of a sampling unit: its zone's position along the curve, from 0, or one past the last
   * zone for one without a zone, so zones next to each other along the curve have ids next to each other
   *
   * @param unit sampling unit, from 0
   * @return the zone id in the table
   */
  public int getZoneIdInTable(int unit) {
    return zonePositionOfUnit[unit] < 0 ? numberOfPlacedZones : zonePositionOfUnit[unit];
  }

  /**
   * The persons of a sampling unit, the total each zone keeps besides its sampling units
   *
   * @param unit sampling unit, from 0
   * @return the number of persons
   */
  public int getPersons(int unit) {
    return personsOfUnit[unit];
  }

  /**
   * The ids of a sampling unit's households in the demand, by which a down-sampling removes them or names them as kept
   *
   * @param unit sampling unit, from 0
   * @return the ids, in member order
   */
  public long[] getHouseholdIds(int unit) {
    return streamMembers(unit, false).mapToLong(member -> memberIds[member]).toArray();
  }

  /**
   * The ids of a sampling unit's persons without a household in the demand
   *
   * @param unit sampling unit, from 0
   * @return the ids, in member order
   */
  public long[] getPersonIdsWithoutHousehold(int unit) {
    return streamMembers(unit, true).mapToLong(member -> memberIds[member]).toArray();
  }

  /**
   * The sampling units in the order a draw takes them: by zone id in the table, so zones next to each other along the
   * curve follow each other, and within a zone in the order given, those with a household before persons without one
   * <p>
   * The order given is the id, and ids follow the order a demand is read in, so the same demand read again gives the
   * same order and a draw with the same seed keeps the same households.
   * </p>
   *
   * @return the sampling units in order
   */
  public int[] createUnitOrder() {
    /* sampling units are numbered as they are added, by their first member, the households before the persons without
     * a household and each by id */
    return IntStream.range(0, numberOfUnits).boxed()
        .sorted(Comparator.comparingInt(this::getZoneIdInTable)
            .thenComparing(unit -> memberIsPerson[unitMemberStarts[unit]]).thenComparingInt(unit -> unit))
        .mapToInt(Integer::intValue).toArray();
  }

  /**
   * The categories of a group, those some sampling unit has, with the value not known last
   *
   * @param group of the categories
   * @return the category labels, unmodifiable
   */
  public List<String> getCategories(DownSamplingBalanceGroup group) {
    return categoriesByGroup.get(group.ordinal());
  }

  /**
   * Number of columns: the categories of every group, the groups in the order of {@link DownSamplingBalanceGroup}
   *
   * @return the number
   */
  public int getNumberOfColumns() {
    return numberOfColumns;
  }

  /**
   * The column of a group's first category, the others following it in the order of {@link #getCategories}
   *
   * @param group of the categories
   * @return the column
   */
  public int getFirstColumn(DownSamplingBalanceGroup group) {
    return firstColumnOfGroup[group.ordinal()];
  }

  /**
   * How much of its group's own unit one value stored in a group's column is: person kilometres are held as whole
   * metres, so one stored is a thousandth of a kilometre; every other group counts, so one stored is one
   *
   * @param group of the column
   * @return the amount
   */
  public double getGroupUnitsPerStoredValue(DownSamplingBalanceGroup group) {
    return group == DownSamplingBalanceGroup.PERSON_KILOMETRES_BY_MODE_GROUP ?
        DistanceUnit.convertMeterTo(UnitType.KM, 1) : 1;
  }

  /**
   * The row of a sampling unit: its value in every column, see {@link #getFirstColumn(DownSamplingBalanceGroup)}
   *
   * @param unit sampling unit, from 0
   * @param row to write the values to, as long as {@link #getNumberOfColumns()}; what it held is overwritten
   */
  public void collectValues(int unit, double[] row) {
    Arrays.fill(row, 0);
    values.addRowTo(unit, columnOfKey, row);
    row[getFirstColumn(DownSamplingBalanceGroup.PERSONS_PER_ZONE)] = getPersons(unit);
    int firstIncomeColumn = getFirstColumn(DownSamplingBalanceGroup.HOUSEHOLDS_BY_INCOME_BAND);
    streamMembers(unit, false).forEach(member -> ++row[firstIncomeColumn + findIncomeCategory(member)]);
    if (originColumnOfPosition != null) {
      carDrivenTripsByOrigin.addRowTo(unit, originColumnOfPosition, row);
    }
  }

  /**
   * The value of a sampling unit for a category of a group
   *
   * @param unit sampling unit, from 0
   * @param group of the category
   * @param category index in the group's categories
   * @return the value, a count, or for kilometres whole metres
   */
  public int getValue(int unit, DownSamplingBalanceGroup group, int category) {
    Objects.checkIndex(category, getCategories(group).size());
    var row = new double[getNumberOfColumns()];
    collectValues(unit, row);
    return (int) row[getFirstColumn(group) + category];
  }

  /**
   * The full demand's total for a category of a group
   *
   * @param group of the category
   * @param category index in the group's categories
   * @return the total
   */
  public long getFullTotal(DownSamplingBalanceGroup group, int category) {
    Objects.checkIndex(category, getCategories(group).size());
    return fullTotalsByGroup.get(group.ordinal())[category];
  }

  /**
   * The cells of a checked table, those some sampling unit has
   *
   * @param checkedTable of the cells
   * @return the cell labels, unmodifiable
   */
  public List<String> getCategories(DownSamplingCheckedTable checkedTable) {
    return cellsByCheckedTable.get(checkedTable.ordinal());
  }

  /**
   * The values of a sampling unit in every cell of a checked table, see
   * {@link #getCategories(DownSamplingCheckedTable)}
   *
   * @param unit sampling unit, from 0
   * @param checkedTable of the cells
   * @param cells to write the values to, as long as the checked table's cells; what it held is overwritten
   */
  public void collectValues(int unit, DownSamplingCheckedTable checkedTable, double[] cells) {
    Arrays.fill(cells, 0);
    if (checkedTable == DownSamplingCheckedTable.PERSON_TRIPS_BY_AREAS_AND_MODE_GROUP) {
      tripZones.addRowTo(unit, areaOfPosition, areaSlots, cellOfAreaCell, cells);
    } else {
      checkedValues.addRowTo(unit, cellOfCheckedKey[checkedTable.ordinal()], cells);
    }
  }

  /**
   * The value of a sampling unit for a cell of a checked table
   *
   * @param unit sampling unit, from 0
   * @param checkedTable of the cell
   * @param cell index in the checked table's cells
   * @return the value, a count
   */
  public int getValue(int unit, DownSamplingCheckedTable checkedTable, int cell) {
    var cells = new double[getCategories(checkedTable).size()];
    Objects.checkIndex(cell, cells.length);
    collectValues(unit, checkedTable, cells);
    return (int) cells[cell];
  }

  /**
   * The zones with a place, in their order along the curve, so a zone's position is its index
   *
   * @return the zones, unmodifiable
   */
  public List<OdZone> getZonesAlongCurve() {
    return zonesAlongCurve;
  }

  /**
   * The car driven trips of a sampling unit leaving from each zone, whoever in it makes them
   *
   * @param unit sampling unit, from 0
   * @param byZonePosition to write the trips to, by zone position along the curve, as long as
   *                       {@link #getZonesAlongCurve()}; what it held is overwritten
   */
  public void collectCarDrivenTripsByOrigin(int unit, double[] byZonePosition) {
    Arrays.fill(byZonePosition, 0);
    carDrivenTripsByOrigin.addRowTo(unit, byZonePosition);
  }

  /**
   * The car driven trips of a sampling unit leaving from a zone
   *
   * @param unit sampling unit, from 0
   * @param origin zone the trips leave from
   * @return the number, 0 for a zone without a place
   */
  public int getCarDrivenTrips(int unit, OdZone origin) {
    int position = findPosition(origin);
    if (position < 0) {
      return 0;
    }
    var byZonePosition = new double[numberOfPlacedZones];
    collectCarDrivenTripsByOrigin(unit, byZonePosition);
    return (int) byZonePosition[position];
  }
}
