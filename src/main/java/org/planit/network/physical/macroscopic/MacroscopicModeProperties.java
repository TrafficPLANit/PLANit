package org.planit.network.physical.macroscopic;

/**
 * Mode specific properties for the macroscopic perspective on the supply side,
 * i.e. on a link segment of a particular type
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
     * Default max speed in km/h
     */
    public static final double DEFAULT_MAXIMUM_SPEED = 80;
    
    /**
     * Default critical speed, i.e. speed at capacity in km/h
     */
    public static final double DEFAULT_CRITICAL_SPEED = 60;

    /**
     * Epsilon margin when comparing speeds (km/h)
     */	
	public static final double DEFAULT_SPEED_EPSILON = 0.000001;
	
    /**
     * Constructor
     * 
     * @param maxSpeed
     *            maximum speed for this mode
     * @param criticalSpeed
     *            critical speed for this mode
     */
    public MacroscopicModeProperties(double maxSpeed, double criticalSpeed) {
        super();
        this.maxSpeed = maxSpeed;
        this.criticalSpeed = criticalSpeed;
    }

    /**
     * Constructor adopting default values
     */
    public MacroscopicModeProperties() {
        super();
        this.maxSpeed = DEFAULT_MAXIMUM_SPEED;
        this.criticalSpeed = DEFAULT_CRITICAL_SPEED;
    }

    // Getter - setters

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public double getCriticalSpeed() {
        return criticalSpeed;
    }

}