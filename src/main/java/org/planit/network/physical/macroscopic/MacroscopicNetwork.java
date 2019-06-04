package org.planit.network.physical.macroscopic;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

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

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(MacroscopicNetwork.class.getName());

    // Private

    private class MacroscopicLinkSegmentTypes implements Iterable<MacroscopicLinkSegmentType> {
        @Override
        public Iterator<MacroscopicLinkSegmentType> iterator() {
            return linkSegmentTypeByIdMap.values().iterator();
        }
    }

    // Protected

    /**
     * Map which stores link segment types by generated Id
     */
    protected Map<Integer, MacroscopicLinkSegmentType> linkSegmentTypeByIdMap = new TreeMap<Integer, MacroscopicLinkSegmentType>();
    
    /**
     * Map which stores link segment types by their external Id
     */
    protected Map<Integer, MacroscopicLinkSegmentType> linkSegmentTypeByExternalIdMap = new TreeMap<Integer, MacroscopicLinkSegmentType>(); 

    /**
     * Register a link segment type on the network
     * 
     * @param linkSegmentType
     *            the MacroscopicLinkSegmentType to be registered
     * @return the registered link segment type
     */
    protected MacroscopicLinkSegmentType registerLinkSegmentType(@Nonnull MacroscopicLinkSegmentType linkSegmentType) {
    	linkSegmentTypeByExternalIdMap.put(linkSegmentType.getLinkTypeExternalId(), linkSegmentType);
        return linkSegmentTypeByIdMap.put(linkSegmentType.getId(), linkSegmentType);
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
     * @param linkSegmentType
     *            the new MacroscopicLinkSegmentType being tested against
     * @return existing MacroscopicLinkSegmentType equal to the new one if one
     *         exists, otherwise null
     */
    public MacroscopicLinkSegmentType findEqualMacroscopicLinkSegmentType(
            @Nonnull MacroscopicLinkSegmentType linkSegmentType) {
        Iterator<MacroscopicLinkSegmentType> iterator = macroscopiclinkSegmentTypes().iterator();
        while (iterator.hasNext()) {
            MacroscopicLinkSegmentType currentLinkSegmentType = iterator.next();
            if (currentLinkSegmentType.equals(linkSegmentType)) {
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
     * @param name
     *            name of the link segment type
     * @param capacity
     *            capacity of the link segment type
     * @param maximumDensity
     *            maximum density of the link segment type
     * @param linkType
     *            the external reference number of this link type
     * @param modeProperties
     *            mode properties of the link segment type
     * @return Pair containing the link segment type, plus boolean which is true if
     *         the link segment type already exists
     * @throws PlanItException
     *             thrown if there is an error
     */
    public Pair<MacroscopicLinkSegmentType, Boolean> registerNewLinkSegmentType(@Nonnull String name, 
    		                                                                                                                                  double capacity,
                                                                                                                                              double maximumDensity,
                                                                                                                                              int linkType,
                                                                                                                                              MacroscopicLinkSegmentTypeModeProperties modeProperties) throws PlanItException {

        if (!(networkBuilder instanceof MacroscopicNetworkBuilder)) {
            throw new PlanItException(
                    "Macroscopic network perspective only allows macroscopic link segment types to be registered");
        }
        MacroscopicLinkSegmentType linkSegmentType = ((MacroscopicNetworkBuilder) networkBuilder).createLinkSegmentType(name, capacity, maximumDensity, linkType, modeProperties);
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
     * Create iterator for macroscopiclinkSegmentTypesContainer
     * 
     * @return LinkSegmentTypes iterator
     */
    public MacroscopicLinkSegmentTypes macroscopiclinkSegmentTypes() {
        return new MacroscopicLinkSegmentTypes();
    }
    
    /**
     * Return the number of link segment types
     * 
     * @return number of link segment types
     */
    public int getNoSegmentTypes() {
    	return linkSegmentTypeByIdMap.keySet().size();
    }
    
    /**
     * Return a link segment type identified by its generated id
     * 
     * @param id id value of the MacroscopicLinkSegmentType 
     * @return MacroscopicLinkSegmentType object found
     */
    public MacroscopicLinkSegmentType findLinkSegmentTypeById(int id) {
    	return linkSegmentTypeByIdMap.get(id);
    }
 
    /**
     * Return a link segment type identified by its external id
     * 
     * @param id external id value of the MacroscopicLinkSegmentType 
     * @return MacroscopicLinkSegmentType object found
     */

    public MacroscopicLinkSegmentType findLinkSegmentTypeByExternalId(int externalId) {
    	return linkSegmentTypeByExternalIdMap.get(externalId);
    }

}
