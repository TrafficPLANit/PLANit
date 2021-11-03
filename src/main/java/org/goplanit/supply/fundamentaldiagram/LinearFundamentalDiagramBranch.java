package org.goplanit.supply.fundamentaldiagram;

import java.text.NumberFormat;

import org.goplanit.utils.misc.HashUtils;

/**
 * A linear fundamental diagram branch implementation
 * 
 * @author markr
 *
 */
public class LinearFundamentalDiagramBranch implements FundamentalDiagramBranch {

  /** the slope of the linear branch */
  double characteristicWaveSpeedKmHour;

  /**
   * Location where the branch intersects with the x-axis (zero flow) of the flow-density diagram representation
   */
  double densityAtZeroFlowPcuKm;

  /**
   * Set the characteristic wave speed in km/h
   * 
   * @param characteristicWaveSpeedKmHour to set
   */
  protected void setCharacteristicWaveSpeedKmHour(double characteristicWaveSpeedKmHour) {
    this.characteristicWaveSpeedKmHour = characteristicWaveSpeedKmHour;
  }

  /**
   * Set the density at zero flow in Pcu/km
   * 
   * @param densityAtZeroFlowPcuKm to set
   */
  protected void setDensityAtZeroFlow(double densityAtZeroFlowPcuKm) {
    this.densityAtZeroFlowPcuKm = densityAtZeroFlowPcuKm;
  }

  /**
   * Constructor
   * 
   * @param characteristicWaveSpeedKmHour to use (either free flow speed to use or backward wave speed)
   * @param densityAtZeroFlow             to use
   */
  public LinearFundamentalDiagramBranch(double characteristicWaveSpeedKmHour, double densityAtZeroFlowPcuKm) {
    this.characteristicWaveSpeedKmHour = characteristicWaveSpeedKmHour;
    this.densityAtZeroFlowPcuKm = densityAtZeroFlowPcuKm;
  }

  /**
   * Copy constructor
   * 
   * @param linearFundamentalDiagramBranch to copy
   */
  public LinearFundamentalDiagramBranch(LinearFundamentalDiagramBranch linearFundamentalDiagramBranch) {
    this.characteristicWaveSpeedKmHour = linearFundamentalDiagramBranch.characteristicWaveSpeedKmHour;
    this.densityAtZeroFlowPcuKm = linearFundamentalDiagramBranch.densityAtZeroFlowPcuKm;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getFlowPcuHour(double densityPcuKm) {
    return (densityPcuKm - densityAtZeroFlowPcuKm) * getCharateristicWaveSpeedKmHour();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getDensityPcuKm(double flowPcuHour) {
    return densityAtZeroFlowPcuKm + (flowPcuHour / getCharateristicWaveSpeedKmHour());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getFlowTangent(double flowPcuHour) {
    return getCharateristicWaveSpeedKmHour();
  }

  /**
   * The characteristic wave speed of the linear branch in km/hour
   * 
   * @return wave speed
   */
  public double getCharateristicWaveSpeedKmHour() {
    return characteristicWaveSpeedKmHour;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int relaxedHashCode(int scale) {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(scale);
    return HashUtils.createCombinedHashCode(nf.format(this.characteristicWaveSpeedKmHour), nf.format(this.densityAtZeroFlowPcuKm));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinearFundamentalDiagramBranch clone() {
    return new LinearFundamentalDiagramBranch(this);
  }

  /**
   * speed at zero flow is the same as the characteristic wave speed
   */
  @Override
  public double getSpeedKmHourAtZeroFlow() {
    return getCharateristicWaveSpeedKmHour();
  }

  /**
   * speed at zero density is the same as the characteristic wave speed
   */
  @Override
  public double getSpeedKmHourAtZeroDensity() {
    return getCharateristicWaveSpeedKmHour();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLinear() {
    return true;
  }

}
