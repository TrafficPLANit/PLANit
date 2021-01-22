package org.planit.od;

import org.planit.utils.zoning.Zone;

/**
 * Interface defining methods for objects which store data related to origin and destination
 * 
 * @author gman6028
 *
 * @param <T> the type of data to be stored for each origin-destination cell
 */
public interface ODData<T> {

  /**
   * Returns the value for a specified origin and destination
   * 
   * @param origin specified origin
   * @param destination specified destination
   * @return value at the specified cell
   */
  public T getValue(Zone origin, Zone destination);

  /**
   * Sets the value for a specified origin and destination
   * 
   * @param origin specified origin
   * @param destination specified destination
   * @param t value at the specified cell
   */
  public void setValue(Zone origin, Zone destination, T t);

  /**
   * Returns the number of zones contained in the object
   * 
   * @return number of zones in the object
   */
  public int getNumberOfTravelAnalysisZones();

  /**
   * Returns an iterator which can iterate through all the origin-destination cells
   * 
   * @return iterator through all the origin-destination cells
   */
  public ODDataIterator<T> iterator();

}
