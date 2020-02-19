package org.planit.network.physical.macroscopic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

	// Protected

	/** Generated UID */
	private static final long serialVersionUID = -6844990013871601434L;

	/**
	 * Map which stores link segment types by generated Id
	 */
	protected Map<Integer, MacroscopicLinkSegmentType> macroscopicLinkSegmentTypeByIdMap = new TreeMap<Integer, MacroscopicLinkSegmentType>();

	/**
	 * Map which stores link segment types by their external Id
	 */
	protected Map<Long, MacroscopicLinkSegmentType> macroscopicLinkSegmentTypeByExternalIdMap = new TreeMap<Long, MacroscopicLinkSegmentType>();

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
      final double maximumDensity, final long externalId, final Map<Mode, MacroscopicModeProperties> modeProperties)
			throws PlanItException {

		if (!(networkBuilder instanceof MacroscopicNetworkBuilder)) {
			throw new PlanItException(
					"Macroscopic network perspective only allows macroscopic link segment types to be registered");
		}
		MacroscopicLinkSegmentType linkSegmentType =
				((MacroscopicNetworkBuilder) networkBuilder).createLinkSegmentType(name, capacity, maximumDensity, externalId, modeProperties);
		final MacroscopicLinkSegmentType existingLinkSegmentType = findEqualMacroscopicLinkSegmentType(linkSegmentType);
		if (existingLinkSegmentType == null) {
			registerLinkSegmentType(linkSegmentType);
		} else {
			linkSegmentType = existingLinkSegmentType;
		}
		return new Pair<MacroscopicLinkSegmentType, Boolean>(linkSegmentType,existingLinkSegmentType == linkSegmentType);
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
	public MacroscopicLinkSegmentType findMacroscopicLinkSegmentTypeById(final int id) {
		return macroscopicLinkSegmentTypeByIdMap.get(id);
	}

	/**
	 * Return a link segment type identified by its external id
	 *
	 * @param externalId external id value of the MacroscopicLinkSegmentType
	 * @return MacroscopicLinkSegmentType object found
	 */
	public MacroscopicLinkSegmentType findMacroscopicLinkSegmentTypeByExternalId(final long externalId) {
		return macroscopicLinkSegmentTypeByExternalIdMap.get(externalId);
	}
	
	/**
	 * Return the BPR parameters for a specified link segment and mode
	 * 
   * @param linkSegment the specified link segment
   * @param mode the specified mode
	 * @return Pair containing the alpha and beta parameters
	 */
  public Pair<Double, Double> getBprParametersForLinkSegmentAndMode(MacroscopicLinkSegment macroscopicLinkSegment, Mode mode) {
    return bprParametersForLinkSegmentAndMode.get(macroscopicLinkSegment).get(mode);
  }
  
  /**
   * Indicates whether BPR parameters have been defined for any links and modes
   * 
   * @return true if BPR parameters defined for any links and modes, false otherwise
   */
  public boolean isBprParametersDefinedForLinkSegments() {
    return (bprParametersForLinkSegmentAndMode != null);
  }

  /**
   * Add BPR parameters for a specified link segment and mode
   * 
   * @param linkSegment the specified link segment
   * @param mode the specified mode
   * @param alpha the BPR alpha parameter
   * @param beta the BPR beta parameter
   */
  public void addBprParametersForLinkSegmentAndMode(MacroscopicLinkSegment linkSegment, Mode mode, double alpha, double beta) {
    if (bprParametersForLinkSegmentAndMode == null) {
      bprParametersForLinkSegmentAndMode = new HashMap<MacroscopicLinkSegment, Map<Mode, Pair<Double, Double>>>();
    }
    if (!bprParametersForLinkSegmentAndMode.containsKey(linkSegment)) {
      bprParametersForLinkSegmentAndMode.put(linkSegment, new HashMap<Mode, Pair<Double, Double>>());
    }
    Pair<Double, Double> alphaBeta = new Pair<Double, Double>(alpha, beta);
    bprParametersForLinkSegmentAndMode.get(linkSegment).put(mode, alphaBeta);
  }

}