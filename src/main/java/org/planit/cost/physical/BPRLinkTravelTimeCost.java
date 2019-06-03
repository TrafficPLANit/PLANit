package org.planit.cost.physical;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.interactor.InteractorAccessee;
import org.planit.interactor.LinkVolumeAccessee;
import org.planit.interactor.LinkVolumeAccessor;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.userclass.Mode;

/**
 * Well known BPR link performance function to compute travel time cost on link
 * segment based on flow and configuration parameters.
 * 
 * @author markr
 */
public class BPRLinkTravelTimeCost extends PhysicalCost implements LinkVolumeAccessor {
    
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
         * @param alpha
         *            alpha value for BPR model
         * @param beta
         *            beta value for BPR model
         */
        public BPRParameters(Map<Long, Double> alpha, Map<Long, Double> beta) {
            this.alpha = alpha;
            this.beta = beta;
        }

        /**
         * Returns alpha value of the BPR model for a specified mode
         * 
         * @param modeId
         *            id of the specified mode
         * @return alpha value of BPR model for specified mode
         */
        public double getAlpha(long modeId) {
            return alpha.get(modeId);
        }
        
        public Map<Long, Double> getAlpha() {
        	return alpha;
        }

        /**
         * Returns beta value of the BPR model for a specified mode
         * 
         * @param modeId
         *            id of the specified mode
         * @return beta value of the BPR value for the specified mode
         */
        public double getBeta(long modeId) {
            return beta.get(modeId);
        }
        
        public Map<Long, Double> getBeta() {
        	return beta;
        }

    }    

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(BPRLinkTravelTimeCost.class.getName());

    /**
     * Link volume accessee object for this cost function
     */
    protected LinkVolumeAccessee linkVolumeAccessee = null;

    /**
     * LinkSegmentVolumes that are provided through interactor
     */
    protected double[] linkSegmentVolumes = null;

    /**
     * Number of link segments we collect volumes and therefore costs for, obtained
     * via interactor
     */
    protected int numberOfLinkSegments = -1;

    /**
     * BPR parameters for all link segments
     */
    protected BPRParameters[] bprEdgeSegmentParameters = null;
    
    /**
     * Map to store alpha values for each link type and mode
     */
    protected Map<Integer, Map<Long, Double>> alphaMapMap;
    
    /**
     * Map to store beta values for each link type and mode
     */
    protected Map<Integer, Map<Long, Double>> betaMapMap;

    /**
     * Constructor
     */
    public BPRLinkTravelTimeCost() {
        super();
    	alphaMapMap = new HashMap<Integer, Map<Long, Double>>();
    	betaMapMap = new HashMap<Integer, Map<Long, Double>>();

    }

    /**
     * Calculate the travel time for the current link for a given mode
     * 
     * @param mode
     *            the current Mode of travel
     * @param linkSegment
     *            the current link segment
     * @return the travel time for the current link
     * @throws PlanItException
     *             thrown if there is an error
     *
     */
    public double calculateSegmentCost(Mode mode, LinkSegment linkSegment) throws PlanItException {
        double flow = linkVolumeAccessee.getTotalNetworkSegmentFlow(linkSegment);
        BPRParameters parameters = bprEdgeSegmentParameters[(int) linkSegment.getId()];

        // BPR function with mode specific free flow time and general PCU based delay
        MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) linkSegment;
        double freeFlowTravelTime = macroscopicLinkSegment.computeFreeFlowTravelTime(mode);
        long modeId = mode.getId();
        double capacity = macroscopicLinkSegment.computeCapacity();
        double alpha = parameters.getAlpha(modeId);
        double beta = parameters.getBeta(modeId);
        double linkTravelTime = freeFlowTravelTime * (1.0 + alpha * Math.pow(flow / capacity, beta)); // Free Flow
                                                                                                      // Travel Time *
                                                                                                      // (1 +
                                                                                                      // alpha*(v/c)^beta)
        return linkTravelTime;
    }

    /**
     * BPR function is PCU based, hence we only have a single function per link
     * segment. The BPR function does not change when moving to a different time
     * period.
     * 
     * @param bprLinkSegmentParameters,
     *            should be the size of the number of link segments in the network
     *            identified by the segment id
     */
    public void populate(BPRParameters[] bprLinkSegmentParameters) {
        // TODO: not invoked yet, population should go via event dispatch when the
        // object is created, the input should populate it
        this.bprEdgeSegmentParameters = bprLinkSegmentParameters;
    }
    
/**
 * Set the alpha value for a given link type and mode
 * 
 * @param linkType       reference to the specified link type
 * @param modeType   reference to the specified mode type
 * @param value           alpha value
 */
    public void setAlpha(int linkType, int modeType, double value) {
    	setParameter(alphaMapMap, linkType, modeType, value);
    }

/**
 * Set the beta value for a given link type and mode
 * 
 * @param linkType        reference to the specified link type
 * @param modeType    reference to the specified mode type
 * @param value            beta value
 */
    public void setBeta(int linkType, int modeType, double value) {
    	setParameter(betaMapMap, linkType, modeType, value);
    }
    
/**
 * Sets the alpha value for all link types for a specified mode type
 * 
 * @param noLinkSegmentTypes     number of link types
 * @param modeType                      reference to the specified mode type
 * @param value                              alpha value
 */
    public void setAlphaForAllLinkTypes(int noLinkTypes, int modeType, double value) {
    	for (int linkType=1; linkType  <= noLinkTypes; linkType++) {
    		setAlpha(linkType, modeType, value);
    	}
    }
    
/**
 * Sets the beta value for all link types for a specified mode type
 * 
 * @param noLinkTypes    number of link types
 * @param modeType       reference to the specified mode type
 * @param value               beta value
 */
    public void setBetaForAllLinkTypes(int noLinkTypes, int modeType, double value) {
    	for (int linkType=1; linkType <= noLinkTypes; linkType++) {
    		setBeta(linkType, modeType, value);
    	}
    }
    
/**
 * Stores a BPR parameter for a specified link type and mode type
 * 
 * @param mapMap       Map to store parameter values
 * @param linkType         reference to specified link type
 * @param modeType     reference to specified mode type
 * @param value             parameter value
 */
    private void setParameter(Map<Integer, Map<Long, Double>> mapMap, int linkType, int modeType, double value) {
    	if (mapMap.get(linkType) == null) {
    		mapMap.put(linkType, new HashMap<Long, Double>());
    	}
    	mapMap.get(linkType).put((long) modeType, value);
    }
    
/**
 * Set a BPR parameter to have value zero for a specified link type and mode type
 * 
 * @param mapMap     Map to store parameter values 
 * @param linkType      reference to specified link type
 * @param modeType   reference to specified mode type
 */
    private void setZeroParameter(Map<Integer, Map<Long, Double>> mapMap, int linkType, int modeType) {
		   if (mapMap.get(linkType).get((long) modeType) == null) {
			   setParameter(mapMap, linkType, modeType, 0.0);
		   }
    }

/**
 * Update the PhysicalNetwork with the alpha and beta parameter values
 * 
 * Call this method after all the calls to methods to set alpha and beta values have been made
 * 
 * @param physicalNetwork     PhysicalNetwork to be updated
 */
   public void update(PhysicalNetwork physicalNetwork) {
	   for (int linkType=1; (linkType + 1) < physicalNetwork.getNoLinkSegmentTypes(); linkType++) {
		   for (int modeType=1; (modeType + 1) <physicalNetwork.getNoModes(); modeType++) {
			   setZeroParameter(alphaMapMap, linkType, modeType);
			   setZeroParameter(betaMapMap, linkType, modeType);
		   }
	   }
        List<LinkSegment> linkSegments = physicalNetwork.linkSegments.toList();
        BPRLinkTravelTimeCost.BPRParameters[] bprLinkSegmentParameters = new BPRLinkTravelTimeCost.BPRParameters[linkSegments.size()];
	    for (LinkSegment linkSegment : linkSegments) {
	    	MacroscopicLinkSegment macroscopiclinkSegment = (MacroscopicLinkSegment) linkSegment;
	    	Map<Long, Double> alphaMap = alphaMapMap.get(macroscopiclinkSegment.getLinkSegmentType().getLinkType());
	    	Map<Long, Double> betaMap = betaMapMap.get(macroscopiclinkSegment.getLinkSegmentType().getLinkType());
	        bprLinkSegmentParameters[(int) linkSegment.getId()] = new BPRLinkTravelTimeCost.BPRParameters(alphaMap, betaMap);
        }
	    populate(bprLinkSegmentParameters);
    }
    
    /**
     * Set Accessee object for this LinkVolumeAccessor
     * 
     * @param accessee
     *            Accessee object for this LinkVolumeAccessor
     */
    @Override
    public void setAccessee(InteractorAccessee accessee) {
        if (!(accessee instanceof LinkVolumeAccessee)) {
            // TODO:
        }
        this.linkVolumeAccessee = (LinkVolumeAccessee) accessee;
    }

    /**
     * Returns the alpha value for a given link segment for the specified mode
     * 
     * @param mode
     *            the specified mode
     * @param linkSegment
     *            the specified link segment
     * @return the alpha value for this link segment for the specified mode
     */
    public double getAlpha(Mode mode, LinkSegment linkSegment) {
        return bprEdgeSegmentParameters[(int) linkSegment.getId()].getAlpha(mode.getId());
    }

    /**
     * Returns the beta value for a given link segment for the specified mode
     * 
     * @param mode
     *            the specified mode
     * @param linkSegment
     *            the specified link segment
     * @return the beta value for this link segment for the specified mode
     */
    public double getBeta(Mode mode, LinkSegment linkSegment) {
        return bprEdgeSegmentParameters[(int) linkSegment.getId()].getBeta(mode.getId());
    }
    
/**
 * Creates and returns an array of BPRParameters objects from Maps of alpha and beta values
 * 
 * @param iterator                                Iterator through all LinkSegment objects
 * @param numberOfLinkSegments      number of LinkSegment object
 * @param alphaMapMap                     Map of alpha values for each LinkSegment and Mode
 * @param betaMapMap                       Map of beta values for each LinkSegment and Mode
 * @return                                              array of BPRParameters objects
 */
    public static BPRLinkTravelTimeCost.BPRParameters[] getBPRParameters(Iterator<LinkSegment> iterator, 
    		                                                                                                                    int numberOfLinkSegments,   
    		                                                                                                                    Map<MacroscopicLinkSegment, Map<Long, Double>> alphaMapMap, 
    		                                                                                                                    Map<MacroscopicLinkSegment, Map<Long, Double>> betaMapMap) {
        BPRLinkTravelTimeCost.BPRParameters[] bprLinkSegmentParameters = new BPRLinkTravelTimeCost.BPRParameters[numberOfLinkSegments];
        while (iterator.hasNext()) {
        	LinkSegment linkSegment = iterator.next();
            Map<Long, Double> alphaMap = alphaMapMap.get(linkSegment);
            Map<Long, Double> betaMap = betaMapMap.get(linkSegment);
            bprLinkSegmentParameters[(int) linkSegment.getId()] = new BPRLinkTravelTimeCost.BPRParameters(alphaMap, betaMap);
        }      
        return bprLinkSegmentParameters;
    }

}
