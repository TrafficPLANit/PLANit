package org.goplanit.converter;

import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.Zone;

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
   * create a function that takes a a class that extends {@link ExternalIdAble} and generate the appropriate id based on the user configuration
   * 
   * @param <T>      ExternalIdable
   * @param clazz    to use
   * @param idMapper the type of mapping function to create
   * @return function that generates node id's for MATSIM node output
   * @throws PlanItException thrown if error
   * 
   */
  protected static <T extends ExternalIdAble> Function<T, String> createIdMappingFunction(Class<T> clazz, final IdMapperType idMapper) throws PlanItException {
    switch (idMapper) {
    case ID:
      return (instance) -> {
        return Long.toString(instance.getId());
      };
    case EXTERNAL_ID:
      return (instance) -> {
        return instance.getExternalId();
      };
    case XML:
      return (instance) -> {
        return instance.getXmlId();
      };
    default:
      throw new PlanItException(String.format("unknown id mapping type found for %s %s", clazz.getName(), idMapper.toString()));
    }
  }

  /**
   * create a function that takes a node and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates node id's for MATSIM node output
   * @throws PlanItException thrown if error
   */
  public static Function<Vertex, String> createVertexIdMappingFunction(final IdMapperType idMapper) throws PlanItException {
    return createIdMappingFunction(Vertex.class, idMapper);
  }

  /**
   * create a function that takes a link and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates mapped link id's for persistence
   * @throws PlanItException thrown if error
   */
  public static Function<Link, String> createLinkIdMappingFunction(final IdMapperType idMapper) throws PlanItException {
    return createIdMappingFunction(Link.class, idMapper);
  }

  /**
   * create a function that takes a link segment type and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates mapped link segment type id's for persistence
   * @throws PlanItException thrown if error
   */
  public static Function<MacroscopicLinkSegmentType, String> createLinkSegmentTypeIdMappingFunction(IdMapperType idMapper) throws PlanItException {
    return createIdMappingFunction(MacroscopicLinkSegmentType.class, idMapper);
  }

  /**
   * create a function that takes a link segment and (optional) id mapper and generates the appropriate link segment id based on the user configuration
   * 
   * @param idMapper that generates mapped link segment id's for persistence
   * @return created function
   * @throws PlanItException thrown if error
   */
  public static Function<MacroscopicLinkSegment, String> createLinkSegmentIdMappingFunction(final IdMapperType idMapper) throws PlanItException {
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
      return createIdMappingFunction(MacroscopicLinkSegment.class, idMapper);
    }
  }

  /**
   * create a function that takes a mode and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates mapped mode id's for persistence
   * @throws PlanItException thrown if error
   */
  public static Function<Mode, String> createModeIdMappingFunction(IdMapperType idMapper) throws PlanItException {
    return createIdMappingFunction(Mode.class, idMapper);
  }

  /**
   * create a function that takes a connectoid and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates directed connectoi id's for node output
   * @throws PlanItException thrown if error
   */
  public static Function<Connectoid, String> createConnectoidIdMappingFunction(final IdMapperType idMapper) throws PlanItException {
    return createIdMappingFunction(Connectoid.class, idMapper);
  }

  /**
   * create a function that takes a zone and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates zone id's for zone output
   * @throws PlanItException thrown if error
   */
  public static Function<Zone, String> createZoneIdMappingFunction(IdMapperType idMapper) throws PlanItException {
    return createIdMappingFunction(Zone.class, idMapper);
  }

  /**
   * create a function that takes a transfer zone group and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates transfer zone group id's for transfer zone group output
   * @throws PlanItException thrown if error
   */
  public static Function<TransferZoneGroup, String> createTransferZoneGroupIdMappingFunction(IdMapperType idMapper) throws PlanItException {
    return createIdMappingFunction(TransferZoneGroup.class, idMapper);
  }

}
