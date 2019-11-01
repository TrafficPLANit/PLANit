package org.planit.network.physical.macroscopic;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.utils.Pair;

/**
 * Macroscopic Network which stores link segment types
 * 
 * @author markr
 *
 */
public class MacroscopicNetwork extends PhysicalNetwork {

	// Protected

	/**
	 * Map which stores link segment types by generated Id
	 */
	protected Map<Integer, MacroscopicLinkSegmentType> macroscopicLinkSegmentTypeByIdMap = new TreeMap<Integer, MacroscopicLinkSegmentType>();

	/**
	 * Map which stores link segment types by their external Id
	 */
	protected Map<Long, MacroscopicLinkSegmentType> macroscopicLinkSegmentTypeByExternalIdMap = new TreeMap<Long, MacroscopicLinkSegmentType>();

	/**
	 * Register a link segment type on the network
	 * 
	 * @param linkSegmentType the MacroscopicLinkSegmentType to be registered
	 * @return the registered link segment type
	 */
	protected MacroscopicLinkSegmentType registerLinkSegmentType(@Nonnull MacroscopicLinkSegmentType linkSegmentType) {
		macroscopicLinkSegmentTypeByExternalIdMap.put(linkSegmentType.getExternalId(), linkSegmentType);
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
	public MacroscopicLinkSegmentType findEqualMacroscopicLinkSegmentType(@Nonnull MacroscopicLinkSegmentType linkSegmentType) {
		for (MacroscopicLinkSegmentType currentLinkSegmentType  : macroscopicLinkSegmentTypeByIdMap.values()) {
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
	public Pair<MacroscopicLinkSegmentType, Boolean> registerNewLinkSegmentType(@Nonnull String name, double capacity,
			double maximumDensity, long externalId, MacroscopicLinkSegmentTypeModeProperties modeProperties)
			throws PlanItException {

		if (!(networkBuilder instanceof MacroscopicNetworkBuilder)) {
			throw new PlanItException(
					"Macroscopic network perspective only allows macroscopic link segment types to be registered");
		}
		MacroscopicLinkSegmentType linkSegmentType = ((MacroscopicNetworkBuilder) networkBuilder)
				.createLinkSegmentType(name, capacity, maximumDensity, externalId, modeProperties);
		MacroscopicLinkSegmentType existingLinkSegmentType = findEqualMacroscopicLinkSegmentType(linkSegmentType);
		if (existingLinkSegmentType == null) {
			registerLinkSegmentType(linkSegmentType);
		} else {
			linkSegmentType = existingLinkSegmentType;
		}
		return new Pair<MacroscopicLinkSegmentType, Boolean>(linkSegmentType,
				existingLinkSegmentType == linkSegmentType);
	}

	/**
	 * Return the Set of link segment type external Ids
	 * 
	 * @return Set of link segment type external Ids
	 */
	public Set<Long> getMacroscopicLinkSegmentExternalIdSet() {
		return macroscopicLinkSegmentTypeByExternalIdMap.keySet();
	}

	/**
	 * Return a link segment type identified by its generated id
	 * 
	 * @param id id value of the MacroscopicLinkSegmentType
	 * @return MacroscopicLinkSegmentType object found
	 */
	public MacroscopicLinkSegmentType findMacroscopicLinkSegmentTypeById(int id) {
		return macroscopicLinkSegmentTypeByIdMap.get(id);
	}

	/**
	 * Return a link segment type identified by its external id
	 * 
	 * @param externalId external id value of the MacroscopicLinkSegmentType
	 * @return MacroscopicLinkSegmentType object found
	 */
	public MacroscopicLinkSegmentType findMacroscopicLinkSegmentTypeByExternalId(long externalId) {
		return macroscopicLinkSegmentTypeByExternalIdMap.get(externalId);
	}

}
