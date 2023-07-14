package org.goplanit.converter.idmapping;

import org.goplanit.utils.exceptions.PlanItRunTimeException;

public class PlanitComponentIdMappers {

  /** id mappers for network entities */
  private NetworkIdMapper networkIdMappers;

  /** id mappers for zoning entities */
  private ZoningIdMapper zoningIdMappers;

  /** id mappers for virtual network entities */
  private VirtualNetworkIdMapper virtualNetworkIdMapper;

  /** id mappers for service network entities */
  private ServiceNetworkIdMapper serviceNetworkIdMapper;

  /** id mappers for routed services entities */
  private RoutedServicesIdMapper routedServicesIdMapper;

  /** id mappers for routed services entities */
  private DemandsIdMapper demandsIdMapperIdMapper;

  /**
   * Constructor
   */
  public PlanitComponentIdMappers(){
  }

  /**
   * Explicitly set an id mapper to an instance, overwrites any existing mapper that already has been set
   *
   * @param componentIdMapper to set
   */
  public void setDedicatedIdMapper(PlanitComponentIdMapper componentIdMapper){
    if(componentIdMapper instanceof  NetworkIdMapper){
      networkIdMappers = (NetworkIdMapper)componentIdMapper;
    }else if(componentIdMapper instanceof  ZoningIdMapper){
      zoningIdMappers = (ZoningIdMapper)componentIdMapper;
    }else if(componentIdMapper instanceof  DemandsIdMapper){
      demandsIdMapperIdMapper = (DemandsIdMapper)componentIdMapper;
    }else if(componentIdMapper instanceof  ServiceNetworkIdMapper){
      serviceNetworkIdMapper = (ServiceNetworkIdMapper)componentIdMapper;
    }else if(componentIdMapper instanceof  RoutedServicesIdMapper){
      routedServicesIdMapper = (RoutedServicesIdMapper)componentIdMapper;
    }else if(componentIdMapper instanceof  VirtualNetworkIdMapper){
      virtualNetworkIdMapper = (VirtualNetworkIdMapper)componentIdMapper;
    }else{
      throw new PlanItRunTimeException("Unsupported id mapper provided as override");
    }
  }

  /**
   * All non-explicitly set id mappers for each component will be initialised with the given id mapper type
   *
   * @param idMapperType to use
   */
  public void populateMissingIdMappers(IdMapperType idMapperType){
    if(networkIdMappers == null) {
      networkIdMappers = new NetworkIdMapper(idMapperType);
    }
    if(zoningIdMappers == null) {
      zoningIdMappers = new ZoningIdMapper(idMapperType);
    }
    if(serviceNetworkIdMapper == null){
      serviceNetworkIdMapper = new ServiceNetworkIdMapper(idMapperType);
    }
    if(routedServicesIdMapper == null) {
      routedServicesIdMapper = new RoutedServicesIdMapper(idMapperType);
    }
    if(demandsIdMapperIdMapper == null) {
      demandsIdMapperIdMapper = new DemandsIdMapper(idMapperType);
    }
    if(virtualNetworkIdMapper == null) {
      virtualNetworkIdMapper = new VirtualNetworkIdMapper(idMapperType);
    }
  }

  public NetworkIdMapper getNetworkIdMappers() {
    return networkIdMappers;
  }

  public ZoningIdMapper getZoningIdMappers() {
    return zoningIdMappers;
  }

  public ServiceNetworkIdMapper getServiceNetworkIdMapper() {
    return serviceNetworkIdMapper;
  }

  public RoutedServicesIdMapper getRoutedServicesIdMapper() {
    return routedServicesIdMapper;
  }

  public DemandsIdMapper getDemandsIdMapperIdMapper() {
    return demandsIdMapperIdMapper;
  }

  public VirtualNetworkIdMapper getVirtualNetworkIdMapper() {
    return virtualNetworkIdMapper;
  }
}
