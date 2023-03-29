package org.goplanit.converter.idmapping;

import org.goplanit.utils.service.routed.RoutedService;
import org.goplanit.utils.service.routed.RoutedTrip;
import org.goplanit.utils.service.routed.RoutedTripDeparture;

import java.util.function.Function;

/**
 * All routed services id mappers in a convenience class
 */
public class RoutedServicesIdMapper extends PlanitComponentIdMapper{

  /**
   * Constructor
   * @param type to use
   */
  public RoutedServicesIdMapper(IdMapperType type){
    super(type);
    add(RoutedTrip.class, IdMapperFunctionFactory.createRoutedTripIdMappingFunction(type));
    add(RoutedTripDeparture.class,  IdMapperFunctionFactory.createRoutedTripDepartureIdMappingFunction(type));
    add(RoutedService.class, IdMapperFunctionFactory.createRoutedServiceIdMappingFunction(type));
  }

  /**
   * Collect how routed service leg ids are to be mapped to the XML ids when persisting
   *
   * @return mapping from routed service to string (XML id to persist)
   */
  public Function<RoutedService, String> getRoutedServiceRefIdMapper() {
    return get(RoutedService.class);
  }

  /**
   * Collect how routed trip ids are to be mapped to the XML ids when persisting
   *
   * @return mapping from routed trip to string (XML id to persist)
   */
  public Function<RoutedTrip, String> getRoutedTripRefIdMapper() {
    return get(RoutedTrip.class);
  }

  /**
   * Collect how RoutedTripDeparture ids are to be mapped to the XML ids when persisting
   *
   * @return mapping from RoutedTripDeparture to string (XML id to persist)
   */
  public Function<RoutedTripDeparture, String> getRoutedTripDepartureRefIdMapper() {
    return get(RoutedTripDeparture.class);
  }

}
