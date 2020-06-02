package org.planit.network.physical.macroscopic;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.utils.misc.Pair;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Macroscopic Network which stores link segment types
 *
 * @author markr
 *
 */
public class MacroscopicNetwork extends PhysicalNetwork {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicNetwork.class.getCanonicalName());

  // Protected

  /** Generated UID */
  private static final long serialVersionUID = -6844990013871601434L;

  /**
   * Map which stores link segment types by generated Id
   */
  protected Map<Long, MacroscopicLinkSegmentType> macroscopicLinkSegmentTypeByIdMap =
      new TreeMap<Long, MacroscopicLinkSegmentType>();

  /**
   * Map containing the BPR parameters for link segment and mode, if these are
   * specified in the network file (null if default values are being used)
   */
  protected Map<MacroscopicLinkSegment, Map<Mode, Pair<Double, Double>>> bprParametersForLinkSegmentAndMode;

  /**
   * Register a link segment type on the network
   *
   * @param linkSegmentType the MacroscopicLinkSegmentType to be registered
   * @return the registered link segment type
   */
  public MacroscopicLinkSegmentType registerLinkSegmentType(@Nonnull final MacroscopicLinkSegmentType linkSegmentType) {
    return macroscopicLinkSegmentTypeByIdMap.put(linkSegmentType.getId(), linkSegmentType);
  }
  // Public

  /**
   * Constructor
   */
  public MacroscopicNetwork() {
    super(new MacroscopicNetworkBuilder());
  }

  /**
   * Create a new macroscopic link segment type on network. 
   *
   * @param name name of the link segment type
   * @param capacity capacity of the link segment type
   * @param maximumDensity maximum density of the link segment type
   * @param linkSegmentExternalId the external reference number of this link type
   * @param modeProperties mode properties of the link segment type
   * @return  the link segment type
   * @throws PlanItException thrown if there is an error
   */
  public MacroscopicLinkSegmentType createNewMacroscopicLinkSegmentType(@Nonnull final String name,
      final double capacity, final double maximumDensity, final Object linkSegmentExternalId,
      final Map<Mode, MacroscopicModeProperties> modeProperties)
      throws PlanItException {

    if (!(networkBuilder instanceof MacroscopicNetworkBuilder)) {
      String errorMessage =
          "Macroscopic network perspective only allows macroscopic link segment types to be registered";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    MacroscopicLinkSegmentType linkSegmentType =
        ((MacroscopicNetworkBuilder) networkBuilder).createLinkSegmentType(name, capacity, maximumDensity,
            linkSegmentExternalId, modeProperties);
    return linkSegmentType;
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
   * This method has the option to convert the external Id parameter into a long value,
   * to find the link segment type  when link segment type objects use long values for external ids.
   * 
   * @param externalId the external Id of the specified link segment type
   * @param convertToLong if true, the external Id is converted into a long before beginning the search
   * @return the retrieved link segment type, or null if no mode was found
   * @throws PlanItException thrown if the external Id cannot be cast into a long
   */
  public MacroscopicLinkSegmentType getMacroscopicLinkSegmentTypeByExternalId(Object externalId, boolean convertToLong)
      throws PlanItException {
    try {
      if (convertToLong) {
        long value = Long.valueOf(externalId.toString());
        return getMacroscopicLinkSegmentTypeByExternalId(value);
      }
      return getMacroscopicLinkSegmentTypeByExternalId(externalId);
    } catch (NumberFormatException e) {
      String errorMessage = "getMacroscopicLinkSegmentTypeByExternalId was passed a " + externalId.getClass()
          .getCanonicalName() + " which cannot be cast into a long.";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
  }

  /**
   * Retrieve a link segment type by its external Id
   * 
   * This method is not efficient, since it loops through all the registered modes in order
   * to find the required time period. The equivalent method in InputBuilderListener is more
   * efficient and should be used in preference to this in Java code.
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
