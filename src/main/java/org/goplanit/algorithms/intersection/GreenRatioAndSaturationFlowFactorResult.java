package org.goplanit.algorithms.intersection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSegment;

/**
 * What an algorithm computing green ratios and saturation flow factors did, each recorded at the moment it was decided:
 * the green ratios it computed, the approaches without a signal of their own it gave values, the values set directly it
 * kept, the number of phases of each junction, the junctions with a rail-based phase, the number of road phases raised
 * to the minimum green time, the intersections it could not compute, and the earlier computed values it removed
 *
 * @author markr
 */
public class GreenRatioAndSaturationFlowFactorResult {

  /** approaches given a computed green ratio, with the value, in the order computed */
  private final Map<IntersectionSegment, Double> computedGreenRatios = new LinkedHashMap<>();

  /** approaches without a signal of their own given a computed saturation flow factor */
  private final Set<IntersectionSegment> approachesWithoutSignalGivenValues = new LinkedHashSet<>();

  /** approaches keeping a value set directly where a value would have been computed */
  private final Set<IntersectionSegment> approachesKeepingValueSetDirectly = new LinkedHashSet<>();

  /** signalised junctions with their number of phases */
  private final Map<Intersection, Integer> numberOfPhasesByJunction = new LinkedHashMap<>();

  /** junctions given a rail-based phase */
  private final Set<Intersection> junctionsWithRailBasedPhase = new LinkedHashSet<>();

  /** junctions with road phases raised to the minimum green time, with how many */
  private final Map<Intersection, Integer> numberOfPhasesRaisedToMinimumGreenTimeByJunction = new LinkedHashMap<>();

  /** intersections that could not be computed, with their green time left to share in seconds, 0 or less */
  private final Map<Intersection, Double> notComputableIntersections = new LinkedHashMap<>();

  /** approaches whose earlier computed value no longer applied and was removed */
  private final Set<IntersectionSegment> approachesWithStaleComputedValueRemoved = new LinkedHashSet<>();

  /**
   * Record a green ratio computed for an approach
   *
   * @param approach given the green ratio
   * @param greenRatio computed
   */
  protected void addComputedGreenRatio(IntersectionSegment approach, double greenRatio) {
    computedGreenRatios.put(approach, greenRatio);
  }

  /**
   * Record an approach without a signal of its own given a computed value
   *
   * @param approach given the value
   */
  protected void addApproachWithoutSignalGivenValue(IntersectionSegment approach) {
    approachesWithoutSignalGivenValues.add(approach);
  }

  /**
   * Record an approach keeping a value set directly where a value would have been computed
   *
   * @param approach keeping it
   */
  protected void addApproachKeepingValueSetDirectly(IntersectionSegment approach) {
    approachesKeepingValueSetDirectly.add(approach);
  }

  /**
   * Record the number of phases of a signalised junction
   *
   * @param junction the junction
   * @param numberOfPhases its number of phases
   */
  protected void addNumberOfPhases(Intersection junction, int numberOfPhases) {
    numberOfPhasesByJunction.put(junction, numberOfPhases);
  }

  /**
   * Record a junction given a rail-based phase
   *
   * @param junction the junction
   */
  protected void addJunctionWithRailBasedPhase(Intersection junction) {
    junctionsWithRailBasedPhase.add(junction);
  }

  /**
   * Record how many road phases of a junction were raised to the minimum green time
   *
   * @param junction the junction
   * @param numberRaised its number of road phases raised, 1 or more
   */
  protected void addNumberOfPhasesRaisedToMinimumGreenTime(Intersection junction, int numberRaised) {
    numberOfPhasesRaisedToMinimumGreenTimeByJunction.put(junction, numberRaised);
  }

  /**
   * Record an intersection that could not be computed
   *
   * @param intersection not computed
   * @param greenTimeToShareSeconds its green time left to share, 0 or less
   */
  protected void addNotComputableIntersection(Intersection intersection, double greenTimeToShareSeconds) {
    notComputableIntersections.put(intersection, greenTimeToShareSeconds);
  }

  /**
   * Record an approach whose earlier computed value was removed
   *
   * @param approach the value was removed from
   */
  protected void addApproachWithStaleComputedValueRemoved(IntersectionSegment approach) {
    approachesWithStaleComputedValueRemoved.add(approach);
  }

  /**
   * The approaches given a computed green ratio, at a junction, an isolated crossing or a ramp meter, with the value
   *
   * @return unmodifiable map of approach to green ratio, in the order computed
   */
  public Map<IntersectionSegment, Double> getComputedGreenRatios() {
    return Collections.unmodifiableMap(computedGreenRatios);
  }

  /**
   * The approaches without a signal of their own at a signalised intersection given a computed saturation flow
   * factor
   *
   * @return unmodifiable set of approaches
   */
  public Set<IntersectionSegment> getApproachesWithoutSignalGivenValues() {
    return Collections.unmodifiableSet(approachesWithoutSignalGivenValues);
  }

  /**
   * The approaches keeping a value set directly where a value would have been computed
   *
   * @return unmodifiable set of approaches
   */
  public Set<IntersectionSegment> getApproachesKeepingValueSetDirectly() {
    return Collections.unmodifiableSet(approachesKeepingValueSetDirectly);
  }

  /**
   * The number of phases of a signalised junction
   *
   * @param junction to get it for
   * @return its number of phases, null when it was not given phases
   */
  public Integer getNumberOfPhases(Intersection junction) {
    return numberOfPhasesByJunction.get(junction);
  }

  /**
   * How many signalised junctions got each number of phases, for example two junctions with two phases and one with
   * three
   *
   * @return map of number of phases to the number of junctions with it, by number of phases
   */
  public SortedMap<Integer, Long> getNumberOfJunctionsByNumberOfPhases() {
    return numberOfPhasesByJunction.values().stream().collect(
        Collectors.groupingBy(numberOfPhases -> numberOfPhases, TreeMap::new, Collectors.counting()));
  }

  /**
   * The junctions given a rail-based phase
   *
   * @return unmodifiable set of junctions
   */
  public Set<Intersection> getJunctionsWithRailBasedPhase() {
    return Collections.unmodifiableSet(junctionsWithRailBasedPhase);
  }

  /**
   * The number of road phases of a junction raised to the minimum green time
   *
   * @param junction to get it for
   * @return its number of road phases raised, 0 when none
   */
  public int getNumberOfPhasesRaisedToMinimumGreenTime(Intersection junction) {
    return numberOfPhasesRaisedToMinimumGreenTimeByJunction.getOrDefault(junction, 0);
  }

  /**
   * The intersections that could not be computed, because no green time was left to share once the time lost at the
   * phase changes and the green time of the rail-based phase were taken off the cycle
   *
   * @return unmodifiable map of intersection to its green time left to share in seconds, 0 or less
   */
  public Map<Intersection, Double> getNotComputableIntersections() {
    return Collections.unmodifiableMap(notComputableIntersections);
  }

  /**
   * The approaches whose earlier computed value no longer applied, for example because their intersection is no
   * longer signalised, and was removed
   *
   * @return unmodifiable set of approaches
   */
  public Set<IntersectionSegment> getApproachesWithStaleComputedValueRemoved() {
    return Collections.unmodifiableSet(approachesWithStaleComputedValueRemoved);
  }

  /**
   * One line summing up what was done, with how many junctions got each number of phases, for example "(1 phase at
   * 75, 2 phases at 348)"
   *
   * @return the summary
   */
  @Override
  public String toString() {
    var junctionsByNumberOfPhases = getNumberOfJunctionsByNumberOfPhases().entrySet().stream().map(
        entry -> String.format("%d %s at %d", entry.getKey(), entry.getKey() == 1 ? "phase" : "phases",
            entry.getValue())).collect(Collectors.joining(", "));
    int numberOfPhasesRaised =
        numberOfPhasesRaisedToMinimumGreenTimeByJunction.values().stream().mapToInt(Integer::intValue).sum();
    return String.format("Green ratios computed for %d approaches at %d junctions%s, %d junctions with a rail-based " +
            "phase, %d road phases at %d junctions raised to the minimum green time, values given to %d approaches " +
            "without a signal of their own, %d approaches keeping values set directly, %d intersections not " +
            "computable, %d stale computed values removed", computedGreenRatios.size(), numberOfPhasesByJunction.size(),
        junctionsByNumberOfPhases.isEmpty() ? "" : " (" + junctionsByNumberOfPhases + ")",
        junctionsWithRailBasedPhase.size(), numberOfPhasesRaised,
        numberOfPhasesRaisedToMinimumGreenTimeByJunction.size(), approachesWithoutSignalGivenValues.size(), approachesKeepingValueSetDirectly.size(),
        notComputableIntersections.size(), approachesWithStaleComputedValueRemoved.size());
  }
}
