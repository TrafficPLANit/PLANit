package org.planit.network.physical.macroscopic;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.PhysicalNetworkBuilder;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Macroscopic Network which stores link segment types
 *
 * @author markr
 *
 */
public class MacroscopicNetwork extends PhysicalNetwork<Node, Link, MacroscopicLinkSegment> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(MacroscopicNetwork.class.getCanonicalName());

  /** Generated UID */
  private static final long serialVersionUID = -6844990013871601434L;

  // Protected

  /**
   * Map which stores link segment types by generated Id
   */
  protected Map<Long, MacroscopicLinkSegmentType> macroscopicLinkSegmentTypeByIdMap = new TreeMap<Long, MacroscopicLinkSegmentType>();

  /**
   * collect the builder as macroscopic network builder
   * 
   * @return macroscopic network builder of this network
   */
  protected MacroscopicPhysicalNetworkBuilder<? extends Node, ? extends Link, ? extends MacroscopicLinkSegment> getMacroscopicNetworkBuilder() {
    return (MacroscopicPhysicalNetworkBuilder<? extends Node, ? extends Link, ? extends MacroscopicLinkSegment>) getNetworkBuilder();
  }

  // Public

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public MacroscopicNetwork(final IdGroupingToken groupId) {
    super(groupId, new MacroscopicPhysicalNetworkBuilderImpl());
  }

  /**
   * Constructor
   * 
   * @param groupId       contiguous id generation within this group for instances of this class
   * @param customBuilder a customBuilder
   */
  @SuppressWarnings("unchecked")
  public MacroscopicNetwork(final IdGroupingToken groupId, MacroscopicPhysicalNetworkBuilder<? extends Node, ? extends Link, ? extends MacroscopicLinkSegment> customBuilder) {
    super(groupId, (PhysicalNetworkBuilder<Node, Link, MacroscopicLinkSegment>) customBuilder);
  }

  /**
   * Create and register new macroscopic link segment type on network.
   *
   * @param name                   name of the link segment type
   * @param capacityPcuPerHour     capacity of the link segment type
   * @param maximumDensityPcuPerKm maximum density of the link segment type
   * @param linkSegmentExternalId  the external reference number of this link type
   * @param modeProperties         mode properties of the link segment type
   * @return the link segment type
   * @throws PlanItException thrown if there is an error
   */
  public MacroscopicLinkSegmentType createAndRegisterNewMacroscopicLinkSegmentType(final String name, final double capacityPcuPerHour, final double maximumDensityPcuPerKm,
      final Object linkSegmentExternalId, final Map<Mode, MacroscopicModeProperties> modeProperties) throws PlanItException {

    MacroscopicLinkSegmentType linkSegmentType = getMacroscopicNetworkBuilder().createLinkSegmentType(name, capacityPcuPerHour, maximumDensityPcuPerKm, linkSegmentExternalId,
        modeProperties);
    registerLinkSegmentType(linkSegmentType);
    return linkSegmentType;
  }

  /**
   * Create and register new macroscopic link segment type on network. No mode properties will be set (null)
   *
   * @param name                   name of the link segment type
   * @param capacityPcuPerHour     capacity of the link segment type
   * @param maximumDensityPcuPerKm maximum density of the link segment type
   * @param linkSegmentExternalId  the external reference number of this link type
   * @return the link segment type
   * @throws PlanItException thrown if there is an error
   */
  public MacroscopicLinkSegmentType createAndRegisterNewMacroscopicLinkSegmentType(final String name, final double capacityPcuPerHour, final double maximumDensityPcuPerKm,
      final Object linkSegmentExternalId) throws PlanItException {

    MacroscopicLinkSegmentType linkSegmentType = getMacroscopicNetworkBuilder().createLinkSegmentType(name, capacityPcuPerHour, maximumDensityPcuPerKm, linkSegmentExternalId);
    registerLinkSegmentType(linkSegmentType);
    return linkSegmentType;
  }

  /**
   * Register a link segment type on the network
   *
   * @param linkSegmentType the MacroscopicLinkSegmentType to be registered
   * @return the registered link segment type
   */
  public MacroscopicLinkSegmentType registerLinkSegmentType(final MacroscopicLinkSegmentType linkSegmentType) {
    return macroscopicLinkSegmentTypeByIdMap.put(linkSegmentType.getId(), linkSegmentType);
  }

  /**
   * Return a link segment type identified by its generated id
   *
   * @param id id value of the MacroscopicLinkSegmentType
   * @return MacroscopicLinkSegmentType object found
   */
  public MacroscopicLinkSegmentType findMacroscopicLinkSegmentTypeById(final long id) {
    return macroscopicLinkSegmentTypeByIdMap.get(id);
  }

  /**
   * Retrieve a link segment type by its external Id
   * 
   * This method has the option to convert the external Id parameter into a long value, to find the link segment type when link segment type objects use long values for external
   * ids.
   * 
   * @param externalId    the external Id of the specified link segment type
   * @param convertToLong if true, the external Id is converted into a long before beginning the search
   * @return the retrieved link segment type, or null if no mode was found
   */
  public MacroscopicLinkSegmentType getMacroscopicLinkSegmentTypeByExternalId(Object externalId, boolean convertToLong) {
    try {
      if (convertToLong) {
        long value = Long.valueOf(externalId.toString());
        return getMacroscopicLinkSegmentTypeByExternalId(value);
      }
      return getMacroscopicLinkSegmentTypeByExternalId(externalId);
    } catch (NumberFormatException e) {
      // do nothing - if conversion to long is not possible, use the general method instead
    }
    return getMacroscopicLinkSegmentTypeByExternalId(externalId);
  }

  /**
   * Retrieve a link segment type by its external Id
   * 
   * This method is not efficient, since it loops through all the registered modes in order to find the required link segment type. The equivalent method in InputBuilderListener is
   * more efficient and should be used in preference to this in Java code.
   * 
   * @param externalId the external Id of the specified link segment type
   * @return the retrieved link segment type, or null if no link segment type was found
   */
  public MacroscopicLinkSegmentType getMacroscopicLinkSegmentTypeByExternalId(Object externalId) {
    for (MacroscopicLinkSegmentType macroscopicLinkSegmentType : macroscopicLinkSegmentTypeByIdMap.values()) {
      if (macroscopicLinkSegmentType.getExternalId().equals(externalId)) {
        return macroscopicLinkSegmentType;
      }
    }
    return null;
  }
}
