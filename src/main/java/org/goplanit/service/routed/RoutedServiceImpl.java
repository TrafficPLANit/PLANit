package org.goplanit.service.routed;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.service.routed.RoutedService;
import org.goplanit.utils.service.routed.RoutedServiceTripInfo;

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

  /** the trip information for this service */
  private final RoutedServiceTripInfo trips;

  /** mode of the routed service */
  private final Mode mode;

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
   * @param mode of the service
   */
  public RoutedServiceImpl(final IdGroupingToken tokenId, final Mode mode) {
    super(generateId(tokenId));
    this.name = null;
    this.nameDescription = null;
    this.serviceDescription = null;
    this.mode = mode;
    this.trips = new RoutedServiceTripInfoImpl(tokenId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public RoutedServiceImpl(RoutedServiceImpl other, boolean deepCopy) {
    super(other);
    this.name = other.name;
    this.nameDescription = other.nameDescription;
    this.serviceDescription = other.serviceDescription;
    this.mode = other.mode;

    // container wrapper so requires clone also for shallow copy
    this.trips = deepCopy ? other.trips.deepClone() : other.trips.clone();
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
    return new RoutedServiceImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServiceImpl deepClone() {
    return new RoutedServiceImpl(this, true);
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
  public RoutedServiceTripInfo getTripInfo() {
    return trips;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mode getMode() {
    return mode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetChildManagedIdEntities() {
    this.trips.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString(){
    return String.format("id: %d XMLid: %s name: %s ", getId(), getXmlId(), getName());
  }

}
