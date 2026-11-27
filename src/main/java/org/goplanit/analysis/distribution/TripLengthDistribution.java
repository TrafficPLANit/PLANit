package org.goplanit.analysis.distribution;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdAble;
import org.goplanit.utils.id.IdAbleUtils;
import org.goplanit.utils.misc.binning.BinnedSum;
import org.goplanit.utils.misc.binning.BinningConfiguration;
import org.goplanit.utils.unit.DistanceUnit;
import org.goplanit.utils.unit.Unit;

import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * How trips spread over distance bins: the trips and the distance they add up to in each bin, per key, a key being a
 * mode, or a user class for OD demand, all distances in the distance unit of the distribution
 * <p>
 * A sample of a demand, a scenario, or a demand next to an observed survey, can be compared by how long their trips
 * are; a mean alone hides whether short trips gave way to long ones. The bins can be of any widths, such as 0 to 1,
 * 1 to 2, 2 to 5 and 5 to 10 km, and a trip in no bin, such as one of 50 km past bins ending at 40 km, is counted apart
 * as out of range, so the bins and the out of range together hold every trip. Short trips, such as those on foot, may
 * be binned in metres instead. Trips and distances are fractional, as OD demand is. Keys are kept in the order of
 * their ids.
 * </p>
 *
 * @author markr
 *
 * @param <K> type of the key, such as a mode or a user class
 */
public class TripLengthDistribution<K extends IdAble> {

  /** the distance bins, in the distance unit */
  private final BinningConfiguration<Double> bins;

  /** unit of the bins, the lengths and the distances */
  private final DistanceUnit distanceUnit;

  /** trips per bin, per key */
  private final SortedMap<K, BinnedSum<Double>> tripsByKey = new TreeMap<>(IdAbleUtils.byId());

  /** distance the trips add up to per bin, per key */
  private final SortedMap<K, BinnedSum<Double>> distanceByKey = new TreeMap<>(IdAbleUtils.byId());

  /**
   * The sums of a key, the trips or the distances, refusing a key without trips
   *
   * @param sumsByKey the trips or the distances by key
   * @param key to collect for
   * @return its sums per bin
   */
  private BinnedSum<Double> findSumsOf(SortedMap<K, BinnedSum<Double>> sumsByKey, K key) {
    var sums = sumsByKey.get(key);
    PlanItRunTimeException.throwIfNull(sums, "Trip length distribution holds no trips for %s", key);
    return sums;
  }

  /**
   * Constructor, the bins and all distances in km
   *
   * @param bins distance bins in km
   */
  public TripLengthDistribution(BinningConfiguration<Double> bins) {
    this(bins, Unit.KM);
  }

  /**
   * Constructor
   *
   * @param bins distance bins in the distance unit
   * @param distanceUnit of the bins, the lengths added and the distances given, such as metres for short trips
   */
  public TripLengthDistribution(BinningConfiguration<Double> bins, DistanceUnit distanceUnit) {
    PlanItRunTimeException.throwIfNull(bins, "Bins of a trip length distribution are null, not allowed");
    PlanItRunTimeException.throwIfNull(distanceUnit, "Distance unit of a trip length distribution is null, not allowed");
    this.bins = bins;
    this.distanceUnit = distanceUnit;
  }

  /**
   * Add trips of a length for a key, in the bin the length falls in, or out of range. A length that is not a number,
   * of trips that could not be measured, counts the trips out of range without adding to the distance
   *
   * @param key of the trips
   * @param length of the trips, in the distance unit
   * @param trips number of trips, fractional allowed
   */
  public void add(K key, double length, double trips) {
    tripsByKey.computeIfAbsent(key, k -> BinnedSum.of(bins)).add(length, trips);
    distanceByKey.computeIfAbsent(key, k -> BinnedSum.of(bins)).add(
        length, Double.isNaN(length) ? 0 : trips * length);
  }

  /**
   * The distance bins
   *
   * @return the bins, in the distance unit
   */
  public BinningConfiguration<Double> getBins() {
    return bins;
  }

  /**
   * The unit of the bins, the lengths and the distances
   *
   * @return the distance unit
   */
  public DistanceUnit getDistanceUnit() {
    return distanceUnit;
  }

  /**
   * The keys holding trips, in the order of their ids
   *
   * @return the keys
   */
  public Set<K> getKeys() {
    return Collections.unmodifiableSet(tripsByKey.keySet());
  }

  /**
   * The trips of a key in a bin
   *
   * @param key holding trips
   * @param binIndex of the bin
   * @return the trips
   */
  public double getTrips(K key, int binIndex) {
    return findSumsOf(tripsByKey, key).getSum(binIndex);
  }

  /**
   * The distance a key's trips in a bin add up to
   *
   * @param key holding trips
   * @param binIndex of the bin
   * @return the distance, in the distance unit
   */
  public double getDistance(K key, int binIndex) {
    return findSumsOf(distanceByKey, key).getSum(binIndex);
  }

  /**
   * The trips of a key in no bin
   *
   * @param key holding trips
   * @return the trips
   */
  public double getOutOfRangeTrips(K key) {
    return findSumsOf(tripsByKey, key).getOutOfRangeSum();
  }

  /**
   * The distance a key's trips in no bin add up to
   *
   * @param key holding trips
   * @return the distance, in the distance unit
   */
  public double getOutOfRangeDistance(K key) {
    return findSumsOf(distanceByKey, key).getOutOfRangeSum();
  }

  /**
   * All trips of a key, out of range included
   *
   * @param key holding trips
   * @return the trips
   */
  public double getTotalTrips(K key) {
    return findSumsOf(tripsByKey, key).getTotal();
  }

  /**
   * The share of a key's trips, out of range included, that fall in a bin
   *
   * @param key holding trips
   * @param binIndex of the bin
   * @return the share, from 0 to 1
   */
  public double getShareOfTrips(K key, int binIndex) {
    return getTrips(key, binIndex) / getTotalTrips(key);
  }

  /**
   * The mean length of a key's trips in a bin
   *
   * @param key holding trips
   * @param binIndex of the bin
   * @return the mean in the distance unit, NaN when the bin holds no trips
   */
  public double getMeanLength(K key, int binIndex) {
    double trips = getTrips(key, binIndex);
    return trips > 0 ? getDistance(key, binIndex) / trips : Double.NaN;
  }
}
