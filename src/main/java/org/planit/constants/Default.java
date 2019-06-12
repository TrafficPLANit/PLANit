package org.planit.constants;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Default values used across classes in PLANit
 * 
 * @author gman6028, markr
 *
 */
public class Default {
				
	/**
	 * Default coordinate reference system used
	 */
	public static CoordinateReferenceSystem COORDINATE_REFERENCE_SYSTEM;
	
	/**
	 * Epsilon margin when comparing flow rates (veh/h)
	 */
	public static final double DEFAULT_FLOW_EPSILON = 0.000001;
	
    /**
     * Epsilon margin when comparing speeds (km/h)
     */	
	public static final double DEFAULT_SPEED_EPSILON = 0.000001;
	
    static {
    	COORDINATE_REFERENCE_SYSTEM = new DefaultGeographicCRS(DefaultGeographicCRS.WGS84);
    }
}
