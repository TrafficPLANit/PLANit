package org.planit.assignment.ltm.sltm;

import java.util.Map;
import java.util.logging.Logger;

import org.planit.assignment.ltm.sltm.consumer.PathLinkInflowUpdateConsumer;
import org.planit.assignment.ltm.sltm.consumer.PathTurnFlowUpdateConsumer;
import org.planit.od.path.OdPaths;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.OdZones;

/**
 * The path absed network loading scheme for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmPathLoading extends StaticLtmNetworkLoading {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StaticLtmPathLoading.class.getCanonicalName());

  /**
   * Od Paths to use
   */
  private OdPaths odPaths;

  //@formatter:off
  /**
   * Conduct a network loading to compute updated turn inflow rates u_ab: Eq. (3)-(4) in paper. We only consider turns on nodes that are potentially blocking to reduce
   * computational overhead.
   * 
   * @return acceptedTurnFlows (on potentially blocking nodes) where key comprises a combined hash of entry and exit edge segment ids and value is the accepted turn flow v_ab
   */
  @Override
  protected Map<Integer, Double> networkLoadingTurnFlowUpdate() {

    /* update path turn flows (and sending flows if POINT_QUEUE_BASIC)*/
    PathTurnFlowUpdateConsumer pathTurnFlowUpdateConsumer = 
        new PathTurnFlowUpdateConsumer(
            solutionScheme, 
            sendingFlowData, 
            splittingRateData, 
            networkLoadingFactorData, 
            odPaths);
    getOdDemands().forEachNonZeroOdDemand(getTransportNetwork().getZoning().odZones, pathTurnFlowUpdateConsumer);
    return pathTurnFlowUpdateConsumer.getAcceptedTurnFlows();
  }
  
  /**
   * Conduct a network loading to compute updated inflow rates (without tracking turn flows): Eq. (3)-(4) in paper
   * 
   * @param linkSegmentFlowArrayToFill the inflows (u_a) to update
   */
  @Override
  protected void networkLoadingLinkSegmentInflowUpdate(final double[] linkSegmentFlowArrayToFill) {
    OdZones odZones = getTransportNetwork().getZoning().odZones;
    double[] flowAcceptanceFactors = this.networkLoadingFactorData.getCurrentFlowAcceptanceFactors();

    /* update path turn flows (and sending flows if POINT_QUEUE_BASIC) */
    PathLinkInflowUpdateConsumer pathLinkInflowUpdateConsumer = new PathLinkInflowUpdateConsumer(odPaths, flowAcceptanceFactors, linkSegmentFlowArrayToFill);
    getOdDemands().forEachNonZeroOdDemand(odZones, pathLinkInflowUpdateConsumer);
  }   

  /**
   * constructor
   * 
   * @param idToken      to use
   * @param assignmentId to use
   * @param settings to use
   */
  public StaticLtmPathLoading(IdGroupingToken idToken, long assignmentId, final StaticLtmSettings settings) {
    super(idToken, assignmentId, settings);
    this.odPaths = null;
  }

  /** Update the od paths to use in the loading
   * 
   * @param odPaths to use
   */
  public void updateOdPaths(final OdPaths odPaths) {
    this.odPaths = odPaths;
  }

}
