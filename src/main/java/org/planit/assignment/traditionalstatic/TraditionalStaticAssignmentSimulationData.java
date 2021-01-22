package org.planit.assignment.traditionalstatic;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.planit.assignment.SimulationData;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.od.odpath.ODPathMatrix;
import org.planit.output.configuration.ODOutputTypeConfiguration;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.SubOutputTypeEnum;
import org.planit.utils.arrays.ArrayUtils;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.zoning.Zones;

/**
 * Simulation data which are specific to Traditional Static Assignment
 * 
 * @author gman6028
 *
 */
public class TraditionalStaticAssignmentSimulationData extends SimulationData {

  /**
   * Contiguous id generation within this group id token for all instances created with factory methods in this class
   */
  private IdGroupingToken groupId;

  /**
   * Stores the mode specific data required during assignment
   */
  private final Map<Mode, ModeData> modeSpecificData; // specific to tsa

  /**
   * Stores the array of link segment costs for each mode
   */
  private Map<Mode, double[]> modalNetworkSegmentCostsMap;

  /**
   * Stores a skim matrix for each mode and skim output type(updated cell by cell for each iteration)
   */
  private Map<Mode, Map<ODSkimSubOutputType, ODSkimMatrix>> modalSkimMatrixMap;

  /**
   * Stores the current OD Path for each mode
   */
  private Map<Mode, ODPathMatrix> modalODPathMatrixMap;

  /**
   * Constructor
   * 
   * @param groupId       contiguous id generation within this group for instances of this class
   * @param outputManager the OutputConfiguration
   * @throws PlanItException thrown if there is an error
   */
  public TraditionalStaticAssignmentSimulationData(final IdGroupingToken groupId) throws PlanItException {
    this.groupId = groupId;
    this.modeSpecificData = new TreeMap<Mode, ModeData>();
    this.modalNetworkSegmentCostsMap = new HashMap<Mode, double[]>();
    this.modalSkimMatrixMap = new HashMap<Mode, Map<ODSkimSubOutputType, ODSkimMatrix>>();
    this.modalODPathMatrixMap = new HashMap<Mode, ODPathMatrix>();
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
  public double collectTotalNetworkSegmentFlow(LinkSegment linkSegment) {
    return modeSpecificData.values().stream().collect((Collectors.summingDouble(modeData -> modeData.getCurrentSegmentFlows()[(int) linkSegment.getId()])));
  }

  /**
   * determine the total flow across all link segments across all modes
   * 
   * @return the total flows per link segment, null if no mode flows are available
   */
  public double[] collectTotalNetworkSegmentFlows() {
    Collection<ModeData> modeData = modeSpecificData.values();
    double[] networkSegmentFlows = null;
    for (ModeData modeDataEntry : modeData) {
      if (networkSegmentFlows == null) {
        networkSegmentFlows = Arrays.copyOf(modeDataEntry.getCurrentSegmentFlows(), modeDataEntry.getCurrentSegmentFlows().length);
      } else {
        ArrayUtils.addTo(networkSegmentFlows, modeDataEntry.getCurrentSegmentFlows());
      }
    }
    return networkSegmentFlows;
  }

  /**
   * Set the link segment costs for a specified mode
   * 
   * @param mode                  the specified mode
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
   * Reset the skim matrix to all zeroes for a specified mode for all activated skim output types
   * 
   * @param mode  the specified mode
   * @param zones Zones object containing all the origin and destination zones
   */
  public void resetSkimMatrix(Mode mode, Zones<?> zones, ODOutputTypeConfiguration originDestinationOutputTypeConfiguration) {
    modalSkimMatrixMap.put(mode, new HashMap<ODSkimSubOutputType, ODSkimMatrix>());

    for (SubOutputTypeEnum odSkimOutputType : originDestinationOutputTypeConfiguration.getActiveSubOutputTypes()) {
      ODSkimMatrix odSkimMatrix = new ODSkimMatrix(zones, (ODSkimSubOutputType) odSkimOutputType);
      modalSkimMatrixMap.get(mode).put((ODSkimSubOutputType) odSkimOutputType, odSkimMatrix);
    }
  }

  /**
   * Reset the path matrix to empty for a specified mode for all activated
   * 
   * @param mode  the specified mode
   * @param zones Zones object containing all the origin and destination zones
   */
  public void resetPathMatrix(Mode mode, Zones<?> zones) {
    modalODPathMatrixMap.put(mode, new ODPathMatrix(groupId, zones));
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

}