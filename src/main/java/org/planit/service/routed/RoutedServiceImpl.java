package org.planit.service.routed;

import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * Implementation of a RoutedService instance based on the RoutedService interface
 * 
 * @author markr
 */
public class RoutedServiceImpl extends ExternalIdAbleImpl implements RoutedService {

  /** short name of the service, often a number in PT context */
  private String name;

  /** name description of the service, often to contextualise beyond number for end user */
  private String nameDescription;

  /** description of the service, often to contextualise the service, longer and not for end user */
  private String serviceDescription;

  /** the trips for this service */
  private final RoutedServiceTrips trips;

  /**
   * Generate id for instances of this class based on the token and class identifier
   * 
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, RoutedService.ROUTED_SERVICE_ID_CLASS);
  }

  /**
   * Constructor
   * 
   * @param tokenId to use for id generation
   */
  public RoutedServiceImpl(final IdGroupingToken tokenId) {
    super(generateId(tokenId));
    this.name = null;
    this.nameDescription = null;
    this.serviceDescription = null;
    this.trips = new RoutedServiceTripsImpl(tokenId);
  }

  /**
   * Copy constructor
   * 
   * @param routedServiceImpl to copy
   */
  public RoutedServiceImpl(RoutedServiceImpl routedServiceImpl) {
    super(routedServiceImpl);
    this.name = routedServiceImpl.name;
    this.nameDescription = routedServiceImpl.nameDescription;
    this.serviceDescription = routedServiceImpl.serviceDescription;
    this.trips = routedServiceImpl.trips.clone();
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
  public RoutedServiceImpl clone() {
    return new RoutedServiceImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getNameDescription() {
    return this.nameDescription;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setNameDescription(String nameDescription) {
    this.nameDescription = nameDescription;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getServiceDescription() {
    return this.serviceDescription;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setServiceDescription(String serviceDescription) {
    this.serviceDescription = serviceDescription;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServiceTrips getTrips() {
    return trips;
  }

}
