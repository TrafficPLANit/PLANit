package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.macroscopic.MacroscopicConstants;

/**
 * Implementation of a Quadratic-Linear (triangular) fundamental diagram for a road section (link segment).
 * quadratic reflects the free flow branch, whereas linear is for the congested branch. This implementation has
 * no capacity drop, so the end point of the quadratic and start point of the linear branch touch at capacity.
 * <p>
 *   Quadratic uncongested branch:
 *    (wave_speed_max - (DENSITY * alpha)) * DENSITY,
 *      with,
 *      alpha = (critical_speed/capacity_per_lane) * (wave_speed_max - critical_speed)
 * </p>
 * <p>
 *   Linear congested branch: capacity * ( (jam_density / (jam_density - critical_Density)) - ( DENSITY /(jam_density- citical_density)) )
 * </p>
 * <p>
 *   Note that these can be inverted to take a flow and provide a density instead or obtain a speed for example..
 * </p>
 * 
 * @author markr
 */
public class QuadraticLinearFundamentalDiagram extends FundamentalDiagramImpl {

  //@formatter:off

  /**
   * Constructor using all defaults except for the free speed and critical speed to apply
   *
   * @param freeSpeedKmHour to use
   * @param criticalSpeed to use
   */
  public QuadraticLinearFundamentalDiagram(double freeSpeedKmHour, double criticalSpeed) {
    this(freeSpeedKmHour, criticalSpeed, MacroscopicConstants.DEFAULT_MAX_DENSITY_PCU_KM_LANE);
  }

  /**
   * Constructor inferring capacity using free speed and critical speed as well as jam density
   *
   * @param freeSpeedKmHour to use
   * @param criticalSpeed to use
   * @param jamDensityPcuKm jam density to use
   */
  public QuadraticLinearFundamentalDiagram(double freeSpeedKmHour, double criticalSpeed, double jamDensityPcuKm) {
    this(freeSpeedKmHour,
            criticalSpeed,
            FundamentalDiagramUtils.computeCapacityPcuHLaneFrom(
                    criticalSpeed, MacroscopicConstants.DEFAULT_BACKWARD_WAVE_SPEED_KM_HOUR, jamDensityPcuKm),
            jamDensityPcuKm);
  }

  /**
   * Constructor using all defaults except for the free speed to apply
   *
   * @param freeSpeedKmHour to use
   * @param speedAtCapacity to use
   * @param capacityPerLaneHour capacity per lane to use
   * @param jamDensityPcuKm maximum density allowed
   */
  public QuadraticLinearFundamentalDiagram(
          double freeSpeedKmHour, double speedAtCapacity, double capacityPerLaneHour, double jamDensityPcuKm) {
    super(new QuadraticFreeFlowFundamentalDiagramBranch(freeSpeedKmHour, speedAtCapacity, capacityPerLaneHour),
            new LinearFundamentalDiagramBranch(MacroscopicConstants.DEFAULT_BACKWARD_WAVE_SPEED_KM_HOUR, jamDensityPcuKm));
    // update linear branch because default backward wave speed depends on capacity point of uncongested branch
    getCongestedBranch().setCharacteristicWaveSpeedKmHour(computeBackwardWaveSpeedForCapacity(capacityPerLaneHour));
  }

  /**
   * Constructor using all defaults except for the free speed to apply
   *
   * @param freeFlowBranch to use
   * @param congestedBranch to use
   */
  public QuadraticLinearFundamentalDiagram(final LinearFundamentalDiagramBranch freeFlowBranch, final LinearFundamentalDiagramBranch congestedBranch) {
    super(freeFlowBranch, congestedBranch);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public QuadraticLinearFundamentalDiagram(QuadraticLinearFundamentalDiagram other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public QuadraticFreeFlowFundamentalDiagramBranch getFreeFlowBranch() {
    return (QuadraticFreeFlowFundamentalDiagramBranch) super.getFreeFlowBranch();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinearFundamentalDiagramBranch getCongestedBranch() {
    return (LinearFundamentalDiagramBranch) super.getCongestedBranch();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCapacityFlowPcuHour() {
    return getFreeFlowBranch().getCapacityPerLaneHour();
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public QuadraticLinearFundamentalDiagram shallowClone() {
    return new QuadraticLinearFundamentalDiagram(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public QuadraticLinearFundamentalDiagram deepClone() {
    return new QuadraticLinearFundamentalDiagram(this, true);
  }

  /**
   * For the QL FD this means that capacity point shifts on FF branch resulting in different critical density, so
   * also linear branch's characteristic wave speed gets changed.
   *
   * @param capacityPcuHour to set
   */   
  @Override
  public void setCapacityPcuHour(double capacityPcuHour) {
    getFreeFlowBranch().setCapacityPerLaneHour(capacityPcuHour);
    double backwardWaveSpeedForCapacity = computeBackwardWaveSpeedForCapacity(capacityPcuHour);
    getCongestedBranch().setCharacteristicWaveSpeedKmHour(backwardWaveSpeedForCapacity);    
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setMaximumDensityPcuKmHour(double maxDensityPcuKm) {
    getCongestedBranch().setDensityAtZeroFlow(maxDensityPcuKm);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void setMaximumSpeedKmHour(double maxSpeedKmHour) {
    getFreeFlowBranch().setMaxWaveSpeedKmHour(maxSpeedKmHour);
  }

  /**
   * Access to internal alpha parameter of free flow branch
   * @return alpha
   */
  public double getAlpha() {
    return getFreeFlowBranch().getAlpha();
  }
}
