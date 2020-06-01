package org.planit.network.physical.macroscopic;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
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
	protected Map<Long, MacroscopicLinkSegmentType> macroscopicLinkSegmentTypeByIdMap = new TreeMap<Long, MacroscopicLinkSegmentType>();

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
	protected MacroscopicLinkSegmentType registerLinkSegmentType(@Nonnull final MacroscopicLinkSegmentType linkSegmentType) {
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
	 * Create and register new macroscopic link segment type on network. If there already exists
	 * a link segment type with the same contents the existing type is returned and
	 * no new type will be registered
	 *
	 * @param name           name of the link segment type
	 * @param capacity       capacity of the link segment type
	 * @param maximumDensity maximum density of the link segment type
	 * @param linkSegmentExternalId     the external reference number of this link type
	 * @param modeProperties mode properties of the link segment type
   * @param inputBuilderListener parser containing modes by external Id
	 * @return Pair containing the link segment type, plus boolean which is true if
	 *         the link segment type already exists
	 * @throws PlanItException thrown if there is an error
	 */
	public Pair<MacroscopicLinkSegmentType, Boolean> registerNewMacroscopicLinkSegmentType(@Nonnull final String name,
			final double capacity, final double maximumDensity, final Object linkSegmentExternalId,
			final Map<Mode, MacroscopicModeProperties> modeProperties, InputBuilderListener inputBuilderListener) throws PlanItException {

		boolean linkSegmentTypeAlreadyExists = true;
		if (!(networkBuilder instanceof MacroscopicNetworkBuilder)) {
			String errorMessage = "Macroscopic network perspective only allows macroscopic link segment types to be registered";
			LOGGER.severe(errorMessage);
			throw new PlanItException(errorMessage);
		}
		MacroscopicLinkSegmentType linkSegmentType = 
				((MacroscopicNetworkBuilder) networkBuilder).createLinkSegmentType(name, capacity, maximumDensity, linkSegmentExternalId, modeProperties);

		MacroscopicLinkSegmentType existingLinkSegmentType = inputBuilderListener.getLinkSegmentTypeByExternalId(linkSegmentType.getExternalId());
		if (existingLinkSegmentType == null) {
			registerLinkSegmentType(linkSegmentType);
			linkSegmentTypeAlreadyExists = false;
		} else {
		  	linkSegmentType = existingLinkSegmentType;
		}

		return new Pair<MacroscopicLinkSegmentType, Boolean>(linkSegmentType, linkSegmentTypeAlreadyExists);
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
   * Retrieve a LinkSegmentType by its external Id
   * 
   * This method is not efficient, since it loops through all the registered link segment types in order 
   * to find the required time period.  The equivalent method in InputBuilderListener is more
   * efficient and should be used in preference to this in Java code.
   * 
   *  This method is intended for use by the Python interface, which cannot access the
   *  InputBuilderListener.
   *  
   *  The Python interface cannot send values as Long objects, it can only send them as
   *  Integers.  The internal map uses Long objects as keys.  So it is necessary to 
   *  convert Integer inputs into Longs before using them.
   * 
   * @param externalId the external Id of the specified link segment type
   * @return the retrieved link segment type, or null if no link segment type was found
   */
	public MacroscopicLinkSegmentType getMacroscopicLinkSegmentTypeByExternalId(Object externalid) {
    if (externalid instanceof Integer) {
      int value = (Integer) externalid;
      externalid = (long) value;
    }
    for (MacroscopicLinkSegmentType macroscopicLinkSegmentType : macroscopicLinkSegmentTypeByIdMap.values()) {
      if (macroscopicLinkSegmentType.getExternalId().equals(externalid)) {
        return macroscopicLinkSegmentType;
      }
    }
    return null;
	}
}