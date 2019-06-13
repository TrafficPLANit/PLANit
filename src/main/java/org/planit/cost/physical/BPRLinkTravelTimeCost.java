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
	// public static class BPRParameters {

	/**
	 * Alpha and Beta parameters in BPR function
	 */
	// protected Map<Mode, Pair<Double, Double>> parametersMap;

	/**
	 * Constructor which injects BPR model parameters
	 * 
	 * @param parametersMap alpha and beta values for BPR model
	 */
	// public BPRParameters(Map<Mode, Pair<Double, Double>> parametersMap) {
	// this.parametersMap = parametersMap;
	// }
	/**
	 * Returns an iterator through the modes which have BPR parameters set
	 * 
	 * @return iterator through Mode objects which have BPR parameters set
	 */
	// public Iterator<Mode> getModeIterator() {
	// return parametersMap.keySet().iterator();
	// }

	/**
	 * Return the alpha value for a specified mode
	 * 
	 * @param mode mode to be analyzed
	 * @return alpha value for this mode
	 */
	// public double getAlpha(Mode mode) {
	// return parametersMap.get(mode).getFirst();
	// }

	/**
	 * Return the beta value for a specified mode
	 * 
	 * @param mode mode to be analyzed
	 * @return beta value for this mode
	 */
	// public double getBeta(Mode mode) {
	// return parametersMap.get(mode).getSecond();
	// }

	// }

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(BPRLinkTravelTimeCost.class.getName());

	/**
	 * Default alpha BPR parameter if not other information is available
	 */
	public static final double DEFAULT_ALPHA = 0.5;

	/**
	 * Default beta BPR parameter if not other information is available
	 */
	public static final double DEFAULT_BETA = 4.0;

	/**
	 * Link volume accessee object for this cost function
	 */
	protected LinkVolumeAccessee linkVolumeAccessee = null;

	/**
	 * LinkSegmentVolumes that are provided through interactor
	 */
	//protected double[] linkSegmentVolumes = null;

	/**
	 * Number of link segments we collect volumes and therefore costs for, obtained
	 * via interactor
	 */
	//protected int numberOfLinkSegments = -1;

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
	 * Constructor
	 */
	public BPRLinkTravelTimeCost() {
		super();
		defaultParametersPerMode = new HashMap<Mode, Pair<Double, Double>>();
		defaultParametersPerLinkSegmentTypeAndMode = new HashMap<MacroscopicLinkSegmentType, Map<Mode, Pair<Double, Double>>>();
		defaultParameters = new Pair<Double, Double>(DEFAULT_ALPHA, DEFAULT_BETA);
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
	@Override
	public double calculateSegmentCost(Mode mode, LinkSegment linkSegment) throws PlanItException {
		double flow = linkVolumeAccessee.getTotalNetworkSegmentFlow(linkSegment);

		// BPR function with mode specific free flow time and general PCU based delay
		MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) linkSegment;
		double freeFlowTravelTime = macroscopicLinkSegment.computeFreeFlowTravelTime(mode);
		double capacity = macroscopicLinkSegment.computeCapacity();
		Pair<Double, Double> bprParameters = macroscopicLinkSegment.getBprParameters(mode);
		double alpha = bprParameters.getFirst();
		double beta = bprParameters.getSecond();
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
	public void setParameters(MacroscopicLinkSegment linkSegment, Mode mode, double alpha, double beta) {
		linkSegment.registerBprParameters(mode, alpha, beta);
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
	public void initialiseBeforeEquilibration(PhysicalNetwork physicalNetwork) throws PlanItException {
		MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) physicalNetwork;
		Iterator<LinkSegment> linkSegmentIterator = macroscopicNetwork.linkSegments.iterator();
		while (linkSegmentIterator.hasNext()) {
			MacroscopicLinkSegment macroscopiclinkSegment = (MacroscopicLinkSegment) linkSegmentIterator.next();
			for (Mode mode : Mode.getAllModes()) {
				if (macroscopiclinkSegment.getBprParameters(mode) == null) {
					MacroscopicLinkSegmentType macroscopicLinkSegmentType = macroscopiclinkSegment.getLinkSegmentType();
					if ((defaultParametersPerLinkSegmentTypeAndMode.get(macroscopicLinkSegmentType) != null)
							&& (defaultParametersPerLinkSegmentTypeAndMode.get(macroscopicLinkSegmentType)
									.get(mode) != null)) {
						macroscopiclinkSegment.registerBprParameters(mode, defaultParametersPerLinkSegmentTypeAndMode.get(macroscopicLinkSegmentType).get(mode));
					} else if (defaultParametersPerMode.get(mode) != null) {
						macroscopiclinkSegment.registerBprParameters(mode, defaultParametersPerMode.get(mode));
					} else {
						macroscopiclinkSegment.registerBprParameters(mode, defaultParameters);
					}
				}
			}
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