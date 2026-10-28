package org.goplanit.algorithms.intersection;

import java.util.logging.Logger;

import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;

/**
 * Settings of the algorithms that compute green ratios and saturation flow factors: which algorithm, and the signal
 * timing it assumes where none is known. Each setting has a default; a setter refuses a value that makes no sense,
 * with a warning, and leaves the setting as it was
 *
 * @author markr
 */
public class GreenRatioAndSaturationFlowFactorSettings {

  /** the logger */
  private static final Logger LOGGER =
      Logger.getLogger(GreenRatioAndSaturationFlowFactorSettings.class.getCanonicalName());

  /**
   * Warn that a value is refused
   *
   * @param setting name of the setting, starting with a capital
   * @param value refused
   * @param validValues what a value must be
   * @return false, the setting was not changed
   */
  private static boolean refuse(String setting, Object value, String validValues) {
    LOGGER.warning(String.format("%s of %s refused, it must be %s; the setting is unchanged", setting, value,
        validValues));
    return false;
  }

  /** the algorithm */
  private GreenRatioAlgorithmType algorithmType = DEFAULT_ALGORITHM_TYPE;

  /** cycle time in seconds */
  private double cycleTimeSeconds = DEFAULT_CYCLE_TIME_SECONDS;

  /** time lost at each phase change in seconds */
  private double lostTimePerPhaseChangeSeconds = DEFAULT_LOST_TIME_PER_PHASE_CHANGE_SECONDS;

  /** green ratio of road traffic at an isolated signalised crossing */
  private double isolatedCrossingGreenRatio = DEFAULT_ISOLATED_CROSSING_GREEN_RATIO;

  /** tolerance in degrees for two approaches to count as on the same axis */
  private double sameAxisApproachToleranceDegrees = DEFAULT_SAME_AXIS_APPROACH_TOLERANCE_DEGREES;

  /** the most phases a junction is given */
  private int maximumNumberOfPhases = DEFAULT_MAXIMUM_NUMBER_OF_PHASES;

  /** saturation flow factor of an approach without a signal of its own at a signalised intersection */
  private double approachWithoutSignalSaturationFlowFactor = DEFAULT_APPROACH_WITHOUT_SIGNAL_SATURATION_FLOW_FACTOR;

  /** default algorithm: each phase gets green time by its capacity, so a main road gets more than a side street */
  public static final GreenRatioAlgorithmType DEFAULT_ALGORITHM_TYPE = GreenRatioAlgorithmType.CAPACITY_WEIGHTED;

  /** default cycle time in seconds; city signals mostly run 80 to 120 s */
  public static final double DEFAULT_CYCLE_TIME_SECONDS = 90;

  /** default time lost at each phase change in seconds, about 3 to 4 s yellow and 1 to 2 s all-red */
  public static final double DEFAULT_LOST_TIME_PER_PHASE_CHANGE_SECONDS = 5;

  /** default green ratio of road traffic at an isolated signalised crossing, which has green most of the time */
  public static final double DEFAULT_ISOLATED_CROSSING_GREEN_RATIO = 0.9;

  /** default tolerance in degrees for two approaches to count as on the same axis: their directions within 30 degrees
   * of each other or of the opposite direction */
  public static final double DEFAULT_SAME_AXIS_APPROACH_TOLERANCE_DEGREES = 30;

  /** default most phases a junction is given; complex junctions give several approaches green together, and four phases
   * keep a green ratio from falling below (90 - 4 x 5) / 90 / 4, about 0.19, at the other defaults */
  public static final int DEFAULT_MAXIMUM_NUMBER_OF_PHASES = 4;

  /** default saturation flow factor of an approach without a signal of its own at a signalised intersection */
  public static final double DEFAULT_APPROACH_WITHOUT_SIGNAL_SATURATION_FLOW_FACTOR = 0.5;

  /**
   * Constructor, with the defaults
   */
  public GreenRatioAndSaturationFlowFactorSettings() {
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   */
  public GreenRatioAndSaturationFlowFactorSettings(GreenRatioAndSaturationFlowFactorSettings other) {
    this.algorithmType = other.algorithmType;
    this.cycleTimeSeconds = other.cycleTimeSeconds;
    this.lostTimePerPhaseChangeSeconds = other.lostTimePerPhaseChangeSeconds;
    this.isolatedCrossingGreenRatio = other.isolatedCrossingGreenRatio;
    this.sameAxisApproachToleranceDegrees = other.sameAxisApproachToleranceDegrees;
    this.maximumNumberOfPhases = other.maximumNumberOfPhases;
    this.approachWithoutSignalSaturationFlowFactor = other.approachWithoutSignalSaturationFlowFactor;
  }

  /**
   * The algorithm, see {@link GreenRatioAlgorithmFactory}
   *
   * @return the algorithm type
   */
  public GreenRatioAlgorithmType getAlgorithmType() {
    return algorithmType;
  }

  /**
   * Set the algorithm
   *
   * @param algorithmType to use, not null
   * @return true when set, false when refused
   */
  public boolean setAlgorithmType(GreenRatioAlgorithmType algorithmType) {
    if (algorithmType == null) {
      return refuse("Algorithm type", null, "given");
    }
    this.algorithmType = algorithmType;
    return true;
  }

  /**
   * The cycle time: the time in which a signal shows each of its phases once
   *
   * @return cycle time in seconds
   */
  public double getCycleTimeSeconds() {
    return cycleTimeSeconds;
  }

  /**
   * Set the cycle time
   *
   * @param cycleTimeSeconds above 0
   * @return true when set, false when refused
   */
  public boolean setCycleTimeSeconds(double cycleTimeSeconds) {
    if (!(cycleTimeSeconds > 0)) {
      return refuse("Cycle time", cycleTimeSeconds, "above 0 s");
    }
    this.cycleTimeSeconds = cycleTimeSeconds;
    return true;
  }

  /**
   * The time lost at each phase change, while the signal shows yellow and all-red
   *
   * @return lost time per phase change in seconds
   */
  public double getLostTimePerPhaseChangeSeconds() {
    return lostTimePerPhaseChangeSeconds;
  }

  /**
   * Set the time lost at each phase change
   *
   * @param lostTimePerPhaseChangeSeconds 0 or more
   * @return true when set, false when refused
   */
  public boolean setLostTimePerPhaseChangeSeconds(double lostTimePerPhaseChangeSeconds) {
    if (!(lostTimePerPhaseChangeSeconds >= 0)) {
      return refuse("Lost time per phase change", lostTimePerPhaseChangeSeconds, "0 s or more");
    }
    this.lostTimePerPhaseChangeSeconds = lostTimePerPhaseChangeSeconds;
    return true;
  }

  /**
   * The green ratio of road traffic at a signalised crossing that is no junction
   *
   * @return the green ratio
   */
  public double getIsolatedCrossingGreenRatio() {
    return isolatedCrossingGreenRatio;
  }

  /**
   * Set the green ratio of road traffic at a signalised crossing that is no junction
   *
   * @param isolatedCrossingGreenRatio from 0 to 1
   * @return true when set, false when refused
   */
  public boolean setIsolatedCrossingGreenRatio(double isolatedCrossingGreenRatio) {
    if (!IntersectionUtils.isValidCapacityFactor(isolatedCrossingGreenRatio)) {
      return refuse("Isolated crossing green ratio", isolatedCrossingGreenRatio, "from 0 to 1");
    }
    this.isolatedCrossingGreenRatio = isolatedCrossingGreenRatio;
    return true;
  }

  /**
   * The tolerance for two approaches to count as on the same axis, and so share a phase: their directions at the stop
   * line are within it of each other, arriving side by side, or of the opposite direction, facing each other
   *
   * @return the tolerance in degrees
   */
  public double getSameAxisApproachToleranceDegrees() {
    return sameAxisApproachToleranceDegrees;
  }

  /**
   * Set the tolerance for two approaches to count as on the same axis
   *
   * @param sameAxisApproachToleranceDegrees from 0 to below 90, so approaches at right angles never share an axis
   * @return true when set, false when refused
   */
  public boolean setSameAxisApproachToleranceDegrees(double sameAxisApproachToleranceDegrees) {
    if (!(sameAxisApproachToleranceDegrees >= 0 && sameAxisApproachToleranceDegrees < 90)) {
      return refuse("Same axis approach tolerance", sameAxisApproachToleranceDegrees, "from 0 to below 90 degrees");
    }
    this.sameAxisApproachToleranceDegrees = sameAxisApproachToleranceDegrees;
    return true;
  }

  /**
   * The most phases a junction is given. A junction with approaches on more axes has the phases whose axes are closest
   * merged until it has this many, since a complex junction gives several approaches green together
   *
   * @return the maximum number of phases
   */
  public int getMaximumNumberOfPhases() {
    return maximumNumberOfPhases;
  }

  /**
   * Set the most phases a junction is given
   *
   * @param maximumNumberOfPhases 1 or more
   * @return true when set, false when refused
   */
  public boolean setMaximumNumberOfPhases(int maximumNumberOfPhases) {
    if (maximumNumberOfPhases < 1) {
      return refuse("Maximum number of phases", maximumNumberOfPhases, "1 or more");
    }
    this.maximumNumberOfPhases = maximumNumberOfPhases;
    return true;
  }

  /**
   * The saturation flow factor of an approach without a signal of its own at a signalised intersection, such as a
   * minor road joining it
   *
   * @return the saturation flow factor
   */
  public double getApproachWithoutSignalSaturationFlowFactor() {
    return approachWithoutSignalSaturationFlowFactor;
  }

  /**
   * Set the saturation flow factor of an approach without a signal of its own at a signalised intersection
   *
   * @param approachWithoutSignalSaturationFlowFactor from 0 to 1
   * @return true when set, false when refused
   */
  public boolean setApproachWithoutSignalSaturationFlowFactor(double approachWithoutSignalSaturationFlowFactor) {
    if (!IntersectionUtils.isValidCapacityFactor(approachWithoutSignalSaturationFlowFactor)) {
      return refuse("Saturation flow factor of an approach without a signal",
          approachWithoutSignalSaturationFlowFactor, "from 0 to 1");
    }
    this.approachWithoutSignalSaturationFlowFactor = approachWithoutSignalSaturationFlowFactor;
    return true;
  }
}
