package org.planit.od;

import org.planit.utils.zoning.Zone;
import org.planit.utils.zoning.Zones;

/**
 * Base class containing common methods required by all classes which implement ODDataIterator
 * 
 * @author gman6028
 *
 * @param <T> the type of data to be stored for each origin-destination cell
 */
public abstract class ODDataIteratorImpl<T> implements ODDataIterator<T> {

  /**
   * Id of the origin zone
   */
  protected int originId;

  /**
   * Id of the destination zone
   */
  protected int destinationId;

  /**
   * Marker used to store the current position in the OD matrix (used internally, not accessible from other classes)
   */
  protected int currentLocation;

  /**
   * Zones object to store travel analysis zones (from Zoning object)
   */
  protected Zones<?> zones;

  /**
   * Increment the location cursor for the next iteration
   */
  protected void updateCurrentLocation() {
    originId = currentLocation / zones.size();
    destinationId = currentLocation % zones.size();
    currentLocation++;
  }

  /**
   * Constructor
   * 
   * @param zones zones considered in the matrix
   */
  public ODDataIteratorImpl(Zones<?> zones) {
    this.zones = zones;
    currentLocation = 0;
  }

  /**
   * Tests whether there are any more cells to iterate through
   * 
   * @return true if there are more cells to iterate through, false otherwise
   */
  @Override
  public boolean hasNext() {
    return currentLocation < (zones.size() * zones.size());
  }

  /**
   * Returns the origin zone object for the current cell
   * 
   * @return the origin zone object at the current cell
   */
  @Override
  public Zone getCurrentOrigin() {
    return zones.get(originId);
  }

  /**
   * Returns the destination zone object for the current cell
   * 
   * @return the destination zone object for the current cell
   */
  @Override
  public Zone getCurrentDestination() {
    return zones.get(destinationId);
  }

}
