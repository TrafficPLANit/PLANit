package org.planit.network.physical.macroscopic;


import javax.annotation.Nonnull;

import org.planit.utils.DefaultValues;
import org.planit.utils.IdGenerator;

/** 
 * Each macroscopic link segment is of a particular type reflecting segment specific properties. On top of the segment specific properties
 * each segment can have user class specific properties as well. 
 * @author markr
 *
 */
public class MacroscopicLinkSegmentType implements Comparable<MacroscopicLinkSegmentType>{
		
	public static final double DEFAULT_MAXIMUM_DENSITY_LANE = 180;
	
	//public static final double DEFAULT_CAPACITY_LANE = 1800;	
			
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
	protected final double capacityPerLane;
	
	/**
	 * Maximum density in veh/km/lane
	 */
	protected final double maximumDensityPerLane;	
	
	/**
	 * All mode specific properties are captured within this member
	 */
	protected final MacroscopicLinkSegmentTypeModeProperties modeProperties;
	
	
	/** generate next id available
	 * @return
	 */
	protected static int generateMacroscopicLinkSegmentTypeId() {
		return IdGenerator.generateId(MacroscopicLinkSegmentType.class);
	}
		
	// Public		

	/** Constructor
	 * @param name
	 * @param capacityPerLane
	 * @param maximumDensityPerLane
	 * @param modeProperties
	 */
	public MacroscopicLinkSegmentType(@Nonnull String name, double capacityPerLane, double maximumDensityPerLane, MacroscopicLinkSegmentTypeModeProperties modeProperties) {
		this.id = generateMacroscopicLinkSegmentTypeId();
		this.name = name;
		this.capacityPerLane = capacityPerLane;
		this.maximumDensityPerLane = maximumDensityPerLane;
		this.modeProperties = modeProperties;
	}

	/** Compare on content but not on name and id
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {		
		// Name is not what defines the characteristics, so we only consider capacity, max density, and mode specific information
		if(super.equals(obj) && obj instanceof MacroscopicLinkSegmentType) {
			MacroscopicLinkSegmentType other = (MacroscopicLinkSegmentType) obj; 
			return 	(Math.abs(this.getCapacityPerLane() - other.getCapacityPerLane()) < DefaultValues.DEFAULT_EPSILON) && 
					        (Math.abs(this.getMaximumDensityPerLane() - other.getMaximumDensityPerLane()) < DefaultValues.DEFAULT_EPSILON) &&
					        (this.modeProperties.equals(other.modeProperties));
		}
		return false; 
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
	
	public double getCapacityPerLane() {
		return capacityPerLane;
	}

	public double getMaximumDensityPerLane() {
		return maximumDensityPerLane;
	}

	/** reference to internal mode properties
	 * @return segmentModeProperties
	 */
	public MacroscopicLinkSegmentTypeModeProperties getModeProperties(){
		return modeProperties;
	}

	@Override
	public int compareTo(MacroscopicLinkSegmentType other) {
		int compare = Double.compare(this.getCapacityPerLane(), other.getCapacityPerLane());
		if (compare != 0) {
			return compare;
		}		
		return Double.compare(this.getMaximumDensityPerLane(), other.getMaximumDensityPerLane());
	}		
		
}
