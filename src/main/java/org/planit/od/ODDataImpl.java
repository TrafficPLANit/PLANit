package org.planit.od;

import org.planit.utils.zoning.Zones;

/**
 * Base class containing common methods required by all classes which implement ODData
 * 
 * @author gman6028
 *
 * @param <T> the type of data to be stored for each origin-destination cell
 */
public abstract class ODDataImpl<T> implements ODData<T> {

  /**
   * holder for zones considered in the matrix
   */
  protected Zones<?> zones;

  /**
   * Constructor
   * 
   * @param zones zones considered in the matrix
   */
  public ODDataImpl(Zones<?> zones) {
    this.zones = zones;

  }

  /**
   * Returns the number of zones contained in the object
   * 
   * @return number of zones in the object
   */
  public int getNumberOfTravelAnalysisZones() {
    return zones.size();
  }

}
