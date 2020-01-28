package org.planit.network.physical.macroscopic;

import javax.annotation.Nonnull;

import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentTypeModeProperties;

/**
 * Each macroscopic link segment is of a particular type reflecting segment
 * specific properties. On top of the segment specific properties each segment
 * can have user class specific properties as well.
 * 
 * @author markr
 *
 */
public class MacroscopicLinkSegmentTypeImpl implements MacroscopicLinkSegmentType {

		

	// Protected

	/**
	 * Unique segment type id
	 */
	protected final int id;

	/**
	 * External reference number of link type
	 */
	private long externalId;

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

	/**
	 * Generate next id available
	 * 
	 * @return id of this link segment
	 */
	protected static int generateMacroscopicLinkSegmentTypeId() {
		return IdGenerator.generateId(MacroscopicLinkSegmentType.class);
	}

	// Public

	/**
	 * Constructor
	 * 
	 * @param name                  name of this link segment type
	 * @param capacityPerLane       capacity per lane of this link segment type
	 * @param maximumDensityPerLane maximum density per lane of this link segment
	 *                              type
	 * @param externalId    external reference number of the link type
	 * @param modeProperties        properties of this link segment type
	 */
	public MacroscopicLinkSegmentTypeImpl(@Nonnull String name, double capacityPerLane, double maximumDensityPerLane,
			long externalId, MacroscopicLinkSegmentTypeModeProperties modeProperties) {
		this.id = generateMacroscopicLinkSegmentTypeId();
		this.name = name;
		this.capacityPerLane = capacityPerLane;
		this.maximumDensityPerLane = maximumDensityPerLane;
		this.externalId = externalId;
		this.modeProperties = modeProperties;
	}

	// Getters - Setters

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public double getCapacityPerLane() {
		return capacityPerLane;
	}

	@Override
	public double getMaximumDensityPerLane() {
		return maximumDensityPerLane;
	}

	@Override
	public long getExternalId() {
		return externalId;
	}

	/**
	 * reference to internal mode properties
	 * 
	 * @return segmentModeProperties
	 */
	@Override
	public MacroscopicLinkSegmentTypeModeProperties getModeProperties() {
		return modeProperties;
	}

}