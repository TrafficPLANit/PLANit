package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.HashUtils;

import java.text.NumberFormat;
import java.util.logging.Logger;

/**
 * A quadratic uncongested fundamental diagram branch implementation based on Bliemer and Raadsen (2019), e.g.,
 * <a href="https://doi.org/10.1016/j.trb.2018.01.001">Bliemer, M. C. J., and Raadsen, M. P. H. (2019). Continuous-time general link transmission model with simplified fanning,
 * Part I: Theory and link model formulation. Transportation Research Part B: Methodological, 126, 442–470</a>
 *
 * <p>
 *   Quadratic uncongested branch:
 *    flow = (wave_speed_max - (DENSITY * alpha)) * DENSITY,
 *      with,
 *      alpha = (critical_speed/capacity_per_lane) * (wave_speed_max - critical_speed)
 * </p>
 * <p>
 *   Quadratic uncongested branch inverted:
 *    density = (1/(2*alpha)) * (wave_speed_max  - SQRT(4 * alpha * FLOW + POW(wave_speed_max,2)))
 * </p>
 *
 * @author markr
 *
 */
public class QuadraticFreeFlowFundamentalDiagramBranch implements FundamentalDiagramBranch {

  private static final Logger LOGGER = Logger.getLogger(QuadraticFreeFlowFundamentalDiagramBranch.class.getCanonicalName());

  /** the alpha parameter derived from physical parameters */
  private double alpha;

  /** the maximum wave speed of the branch */
  private double maxWaveSpeedKmHour;

  /** the capacityPerLanePerHour of the branch */
  private double capacityPerLanePerHour;

  /** the criticalSpeedKmHour of the branch */
  private double criticalSpeedKmHour;

  /**
   * Based on information construct alpha which ensures that at capacity the branch bends such that its derivative
   * equates to the speed at capacity.
   * Note that alpha = (critical_speed/capacity_per_lane) * (wave_speed_max - critical_speed)
   */
  private void updateAlpha() {
    if(Precision.greaterEqual(criticalSpeedKmHour, maxWaveSpeedKmHour)){
      LOGGER.severe(String.format(
              "Quadratic free flow branch of FD cannot have free speed (%.2f) equal or smaller than critical speed (%.2f)", maxWaveSpeedKmHour, criticalSpeedKmHour));
    }
    alpha = (criticalSpeedKmHour /capacityPerLanePerHour) * (maxWaveSpeedKmHour - criticalSpeedKmHour);
  }

  /**
   * Set the max wave speed in km/h
   *
   * @param maxWaveSpeedKmHour to set
   */
  protected void setMaxWaveSpeedKmHour(double maxWaveSpeedKmHour) {
    this.maxWaveSpeedKmHour = maxWaveSpeedKmHour;
    updateAlpha();
  }

  /**
   * Set the speedAtCapacityKmHour
   *
   * @param criticalSpeedKmHour to set
   */
  protected void setCriticalSpeedKmHour(double criticalSpeedKmHour) {
    this.criticalSpeedKmHour = criticalSpeedKmHour;
    updateAlpha();
  }

  /**
   * Set the capacityPerLanePerHour
   *
   * @param capacityPerLanePerHour to set
   */
  protected void setCapacityPerLaneHour(double capacityPerLanePerHour) {
    this.capacityPerLanePerHour = capacityPerLanePerHour;
    updateAlpha();
  }

  /**
   * The maximum wave speed of the branch in km/hour
   *
   * @return wave speed
   */
  protected double getMaximumWaveSpeedKmHour() {
    return maxWaveSpeedKmHour;
  }

  /**
   * get the capacityPerLanePerHour
   *
   * @return capacityPerLanePerHour
   */
  protected double getCapacityPerLaneHour() {
    return this.capacityPerLanePerHour;
  }

  /**
   * get the criticalSpeedKmHour
   *
   * @return criticalSpeedKmHour
   */
  protected double getCriticalSpeedKmHour() {
    return this.criticalSpeedKmHour;
  }

  /**
   * Constructor
   *
   * @param maxWaveSpeedKmHour to use (typically the maximum speed of link)
   * @param criticalSpeedKmHour speed at capacity in km/h
   * @param capacityPerLanePerHour capacity per lane per hour
   */
  public QuadraticFreeFlowFundamentalDiagramBranch(
          double maxWaveSpeedKmHour, double criticalSpeedKmHour, double capacityPerLanePerHour) {
    this.maxWaveSpeedKmHour = maxWaveSpeedKmHour;
    this.capacityPerLanePerHour = capacityPerLanePerHour;
    this.criticalSpeedKmHour = criticalSpeedKmHour;

    if(Precision.greaterEqual(criticalSpeedKmHour, maxWaveSpeedKmHour, Precision.EPSILON_3)){
      throw new PlanItRunTimeException("Cannot create a quadratic free flow branch when critical speed is the same " +
              "or larger than free speed, abort");
    }else if(Precision.smallerEqual(criticalSpeedKmHour*2, maxWaveSpeedKmHour , Precision.EPSILON_3)){
      throw new PlanItRunTimeException("Cannot create a quadratic free flow branch when critical speed is more (equal) " +
              "than half the free speed as it causes zero/negative derivatives and bends backwards within free flow branch, abort");
    }

    updateAlpha();
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   */
  public QuadraticFreeFlowFundamentalDiagramBranch(QuadraticFreeFlowFundamentalDiagramBranch other) {
    this.maxWaveSpeedKmHour = other.maxWaveSpeedKmHour;
    this.capacityPerLanePerHour = other.capacityPerLanePerHour;
    this.criticalSpeedKmHour = other.criticalSpeedKmHour;
    this.alpha = other.alpha;
  }

  /**
   * <p>
   *   Quadratic uncongested branch:
   *    flow = (wave_speed_max - (DENSITY * alpha)) * DENSITY,
   *      with,
   *      alpha = (critical_speed/capacity_per_lane) * (wave_speed_max - critical_speed)
   * </p>
   */
  @Override
  public double getFlowPcuHour(double densityPcuKm) {
    return (maxWaveSpeedKmHour - (densityPcuKm * alpha)) * densityPcuKm;
  }

  /**
   * <p>
   *   Quadratic uncongested branch inverted:
   *    density = (1/(2*alpha)) * (wave_speed_max  - SQRT(4 * alpha * FLOW + POW(wave_speed_max,2)))
   * </p>
   */
  @Override
  public double getDensityPcuKm(double flowPcuHour) {
    return (1.0/(2 * alpha))
            *
            (maxWaveSpeedKmHour  - Math.sqrt(-4 * alpha * flowPcuHour + Math.pow(maxWaveSpeedKmHour,2)));
  }

  /**
   *
   * The derivative of flow towards a change in density given a particular flow
   *
   * <p>
   *   dFlow/dDensity = take derivative of density function (dq/dk=wave_speed_max - 2 * alpha * DENSITY) and
   *   rewrite to flow
   * </p>
   *
   * @param flowPcuHour to use
   * @return tangent of flow
   */
  @Override
  public double getDFlowDDensityAtFlow(double flowPcuHour) {
    return getDFlowDDensityAtDensity(getDensityPcuKm(flowPcuHour));
  }

  /**
   * The dFlow/dDensity given a particular density
   *
   * <p>
   *   dFlow/dDensity = take derivative of density function (dq/dk=wave_speed_max - 2 * alpha * DENSITY)
   * </p>
   *
   * @param densityPcuKm to use
   * @return tangent at density
   */
  @Override
  public double getDFlowDDensityAtDensity(double densityPcuKm) {
    return maxWaveSpeedKmHour - 2 * alpha * densityPcuKm;
  }

  /**
   * The dSpeed/dFlow given a particular flow. We can reuse {@link #getDSpeedDDensityAtDensity(double)} since in this
   * case the derivative is fixed and independent of the density (or flow), we use the result directly, i.e., -alpha.
   *
   * @param flowPcuHour to use (not needed for QL fd)
   */
  @Override
  public double getDSpeedDFlowAtFlow(double flowPcuHour) {
    return -getAlpha();
  }

  /**
   * The dSpeed/dDensity given a particular density
   * <p>
   *   Quadratic uncongested branch as a flow density relationship:
   *    flow = (wave_speed_max - (DENSITY * alpha)) * DENSITY,
   *      with,
   *      alpha = (critical_speed/capacity_per_lane) * (wave_speed_max - critical_speed)
   * </p>
   * <p>
   *   since flow = speed * density, the above suggests that the speed density function can be described by
   *   speed = (wave_speed_max - (DENSITY * alpha)).
   * </p>
   * <p>
   *   the dSpeed/dDensity is simply -alpha, with alpha = (critical_speed/capacity_per_lane) * (wave_speed_max - critical_speed)
   *   so the derivative is fixed but non-zero
   * </p>
   *
   * @param densityPcuKm to use (not needed for QL fd)
   * @return derivative of speed towards density for a given density
   */
  @Override
  public double getDSpeedDDensityAtDensity(double densityPcuKm) {
    return -getAlpha();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int relaxedHashCode(int scale) {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(scale);
    return HashUtils.createCombinedHashCode(
            nf.format(this.maxWaveSpeedKmHour),
            nf.format(this.criticalSpeedKmHour),
            nf.format(this.capacityPerLanePerHour),
            nf.format(this.alpha));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public QuadraticFreeFlowFundamentalDiagramBranch shallowClone() {
    return new QuadraticFreeFlowFundamentalDiagramBranch(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FundamentalDiagramBranch deepClone() {
    return new QuadraticFreeFlowFundamentalDiagramBranch(this);
  }

  /**
   * speed at zero flow is the same as the max wave speed
   */
  @Override
  public double getSpeedKmHourAtZeroFlow() {
    return getMaximumWaveSpeedKmHour();
  }

  /**
   * speed at zero density is the same as the max wave speed
   */
  @Override
  public double getSpeedKmHourAtZeroDensity() {
    return getMaximumWaveSpeedKmHour();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLinear() {
    return false;
  }

  /**
   * Access to internal alpha parameter of free flow branch
   * @return alpha
   */
  public double getAlpha() {
    return this.alpha;
  }
}
