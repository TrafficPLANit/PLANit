package org.planit.cost.physical;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.interactor.InteractorAccessee;
import org.planit.interactor.LinkVolumeAccessee;
import org.planit.interactor.LinkVolumeAccessor;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.PhysicalNetwork.LinkSegments;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
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
		 * @param modeExternalId id of the specified mode
		 * @return alpha value of BPR model for specified mode
		 */
		private double getAlpha(long modeExternalId) {
			return alpha.get(modeExternalId);
		}

		/**
		 * Returns beta value of the BPR model for a specified mode
		 * 
		 * @param modeExternalId id of the specified mode
		 * @return beta value of the BPR value for the specified mode
		 */
		private double getBeta(long modeExternalId) {
			return beta.get(modeExternalId);
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
	 * Default alpha and beta values for all links
	 */
	protected Pair<Double, Double> defaultParameters;

	/**
	 * Map to store default alpha and beta values for each mode
	 */
	protected Map<Long, Pair<Double, Double>> defaultParametersPerMode;

	/**
	 * Map to store default alpha and beta values for each link type and mode
	 */
	protected Map<Long, Map<Long, Pair<Double, Double>>> defaultParametersPerLinkSegmentTypeAndMode;

	/**
	 * Map to store default alpha and beta values for a specific link each link/mode
	 * combination
	 */
	protected Map<Long, Map<Long, Pair<Double, Double>>> parametersPerLinkAndMode;

	/**
	 * Constructor
	 */
	public BPRLinkTravelTimeCost() {
		super();
		defaultParametersPerMode = new HashMap<Long, Pair<Double, Double>>();
		defaultParametersPerLinkSegmentTypeAndMode = new HashMap<Long, Map<Long, Pair<Double, Double>>>();
		parametersPerLinkAndMode = new HashMap<Long, Map<Long, Pair<Double, Double>>>();
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
	 * Set the alpha and beta values for a given link segment and mode
	 * 
	 * @param linkSegmentId  reference to the specified link segment
	 * @param modeExternalId reference to the specified mode type
	 * @param alpha          alpha value
	 * @param beta           beta value
	 */
	public void setParameters(long linkSegmentId, long modeExternalId, double alpha, double beta) {
		Pair<Double, Double> pair = new Pair<Double, Double>(alpha, beta);
		if (parametersPerLinkAndMode.get(linkSegmentId) == null) {
			parametersPerLinkAndMode.put(linkSegmentId, new HashMap<Long, Pair<Double, Double>>());
		}
		parametersPerLinkAndMode.get(linkSegmentId).put(modeExternalId, pair);
	}

	/**
	 * Set the default alpha and beta values for a mode
	 * 
	 * @param modeExternalId reference to the specified mode type
	 * @param alpha          alpha value
	 * @param beta           beta value
	 */
	// public void setDefaultParametersPerMode(long modeExternalId, double alpha,
	// double beta) {
	public void setDefaultParameters(long modeExternalId, double alpha, double beta) {
		defaultParametersPerMode.put(modeExternalId, new Pair<Double, Double>(alpha, beta));
	}

	/**
	 * Set the default alpha and beta values for a given link type and mode
	 * 
	 * @param linkTypeExternalId reference to the specified link type
	 * @param modeExternalId     reference to the specified mode type
	 * @param alpha              alpha value
	 * @param beta               beta value
	 */
	public void setDefaultParameters(long linkTypeExternalId, long modeExternalId, double alpha, double beta) {
		Pair<Double, Double> pair = new Pair<Double, Double>(alpha, beta);
		if (defaultParametersPerLinkSegmentTypeAndMode.get(linkTypeExternalId) == null) {
			defaultParametersPerLinkSegmentTypeAndMode.put(linkTypeExternalId,
					new HashMap<Long, Pair<Double, Double>>());
		}
		defaultParametersPerLinkSegmentTypeAndMode.get(linkTypeExternalId).put(modeExternalId, pair);
	}

	/**
	 * Set the default alpha and beta values
	 * 
	 * @param alpha alpha value
	 * @param beta  beta value
	 */
	public void setDefaultParameters(double alpha, double beta) {
		defaultParameters = new Pair<Double, Double>(alpha, beta);
	}

	/**
	 * Register the BPR cost parameter values on the PhysicalNetwork
	 * 
	 * Call this method after all the calls to set the cost parameters have been
	 * made
	 * 
	 * @param physicalNetwork PhysicalNetwork object containing the updated
	 *                        parameter values
	 * @throws PlanItException thrown if a link/mode combination exists for which no
	 *                         cost parameters have been set
	 */
	@Override
	public void updateCostParameters(PhysicalNetwork physicalNetwork) throws PlanItException {
		MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) physicalNetwork;
		LinkSegments linkSegments = macroscopicNetwork.linkSegments;
		this.bprEdgeSegmentParameters = new BPRLinkTravelTimeCost.BPRParameters[linkSegments.getNumberOfLinkSegments()];
		Iterator<LinkSegment> linkSegmentIterator = linkSegments.iterator();
		while(linkSegmentIterator.hasNext()) {
		    MacroscopicLinkSegment macroscopiclinkSegment = (MacroscopicLinkSegment) linkSegmentIterator.next();
			long linkSegmentId = macroscopiclinkSegment.getLinkSegmentId();
			long linkTypeExternalId = macroscopiclinkSegment.getLinkSegmentType().getLinkTypeExternalId();

			Map<Long, Double> alphaMap = new HashMap<Long, Double>();
			Map<Long, Double> betaMap = new HashMap<Long, Double>();

			for (Mode mode : Mode.getAllModes()) {
			    long modeExternalId = mode.getExternalId();
			    //TODO: Link specific check is missing!
			    //TODO: all of this should be done by the object not the externalId!
				if ((parametersPerLinkAndMode.get(linkSegmentId) != null)
						&& (parametersPerLinkAndMode.get(linkSegmentId).get(modeExternalId) != null)) {
					Pair<Double, Double> pair = parametersPerLinkAndMode.get(linkSegmentId).get(mode.getExternalId());
					alphaMap.put(modeExternalId, pair.getFirst());
					betaMap.put(modeExternalId, pair.getSecond());
				} else if ((defaultParametersPerLinkSegmentTypeAndMode.get(linkTypeExternalId) != null)
						&& (defaultParametersPerLinkSegmentTypeAndMode.get(linkTypeExternalId)
								.get(modeExternalId) != null)) {
					Pair<Double, Double> pair = defaultParametersPerLinkSegmentTypeAndMode.get(linkTypeExternalId)
							.get(modeExternalId);
					alphaMap.put(modeExternalId, pair.getFirst());
					betaMap.put(modeExternalId, pair.getSecond());
				} else if (defaultParametersPerMode.get(modeExternalId) != null) {
					Pair<Double, Double> pair = defaultParametersPerMode.get(modeExternalId);
					alphaMap.put(modeExternalId, pair.getFirst());
					betaMap.put(modeExternalId, pair.getSecond());
				} else if (defaultParameters != null) {
					alphaMap.put(modeExternalId, defaultParameters.getFirst());
					betaMap.put(modeExternalId, defaultParameters.getSecond());
				} else {
					throw new PlanItException("Error: No alpha and beta pair could be set for link type "
							+ linkTypeExternalId + " and mode " + modeExternalId);
				}
			}
			bprEdgeSegmentParameters[(int) linkSegmentId] = new BPRLinkTravelTimeCost.BPRParameters(alphaMap,
					betaMap);
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
}
