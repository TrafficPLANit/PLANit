package org.goplanit.assignment.ltm.sltm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.goplanit.assignment.ModalSkimMatrixData;
import org.goplanit.assignment.SimulationData;
import org.goplanit.od.skim.OdSkimMatrix;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;

/**
 * Class to hold variables specific to running an sLTM assignment
 * 
 * @author markr
 *
 */
public class StaticLtmSimulationData extends SimulationData {

  private static final Logger LOGGER = Logger.getLogger(StaticLtmSimulationData.class.getCanonicalName());

  /**
   * Track whether initial costs were applied to construct initial solution. If so, we may need to
   * recalculate link segment costs after first iteration even if some links/nodes are not blocking otherwise
   * they remain inconsistent with actual link costs.
   */
  private final TreeMap<Mode, Boolean> initialCostsAppliedInFirstIteration;

  /**
   * Track the mode link segment costs in a 2d raw array where the first dimension is based on mode id while the second uses the link segment id to place the cost
   */
  private double[][] modeLinkSegmentCost;

  /** the currently active time period */
  private TimePeriod timePeriod;

  /** the supported mode for the time period */
  private Set<Mode> supportedModes;

  /** track skim matrix data (if any) */
  private ModalSkimMatrixData skimMatrixData;

  /**
   * Constructor
   * 
   * @param timePeriod                currently in action
   * @param supportedModes            used by the simulation
   * @param numberOfTotalLinkSegments used to correctly initialise the size of the internal data arrays for link segment data
   */
  public StaticLtmSimulationData(final TimePeriod timePeriod, Set<Mode> supportedModes, long numberOfTotalLinkSegments) {
    super();
    this.timePeriod = timePeriod;
    this.supportedModes = supportedModes;
    this.modeLinkSegmentCost = new double[supportedModes.size()][(int) numberOfTotalLinkSegments];
    this.skimMatrixData = new ModalSkimMatrixData();

    this.initialCostsAppliedInFirstIteration = new TreeMap<>();
    supportedModes.forEach(m -> initialCostsAppliedInFirstIteration.put(m, false));
  }

  /**
   * Copy Constructor
   * 
   * @param simulationData to copy
   */
  public StaticLtmSimulationData(final StaticLtmSimulationData simulationData) {
    super(simulationData);
    if (simulationData.modeLinkSegmentCost.length > 0) {
      this.modeLinkSegmentCost = new double[simulationData.modeLinkSegmentCost.length][simulationData.modeLinkSegmentCost[0].length];
      for (int index = 0; index < simulationData.modeLinkSegmentCost.length; ++index) {
        this.modeLinkSegmentCost[index] = Arrays.copyOf(simulationData.modeLinkSegmentCost[index], simulationData.modeLinkSegmentCost[index].length);
      }
    } else {
      this.modeLinkSegmentCost = null;
    }
    this.supportedModes = simulationData.supportedModes;
    this.skimMatrixData = simulationData.skimMatrixData.shallowClone();

    this.initialCostsAppliedInFirstIteration = new TreeMap<>(simulationData.initialCostsAppliedInFirstIteration);
  }

  // GETTERS/SETTERS

  /**
   * Set the costs(travel time in hours per link segment) for a given mode supported by the loading of this data
   * 
   * @param theMode            to set it for
   * @param travelTimeCostHour travel time cost in hours per link segment by link segment id
   */
  public void setLinkSegmentTravelTimePcuH(Mode theMode, double[] travelTimeCostHour) {
    modeLinkSegmentCost[(int) theMode.getId()] = travelTimeCostHour;
  }

  /**
   * Collect the travel time costs for a given mode supported by the loading of this data
   * 
   * @param theMode to set it for
   * @return travelTimeCostHour travel time cost in hours per link segment by link segment id
   */
  public double[] getLinkSegmentTravelTimePcuH(Mode theMode) {
    return modeLinkSegmentCost[(int) theMode.getId()];
  }

  /**
   * Active time period
   * 
   * @return active time period
   */
  public TimePeriod getTimePeriod() {
    return timePeriod;
  }

  /**
   * Active modes
   *
   * @return supported modes in time period
   */
  public Set<Mode> getSupportedModes(){
    return this.supportedModes;
  }

  /**
   * Active time period
   * 
   * @param timePeriod to use
   */
  public void setTimePeriod(TimePeriod timePeriod) {
    this.timePeriod = timePeriod;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StaticLtmSimulationData shallowClone() {
    return new StaticLtmSimulationData(this);
  }

  /**
   * Reset the data
   */
  public void reset() {
    super.reset();

    if (modeLinkSegmentCost != null && modeLinkSegmentCost.length > 0 && modeLinkSegmentCost[0] != null) {
      int numLinkSegments = modeLinkSegmentCost[0].length;
      modeLinkSegmentCost = new double[modeLinkSegmentCost.length][numLinkSegments];
    }

    if(skimMatrixData != null) {
      this.skimMatrixData.reset();
    }

    if(supportedModes != null && initialCostsAppliedInFirstIteration!=null) {
      supportedModes.forEach(m -> initialCostsAppliedInFirstIteration.put(m, false));
    }
  }

  /**
   * Access to container class for skim matrices by mode
   *
   * @return skim matrices data
   */
  public ModalSkimMatrixData getSkimMatrixData() {
    return skimMatrixData;
  }

  /**
   * Indicate initial costs were applied to all link segments when initialising
   * @param mode they were applied for
   * @param flag flag
   */
  public void setInitialCostsAppliedInFirstIteration(Mode mode, boolean flag) {
    if(!supportedModes.contains(mode)){
      LOGGER.severe("Mode used for initial costs that is not available on simulation, ignored");
      return;
    }
    initialCostsAppliedInFirstIteration.put(mode, flag);
  }

  /**
   * Verify if initial costs were applied to all link segments when initialising
   * @param mode they were applied for
   */
  public boolean isInitialCostsAppliedInFirstIteration(Mode mode) {
    return initialCostsAppliedInFirstIteration.containsKey(mode) && initialCostsAppliedInFirstIteration.get(mode);
  }
}
