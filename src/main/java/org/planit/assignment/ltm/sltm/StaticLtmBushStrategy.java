package org.planit.assignment.ltm.sltm;

import java.util.logging.Logger;

import org.planit.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.OneToAllShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.ShortestPathResult;
import org.planit.assignment.ltm.sltm.consumer.InitialiseBushEdgeSegmentDemandConsumer;
import org.planit.assignment.ltm.sltm.loading.StaticLtmNetworkLoading;
import org.planit.network.transport.TransportModelNetwork;
import org.planit.od.demand.OdDemands;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.zoning.OdZone;
import org.planit.zoning.Zoning;

/**
 * Implementation to support a bush absed solution for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmBushStrategy extends StaticLtmAssignmentStrategy {

  /** logegr to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmBushStrategy.class.getCanonicalName());

  /** track bushes per origin (with non-zero demand) */
  private final Bush[] originBushes;

  /**
   * Initialise bushes. Find shortest path for each origin and add the links to the bush
   * 
   * @param odDemands        demands used
   * @param linkSegmentCosts costs to use
   * @throws PlanItException thrown when error
   */
  private void initialiseBushes(final OdDemands odDemands, final double[] linkSegmentCosts) throws PlanItException {
    final int numberOfEdgeSegments = getTransportNetwork().getNumberOfEdgeSegmentsAllLayers();
    final int numberOfVertices = getTransportNetwork().getNumberOfVerticesAllLayers();
    final OneToAllShortestPathAlgorithm shortestPathAlgorithm = new DijkstraShortestPathAlgorithm(linkSegmentCosts, numberOfEdgeSegments, numberOfVertices);

    Zoning zoning = getTransportNetwork().getZoning();
    for (OdZone origin : zoning.getOdZones()) {
      ShortestPathResult oneToAllResult = null;
      InitialiseBushEdgeSegmentDemandConsumer initialiseBushConsumer = null;
      Bush originBush = null;
      for (OdZone destination : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        Double currOdDemand = odDemands.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {

          if (originBush == null) {
            /* register new bush */
            originBush = new Bush(getIdGroupingToken(), origin, numberOfEdgeSegments);
            originBushes[(int) origin.getOdZoneId()] = originBush;
            initialiseBushConsumer = new InitialiseBushEdgeSegmentDemandConsumer(originBush);
          } else {
            /* ensure bush initialisation is applied to the right destination/demand */
            initialiseBushConsumer.setDestination(destination.getCentroid(), currOdDemand);
          }

          /* find one-to-all shortest paths */
          if (oneToAllResult == null) {
            oneToAllResult = shortestPathAlgorithm.executeOneToAll(origin.getCentroid());
          }

          /* initialise bush with this destination shortest path */
          oneToAllResult.forEachBackwardEdgeSegment(origin.getCentroid(), destination.getCentroid(), initialiseBushConsumer);
        }

      }
    }
  }

  @Override
  protected StaticLtmNetworkLoading createNetworkLoading() {
    // TODO:
    return null;
  }

  /**
   * Create initial bushes, where for each origin the bush is initialised with the shortest path only
   */
  @Override
  protected void createInitialSolution(TimePeriod timePeriod, OdDemands odDemands, double[] initialLinkSegmentCosts) {
    try {
      initialiseBushes(odDemands, initialLinkSegmentCosts);
    } catch (PlanItException e) {
      LOGGER.severe(String.format("Unable to create initial bushes for sLTM %d", getAssignmentId()));
    }
  }

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
    originBushes = new Bush[transportModelNetwork.getZoning().getOdZones().size()];
  }

  @Override
  public void performIteration() {
    // TODO:
  }

}
