package org.planit.network.physical.macroscopic;


import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.utils.IdGenerator;

/** 
 * Each macroscopic link segment is of a particular type reflecting segment specific properties. On top of the segment specific properties
 * each segment can have user class specific properties as well. 
 * @author markr
 *
 */
public class MacroscopicLinkSegmentType {
		
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentType.class.getName());
        
	public static final double DEFAULT_MAXIMUM_DENSITY_LANE = 180;
	
	// Protected
		
	/**
	 * Unique segment type id
	 */
	protected final int id;
	
	/**
	 * name of the link segment type
	 */
	protected String name;	

	/**
	 * Maximum flow, i.e. capacity in veh/h/lane
	 */
	protected final Map<Long, Double> capacityPerLane;
	
	/**
	 * Maximum density in veh/km/lane
	 */
	protected final Map<Long, Double> maximumDensityPerLane;  
	
	/**
	 * All mode specific properties are captured within this member
	 */
	protected final MacroscopicLinkSegmentTypeModeProperties modeProperties;
		
/** 
 * Generate next id available
 * 
 * @return    id of this link segment
 */
	protected static int generateMacroscopicLinkSegmentTypeId() {
		return IdGenerator.generateId(MacroscopicLinkSegmentType.class);
	}
		
	// Public		

/** 
 * Constructor
 * 
 * @param name                                      name of this link segment type
 * @param capacityPerLane                    capacity per lane of this link segment type
 * @param maximumDensityPerLane     maximum density per lane of this link segment type
 * @param modeProperties                     properties of this link segment type
 */
	public MacroscopicLinkSegmentType(@Nonnull String name, Map<Long, Double> capacityPerLane, Map<Long, Double> maximumDensityPerLane, MacroscopicLinkSegmentTypeModeProperties modeProperties) {
		this.id = generateMacroscopicLinkSegmentTypeId();
		this.name = name;
		this.capacityPerLane = capacityPerLane;
		this.maximumDensityPerLane = maximumDensityPerLane;
		this.modeProperties = modeProperties;
	}

	// Getters - Setters
	
	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public double getCapacityPerLane(long modeId) {
	    return capacityPerLane.get(modeId);
	}

	public double getMaximumDensityPerLane(long modeId) {
	    return maximumDensityPerLane.get(modeId);
	}

	/** reference to internal mode properties
	 * @return segmentModeProperties
	 */
	public MacroscopicLinkSegmentTypeModeProperties getModeProperties(){
		return modeProperties;
	}

}
