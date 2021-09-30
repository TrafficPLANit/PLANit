package org.planit.assignment.ltm.sltm;

import org.planit.network.transport.TransportModelNetwork;
import org.planit.od.demand.OdDemands;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.time.TimePeriod;

/**
 * Implementation to support a bush absed solution for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmBushStrategy extends StaticLtmAssignmentStrategy {

  /**
   * Constructor
   * 
   * @param idGroupingToken       to use for internal managed ids
   * @param assignmentId          of parent assignment
   * @param transportModelNetwork to use
   * @param settings              to use
   */
  public StaticLtmBushStrategy(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork, final StaticLtmSettings settings) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings);
  }

  @Override
  protected StaticLtmNetworkLoading createNetworkLoading() {
    // TODO:
    return null;
  }

  @Override
  protected void createInitialSolution(TimePeriod timePeriod, OdDemands odDemands, double[] initialLinkSegmentCosts) {
    // TODO:
  }

  @Override
  public void performIteration() {
    // TODO:
  }

}
