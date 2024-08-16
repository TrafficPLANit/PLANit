package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.macroscopic.MacroscopicConstants;

/**
 * Implementation of a Newell (triangular) fundamental diagram for a road section (link segment)
 * 
 * @author markr
 */
public class NewellFundamentalDiagram extends FundamentalDiagramImpl {

  //@formatter:off

  /**
   * Constructor using all defaults except for the free speed to apply
   * 
   * @param freeSpeedKmHour to use
   */
  public NewellFundamentalDiagram(double freeSpeedKmHour) {
    this(freeSpeedKmHour, MacroscopicConstants.DEFAULT_MAX_DENSITY_PCU_KM_LANE);
  }

  /**
   * Constructor using all defaults except for the free speed to apply
   * 
   * @param freeSpeedKmHour to use
   * @param jamDensityPcuKm maximum density allowed
   */
  public NewellFundamentalDiagram(double freeSpeedKmHour, double jamDensityPcuKm) {
    super(new LinearFundamentalDiagramBranch(freeSpeedKmHour, MacroscopicConstants.DEFAULT_EMPTY_DENSITY_PCU_HOUR_LANE),
        new LinearFundamentalDiagramBranch(MacroscopicConstants.DEFAULT_BACKWARD_WAVE_SPEED_KM_HOUR, jamDensityPcuKm));
  }

  /**
   * Constructor using all defaults except for the free speed to apply
   * 
   * @param freeSpeedKmHour to use
   * @param capacityPcuHour to allow
   * @param jamDensityPcuKm maximum density allowed
   */
  public NewellFundamentalDiagram(double freeSpeedKmHour, double capacityPcuHour, double jamDensityPcuKm) {
    this(freeSpeedKmHour, jamDensityPcuKm);
    setCapacityPcuHour(capacityPcuHour);
  }

  /**
   * Constructor using all defaults except for the free speed to apply
   * 
   * @param freeFlowBranch to use
   * @param congestedBranch to use
   */
  public NewellFundamentalDiagram(final LinearFundamentalDiagramBranch freeFlowBranch, final LinearFundamentalDiagramBranch congestedBranch) {
    super(freeFlowBranch, congestedBranch);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public NewellFundamentalDiagram(NewellFundamentalDiagram other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinearFundamentalDiagramBranch getFreeFlowBranch() {
    return (LinearFundamentalDiagramBranch) super.getFreeFlowBranch();
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
    /* capacity = (k_crit-0)*maxspeed
     * capacity = (k_crit-k_jam)*backwardwavespeed
     * so:
     * (k_crit-0)*maxspeed = (k_crit-k_jam)*backwardwavespeed
     * k_crit(maxspeed -backwardwavespeed) = -k_jam *backwardwavespeed
     * k_crit = -(k_jam *backwardwavespeed)/(maxspeed -backwardwavespeed)
     * capacity = k_crit * maxspeed  
     */
    double maxSpeed = getMaximumSpeedKmHour();
    double backwardWaveSpeed = getCongestedBranch().getCharateristicWaveSpeedKmHour();
    double kCrit = -((getMaximumDensityPcuKm()*backwardWaveSpeed)
                    / 
                    (maxSpeed - backwardWaveSpeed));
    return kCrit * maxSpeed;
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public NewellFundamentalDiagram shallowClone() {
    return new NewellFundamentalDiagram(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NewellFundamentalDiagram deepClone() {
    return new NewellFundamentalDiagram(this, true);
  }

  /**
   * For the Newell FD this means that all remains the same except for the congested wave speed to ensure the FD remains viable since
   * the capacity is derived and not explicitly set. By chaning the backward wave speed to the adjusted value we ensure we obtain the
   * desired capacity
   */   
  @Override
  public void setCapacityPcuHour(double capacityPcuHour) {
    /* capacity = (k_crit-k_jam)*backwardwavespeed 
     * backwardwavespeed = (k_crit-k_jam)/capacity 
     */
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
    getFreeFlowBranch().setCharacteristicWaveSpeedKmHour(maxSpeedKmHour);
  }


}
