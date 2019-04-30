package org.planit.interactor;

/**
 * Link Volume accessee object
 * 
 * @author gman6028
 *
 */
public interface LinkVolumeAccessee extends InteractorAccessee {
	
/**
 * Get link segment flows
 * 
 * @return       array storing link segment flows
 */
	double[] getTotalNetworkSegmentFlows();
	
/**
 * Get number of link segments
 * 
 * @return       number of link segments
 */
	int getNumberOfLinkSegments();
		
}
