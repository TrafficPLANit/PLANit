package org.planit.od;

import org.planit.utils.id.IdAble;
import org.planit.utils.zoning.Zone;

/**
 * Interface defining methods for objects which store data related to origin and destination
 * 
 * @author gman6028
 *
 * @param <T> the type of data to be stored for each origin-destination cell
 */
public interface OdData<T> extends IdAble, Iterable<T>, Cloneable {

  /**
   * Returns the value for a specified origin and destination
   * 
   * @param origin      specified origin
   * @param destination specified destination
   * @return value at the specified cell
   */
  public T getValue(Zone origin, Zone destination);

  /**
   * Returns the value for a specified origin and destination by their internal id
   * 
   * @param originId      specified origin
   * @param destinationId specified destination
   * @return value at the specified cell
   */
  public T getValue(long originId, long destinationId);

  /**
   * Sets the value for a specified origin and destination
   * 
   * @param origin      specified origin
   * @param destination specified destination
   * @param value       value at the specified cell
   */
  public void setValue(Zone origin, Zone destination, T value);

  /**
   * Returns the number of zones contained in the object
   * 
   * @return number of zones in the object
   */
  public int getNumberOfOdZones();

  /**
   * Returns an iterator which can iterate through all the origin-destination cells
   * 
   * @return iterator through all the origin-destination cells
   */
  public OdDataIterator<T> iterator();

}
