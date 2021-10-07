package org.planit.assignment.ltm.sltm.loading;

import java.util.Map;
import java.util.logging.Logger;

import org.planit.assignment.ltm.sltm.StaticLtmSettings;
import org.planit.utils.id.IdGroupingToken;

/**
 * The bush based network loading scheme for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmLoadingBush extends StaticLtmNetworkLoading {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StaticLtmLoadingBush.class.getCanonicalName());

  //@formatter:off
  /**
   * Conduct a network loading to compute updated turn inflow rates u_ab: Eq. (3)-(4) in paper. We only consider turns on nodes that are potentially blocking to reduce
   * computational overhead.
   * 
   * @return acceptedTurnFlows (on potentially blocking nodes) where key comprises a combined hash of entry and exit edge segment ids and value is the accepted turn flow v_ab
   */
  @Override
  protected Map<Integer, Double> networkLoadingTurnFlowUpdate() {

    //TODO
    
//    /* update path turn flows (and sending flows if POINT_QUEUE_BASIC)*/
//    PathTurnFlowUpdateConsumer pathTurnFlowUpdateConsumer = 
//        new PathTurnFlowUpdateConsumer(
//            solutionScheme, 
//            sendingFlowData, 
//            splittingRateData, 
//            networkLoadingFactorData, 
//            odPaths);
//    getOdDemands().forEachNonZeroOdDemand(getTransportNetwork().getZoning().odZones, pathTurnFlowUpdateConsumer);
//    return pathTurnFlowUpdateConsumer.getAcceptedTurnFlows();
    
    return null;
  }
  
  /**
   * Conduct a network loading to compute updated inflow rates (without tracking turn flows): Eq. (3)-(4) in paper
   * 
   * @param linkSegmentFlowArrayToFill the inflows (u_a) to update
   */
  @Override
  protected void networkLoadingLinkSegmentInflowUpdate(final double[] linkSegmentFlowArrayToFill) {
    
    //TODO
    
//    OdZones odZones = getTransportNetwork().getZoning().odZones;
//    double[] flowAcceptanceFactors = this.networkLoadingFactorData.getCurrentFlowAcceptanceFactors();
//
//    /* update path turn flows (and sending flows if POINT_QUEUE_BASIC) */
//    PathLinkInflowUpdateConsumer pathLinkInflowUpdateConsumer = new PathLinkInflowUpdateConsumer(odPaths, flowAcceptanceFactors, linkSegmentFlowArrayToFill);
//    getOdDemands().forEachNonZeroOdDemand(odZones, pathLinkInflowUpdateConsumer);
  }   

  /**
   * Constructor
   * 
   * @param idToken      to use
   * @param assignmentId to use
   * @param settings to use
   */
  public StaticLtmLoadingBush(IdGroupingToken idToken, long assignmentId, final StaticLtmSettings settings) {
    super(idToken, assignmentId, settings);
  }

}
