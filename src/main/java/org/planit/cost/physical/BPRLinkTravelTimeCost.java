package org.planit.cost.physical;

import java.util.Map;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.interactor.InteractorAccessee;
import org.planit.interactor.LinkVolumeAccessee;
import org.planit.interactor.LinkVolumeAccessor;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.userclass.Mode;

/**
 * Well known BPR link performance function to compute travel time cost on link segment based on flow and configuration parameters.
 * 
 * @author markr
 */
public class BPRLinkTravelTimeCost extends PhysicalCost implements LinkVolumeAccessor  {
	
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(BPRLinkTravelTimeCost.class.getName());
        
/**
 *   Link volume accessee object for this cost function
 */
	protected LinkVolumeAccessee linkVolumeAccessee = null;
	
/**
 * LinkSegmentVolumes that are provided through interactor
 */
	protected double[] linkSegmentVolumes = null;
	
/**
 * Number of link segments we collect volumes and therefore costs for, obtained via interactor
 */
	protected int numberOfLinkSegments = -1;
	
/**
 * alpha and beta parameters used in BPR function
 */
	public static class BPRParameters {
		
/**
 * Alpha parameter in BPR function
 */
	    protected final Map<Long, Double> alpha;
		
/**
 * Beta parameter in BPR function
 */		
	    protected final Map<Long, Double> beta;
				
/** 
 * Constructor which injects BPR model parameters
 * 
 * @param alpha				alpha value for BPR model
 * @param beta				beta value for BPR model
 */
	    public BPRParameters(Map<Long, Double> alpha, Map<Long, Double> beta) {
			this.alpha = alpha;
			this.beta = beta;
		}
			
/**
 * Returns alpha value of the BPR model for a specified mode
 * 
 * @param modeId    id of the specified mode
 * @return		           alpha value of BPR model for specified mode
 */
	    public double getAlpha(long modeId) {
	        return alpha.get(modeId);
		}
	
/**
 * Returns beta value of the BPR model for a specified mode
 * 
 * @param modeId   id of the specified mode
 * @return		          beta value of the BPR value for the specified mode
 */
	    public double getBeta(long modeId) {
	        return beta.get(modeId);
		}		
	}
	
/**
 * BPR parameters for all link segments
 */
	protected BPRParameters[] bprEdgeSegmentParameters = null;
		
/**
 * Constructor
 */
	public BPRLinkTravelTimeCost(){
		super();
	}

/**
 * Calculate the travel time for the current link for a given mode
 *  
 *  @param mode					the current Mode of travel
 *  @param linkSegment		the current link segment
 *  @return 							the travel time for the current link
 *  @throws PlanItException 	thrown if there is an error
 *
 */	
	public double calculateSegmentCost(Mode mode, LinkSegment linkSegment) throws PlanItException {
	    double flow = linkVolumeAccessee.getTotalNetworkSegmentFlow(linkSegment);
        BPRParameters parameters = bprEdgeSegmentParameters[(int) linkSegment.getId()];	
        
		// BPR function with mode specific free flow time and general PCU based delay
		MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) linkSegment;
		double freeFlowTravelTime = macroscopicLinkSegment.computeFreeFlowTravelTime(mode);
		long modeId = mode.getId();
		double capacity = macroscopicLinkSegment.computeTotalLinkSegmentCapacity();
		double alpha = parameters.getAlpha(modeId);
		double beta = parameters.getBeta(modeId);
		double linkTravelTime = freeFlowTravelTime * (1.0 + alpha * Math.pow(flow/capacity, beta));   //Free Flow Travel Time * (1 + alpha*(v/c)^beta)
		return linkTravelTime;
	}
	
/** 
 * BPR function is PCU based, hence we only have a single function per link segment. The BPR function does not change when moving to a different time period. 
 * 
 * @param bprLinkSegmentParameters, should be the size of the number of link segments in the network identified by the segment id
 */
	public void populate(BPRParameters[] bprLinkSegmentParameters) {
//TODO: not invoked yet, population should go via event dispatch when the object is created, the input should populate it
		this.bprEdgeSegmentParameters = bprLinkSegmentParameters;
	}
	
/**
 * Set Accessee object for this LinkVolumeAccessor
 * 
 * @param accessee					Accessee object for this LinkVolumeAccessor
 */
	@Override
	public void setAccessee(InteractorAccessee accessee) {
		if (!(accessee instanceof LinkVolumeAccessee)) {
			//TODO:
		}
		this.linkVolumeAccessee = (LinkVolumeAccessee) accessee;		
	}
	
/**
 * Returns the alpha value for a given link segment for the specified mode
 * 
 * @param mode                    the specified mode
 * @param linkSegment			the specified link segment
 * @return								the alpha value for this link segment for the specified mode
 */
	public double getAlpha(Mode mode, LinkSegment linkSegment) {
	    return bprEdgeSegmentParameters[(int) linkSegment.getId()].getAlpha(mode.getId());
	}
	
/**
 * Returns the beta value for a given link segment for the specified mode
 * 
 * @param mode                    the specified mode
 * @param linkSegment			the specified link segment
 * @return								the beta value for this link segment for the specified mode
 */
	public double getBeta(Mode mode, LinkSegment linkSegment) {
	    return bprEdgeSegmentParameters[(int) linkSegment.getId()].getBeta(mode.getId());
	}
}
