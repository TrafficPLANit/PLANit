package org.planit.cost.physical;

import java.util.HashMap;
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
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.userclass.Mode;
import org.planit.utils.Pair;

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
		 * @param alpha alpha value for BPR model
		 * @param beta  beta value for BPR model
		 */
		public BPRParameters(Map<Long, Double> alpha, Map<Long, Double> beta) {
			this.alpha = alpha;
			this.beta = beta;
		}

		/**
		 * Returns alpha value of the BPR model for a specified mode
		 * 
		 * @param modeId id of the specified mode
		 * @return alpha value of BPR model for specified mode
		 */
		private double getAlpha(long modeId) {
			return alpha.get(modeId);
		}

		/**
		 * Returns beta value of the BPR model for a specified mode
		 * 
		 * @param modeId id of the specified mode
		 * @return beta value of the BPR value for the specified mode
		 */
		private double getBeta(long modeId) {
			return beta.get(modeId);
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
	 * Map to store alpha and beta values for each link type and mode
	 */
	protected Map<Integer, Map<Long, Pair<Double, Double>>> parametersMapMap;

	/**
	 * Constructor
	 */
	public BPRLinkTravelTimeCost() {
		super();
		parametersMapMap = new HashMap<Integer, Map<Long, Pair<Double, Double>>>();
	}

	/**
	 * Calculate the travel time for the current link for a given mode
	 * 
	 * @param mode        the current Mode of travel
	 * @param linkSegment the current link segment
	 * @return the travel time for the current link
	 * @throws PlanItException thrown if there is an error
	 *
	 */
	public double calculateSegmentCost(Mode mode, LinkSegment linkSegment) throws PlanItException {
		double flow = linkVolumeAccessee.getTotalNetworkSegmentFlow(linkSegment);
		BPRParameters parameters = bprEdgeSegmentParameters[(int) linkSegment.getId()];

		// BPR function with mode specific free flow time and general PCU based delay
		MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) linkSegment;
		double freeFlowTravelTime = macroscopicLinkSegment.computeFreeFlowTravelTime(mode);
		long modeExternalId = mode.getExternalId();
		double capacity = macroscopicLinkSegment.computeCapacity();
		double alpha = parameters.getAlpha(modeExternalId);
		double beta = parameters.getBeta(modeExternalId);
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
	 * @param bprLinkSegmentParameters, should be the size of the number of link
	 *        segments in the network identified by the segment id
	 */
	public void populate(BPRParameters[] bprLinkSegmentParameters) {
		// TODO: not invoked yet, population should go via event dispatch when the
		// object is created, the input should populate it
		this.bprEdgeSegmentParameters = bprLinkSegmentParameters;
	}

	/**
	 * Set the alpha and beta values for a given link type and mode
	 * 
	 * @param linkTypeExternalId reference to the specified link type
	 * @param modeExternalId     reference to the specified mode type
	 * @param alpha              alpha value
	 * @param beta               beta value
	 */
	public void setParameters(int linkTypeExternalId, long modeExternalId, double alpha, double beta) {
		Pair<Double, Double> pair = new Pair<Double, Double>(alpha, beta);
		setParameterPair(linkTypeExternalId, modeExternalId, pair);
	}

	/**
	 * Set the alpha and beta values for a given link type and mode
	 * 
	 * @param linkSegmentType the specified link type
	 * @param mode            the specified mode type
	 * @param alpha           alpha value
	 * @param beta            beta value
	 */
	public void setParameters(MacroscopicLinkSegmentType linkSegmentType, Mode mode, double alpha, double beta) {
		setParameters(linkSegmentType.getLinkTypeExternalId(), mode.getExternalId(), alpha, beta);
	}

	/**
	 * Set the alpha and beta values for all link types for a specified mode type
	 * 
	 * @param physicalNetwork the physical network
	 * @param modeExternalId  external Id of the specified mode type
	 * @param value           alpha value
	 * @param beta            beta value
	 */
	public void setDefaultParameters(PhysicalNetwork physicalNetwork, long modeExternalId, double alpha, double beta) {
		MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) physicalNetwork;
		for (Integer linkTypeExternalId : macroscopicNetwork.getLinkSegmentExternalIdSet()) {
			setParameters(linkTypeExternalId, modeExternalId, alpha, beta);
		}
	}

	/**
	 * Set the alpha and beta values for all link types for a specified mode type
	 * 
	 * @param physicalNetwork the physical network
	 * @param mode            the specified mode type
	 * @param value           alpha value
	 * @param beta            beta value
	 */
	public void setDefaultParameters(PhysicalNetwork physicalNetwork, Mode mode, double alpha, double beta) {
		setDefaultParameters(physicalNetwork, mode.getExternalId(), alpha, beta);
	}

	/**
	 * Set the alpha and beta values for all link types and mode types
	 * 
	 * @param physicalNetwork the physical network
	 * @param alpha           alpha value
	 * @param beta            beta value
	 */
	public void setDefaultParameters(PhysicalNetwork physicalNetwork, double alpha, double beta) {
		for (Long modeExternalId : Mode.getExternalIdSet()) {
			setDefaultParameters(physicalNetwork, modeExternalId, alpha, beta);
		}
	}

	/**
	 * Stores a BPR parameters for a specified link type and mode type
	 * 
	 * @param linkTypeExternalId reference to specified link type
	 * @param modeExternalId     reference to specified mode type
	 * @param value              parameter value
	 */
	private void setParameterPair(int linkTypeExternalId, long modeExternalId, Pair<Double, Double> pair) {
		if (parametersMapMap.get(linkTypeExternalId) == null) {
			parametersMapMap.put(linkTypeExternalId, new HashMap<Long, Pair<Double, Double>>());
		}
		parametersMapMap.get(linkTypeExternalId).put((long) modeExternalId, pair);
	}

	/**
	 * Set BPR parameters to have zero values for a specified link type and mode
	 * type if the user has not already set a value
	 * 
	 * @param linkTypeExternalId reference to specified link type
	 * @param modeExternalId     reference to specified mode type
	 */
	private void setZeroParameterPair(int linkTypeExternalId, long modeExternalId) {
		if (parametersMapMap.get(linkTypeExternalId).get((long) modeExternalId) == null) {
			Pair<Double, Double> zeroPair = new Pair<Double, Double>(0.0, 0.0);
			setParameterPair(linkTypeExternalId, modeExternalId, zeroPair);
		}
	}

	/**
	 * Set Accessee object for this LinkVolumeAccessor
	 * 
	 * @param accessee Accessee object for this LinkVolumeAccessor
	 */
	@Override
	public void setAccessee(InteractorAccessee accessee) {
		if (!(accessee instanceof LinkVolumeAccessee)) {
			// TODO:
		}
		this.linkVolumeAccessee = (LinkVolumeAccessee) accessee;
	}

	/**
	 * Register the BPR cost parameter values on the PhysicalNetwork
	 * 
	 * Call this method after all the calls to set the cost parameters have been
	 * made
	 * 
	 * @param physicalNetwork PhysicalNetwork object containing the updated
	 *                        parameter values
	 */
	@Override
	public void updateCostParameters(PhysicalNetwork physicalNetwork) {
		MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) physicalNetwork;
		for (Integer linkTypeExternalId : macroscopicNetwork.getLinkSegmentExternalIdSet()) {
			for (int modeExternalId = 1; (modeExternalId + 1) < Mode.getAllModes().size(); modeExternalId++) {
				setZeroParameterPair(linkTypeExternalId, modeExternalId);
			}
		}
		List<LinkSegment> linkSegments = macroscopicNetwork.linkSegments.toList();
		BPRLinkTravelTimeCost.BPRParameters[] bprLinkSegmentParameters = new BPRLinkTravelTimeCost.BPRParameters[linkSegments
				.size()];
		for (LinkSegment linkSegment : linkSegments) {
			MacroscopicLinkSegment macroscopiclinkSegment = (MacroscopicLinkSegment) linkSegment;
			Map<Long, Double> alphaMap = new HashMap<Long, Double>();
			Map<Long, Double> betaMap = new HashMap<Long, Double>();
			for (Long modeExternalId : Mode.getExternalIdSet()) {
				double alpha = parametersMapMap.get(macroscopiclinkSegment.getLinkSegmentType().getLinkTypeExternalId())
						.get(modeExternalId).getFirst();
				double beta = parametersMapMap.get(macroscopiclinkSegment.getLinkSegmentType().getLinkTypeExternalId())
						.get(modeExternalId).getSecond();
				alphaMap.put(modeExternalId, alpha);
				betaMap.put(modeExternalId, beta);
			}
			bprLinkSegmentParameters[(int) linkSegment.getId()] = new BPRLinkTravelTimeCost.BPRParameters(alphaMap,
					betaMap);
		}
		populate(bprLinkSegmentParameters);
	}

}
