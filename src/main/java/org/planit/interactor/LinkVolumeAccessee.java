package org.planit.interactor;

import org.planit.network.physical.LinkSegment;
import org.planit.userclass.Mode;

/**
 * Link Volume accessee object
 * 
 * @author markr
 *
 */
public interface LinkVolumeAccessee extends InteractorAccessee {

    /**
     * Get the total flow across a link over all modes
     * 
     * @param linkSegment
     *            the specified link segment
     * @return the total flow across this link segment
     */
    public double getTotalNetworkSegmentFlow(LinkSegment linkSegment);

    /**
     * Get link segment flows for a specified mode
     * 
     * @param mode
     *            mode
     * @return link segment flows for all modes
     */
    public double[] getModalNetworkSegmentFlows(Mode mode);

    /**
     * Get number of link segments
     * 
     * @return number of link segments
     */
    int getNumberOfLinkSegments();

}