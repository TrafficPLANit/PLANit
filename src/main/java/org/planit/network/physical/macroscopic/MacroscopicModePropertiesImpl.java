package org.planit.network.physical.macroscopic;

import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Mode specific properties for the macroscopic perspective on the supply side,
 * i.e. on a link segment of a particular type
 * 
 * @author markr
 *
 */
public class MacroscopicModePropertiesImpl implements MacroscopicModeProperties {

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
     * Constructor
     * 
     * @param maxSpeed
     *            maximum speed for this mode
     * @param criticalSpeed
     *            critical speed for this mode
     */
    public MacroscopicModePropertiesImpl(double maxSpeed, double criticalSpeed) {
        super();
        this.maxSpeed = maxSpeed;
        this.criticalSpeed = criticalSpeed;
    }

    /**
     * Constructor adopting default values
     */
    public MacroscopicModePropertiesImpl() {
        super();
        this.maxSpeed = DEFAULT_MAXIMUM_SPEED;
        this.criticalSpeed = DEFAULT_CRITICAL_SPEED;
    }

    // Getter - setters

    @Override
	public double getMaxSpeed() {
        return maxSpeed;
    }

    @Override
	public double getCriticalSpeed() {
        return criticalSpeed;
    }

}