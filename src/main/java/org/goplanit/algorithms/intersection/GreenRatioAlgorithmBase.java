package org.goplanit.algorithms.intersection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.DoublePredicate;
import java.util.logging.Logger;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.network.layer.macroscopic.intersection.CapacityFactorOrigin;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSignalSegment;
import org.goplanit.utils.network.layer.physical.LinkSegmentUtils;

/**
 * What the algorithms computing green ratios and saturation flow factors share. At each signalised intersection given:
 * <ul>
 * <li>an approach without a signal of its own gets the saturation flow factor of the settings for such approaches;
 * having no signal, it has no green ratio;</li>
 * <li>at a crossing that is no junction, each approach with a signal gets the isolated crossing green ratio of the
 * settings;</li>
 * <li>at a junction, the approaches with a signal are grouped into phases of two opposite approaches, an approach
 * without an opposite a phase of its own. The usable green time, the cycle time less the time lost at each phase
 * change, is shared between the phases, the one thing the algorithms differ in, and each approach gets its phase's
 * green time divided by the cycle time. A junction without usable green time, or whose green times as shared are not
 * valid, gets no values.</li>
 * </ul>
 * <p>
 * Only values that are not set or were computed before change: a value set directly is kept, and its approach still
 * takes part in the phases. A value computed before that no longer applies, for example because its intersection is
 * no longer signalised, is removed. The saturation flow factor of an approach with a signal is not computed.
 * </p>
 * <p>
 * The direction of an approach at its stop line is measured over the last stretch of its link into the junction, up
 * to a reach set on the algorithm, see {@link #setStopLineDirectionReachMeters(double)}.
 * </p>
 *
 * @author markr
 */
public abstract class GreenRatioAlgorithmBase implements GreenRatioAlgorithm {

  /** What happened to one value of an approach */
  private enum ValueOutcome {

    /** set to the value computed */
    COMPUTED,

    /** kept as set directly, where a value was computed */
    KEPT_SET_DIRECTLY,

    /** an earlier computed value removed, since none was computed */
    STALE_REMOVED,

    /** left as it was */
    UNCHANGED;
  }

  /** Two approaches with a signal that may form a phase, by their index, with how far they are from opposite */
  private static final class OppositeApproachCandidate {

    /** index of the first approach */
    private final int first;

    /** index of the second approach */
    private final int second;

    /** how far their directions are from 180 degrees apart, in degrees */
    private final double degreesFromOpposite;

    /**
     * Constructor
     *
     * @param first index of the first approach
     * @param second index of the second approach
     * @param degreesFromOpposite how far their directions are from 180 degrees apart
     */
    private OppositeApproachCandidate(int first, int second, double degreesFromOpposite) {
      this.first = first;
      this.second = second;
      this.degreesFromOpposite = degreesFromOpposite;
    }
  }

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(GreenRatioAlgorithmBase.class.getCanonicalName());

  /**
   * Give one value of an approach the value computed for it. It is set as computed unless it was set directly, in which
   * case it is kept; when no value is computed for it, a value computed earlier is removed
   *
   * @param origin how the value got its current value, null when not set
   * @param computed the value computed for it, null when none
   * @param setAsComputed sets a value as computed
   * @param remove removes the value
   * @return what happened to the value
   */
  private static ValueOutcome applyComputedValue(
      CapacityFactorOrigin origin, Double computed, DoublePredicate setAsComputed, BooleanSupplier remove) {
    if (origin == CapacityFactorOrigin.SET_DIRECTLY) {
      return computed != null ? ValueOutcome.KEPT_SET_DIRECTLY : ValueOutcome.UNCHANGED;
    }
    if (computed != null) {
      setAsComputed.test(computed);
      return ValueOutcome.COMPUTED;
    }
    if (origin == CapacityFactorOrigin.COMPUTED) {
      remove.getAsBoolean();
      return ValueOutcome.STALE_REMOVED;
    }
    return ValueOutcome.UNCHANGED;
  }

  /**
   * Give the green ratio of an approach the value computed for it, see
   * {@link #applyComputedValue(CapacityFactorOrigin, Double, DoublePredicate, BooleanSupplier)}. An approach without a
   * signal has no green ratio
   *
   * @param approach to give it to
   * @param computed the green ratio computed for it, null when none
   * @return what happened to its green ratio
   */
  private static ValueOutcome applyComputedGreenRatio(IntersectionSegment approach, Double computed) {
    if (!(approach instanceof IntersectionSignalSegment)) {
      return ValueOutcome.UNCHANGED;
    }
    var withSignal = (IntersectionSignalSegment) approach;
    return applyComputedValue(withSignal.getGreenRatioOrigin(), computed,
        value -> withSignal.setGreenRatio(value, CapacityFactorOrigin.COMPUTED), withSignal::removeGreenRatio);
  }

  /** the settings */
  private final GreenRatioAndSaturationFlowFactorSettings settings;

  /** reach in metres back from the stop line over which the direction of an approach is measured */
  private double stopLineDirectionReachMeters = DEFAULT_STOP_LINE_DIRECTION_REACH_METERS;

  /**
   * Group the approaches with a signal of a junction into phases of two opposite approaches. Two are opposite when
   * their directions at the stop line are 180 degrees apart within the tolerance of the settings. Pairs are made
   * greedily, the most nearly opposite first, each approach in at most one pair; an approach without an opposite is a
   * phase of its own
   *
   * @param withSignal the approaches with a signal, in the order of the junction
   * @param crsUtils to measure directions with
   * @return the phases, each of one or two approaches
   */
  private List<List<IntersectionSegment>> groupIntoPhasesOfOppositeApproaches(
      List<IntersectionSegment> withSignal, PlanitJtsCrsUtils crsUtils) {
    var directions = withSignal.stream().mapToDouble(approach -> LinkSegmentUtils
        .getAzimuthArrivingAtDownstreamNodeInDegrees(approach.getLinkSegment(), stopLineDirectionReachMeters, crsUtils))
        .toArray();
    var candidates = new ArrayList<OppositeApproachCandidate>();
    for (int first = 0; first < directions.length; ++first) {
      for (int second = first + 1; second < directions.length; ++second) {
        double degreesFromOpposite = 180 - PlanitJtsUtils.minDiffAngleInDegrees(directions[first], directions[second]);
        if (degreesFromOpposite <= settings.getOppositeApproachToleranceDegrees()) {
          candidates.add(new OppositeApproachCandidate(first, second, degreesFromOpposite));
        }
      }
    }
    candidates.sort(Comparator.comparingDouble(candidate -> candidate.degreesFromOpposite));

    var paired = new boolean[directions.length];
    var phases = new ArrayList<List<IntersectionSegment>>();
    for (var candidate : candidates) {
      if (!paired[candidate.first] && !paired[candidate.second]) {
        phases.add(List.of(withSignal.get(candidate.first), withSignal.get(candidate.second)));
        paired[candidate.first] = true;
        paired[candidate.second] = true;
      }
    }
    for (int index = 0; index < directions.length; ++index) {
      if (!paired[index]) {
        phases.add(List.of(withSignal.get(index)));
      }
    }
    return phases;
  }

  /**
   * Validate the green times an algorithm shared out: one per phase, each 0 or more, adding up to no more than the
   * usable green time. Invalid green times are refused with a warning
   *
   * @param intersection they are for
   * @param greenTimesSeconds as shared, in the order of the phases
   * @param numberOfPhases of the junction
   * @param usableGreenTimeSeconds shared out
   * @return true when valid, false otherwise
   */
  private boolean validateGreenTimes(
      Intersection intersection, double[] greenTimesSeconds, int numberOfPhases, double usableGreenTimeSeconds) {
    boolean valid = greenTimesSeconds != null && greenTimesSeconds.length == numberOfPhases &&
        Arrays.stream(greenTimesSeconds).allMatch(greenTime -> Double.isFinite(greenTime) && greenTime >= 0) &&
        Arrays.stream(greenTimesSeconds).sum() <= usableGreenTimeSeconds * (1 + 1e-9);
    if (!valid) {
      LOGGER.warning(String.format("Green times %s shared by %s at intersection %s are not valid for %d phases and " +
              "%s s of usable green time, no values computed", Arrays.toString(greenTimesSeconds), getType(),
          intersection.getIdsAsString(), numberOfPhases, usableGreenTimeSeconds));
    }
    return valid;
  }

  /**
   * Work out the values to compute for the approaches of a signalised intersection, see the class description
   *
   * @param intersection signalised
   * @param crsUtils to measure directions with
   * @param result to record its number of phases in, or that it cannot be computed
   * @param greenRatios to add each approach's computed green ratio to
   * @param saturationFlowFactors to add each approach's computed saturation flow factor to
   */
  private void collectComputedValues(Intersection intersection, PlanitJtsCrsUtils crsUtils,
      GreenRatioAndSaturationFlowFactorResult result, Map<IntersectionSegment, Double> greenRatios,
      Map<IntersectionSegment, Double> saturationFlowFactors) {
    var withSignal = new ArrayList<IntersectionSegment>();
    var withoutSignal = new ArrayList<IntersectionSegment>();
    intersection.getApproaches().forEach(
        approach -> (approach.hasSignal() ? withSignal : withoutSignal).add(approach));

    if (intersection.isJunction()) {
      var phases = groupIntoPhasesOfOppositeApproaches(withSignal, crsUtils);
      double cycleTimeSeconds = settings.getCycleTimeSeconds();
      double usableGreenTimeSeconds = cycleTimeSeconds - phases.size() * settings.getLostTimePerPhaseChangeSeconds();
      if (usableGreenTimeSeconds <= 0) {
        result.addNotComputableIntersection(intersection, usableGreenTimeSeconds);
        return;
      }
      var greenTimesSeconds = shareUsableGreenTime(intersection, phases, usableGreenTimeSeconds);
      if (!validateGreenTimes(intersection, greenTimesSeconds, phases.size(), usableGreenTimeSeconds)) {
        result.addNotComputableIntersection(intersection, usableGreenTimeSeconds);
        return;
      }
      result.addNumberOfPhases(intersection, phases.size());
      for (int phase = 0; phase < phases.size(); ++phase) {
        double greenRatio = greenTimesSeconds[phase] / cycleTimeSeconds;
        phases.get(phase).forEach(approach -> greenRatios.put(approach, greenRatio));
      }
    } else {
      withSignal.forEach(approach -> greenRatios.put(approach, settings.getIsolatedCrossingGreenRatio()));
    }

    withoutSignal.forEach(
        approach -> saturationFlowFactors.put(approach, settings.getApproachWithoutSignalSaturationFlowFactor()));
  }

  /**
   * Compute the values of the approaches of one intersection, and record what happened to each
   *
   * @param intersection to compute them for
   * @param crsUtils to measure directions with
   * @param result to record in
   */
  private void computeFor(
      Intersection intersection, PlanitJtsCrsUtils crsUtils, GreenRatioAndSaturationFlowFactorResult result) {
    var greenRatios = new IdentityHashMap<IntersectionSegment, Double>();
    var saturationFlowFactors = new IdentityHashMap<IntersectionSegment, Double>();
    if (intersection.isSignalised()) {
      collectComputedValues(intersection, crsUtils, result, greenRatios, saturationFlowFactors);
    }

    for (var approach : intersection.getApproaches()) {
      var greenRatioOutcome = applyComputedGreenRatio(approach, greenRatios.get(approach));
      var saturationFlowFactorOutcome = applyComputedValue(approach.getSaturationFlowFactorOrigin(),
          saturationFlowFactors.get(approach),
          value -> approach.setSaturationFlowFactor(value, CapacityFactorOrigin.COMPUTED),
          approach::removeSaturationFlowFactor);

      var outcomes = List.of(greenRatioOutcome, saturationFlowFactorOutcome);
      if (greenRatioOutcome == ValueOutcome.COMPUTED) {
        result.addComputedGreenRatio(approach, greenRatios.get(approach));
      }
      if (!approach.hasSignal() && saturationFlowFactorOutcome == ValueOutcome.COMPUTED) {
        result.addApproachWithoutSignalGivenValue(approach);
      }
      if (outcomes.contains(ValueOutcome.KEPT_SET_DIRECTLY)) {
        result.addApproachKeepingValueSetDirectly(approach);
      }
      if (outcomes.contains(ValueOutcome.STALE_REMOVED)) {
        result.addApproachWithStaleComputedValueRemoved(approach);
      }
    }
  }

  /**
   * Share the usable green time equally between the phases
   *
   * @param numberOfPhases to share it between
   * @param usableGreenTimeSeconds to share
   * @return the green time of each phase in seconds
   */
  protected static double[] shareEqually(int numberOfPhases, double usableGreenTimeSeconds) {
    var greenTimes = new double[numberOfPhases];
    Arrays.fill(greenTimes, usableGreenTimeSeconds / numberOfPhases);
    return greenTimes;
  }

  /**
   * Constructor
   *
   * @param settings to use
   */
  protected GreenRatioAlgorithmBase(GreenRatioAndSaturationFlowFactorSettings settings) {
    this.settings = settings;
  }

  /**
   * Share the usable green time of a junction between its phases
   *
   * @param junction the phases are of
   * @param phases of the junction, each of one or two approaches with a signal
   * @param usableGreenTimeSeconds the cycle time less the time lost at the phase changes, above 0
   * @return the green time of each phase in seconds, in the order of the phases, adding up to the usable green time
   */
  protected abstract double[] shareUsableGreenTime(
      Intersection junction, List<List<IntersectionSegment>> phases, double usableGreenTimeSeconds);

  /** default reach in metres back from the stop line over which the direction of an approach is measured */
  public static final double DEFAULT_STOP_LINE_DIRECTION_REACH_METERS = 25;

  /**
   * The reach back from the stop line, along the link, over which the direction of an approach is measured, see
   * {@link LinkSegmentUtils#getAzimuthArrivingAtDownstreamNodeInDegrees}
   *
   * @return the reach in metres
   */
  public double getStopLineDirectionReachMeters() {
    return stopLineDirectionReachMeters;
  }

  /**
   * Set the reach back from the stop line over which the direction of an approach is measured
   *
   * @param stopLineDirectionReachMeters 0 or more
   * @return true when set, false when refused with a warning, leaving the reach as it was
   */
  public boolean setStopLineDirectionReachMeters(double stopLineDirectionReachMeters) {
    if (!(stopLineDirectionReachMeters >= 0) || Double.isInfinite(stopLineDirectionReachMeters)) {
      LOGGER.warning(String.format("Stop line direction reach of %s refused, it must be 0 m or more; the reach is " +
          "unchanged", stopLineDirectionReachMeters));
      return false;
    }
    this.stopLineDirectionReachMeters = stopLineDirectionReachMeters;
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GreenRatioAndSaturationFlowFactorResult computeGreenRatiosAndSaturationFlowFactors(
      Collection<? extends Intersection> intersections, PlanitJtsCrsUtils crsUtils) {
    PlanItRunTimeException.throwIfNull(intersections, "No intersections given to compute green ratios for");
    PlanItRunTimeException.throwIfNull(crsUtils, "No coordinate reference system given to compute green ratios in");
    var result = new GreenRatioAndSaturationFlowFactorResult();
    intersections.forEach(intersection -> computeFor(intersection, crsUtils, result));
    return result;
  }
}
