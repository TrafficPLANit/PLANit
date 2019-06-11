package org.planit.cost.physical;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.constants.Default;
import org.planit.exceptions.PlanItException;
import org.planit.interactor.InteractorAccessee;
import org.planit.interactor.LinkVolumeAccessee;
import org.planit.interactor.LinkVolumeAccessor;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.PhysicalNetwork.LinkSegments;
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
		 * Alpha and Beta parameters in BPR function
		 */
		protected Map<Mode, Pair<Double, Double>> parametersMap;

		/**
		 * Constructor which injects BPR model parameters
		 * 
		 * @param parametersMap alpha and beta values for BPR model
		 */
		public BPRParameters(Map<Mode, Pair<Double, Double>> parametersMap) {
			this.parametersMap = parametersMap;
		}

		/**
		 * Returns alpha and beta values of the BPR model for a specified mode
		 * 
		 * @param modeExternalId id of the specified mode
		 * @return pair containing alpha and beta values of BPR model for specified mode
		 */
		private Pair<Double, Double> getParametersMap(Mode mode) {
			return parametersMap.get(mode);
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
	protected Map<Mode, Pair<Double, Double>> defaultParametersPerMode;

	/**
	 * Map to store default alpha and beta values for each link type and mode
	 */
	protected Map<MacroscopicLinkSegmentType, Map<Mode, Pair<Double, Double>>> defaultParametersPerLinkSegmentTypeAndMode;

	/**
	 * Map to store default alpha and beta values for a specific link/mode
	 * combination
	 */
	protected Map<LinkSegment, Map<Mode, Pair<Double, Double>>> parametersPerLinkAndMode;

	/**
	 * Constructor
	 */
	public BPRLinkTravelTimeCost() {
		super();
		defaultParametersPerMode = new HashMap<Mode, Pair<Double, Double>>();
		defaultParametersPerLinkSegmentTypeAndMode = new HashMap<MacroscopicLinkSegmentType, Map<Mode, Pair<Double, Double>>>();
		parametersPerLinkAndMode = new HashMap<LinkSegment, Map<Mode, Pair<Double, Double>>>();
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
		double capacity = macroscopicLinkSegment.computeCapacity();
		double alpha = parameters.getParametersMap(mode).getFirst();
		double beta = parameters.getParametersMap(mode).getSecond();
		double linkTravelTime = freeFlowTravelTime * (1.0 + alpha * Math.pow(flow / capacity, beta)); // Free Flow
																										// Travel Time *
																										// (1 +
																										// alpha*(v/c)^beta)
		return linkTravelTime;
	}

	/**
	 * Set the alpha and beta values for a given link segment and mode
	 * 
	 * @param linkSegment the specified link segment
	 * @param mode        specified mode type
	 * @param alpha       alpha value
	 * @param beta        beta value
	 */
	public void setParameters(LinkSegment linkSegment, Mode mode, double alpha, double beta) {
		Pair<Double, Double> pair = new Pair<Double, Double>(alpha, beta);
		if (parametersPerLinkAndMode.get(linkSegment) == null) {
			parametersPerLinkAndMode.put(linkSegment, new HashMap<Mode, Pair<Double, Double>>());
		}
		parametersPerLinkAndMode.get(linkSegment).put(mode, pair);
	}

	/**
	 * Set the default alpha and beta values for a mode
	 * 
	 * @param mode  the specified mode type
	 * @param alpha alpha value
	 * @param beta  beta value
	 */
	public void setDefaultParameters(Mode mode, double alpha, double beta) {
		defaultParametersPerMode.put(mode, new Pair<Double, Double>(alpha, beta));
	}

	/**
	 * Set BPR parameters directly
	 * 
	 * Use this setter if the alphaMap and betaMap have been set externally
	 * 
	 * @param bprEdgeSegmentParameters
	 */
//TODO - This method is only used by MetroScan, and we plan to refactor MetroScan so that this method will not be required
	public void setBprEdgeSegmentParameters(BPRParameters[] bprEdgeSegmentParameters) {
		this.bprEdgeSegmentParameters = bprEdgeSegmentParameters;
	}

	/**
	 * Set the default alpha and beta values for a given link type and mode
	 * 
	 * @param macroscopicLinkSegmentType the specified link type
	 * @param mode                       the specified mode type
	 * @param alpha                      alpha value
	 * @param beta                       beta value
	 */
	public void setDefaultParameters(MacroscopicLinkSegmentType macroscopicLinkSegmentType, Mode mode, double alpha,
			double beta) {
		Pair<Double, Double> pair = new Pair<Double, Double>(alpha, beta);
		if (defaultParametersPerLinkSegmentTypeAndMode.get(macroscopicLinkSegmentType) == null) {
			defaultParametersPerLinkSegmentTypeAndMode.put(macroscopicLinkSegmentType,
					new HashMap<Mode, Pair<Double, Double>>());
		}
		defaultParametersPerLinkSegmentTypeAndMode.get(macroscopicLinkSegmentType).put(mode, pair);
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
		while (linkSegmentIterator.hasNext()) {
			MacroscopicLinkSegment macroscopiclinkSegment = (MacroscopicLinkSegment) linkSegmentIterator.next();
			long linkSegmentId = macroscopiclinkSegment.getLinkSegmentId();
			MacroscopicLinkSegmentType macroscopicLinkSegmentType = macroscopiclinkSegment.getLinkSegmentType();
			Map<Mode, Pair<Double, Double>> parametersMap = new HashMap<Mode, Pair<Double, Double>>();
			for (Mode mode : Mode.getAllModes()) {
				Pair<Double, Double> pair;
				if ((parametersPerLinkAndMode.get(macroscopiclinkSegment) != null)
						&& (parametersPerLinkAndMode.get(macroscopiclinkSegment).get(mode) != null)) {
					pair = parametersPerLinkAndMode.get(macroscopiclinkSegment).get(mode);
				} else if ((defaultParametersPerLinkSegmentTypeAndMode.get(macroscopicLinkSegmentType) != null)
						&& (defaultParametersPerLinkSegmentTypeAndMode.get(macroscopicLinkSegmentType)
								.get(mode) != null)) {
					pair = defaultParametersPerLinkSegmentTypeAndMode.get(macroscopicLinkSegmentType).get(mode);
				} else if (defaultParametersPerMode.get(mode) != null) {
					pair = defaultParametersPerMode.get(mode);
				} else if (defaultParameters != null) {
					pair = defaultParameters;
				} else {
					pair = new Pair<Double, Double>(Default.ALPHA, Default.BETA);
				}
				parametersMap.put(mode, pair);
			}
			bprEdgeSegmentParameters[(int) linkSegmentId] = new BPRLinkTravelTimeCost.BPRParameters(parametersMap);
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
