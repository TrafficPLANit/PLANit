package org.planit.interactor;

import org.djutils.event.EventType;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;

/**
 * Link Volume accessee object.
 * 
 * @author markr
 *
 */
public interface LinkVolumeAccessee extends InteractorAccessee {
	
	/** event type fired off when this accessee is identified as a suitable candidate for an accessee request */
	public static final EventType INTERACTOR_PROVIDE_LINKVOLUMEACCESSEE = new EventType("INTERACTOR.REQUEST.LINKVOLUMEACCESSEE.TYPE");
	
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