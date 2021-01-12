package org.planit.network.converter;

import java.util.function.Function;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.ExternalIdable;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;

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
   * create a function that takes a a class that extends {@link ExternalIdable} and generate the appropriate id based on the user configuration
   * 
   * @param <T>      ExternalIdable
   * @param idMapper the type of mapping function to create
   * @return function that generates node id's for MATSIM node output
   * @throws PlanItException thrown if error
   * 
   */
  protected static <T extends ExternalIdable> Function<T, String> createIdMappingFunction(Class<T> Clazz, final IdMapperType idMapper) throws PlanItException {
    switch (idMapper) {
    case ID:
      return (instance) -> {
        return Long.toString(instance.getId());
      };
    case EXTERNAL_ID:
      return (instance) -> {
        if (instance.hasExternalId()) {
          return instance.getExternalId();
        } else if (instance.hasXmlId()) {
          return instance.getXmlId();
        } else {
          return String.format("%s", instance.getId());
        }
      };
    case DEFAULT:
      return (instance) -> {
        if (instance.hasXmlId()) {
          return instance.getXmlId();
        } else if (instance.hasExternalId()) {
          return instance.getExternalId();
        } else {
          return String.format("%s", instance.getId());
        }
      };
    default:
      throw new PlanItException(String.format("unknown id mapping type found for %s %s", Clazz.getName(), idMapper.toString()));
    }
  }

  /**
   * create a function that takes a node and generates the appropriate id based on the user configuration
   * 
   * @param idMapper the type of mapping function to create
   * @return function that generates node id's for MATSIM node output
   * @throws PlanItException thrown if error
   */
  public static Function<Node, String> createNodeIdMappingFunction(final IdMapperType idMapper) throws PlanItException {
    return createIdMappingFunction(Node.class, idMapper);
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
   * create a function that takes a link segment and (optional) id mapper and generates the appropriate MATSIM link id based on the user configuration
   * 
   * @return function that generates mapped link segment id's for persistence
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

}
