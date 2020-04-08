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
	protected Map<Integer, MacroscopicLinkSegmentType> macroscopicLinkSegmentTypeByIdMap = new TreeMap<Integer, MacroscopicLinkSegmentType>();

	/**
	 * Map containing the BPR parameters for link segment and mode, if these are specified in the network file (null if default values are being used)
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
	 * If there already exists a link segment type with the same contents return it,
	 * otherwise return null
	 *
	 * @param linkSegmentType the new MacroscopicLinkSegmentType being tested against
	 * @return existing MacroscopicLinkSegmentType equal to the new one if one exists, otherwise null
	 */
	public MacroscopicLinkSegmentType findEqualMacroscopicLinkSegmentType(@Nonnull final MacroscopicLinkSegmentType linkSegmentType) {
		for (final MacroscopicLinkSegmentType currentLinkSegmentType  : macroscopicLinkSegmentTypeByIdMap.values()) {
			if (currentLinkSegmentType.getExternalId() == linkSegmentType.getExternalId()) {
				return currentLinkSegmentType;
			}
		}
		return null;
	}

	/**
	 * Create and register new link segment type on network. If there already exists
	 * a link segment type with the same contents the existing type is returned and
	 * no new type will be registered
	 *
	 * @param name           name of the link segment type
	 * @param capacity       capacity of the link segment type
	 * @param maximumDensity maximum density of the link segment type
	 * @param externalId       the external reference number of this link type
	 * @param modeProperties mode properties of the link segment type
	 * @return Pair containing the link segment type, plus boolean which is true if
	 *         the link segment type already exists
	 * @throws PlanItException thrown if there is an error
	 */
  public Pair<MacroscopicLinkSegmentType, Boolean> registerNewLinkSegmentType(@Nonnull final String name, final double capacity,
      final double maximumDensity, final Object externalId, final Map<Mode, MacroscopicModeProperties> modeProperties)
			throws PlanItException {

    boolean linkSegmentTypeAlreadyExists = true;
		if (!(networkBuilder instanceof MacroscopicNetworkBuilder)) {
			String errorMessage = "Macroscopic network perspective only allows macroscopic link segment types to be registered";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
		}
		MacroscopicLinkSegmentType linkSegmentType =
				((MacroscopicNetworkBuilder) networkBuilder).createLinkSegmentType(name, capacity, maximumDensity, externalId, modeProperties);
		final MacroscopicLinkSegmentType existingLinkSegmentType = findEqualMacroscopicLinkSegmentType(linkSegmentType);
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
	public MacroscopicLinkSegmentType findMacroscopicLinkSegmentTypeById(final int id) {
		return macroscopicLinkSegmentTypeByIdMap.get(id);
	}
	
}