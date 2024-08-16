package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.misc.HashUtils;

import java.text.NumberFormat;

/**
 * A quadratic uncongested fundamental diagram branch implementation based on Bliemer and Raadsen (2019), e.g.,
 * <a href="https://doi.org/10.1016/j.trb.2018.01.001">Bliemer, M. C. J., & Raadsen, M. P. H. (2019). Continuous-time general link transmission model with simplified fanning,
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
   *   rewrite to flow -->
   * </p>
   *
   * @param flowPcuHour to use
   * @return tangent of flow
   */
  @Override
  public double getdFlowdDensityAtFlow(double flowPcuHour) {
    return getdFlowdDensityAtDensity(getDensityPcuKm(flowPcuHour));
  }

  /**
   * The dFlow/dDensity given a particular density
   *
   * @param densityPcuKm to use
   * @return tangent at density
   */
  public double getdFlowdDensityAtDensity(double densityPcuKm) {
    return maxWaveSpeedKmHour - 2 * alpha * densityPcuKm;
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
