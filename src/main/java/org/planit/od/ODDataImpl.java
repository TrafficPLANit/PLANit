package org.planit.od;

import org.planit.utils.id.IdAbleImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.OdZones;

/**
 * Base class containing common methods required by all classes which implement ODData
 * 
 * @author gman6028
 *
 * @param <T> the type of data to be stored for each origin-destination cell
 */
public abstract class OdDataImpl<T> extends IdAbleImpl implements OdData<T> {

  /**
   * holder for zones considered in the matrix
   */
  protected OdZones zones;

  /**
   * Access to underlying zones
   * 
   * @return odZones
   */
  protected OdZones getOdZones() {
    return zones;
  }

  /**
   * Constructor
   * 
   * @param idTokenClass to use for id grouping
   * @param idToken      to use for id generation
   * @param zones        zones considered in the matrix
   */
  public OdDataImpl(final Class<?> idTokenClass, IdGroupingToken idToken, final OdZones zones) {
    super(generateId(idTokenClass, idToken));
    this.zones = zones;
  }

  /**
   * Copy Constructor
   * 
   * @param other to copy
   */
  public OdDataImpl(OdDataImpl<T> other) {
    super(other);
    this.zones = other.zones;
  }

  /**
   * Returns the number of zones contained in the object
   * 
   * @return number of zones in the object
   */
  public int getNumberOfOdZones() {
    return zones.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract OdDataImpl<T> clone();

}
