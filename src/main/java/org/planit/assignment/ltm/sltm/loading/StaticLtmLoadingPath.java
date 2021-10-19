package org.planit.assignment.ltm.sltm.loading;

import java.util.Map;
import java.util.logging.Logger;

import org.planit.assignment.ltm.sltm.StaticLtmSettings;
import org.planit.assignment.ltm.sltm.consumer.PathLinkInflowUpdateConsumer;
import org.planit.assignment.ltm.sltm.consumer.PathTurnFlowUpdateConsumer;
import org.planit.od.path.OdPaths;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.OdZones;

/**
 * The path based network loading scheme for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmLoadingPath extends StaticLtmNetworkLoading {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StaticLtmLoadingPath.class.getCanonicalName());

  /**
   * Od Paths to use
   */
  private OdPaths odPaths;

  //@formatter:off

  /**
   * {@inheritDoc}
   */
  @Override
  protected Map<Integer, Double> networkLoadingTurnFlowUpdate() {
    /* path based update using the known od paths */
    
    /* update path turn flows (and sending flows if POINT_QUEUE_BASIC)*/
    
    TODO COMBINE WITH TURN FLOW UPDATE -> PASS IN CONFIGURATION ON WHETHER OT UPDATE LINK SENDING FLOWS, TURN SENDING FLOWS OR BOTH
    NOW WE HAVE SEPARATE CONSUMERS WITH BASE CLASS THAT DETERMINES THESE FLAGS --> UGLY --> THEN PROCEED BY DOING THE SAME FOR THE BUSH
    
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
   * {@inheritDoc}
   */
  @Override
  protected void networkLoadingLinkSegmentInflowUpdate(final double[] linkSegmentFlowArrayToFill) {
    /* path based update using the known od paths */
    
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
  public StaticLtmLoadingPath(IdGroupingToken idToken, long assignmentId, final StaticLtmSettings settings) {
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
