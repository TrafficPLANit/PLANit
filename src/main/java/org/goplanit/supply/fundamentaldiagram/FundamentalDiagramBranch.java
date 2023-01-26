package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.math.Precision;

/**
 * A fundamental diagram has two branches each one can have a particular shape. Each branch is to be derived from this interface
 * 
 * @author markr
 *
 */
public interface FundamentalDiagramBranch extends Cloneable {

  /**
   * The flow at a given density
   * 
   * @param densityPcuKm to use
   * @return flowPcuHour found
   */
  public abstract double getFlowPcuHour(double densityPcuKm);

  /**
   * The flow at a given density
   * 
   * @param flowPcuHour to use
   * @return densityPcuKm found
   */
  public abstract double getDensityPcuKm(double flowPcuHour);

  /**
   * The speed at a given flow. If flow is zero, the speed at zero flow is returned
   * 
   * @param flowPcuHour to use
   * @return speedKmHour found
   */
  public default double getSpeedKmHourByFlow(double flowPcuHour) {
    if (Precision.greater(flowPcuHour, 0)) {
      return flowPcuHour / getDensityPcuKm(flowPcuHour);
    }
    return getSpeedKmHourAtZeroFlow();
  }

  /**
   * Collect the speed at zero flow when flow/density is either not feasible to compute or might not be representative, i.e., when there is no flow, the speed likely should not be
   * zero, but instead reflect the maximum allowed speed instead
   * 
   * @return speedKmHour
   */
  public abstract double getSpeedKmHourAtZeroFlow();

  /**
   * The speed at a given density. If density is zero, the speed at zero density is returned
   * 
   * @param densityPcuKm to use
   * @return speedKmHour found
   */
  public default double getSpeedKmHourByDensity(double densityPcuKm) {
    if (Precision.greater(densityPcuKm, 0)) {
      return getFlowPcuHour(densityPcuKm) / densityPcuKm;
    }
    return getSpeedKmHourAtZeroDensity();
  }

  /**
   * Collect the speed at zero density when flow/density cannot be computed.
   * 
   * @return speedKmHour found
   */
  public abstract double getSpeedKmHourAtZeroDensity();

  /**
   * The derivative of flow towards a change in density given a particualr flow
   * 
   * @param flowPcuHour to use
   * @return tangent of flow
   */
  public abstract double getFlowTangent(double flowPcuHour);

  /**
   * The derivative of density towards a change in flow given a particular density
   * 
   * @param densityPcuKm to use
   * @return tangent of density
   */
  public default double getDensityTangent(double densityPcuKm) {
    return 1.0 / getFlowTangent(getFlowPcuHour(densityPcuKm));
  }

  /**
   * A fundamental diagram branch is based on a limited number of double variables to define it. In case we want to reuse the same branch for extremely similar variables, then we
   * can use this relaxed hash code that ensures that for the given precision level identical hashes are created even if the underlying floating point variables differ beyond this
   * precision.
   * 
   * @param scale indicating how many decimals to consider, e.g., 2 considers 2 decimals for precision
   * @return the created relaxed hash code
   */
  public abstract int relaxedHashCode(int scale);

  /**
   * shallow clone a branch
   * 
   * @return cloned branch
   */
  public abstract FundamentalDiagramBranch clone();

  /**
   * Verify if the branch is linear or not
   * 
   * @return true when linear, false otherwise
   */
  public abstract boolean isLinear();

}
