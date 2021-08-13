package org.planit.assignment.ltm.sltm;

import org.planit.network.transport.TransportModelNetwork;
import org.planit.od.path.OdPaths;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;

/**
 * Class exposing the various sLTM network loading solution method components of sLTM (not considering path choice, this is assumed to be given). Network loading solution method
 * Based on Raadsen and Bliemer (2021) General solution scheme for the Static Link Transmission Model .
 * 
 * @author markr
 *
 */
public class StaticLtmNetworkLoading {

  /** variables tracked for sending flow update step **/
  private final SendingFlowData sendingFlowData;

  /** variables tracked for splitting rate update step **/
  private final SplittingRateData splittingRateData;

  /** tracks flow acceptance factors as well as its two related other factors, storage and capacity factors */
  private final NetworkLoadingFactorData networkLoadingFactorData;

  /** transport network used */
  private final TransportModelNetwork network;

  /** mode used */
  private final Mode mode;

  /** odPaths to load */
  final OdPaths odPaths;

  /**
   * Validate provided constructor parameters
   * 
   * @param network to validate
   * @param mode    to validate
   */
  private void validate(TransportModelNetwork network, Mode mode) {
    if (network == null || network.getInfrastructureNetwork() == null || network.getInfrastructureNetwork().getLayerByMode(mode) == null) {
      throw new IllegalArgumentException(" network or network layer or mode of network layer not available for static LTM network loading");
    }

    if (!(network.getInfrastructureNetwork().getLayerByMode(mode) instanceof MacroscopicNetworkLayer)) {
      throw new IllegalArgumentException(String.format("Network layer for mode %s not of compatible type, expected MacroscopicNetworkLayer", mode.getXmlId()));
    }
  }

  /**
   * Constructor
   * 
   * @param network to run on
   * @param mode    to use
   * @param odPaths that require loading
   */
  protected StaticLtmNetworkLoading(final TransportModelNetwork network, final Mode mode, final OdPaths odPaths) {
    validate(network, mode);
    this.network = network;
    this.mode = mode;

    MacroscopicNetworkLayer networkLayer = (MacroscopicNetworkLayer) network.getInfrastructureNetwork().getLayerByMode(mode);
    double[] referenceEmptyArray = new double[networkLayer.getLinkSegments().size()];
    this.sendingFlowData = new SendingFlowData(referenceEmptyArray);
    this.splittingRateData = new SplittingRateData();
    this.networkLoadingFactorData = new NetworkLoadingFactorData(referenceEmptyArray);
    this.odPaths = odPaths;
  }

  //@formatter:off
  /**
   * Perform initialisation of the network loading:
   * 
   * 1. Initial acceptance flow, capacity, and storage factors, all set to one  
   * 2. Initial in/outflows via network loading Eq. (3)-(4) in paper: unconstrained network loading
   * 3. Initial sending and receiving flows: s_a=u_a, r_a=capacity_a for all link segments a
   * 4. Set iteration number to one
   * (Extension A):  
   * 5. Restrict receiving flows to storage capacity Eq. (8) - only relevant when storage capacity is activated
   */
  public void stepZeroInitialisation() {
    
    /* 1. Initial acceptance flow, capacity, and storage factors, all set to one */
    networkLoadingFactorData.initialiseAll(1.0);
    
    /* 2. Initial in/outflows via network loading Eq. (3)-(4) in paper: unconstrained network loading */
    
  }
  
  //@formatter:off
  /**
   * Perform splitting rate update (before sending flow update) of the network loading:
   * 
   * 1. Update inflows via network loading Eq. (3)
   * 2. Update splitting rates Eq. (6),(4) 
   * (Extension B)
   * 3. If not first iteration then update splitting rates, Eq. (13)
   */
  public void stepOneSplittingRatesUpdate() {
    //TODO
  }  
  
  //@formatter:off
  /**
   * Perform splitting rate update (before sending flow update) of the network loading:
   * 
   * 1. Update node model to compute new inflows, Eq. (5)
   * 2. Update next sending flows via inflows, Eq. (7)
   * 3. Compute gap,  then update sending flows to next sending flows
   * 4. If converged containue, otherwise continue go back to Step 2-(1).
   * 5. Update storage capacity factors, Eq. (11)
   * (Extension B)
   * 6. Update smoothed storage capacity factors, Eq. (14)
   */
  public void stepTwoSendingFlowUpdate() {
    //TODO
  }   
  
  //@formatter:off
  /**
   * Perform splitting rate update (before receiving flow update) of the network loading:
   * 
   * 1. Update intermediate flow acceptance factors, Eq. (9)
   * 2. Update inflows via network loading, Eq. (3)
   * 3. Update splitting rates, Eq. (6)
   * (Extension B)
   * 4. If not first iteration then update splitting rates, Eq. (13)
   * (Extension C)
   * 5. Estimate new multiplication factor used in Step 4, Eq. (16),(17)
   */
  public void stepThreeSplittingRateUpdate() {
    //TODO
  }  
  
  /**
   * 1. Update node model, to compute outflows Eq. (5)
   * 2. Update receiving flows based on outflows, Eq. (8)
   * (Extension B)
   * 3. Transform to nudged receiving flows using multiplication factor, Eq. (18)
   * (end Extension B)
   * 4. Compute gap then set next receicing flows to current receiving flows   
   * 5. If converged continue, else go back to Step 4-(1).
   * 6. Update link capacity factors, Eq. (10)
   * (Extension C)
   * 7. Update smoothed link capacity factors, Eq. (14)
   */
  public void stepFourReceivingFlowUpdate() {
    //TODO
  }

  /**
   * 1. Update flow acceptance factors, Eq. (9)
   * 2. Compute gap using flow acceptance factors,  
   * 3. Increment iteration index,  
   * 4. If converged done, else go back to Step 1.
   * 
   * @return true when converged, false otherwise
   */
  public boolean stepFiveCheckNetworkLoadingConvergence() {
    //TODO
    return false;
  }
}
