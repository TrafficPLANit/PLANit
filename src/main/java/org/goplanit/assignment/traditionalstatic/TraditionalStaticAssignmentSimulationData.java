package org.goplanit.assignment.traditionalstatic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.goplanit.assignment.ModalSkimMatrixData;
import org.goplanit.assignment.SimulationData;
import org.goplanit.od.path.OdPathMatrix;
import org.goplanit.od.skim.OdSkimMatrix;
import org.goplanit.output.configuration.OdOutputTypeConfiguration;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.zoning.OdZones;

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
   * Stores the current OD Path for each mode
   */
  private Map<Mode, OdPathMatrix> modalOdPathMatrixMap;

  /*
   * Stores a skim matrix for each mode and skim output type(updated cell by cell for each iteration)
   */
  private ModalSkimMatrixData modalSkimMatrixData;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public TraditionalStaticAssignmentSimulationData(final IdGroupingToken groupId) {
    this.groupId = groupId;
    this.modeSpecificData = new TreeMap<>();
    this.modalNetworkSegmentCostsMap = new HashMap<>();
    this.modalSkimMatrixData = new ModalSkimMatrixData();
    this.modalOdPathMatrixMap = new HashMap<>();
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
    var modeData = modeSpecificData.values();
    double[] networkSegmentFlows = null;
    for (var modeDataEntry : modeData) {
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
   * @param mode                                     the specified mode
   * @param zones                                    Zones object containing all the origin and destination zones
   * @param originDestinationOutputTypeConfiguration configuration to use
   */
  public void resetSkimMatrix(Mode mode, OdZones zones, OdOutputTypeConfiguration originDestinationOutputTypeConfiguration) {
    modalSkimMatrixData.resetAndCreateEmptySkimMatrices(mode, zones, originDestinationOutputTypeConfiguration);
  }

  /**
   * Reset the path matrix to empty for a specified mode for all activated
   * 
   * @param mode  the specified mode
   * @param zones Zones object containing all the origin and destination zones
   */
  public void resetPathMatrix(Mode mode, OdZones zones) {
    modalOdPathMatrixMap.put(mode, new OdPathMatrix(groupId, zones));
  }

  /**
   * Retrieve the skim matrix for a specified mode and skim output type
   * 
   * @param odSkimOutputType the specified Skim Output type
   * @param mode             the specified mode
   * @return the skim matrix for the specified mode
   */
  public OdSkimMatrix getOdSkimMatrix(OdSkimSubOutputType odSkimOutputType, Mode mode) {
    return modalSkimMatrixData.getOdSkimMatrix(odSkimOutputType, mode);
  }

  /**
   * Retrieve the current OD path for a specified mode
   * 
   * @param mode the specified mode
   * @return the OD path for this mode
   */
  public OdPathMatrix getOdPaths(Mode mode) {
    return modalOdPathMatrixMap.get(mode);
  }

  /**
   * Retrieve the Map of OD Skim matrices for all active OD Skim Output Types for a specified mode
   * 
   * @param mode the specified mode
   * @return Map of OD Skim matrices for all active OD Skim Output Types
   */
  public Map<OdSkimSubOutputType, OdSkimMatrix> getSkimMatrixMap(Mode mode) {
    return modalSkimMatrixData.getSkimMatricesByMode(mode);
  }

}