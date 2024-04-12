package org.goplanit.converter.idmapping;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.userclass.TravellerType;
import org.goplanit.userclass.UserClass;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.id.IdMappingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.service.routed.*;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.zoning.Zoning;

import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Factory that creates functions for id mapping from PLANit ids to ids to be used for persistence. Based on the passed in IdMapper type functions will generate different ids when
 * applied to nodes, link segments, etc.
 * 
 * @author markr
 *
 */
public class IdMapperFunctionFactory {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(IdMapperFunctionFactory.class.getCanonicalName());

  /**
   * create a function that takes a node and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates node id's for MATSIM node output
   */
  public static Function<Vertex, String> createVertexIdMappingFunction(final IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(Vertex.class, idMapper);
  }

  /**
   * create a function that takes a link and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates mapped link id's for persistence
   */
  public static Function<Link, String> createLinkIdMappingFunction(final IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(Link.class, idMapper);
  }

  /**
   * create a function that takes a link segment type and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates mapped link segment type id's for persistence
   */
  public static Function<MacroscopicLinkSegmentType, String> createLinkSegmentTypeIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(MacroscopicLinkSegmentType.class, idMapper);
  }

  /**
   * create a function that takes a link segment and (optional) id mapper and generates the appropriate link segment id based on the user configuration
   * 
   * @param idMapper that generates mapped link segment id's for persistence
   * @return created function
   */
  public static Function<MacroscopicLinkSegment, String> createLinkSegmentIdMappingFunction(final IdMapperType idMapper) {
    switch (idMapper) {
    case EXTERNAL_ID:
      return (macroscopicLinkSegment) -> {
        /* when present on link segment use that external id, otherwise try link */
        if (macroscopicLinkSegment.getExternalId() != null) {
          return String.format("%s", macroscopicLinkSegment.getExternalId());
        } else if (macroscopicLinkSegment.getParentLink() != null && macroscopicLinkSegment.getParentLink().getExternalId() != null) {
          return String.format("%s_%s", macroscopicLinkSegment.getParentLink().getExternalId(), macroscopicLinkSegment.isDirectionAb() ? "ab" : "ba");
        } else {
          LOGGER.severe(String.format("unable to map id for link, PLANit link segment external id not available or parent link missing (id:%d)", macroscopicLinkSegment.getId()));
          return "-1";
        }
      };
    default:
      return IdMappingUtils.createIdMappingFunction(MacroscopicLinkSegment.class, idMapper);
    }
  }

  /**
   * create a function that takes a mode and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates mapped mode id's for persistence
   */
  public static Function<Mode, String> createModeIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(Mode.class, idMapper);
  }

  /**
   * create a function that takes a connectoid and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates directed connectoi id's for node output
   */
  public static Function<Connectoid, String> createConnectoidIdMappingFunction(final IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(Connectoid.class, idMapper);
  }

  /**
   * create a function that takes a zone and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates zone id's for zone output
   */
  public static Function<Zone, String> createZoneIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(Zone.class, idMapper);
  }

  /**
   * create a function that takes a transfer zone group and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates transfer zone group id's for transfer zone group output
   */
  public static Function<TransferZoneGroup, String> createTransferZoneGroupIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(TransferZoneGroup.class, idMapper);
  }

  /**
   * create a function that takes a traveller type and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates traveller type ids for traveller type output
   */
  public static Function<TravellerType, String> createTravellerTypeIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(TravellerType.class, idMapper);
  }

  /**
   * create a function that takes a traveller type and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates time period ids for time period output
   */
  public static Function<TimePeriod, String> createTimePeriodIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(TimePeriod.class, idMapper);
  }

  /**
   * create a function that takes a user class and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates user class ids for user class output
   */
  public static Function<UserClass, String> createUserClassIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(UserClass.class, idMapper);
  }

  /**
   * create a function that takes a service leg and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates service leg ids for service leg output
   */
  public static Function<ServiceLeg, String> createServiceLegIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(ServiceLeg.class, idMapper);
  }

  /**
   * create a function that takes a service leg segment and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates service leg segment ids for service leg segment output
   */
  public static Function<ServiceLegSegment, String> createServiceLegSegmentIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(ServiceLegSegment.class, idMapper);
  }

  /**
   * create a function that takes a routed trip and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates routed trip ids
   */
  public static Function<RoutedTrip, String> createRoutedTripIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(RoutedTrip.class, idMapper);
  }

  /**
   * create a function that takes a RoutedTripDeparture and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates RoutedTripDeparture ids
   */
  public static Function<RoutedTripDeparture, String> createRoutedTripDepartureIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(RoutedTripDeparture.class, idMapper);
  }

  /**
   * create a function that takes a RoutedService and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates RoutedService ids
   */
  public static Function<RoutedService, String> createRoutedServiceIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(RoutedService.class , idMapper);
  }

  /**
   * create a function that takes a RoutedTripSchedule and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates RoutedTripSchedule ids
   */
  public static Function<RoutedTripSchedule, String> createRoutedTripScheduleIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(RoutedTripSchedule.class , idMapper);
  }

  /**
   * create a function that takes a NetworkLayer and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates NetworkLayer ids
   */
  public static Function<MacroscopicNetworkLayer, String> createMacroscopicNetworkLayerIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(MacroscopicNetworkLayer.class , idMapper);
  }

  /**
   * create a function that takes a MacroscopicNetwork and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates MacroscopicNetwork ids
   */
  public static Function<MacroscopicNetwork, String> createMacroscopicNetworkIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(MacroscopicNetwork.class , idMapper);
  }

  /**
   * create a function that takes a ServiceNetwork and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates ServiceNetwork ids
   */
  public static Function<ServiceNetwork, String> createServiceNetworkIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(ServiceNetwork.class , idMapper);
  }

  /**
   * create a function that takes a RoutedServicesLayer and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates RoutedServicesLayer ids
   */
  public static Function<RoutedServicesLayer, String> createRoutedServiceLayerIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(RoutedServicesLayer.class , idMapper);
  }

  /**
   * create a function that takes a ServiceNetworkLayer and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates ServiceNetworkLayer ids
   */
  public static Function<ServiceNetworkLayer, String> createServiceNetworkLayerIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(ServiceNetworkLayer.class , idMapper);
  }

  /**
   * create a function that takes a Zoning and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates Zoning ids
   */
  public static Function<Zoning, String> createZoningIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(Zoning.class , idMapper);
  }

  /**
   * create a function that takes a ConnectoidEdge and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates ConnectoidEdge ids
   */
  public static Function<? extends ExternalIdAble, String> createConnectoidEdgeIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(ConnectoidEdge.class , idMapper);
  }

  /**
   * create a function that takes a ConnectoidSegment and generates the appropriate id based on the user configuration
   *
   * @param idMapper the type of mapping function to create
   * @return function that generates ConnectoidSegment ids
   */
  public static Function<? extends ExternalIdAble, String> createConnectoidSegmentIdMappingFunction(IdMapperType idMapper) {
    return IdMappingUtils.createIdMappingFunction(ConnectoidSegment.class , idMapper);
  }
}
