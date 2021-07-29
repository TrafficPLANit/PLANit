package org.planit.service.routed;

import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * Implementation of a RoutedServiceTripInfo interface
 * 
 * @author markr
 */
public class RoutedServiceTripInfoImpl extends ExternalIdAbleImpl implements RoutedServiceTripInfo {

  /**
   * Generate id for instances of this class based on the token and class identifier
   * 
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, RoutedServiceTripInfo.ROUTED_SERVICE_TRIP_INFO_ID_CLASS);
  }

  /**
   * Constructor
   * 
   * @param tokenId to use for id generation
   */
  public RoutedServiceTripInfoImpl(final IdGroupingToken tokenId) {
    super(generateId(tokenId));
  }

  /**
   * Copy constructor
   * 
   * @param routedServiceTripInfoImpl to copy
   */
  public RoutedServiceTripInfoImpl(RoutedServiceTripInfoImpl routedServiceTripInfoImpl) {
    super(routedServiceTripInfoImpl);
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
  public RoutedServiceTripInfoImpl clone() {
    return new RoutedServiceTripInfoImpl(this);
  }

}
