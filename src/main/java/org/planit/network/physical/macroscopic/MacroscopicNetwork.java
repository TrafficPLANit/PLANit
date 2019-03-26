package org.planit.network.physical.macroscopic;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.utils.Pair;

public class MacroscopicNetwork extends PhysicalNetwork {
	
	// Private	
	
	private class MacroscopicLinkSegmentTypes implements Iterable<MacroscopicLinkSegmentType> {
		@Override
		public Iterator<MacroscopicLinkSegmentType> iterator() {
			return linkSegmentTypeMap.values().iterator();
		}
	}
	
	// Protected
	
	protected Map<Integer, MacroscopicLinkSegmentType> linkSegmentTypeMap = new TreeMap<Integer, MacroscopicLinkSegmentType>();
	
	/** Register a link segment type on the network
	 * @param linkSegmentType
	 */
	protected MacroscopicLinkSegmentType registerLinkSegmentType(@Nonnull MacroscopicLinkSegmentType linkSegmentType) {	
		return linkSegmentTypeMap.put(linkSegmentType.getId(), linkSegmentType);
	}	
		
	
	// Public

	/**
	 * Constructor
	 */
	public MacroscopicNetwork() {
		super(new MacroscopicNetworkBuilder());
	}
	
	/**
	 * Find if there already exists a link segment type with the same contents, if so return it, otherwise return null
	 * @return equalLinkSegmentType
	 */
	public MacroscopicLinkSegmentType findEqualMacroscopicLinkSegmentType(@Nonnull MacroscopicLinkSegmentType linkSegmentType) {
		for(Iterator<MacroscopicLinkSegmentType> i = macroscopiclinkSegmentTypes().iterator();i.hasNext();) {
			MacroscopicLinkSegmentType currentLinkSegmentType = i.next();
			if(currentLinkSegmentType.equals(linkSegmentType)) {
				return currentLinkSegmentType;
			}
		}
		return null;
	}
	
	/** Create and register new link segment type on network. If there already exists a link segment type with the same contents the existing
	 * type is returned and no new type will be registered
	 * @param name
	 * @param capacity
	 * @param maximumDensity
	 * @param macroscopicLinkSegmentTypeModeProperties
	 * @return <createdLinkSegmentType, isAdded>, when the proposed link segment type contents (all but name and id) are unique, it is registered on the network isAdded is set to true and the object is returned.
	 * Otherwise the returned object is the already registered (equal) link segment type and isAdded is set to false. 
	 * @throws PlanItException 
	 */
	public Pair<MacroscopicLinkSegmentType,Boolean> registerNewLinkSegmentType(@Nonnull String name, double capacity, double maximumDensity, MacroscopicLinkSegmentTypeModeProperties modeProperties) throws PlanItException {
		if(!(networkBuilder instanceof MacroscopicNetworkBuilder)){
			throw new PlanItException("Macroscopic network perspective only allows macroscopic link segment types to be registered");
		}
		MacroscopicLinkSegmentType linkSegmentType = ((MacroscopicNetworkBuilder) networkBuilder).createLinkSegmentType(name, capacity, maximumDensity, modeProperties);
		MacroscopicLinkSegmentType existingLinkSegmentType = findEqualMacroscopicLinkSegmentType(linkSegmentType);
		if(existingLinkSegmentType == null){
			registerLinkSegmentType(linkSegmentType);			
		}else {
			linkSegmentType = existingLinkSegmentType;
		}
		return new Pair<MacroscopicLinkSegmentType, Boolean>(linkSegmentType,existingLinkSegmentType == linkSegmentType);
	}	
		
	/**
	 * Create iterator for macroscopiclinkSegmentTypesContainer
	 * @return LinkSegmentTypes iterator
	 */
	public MacroscopicLinkSegmentTypes macroscopiclinkSegmentTypes() {
		return new MacroscopicLinkSegmentTypes();
	}		
	
	

}
