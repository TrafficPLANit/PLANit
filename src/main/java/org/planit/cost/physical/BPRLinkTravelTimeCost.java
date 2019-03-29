package org.planit.cost.physical;

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
		
	protected LinkVolumeAccessee linkVolumeAccessee = null;
	
	/**
	 * linkSegmentVolumes that are provided through interactor
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
		protected final double alpha;
		
		/**
		 * Beta parameter in BPR function
		 */		
		protected final double beta;
				
	/** 
	 * Constructor which injects BPR model parameters
	 * 
	 * @param alpha				alpha value for BPR model
	 * @param beta				beta value for BPR model
	 */
		public BPRParameters(double alpha, double beta) {
			this.alpha = alpha;
			this.beta = beta;
		}
			
	/**
	 * Returns alpha value for BPR model
	 * 
	 * @return		alpha value for BPR model
	 */
		public double getAlpha() {
			return alpha;
		}
	
	/**
	 * Returns beta value for BPR model
	 * 
	 * @return		beta value for BPR value
	 */
		public double getBeta() {
			return beta;
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
 *  @return the travel time for the current link
 * @throws PlanItException 
 *
 */
	public double calculateSegmentCost(Mode mode, LinkSegment linkSegment) throws PlanItException {
		double flow = linkVolumeAccessee.getLinkSegmentFlows()[(int)linkSegment.getId()];
		BPRParameters parameters = bprEdgeSegmentParameters[(int) linkSegment.getId()];
	
		// BPR function with mode specific free flow time and general pcu based delay
		MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) linkSegment;
		double freeFlowTravelTime = macroscopicLinkSegment.computeFreeFlowTravelTime(mode);
		double capacity = macroscopicLinkSegment.computeCapacity();
		double alpha = parameters.getAlpha();
		double beta = parameters.getBeta();
		double linkTravelTime = freeFlowTravelTime * (1.0 + alpha * Math.pow(flow/capacity, beta));   //Free Flow Travel Time * (1 + alpha*(v/c)^beta)
		return linkTravelTime;
	}
	
/** 
 * BPR function is pcu based, hence we only have a single function per link segment. The BPR function does not change when moving to a different time period. 
 * 
 * @param bprLinkSegmentParameters, should be the size of the number of link segments in the network identified by the segment id
 */
	public void populate(BPRParameters[] bprLinkSegmentParameters) {
//TODO: not invoked yet, population should go via event dispatch when the object is created, the input should populate it
		this.bprEdgeSegmentParameters = bprLinkSegmentParameters;
	}
	
/**
 * Set accessee
 * 
 * @param accessee		
 */

	@Override
	public void setAccessee(InteractorAccessee accessee) {
		if(!(accessee instanceof LinkVolumeAccessee)) {
			//TODO:
		}
		this.linkVolumeAccessee = (LinkVolumeAccessee) accessee;		
	}
	
	public double getAlpha(LinkSegment linkSegment) {
		return bprEdgeSegmentParameters[(int) linkSegment.getId()].getAlpha();
	}
	
	public double getBeta(LinkSegment linkSegment) {
		return bprEdgeSegmentParameters[(int) linkSegment.getId()].getBeta();
	}
}
