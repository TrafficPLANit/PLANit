package org.planit.assignment.ltm.sltm;

import java.util.Arrays;

import org.planit.assignment.SimulationData;
import org.planit.utils.mode.Mode;

/**
 * Class to hold variables specific to running an sLTM assignment
 * 
 * @author markr
 *
 */
public class StaticLtmSimulationData extends SimulationData {

  /**
   * Network loading to use
   */
  private final StaticLtmNetworkLoading networkLoading;

  /**
   * Track the mode link segment costs in a 2d raw array where the first dimension is based on mode id while the second uses the link segment id to place the cost
   */
  private double[][] modeLinkSegmentCost;

  /**
   * Constructor
   * 
   * @param networkLoading            used by the simulation
   * @param numberOfTotalLinkSegments used to correctly initialise the size of the internal data arrays for link segment data
   */
  public StaticLtmSimulationData(final StaticLtmNetworkLoading networkLoading, long numberOfTotalLinkSegments) {
    super();
    this.networkLoading = networkLoading;
    this.modeLinkSegmentCost = new double[networkLoading.getSupportedModes().size()][(int) numberOfTotalLinkSegments];
  }

  /**
   * Copy Constructor
   * 
   * @param simulationData to copy
   */
  public StaticLtmSimulationData(final StaticLtmSimulationData simulationData) {
    super(simulationData);
    this.networkLoading = simulationData.networkLoading;
    if (networkLoading.getSupportedModes().size() > 0) {
      this.modeLinkSegmentCost = new double[networkLoading.getSupportedModes().size()][simulationData.modeLinkSegmentCost[0].length];
      for (int index = 0; index < networkLoading.getSupportedModes().size(); ++index) {
        this.modeLinkSegmentCost[index] = Arrays.copyOf(simulationData.modeLinkSegmentCost[index], simulationData.modeLinkSegmentCost[index].length);
      }
    } else {
      this.modeLinkSegmentCost = null;
    }
  }

  /**
   * Access to network loading instance
   * 
   * @return network loading
   */
  public StaticLtmNetworkLoading getNetworkLoading() {
    return networkLoading;
  }

  // GETTERS/SETTERS

  /**
   * Set the costs(travel time in hours per link segment) for a given mode supported by the loading of this data
   * 
   * @param theMode            to set it for
   * @param travelTimeCostHour travel time cost in hours per link segment by link segment id
   */
  public void setLinkSegmentTravelTimeHour(Mode theMode, double[] travelTimeCostHour) {
    modeLinkSegmentCost[(int) theMode.getId()] = travelTimeCostHour;
  }

  /**
   * Collect the travel time costs for a given mode supported by the loading of this data
   * 
   * @param theMode to set it for
   * @return travelTimeCostHour travel time cost in hours per link segment by link segment id
   */
  public double[] getLinkSegmentTravelTimeHour(Mode theMode) {
    return modeLinkSegmentCost[(int) theMode.getId()];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StaticLtmSimulationData clone() {
    return new StaticLtmSimulationData(this);
  }

  /**
   * Reset the data
   */
  public void reset() {
    super.reset();
    networkLoading.reset();

    if (modeLinkSegmentCost.length > 0 && modeLinkSegmentCost[0] != null) {
      int numLinkSegments = modeLinkSegmentCost[0].length;
      modeLinkSegmentCost = new double[networkLoading.getSupportedModes().size()][numLinkSegments];
    }
  }

}
