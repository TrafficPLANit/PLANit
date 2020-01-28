package org.planit.network.physical.macroscopic;

import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentTypeModeProperties;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Holds mode properties for a particular link segment type
 */
public class MacroscopicLinkSegmentTypeModePropertiesImpl implements MacroscopicLinkSegmentTypeModeProperties {

    /**
     * Mode specific link segment type properties
     */
    protected Map<Mode, MacroscopicModeProperties> allModeProperties;

    /**
     * Constructor
     */
    public MacroscopicLinkSegmentTypeModePropertiesImpl() {
        this.allModeProperties = new TreeMap<Mode, MacroscopicModeProperties>();
    }

    /**
     * Constructor initializing mode properties with a single entry
     * 
     * @param mode
     *            mode of this link segment type
     * @param modeProperties
     *            properties of this mode
     */
    public MacroscopicLinkSegmentTypeModePropertiesImpl(Mode mode, MacroscopicModeProperties modeProperties) {
        this.allModeProperties = new TreeMap<Mode, MacroscopicModeProperties>();
        addProperties(mode, modeProperties);
    }

    /**
     * Add mode properties for a specific mode
     * 
     * @param mode
     *            mode of this link segment type
     * @param modeProperties
     *            properties of this link segment type
     * @return modeProperties that were overwritten (if any)
     */
    @Override
	public MacroscopicModeProperties addProperties(Mode mode, MacroscopicModeProperties modeProperties) {
        return this.allModeProperties.put(mode, modeProperties);
    }

    /**
     * Get mode properties for a specific mode
     * 
     * @param mode
     *            mode
     * @return properties for specified mode
     */
    @Override
	public MacroscopicModeProperties getProperties(Mode mode) {
        return this.allModeProperties.get(mode);
    }

}