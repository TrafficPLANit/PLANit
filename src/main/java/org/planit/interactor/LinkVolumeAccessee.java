package org.planit.interactor;

import org.planit.userclass.Mode;

/**
 * Link Volume accessee object
 * 
 * @author gman6028
 *
 */
public interface LinkVolumeAccessee extends InteractorAccessee {
    
/**
 * Get link segment flows for all modes
 * 
 * @return       array storing link segment flows
 */
    double[] getTotalNetworkSegmentFlows();
    
 /**
  * Get link segment flows for a specified mode
  * 
  * @param mode             mode
  * @return                       link segment flows for all modes
  */
    double[] getModalNetworkSegmentFlows(Mode mode);
    
/**
 * Get number of link segments
 * 
 * @return       number of link segments
 */
    int getNumberOfLinkSegments();
        
}