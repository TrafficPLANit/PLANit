package org.planit.network.physical.macroscopic;

import java.util.Map;
import java.util.TreeMap;

import org.planit.userclass.Mode;

/**
 * Holds mode properties for a particular link segment type 
 */
public class MacroscopicLinkSegmentTypeModeProperties{
	
	/**
	 * Mode specific link segment type properties
	 */		
	protected Map<Mode, MacroscopicModeProperties> allModeProperties;
	
	
	/**
	 * Constructor
	 */
	public MacroscopicLinkSegmentTypeModeProperties() {
		this.allModeProperties = new TreeMap<Mode, MacroscopicModeProperties>();
	}
	
	/**
	 * Constructor initialising mode properties with a single entry
	 */
	public MacroscopicLinkSegmentTypeModeProperties(Mode mode, MacroscopicModeProperties modeProperties) {
		this.allModeProperties = new TreeMap<Mode, MacroscopicModeProperties>();
		addProperties(mode, modeProperties);
	}	
	
	/** Add mode properties for a specific mode
	 * @param mode
	 * @param modeProperties
	 * @return modeProperties that were overwritten (if any)
	 */
	public MacroscopicModeProperties addProperties(Mode mode, MacroscopicModeProperties modeProperties) {
		return this.allModeProperties.put(mode, modeProperties);
	}
	
	/** collect mode properties for a specific mode 
	 * @param mode
	 * @return mode properties, if any
	 */
	public MacroscopicModeProperties getProperties(Mode mode) {
		return this.allModeProperties.get(mode);
	}

	/** Compare by content
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MacroscopicLinkSegmentTypeModeProperties) {
			return super.equals(obj) && allModeProperties.equals(((MacroscopicLinkSegmentTypeModeProperties)obj).allModeProperties);				
		}
		return false;
	}
}