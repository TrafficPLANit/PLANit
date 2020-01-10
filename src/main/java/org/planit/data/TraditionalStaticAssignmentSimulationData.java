package org.planit.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.od.odpath.ODPathMatrix;
import org.planit.output.configuration.OriginDestinationOutputTypeConfiguration;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.SubOutputTypeEnum;
import org.planit.userclass.Mode;
import org.planit.zoning.Zoning;

/**
 * Simulation data which are specific to Traditional Static Assignment
 * 
 * @author gman6028
 *
 */
public class TraditionalStaticAssignmentSimulationData extends SimulationData {

	/**
	 * segment flows for each mode
	 */
	private Map<Mode, double[]> modalNetworkSegmentFlows = null;

	/**
	 * Stores the mode specific data required during assignment
	 */
	private final Map<Mode, ModeData> modeSpecificData; // specific to tsa

	/**
	 * Stores the array of link segment costs for each mode
	 */
	private Map<Mode, double[]> modalNetworkSegmentCostsMap;

	/**
	 * Stores a skim matrix for each mode and skim output type(updated cell by cell
	 * for each iteration)
	 */
	private Map<Mode, Map<ODSkimSubOutputType, ODSkimMatrix>> modalSkimMatrixMap;
	
	/**
	 * Stores the current OD Path for each mode
	 */
	private Map<Mode, ODPathMatrix> modalODPathMatrixMap;

	/**
	 * Set of active OD skim output types
	 */
	private Set<ODSkimSubOutputType> activeOdSkimOutputTypes;

	/**
	 * Constructor
	 * 
	 * @param outputConfiguration the OutputConfiguration
	 * @throws PlanItException thrown if there is an error
	 */
	public TraditionalStaticAssignmentSimulationData(OutputConfiguration outputConfiguration) throws PlanItException {
		modalNetworkSegmentFlows = new HashMap<Mode, double[]>();
		modeSpecificData = new TreeMap<Mode, ModeData>();
		modalNetworkSegmentCostsMap = new HashMap<Mode, double[]>();
		modalSkimMatrixMap = new HashMap<Mode, Map<ODSkimSubOutputType, ODSkimMatrix>>();
		if (outputConfiguration.isOutputTypeActive(OutputType.OD)) {
			OriginDestinationOutputTypeConfiguration originDestinationOutputTypeConfiguration = (OriginDestinationOutputTypeConfiguration) outputConfiguration	.getOutputTypeConfiguration(OutputType.OD);
			// map to correct concrete subtype, so we avoid having to type cast every time we access it
			Set<SubOutputTypeEnum> topLevelSet = originDestinationOutputTypeConfiguration.getActiveSubOutputTypes();
			// NOTE: this assumes all subtypes are of type ODSkimOutputType!
			activeOdSkimOutputTypes = topLevelSet.stream().map( e -> (ODSkimSubOutputType) e).collect(Collectors.toSet());
		} else {
			activeOdSkimOutputTypes = new HashSet<ODSkimSubOutputType>();
		}
		modalODPathMatrixMap = new HashMap<Mode, ODPathMatrix>();
	}
	
	/**
	 * Get the flows for a specified mode
	 * 
	 * @param mode the specified mode
	 * @return array of flows for current mode
	 */
	public double[] getModalNetworkSegmentFlows(Mode mode) {
		return modalNetworkSegmentFlows.get(mode);
	}

	/**
	 * Collect the data per mode for all modes
	 * 
	 * @return mode specific data map
	 */
	public Map<Mode, ModeData> getModeSpecificData() {
		return modeSpecificData;
	}

	/**
	 * Return the total flow through a link segment across all modes
	 * 
	 * @param linkSegment the specified link segment
	 * @return the total flow through this link segment
	 */
	public double getTotalNetworkSegmentFlow(LinkSegment linkSegment) {
		return modalNetworkSegmentFlows.values().stream()
				.collect((Collectors.summingDouble(flows -> flows[(int) linkSegment.getId()])));
	}

	/**
	 * Reset modal network segment flows by cloning empty array
	 * 
	 * @param mode the mode whose flows are to be reset
	 */
	public void resetModalNetworkSegmentFlows(Mode mode, int numberOfNetworkSegments) {
		setModalNetworkSegmentFlows(mode, new double[numberOfNetworkSegments]);
	}

	/**
	 * Set the flows for a specified mode
	 * 
	 * @param mode                     the specified mode
	 * @param modalNetworkSegmentFlows array of flows for the specified mode
	 */
	public void setModalNetworkSegmentFlows(Mode mode, double[] modalNetworkSegmentFlows) {
		this.modalNetworkSegmentFlows.put(mode, modalNetworkSegmentFlows);
	}

	/**
	 * Set the link segment costs for a specified mode
	 * 
	 * @param mode                     the specified mode
	 * @param modalLinkSegmentCosts array of costs for the specified mode
	 */
	public void setModalLinkSegmentCosts(Mode mode, double[] modalLinkSegmentCosts) {
		modalNetworkSegmentCostsMap.put(mode, modalLinkSegmentCosts);
	}

	/**
	 * Retrieve the link segment costs for a specified mode
	 * 
	 * @param mode the specified mode
	 * @return array of costs for the specified mode
	 */
	public double[] getModalLinkSegmentCosts(Mode mode) {
		return modalNetworkSegmentCostsMap.get(mode);
	}

	/**
	 * Reset the skim matrix to all zeroes for a specified mode for all activated
	 * skim output types
	 * 
	 * @param odSkimOutputType the specified Skim Output type
	 * @param mode             the specified mode
	 * @param zones            Zones object containing all the origin and
	 *                         destination zones
	 */
	public void resetSkimMatrix(Mode mode, Zoning.Zones zones) {
		modalSkimMatrixMap.put(mode, new HashMap<ODSkimSubOutputType, ODSkimMatrix>());
		for (ODSkimSubOutputType odSkimOutputType : activeOdSkimOutputTypes) {
			ODSkimMatrix odSkimMatrix = new ODSkimMatrix(zones, odSkimOutputType);
			modalSkimMatrixMap.get(mode).put(odSkimOutputType, odSkimMatrix);
		}
		modalODPathMatrixMap.put(mode, new ODPathMatrix(zones));
	}
	
    /**
     * Reset the path matrix to empty for a specified mode for all activated
     * 
     * @param mode             the specified mode
     * @param zones            Zones object containing all the origin and
     *                         destination zones
     */
    public void resetPathMatrix(Mode mode, Zoning.Zones zones) {
        modalODPathMatrixMap.put(mode, new ODPathMatrix(zones));
    }	
	
	/**
	 * Retrieve the skim matrix for a specified mode and skim output type
	 * 
	 * @param odSkimOutputType the specified Skim Output type
	 * @param mode             the specified mode
	 * @return the skim matrix for the specified mode
	 */
	public ODSkimMatrix getODSkimMatrix(ODSkimSubOutputType odSkimOutputType, Mode mode) {
		if (modalSkimMatrixMap.containsKey(mode)) {
			Map<ODSkimSubOutputType, ODSkimMatrix> skimMatrixMap = modalSkimMatrixMap.get(mode);
			if (skimMatrixMap.containsKey(odSkimOutputType)) {
				return skimMatrixMap.get(odSkimOutputType);
			}
		}
		return null;
	}
	
	/**
	 * Retrieve the current OD path for a specified mode
	 * 
	 * @param mode the specified mode
	 * @return the OD path for this mode
	 */
	public ODPathMatrix getODPathMatrix(Mode mode) {
		return modalODPathMatrixMap.get(mode);
	}

	/**
	 * Retrieve the Map of OD Skim matrices for all active OD Skim Output Types for a specified mode
	 * 
	 * @param mode the specified mode
	 * @return Map of OD Skim matrices for all active OD Skim Output Types
	 */
	public Map<ODSkimSubOutputType, ODSkimMatrix> getSkimMatrixMap(Mode mode) {
		return modalSkimMatrixMap.get(mode);
	}
	
	/**
	 * Returns a Set of activated OD skim output types
	 * 
	 * @return Set of activated OD skim output types
	 */
	public Set<ODSkimSubOutputType> getActiveSkimOutputTypes() {
		return activeOdSkimOutputTypes;
	}

}