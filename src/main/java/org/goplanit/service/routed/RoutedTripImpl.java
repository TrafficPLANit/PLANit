package org.goplanit.service.routed;

import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.service.routed.RoutedTrip;

/**
 * Implementation of a RoutedTrip interface. Acts as a base class for derived implementations that are not abstract
 * 
 * @author markr
 */
public abstract class RoutedTripImpl extends ExternalIdAbleImpl implements RoutedTrip {

  /**
   * Generate id for instances of this class based on the token and class identifier
   * 
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, RoutedTrip.ROUTED_TRIP_ID_CLASS);
  }

  /**
   * Constructor
   * 
   * @param tokenId to use for id generation
   */
  public RoutedTripImpl(final IdGroupingToken tokenId) {
    super(generateId(tokenId));
  }

  /**
   * Copy constructor
   * 
   * @param routedTripImpl to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public RoutedTripImpl(RoutedTripImpl routedTripImpl, boolean deepCopy /* no impact yet */) {
    super(routedTripImpl);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedTripImpl clone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedTripImpl deepClone();

}
