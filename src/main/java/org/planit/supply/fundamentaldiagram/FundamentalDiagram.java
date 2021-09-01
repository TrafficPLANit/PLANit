package org.planit.supply.fundamentaldiagram;

/**
 * The base interface for the Fundamental Diagram component type. Also specifies all out-of-the-box supported fundamental diagrams supported directly by PLANit and available for
 * users to create and register on their chosen compatible assignments.
 * 
 * @author markr
 *
 */
public interface FundamentalDiagram {

  /**
   * short hand for Newell fundamental diagram class type
   */
  public static final String NEWELL = NewellFundamentalDiagramComponent.class.getCanonicalName();

  /**
   * Free flow branch of the FD
   * 
   * @return free flow branch
   */
  public abstract FundamentalDiagramBranch getFreeFlowBranch();

  /**
   * Congested branch of the FD
   * 
   * @return congested branch
   */
  public abstract FundamentalDiagramBranch getCongestedBranch();

  /**
   * Provide the capacity flow rate per hour
   * 
   * @return capacity flow rate in pcu per hour
   */
  public abstract double getCapacityFlowPcuHour();

  /**
   * Collect maximum viable density
   * 
   * @return jam density pcu/km
   */
  public default double getJamDensityPcuKm() {
    return getCongestedBranch().getDensityPcuKm(0);
  }

  /**
   * Collect maximum viable speed assuming the free flow branch is concave, so maximum speed occurs at zero density
   * 
   * @return max speed km/h
   */
  public default double getMaximumSpeedKmHour() {
    return getFreeFlowBranch().getSpeedKmHourByDensity(0);
  }

  /**
   * A fundamental diagram is based on a limited number of double variables to define it. In case we want to use the same FD for extremely similar variables we can use this relaxed
   * hash code that ensures that for the given precision level identical hashes are created even if the underlying floating point variables differ beyond this precision.
   * 
   * @param scale indicating how many decimals to consider, e.g., 2 considers 2 decimals for precision
   */
  public abstract int relaxedHashCode(int scale);

}
