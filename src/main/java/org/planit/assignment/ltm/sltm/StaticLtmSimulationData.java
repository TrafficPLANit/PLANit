package org.planit.assignment.ltm.sltm;

import org.planit.assignment.SimulationData;

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
   * Constructor
   * 
   * @param networkLoading
   */
  public StaticLtmSimulationData(final StaticLtmNetworkLoading networkLoading) {
    super();
    this.networkLoading = networkLoading;
  }

  /**
   * Access to network loading instance
   * 
   * @return network loading
   */
  public StaticLtmNetworkLoading getNetworkLoading() {
    return networkLoading;
  }

}
