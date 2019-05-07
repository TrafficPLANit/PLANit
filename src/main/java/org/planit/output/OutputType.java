package org.planit.output;

/**
 * Different configurations exist for different types of output which we identify via this enum
 * LINK:        link based output
 * SIMULATION:  simulation based output such as profile information, objects created etc.
 * OD:          origin-destination based output regarding travel times and other costs
 * 
 * @author markr
 */
public enum OutputType {
        GENERAL,
        LINK,
        SIMULATION,
        OD
}