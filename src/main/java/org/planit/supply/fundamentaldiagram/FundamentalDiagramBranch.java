package org.planit.supply.fundamentaldiagram;

/**
 * A fundamental diagram has two branches each one can have a particular shape. Each branch is to be derived from this interface
 * 
 * @author markr
 *
 */
public interface FundamentalDiagramBranch {

  /** Default jam density */
  public static double DEFAULT_JAM_DENSITY_PCU_HOUR = 180.0;

  /** Default density at zero flow in free flow */
  public static double DEFAULT_EMPTY_DENSITY_PCU_HOUR = 0.0;

  /** Default free flow speed */
  public static double DEFAULT_FREEFLOW_SPEED_KM_HOUR = 80.0;

  /** Default backward wave speed speed */
  public static double DEFAULT_BACKWARD_WAVE_SPEED_KM_HOUR = -13.0;

  /**
   * The flow at a given density
   * 
   * @param densityPcuKm
   * @return flowPcuHour
   */
  public abstract double getFlowPcuHour(double densityPcuKm);

  /**
   * The flow at a given density
   * 
   * @param flowPcuHour
   * @return densityPcuKm
   */
  public abstract double getDensityPcuKm(double flowPcuHour);

  /**
   * The speed at a given flow
   * 
   * @param flowPcuHour to use
   * @return speedKmHour
   */
  public default double getSpeedKmHourByFlow(double flowPcuHour) {
    return flowPcuHour / getDensityPcuKm(flowPcuHour);
  }

  /**
   * The speed at a given density
   * 
   * @param densityPcuKm to use
   * @return speedKmHour
   */
  public default double getSpeedKmHourByDensity(double densityPcuKm) {
    return getFlowPcuHour(densityPcuKm) / densityPcuKm;
  }

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
   * @param densityPcuKm
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
   */
  public abstract int relaxedHashCode(int scale);

}
