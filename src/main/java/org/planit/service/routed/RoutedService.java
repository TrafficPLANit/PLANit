package org.planit.service.routed;

import org.planit.utils.id.ExternalIdAble;
import org.planit.utils.id.ManagedId;

/**
 * Interface to reflect a routed service. A routed service reflects a route on a service network comprising of one or more legs. A leg comprises one or more physical links on an
 * underlying physical network.
 * 
 * @author markr
 *
 */
public interface RoutedService extends ManagedId, ExternalIdAble {

  /** id class for generating ids */
  public static final Class<RoutedService> ROUTED_SERVICE_ID_CLASS = RoutedService.class;

  /**
   * {@inheritDoc}
   */
  @Override
  public default Class<RoutedService> getIdClass() {
    return ROUTED_SERVICE_ID_CLASS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedService clone();

  /**
   * Name of the service, can be a number. Shortest visual (user) identifier for the service
   * 
   * @return name
   */
  public abstract String getName();

  /**
   * Name of the service, can be a number. Shortest visual (user) identifier for the service
   * 
   * @param name to use
   */
  public abstract void setName(String name);

  /**
   * Description of the name of the service, usually a short elaboration in addition to the name (number), for example for a bus the name could be 370 and the nameDescription
   * "Balmain to Coogee"
   * 
   * @return nameDescription
   */
  public abstract String getNameDescription();

  /**
   * Description of the name of the service, usually a short elaboration in addition to the name (number), for example for a bus the name could be 370 and the nameDescription
   * "Balmain to Coogee"
   * 
   * @param nameDescription to use
   */
  public abstract void setNameDescription(String nameDescription);

  /**
   * Description of the service, longer (if at all) and contextual not meant for end user in reality
   * 
   * @return serviceDescription
   */
  public abstract String getServiceDescription();

  /**
   * Description of the service, longer (if at all) and contextual not meant for end user in reality
   * 
   * @param serviceDescription to use
   */
  public abstract void setServiceDescription(String serviceDescription);

  /**
   * Access to the trips available for this service
   * 
   * @return known routed service trips
   */
  public abstract RoutedServiceTrips getTrips();

}
