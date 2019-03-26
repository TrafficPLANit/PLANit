package org.planit.network.physical.macroscopic;

import org.planit.utils.DefaultValues;

/** Mode specific properties for the macroscopic perspective on the supply side, i.e. on a link segment of a particular type
 * 
 * @author markr
 *
 */
public class MacroscopicModeProperties {
	
	/**
	 * Maximum speed of mode (tied to a road segment)
	 */
	protected final double maxSpeed;
	/**
	 * Maximum speed of mode (tied to a road segment)
	 */	
	protected final double criticalSpeed;
	
	// Public
	
	/**
	 *  Default max speed in km/h
	 */	
	public static final double DEFAULT_MAX_SPEED = 80;
	/**
	 *  Default critical speed, i.e. speed at capacity in km/h
	 */	
	public static final double DEFAULT_CRITICAL_SPEED = 60;		
	
	/** Constructor 
	 * @param maxSpeed
	 * @param criticalSpeed
	 */
	public MacroscopicModeProperties(double maxSpeed, double criticalSpeed) {
		super();
		this.maxSpeed = maxSpeed;
		this.criticalSpeed = criticalSpeed;
	}	
	
	/** Constructor adopting default values 
	 */
	public MacroscopicModeProperties() {
		super();
		this.maxSpeed = DEFAULT_MAX_SPEED;
		this.criticalSpeed = DEFAULT_CRITICAL_SPEED;
	}		
	
	/** Compare on all content members
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MacroscopicModeProperties){
			return 	super.equals(obj) && 
							Math.abs(this.getMaxSpeed()-((MacroscopicModeProperties)obj).getMaxSpeed()) < DefaultValues.DEFAULT_EPSILON &&
							Math.abs(this.getCriticalSpeed()-((MacroscopicModeProperties)obj).getCriticalSpeed()) < DefaultValues.DEFAULT_EPSILON;
		}
		return false;
	}	
	
	// Getter - setters
	
	public double getMaxSpeed() {
		return maxSpeed;
	}
	public double getCriticalSpeed() {
		return criticalSpeed;
	}

}
