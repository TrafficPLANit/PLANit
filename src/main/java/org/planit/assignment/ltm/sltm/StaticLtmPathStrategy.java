package org.planit.assignment.ltm.sltm;

import java.util.logging.Logger;

import org.planit.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.OneToAllShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.ShortestPathResult;
import org.planit.network.transport.TransportModelNetwork;
import org.planit.od.demand.OdDemands;
import org.planit.od.path.OdPaths;
import org.planit.od.path.OdPathsHashed;
import org.planit.path.DirectedPathFactoryImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.path.DirectedPathFactory;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.zoning.OdZone;
import org.planit.zoning.Zoning;

/**
 * Implementation to deal with a path based sLTM implementation
 * 
 * @author markr
 *
 */
public class StaticLtmPathStrategy extends StaticLtmAssignmentStrategy {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmPathStrategy.class.getCanonicalName());

  /** odPaths to load */
  private OdPaths odPaths;

  /**
   * Create the od paths based on provided costs. Only create paths for od pairs with non-zero flow.
   * 
   * @param currentSegmentCosts costs to use for the shortest path algorithm
   * @param mode                to use
   * @param timePeriod
   * @return create odPaths
   * @throws PlanItException thrown if error
   */
  private OdPaths createOdPaths(final OdDemands odDemand, final double[] currentSegmentCosts) throws PlanItException {
    final OneToAllShortestPathAlgorithm shortestPathAlgorithm = new DijkstraShortestPathAlgorithm(currentSegmentCosts, getTransportNetwork().getNumberOfEdgeSegmentsAllLayers(),
        getTransportNetwork().getNumberOfVerticesAllLayers());
    DirectedPathFactory pathFactory = new DirectedPathFactoryImpl(getIdGroupingToken());
    OdPaths odPaths = new OdPathsHashed(getIdGroupingToken(), getTransportNetwork().getZoning().getOdZones());

    Zoning zoning = getTransportNetwork().getZoning();
    for (OdZone origin : zoning.getOdZones()) {
      ShortestPathResult oneToAllResult = shortestPathAlgorithm.executeOneToAll(origin.getCentroid());
      for (OdZone destination : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        /* for positive demand on OD generate the shortest path under given costs */
        Double currOdDemand = odDemand.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {
          DirectedPath path = oneToAllResult.createPath(pathFactory, origin.getCentroid(), destination.getCentroid());
          if (path == null) {
            LOGGER.warning(String.format("%sUnable to create path for OD (%s,%s) with non-zero demand (%.2f)", LoggingUtils.createRunIdPrefix(getAssignmentId()), origin.getXmlId(),
                destination.getXmlId(), currOdDemand));
            continue;
          }
          odPaths.setValue(origin, destination, path);
        }
      }
    }
    return odPaths;
  }

  /** create a path based network loading for this solution scheme */
  @Override
  protected StaticLtmPathLoading createNetworkLoading() {
    return new StaticLtmPathLoading(getIdGroupingToken(), getAssignmentId(), getSettings());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtmPathLoading getLoading() {
    return (StaticLtmPathLoading) super.getLoading();
  }

  /** create initial solution based on generating shortest paths */
  @Override
  protected void createInitialSolution(TimePeriod timePeriod, OdDemands odDemands, double[] initialLinkSegmentCosts) {
    try {
      /* create shortest paths for each OD and place on loading */
      this.odPaths = createOdPaths(odDemands, initialLinkSegmentCosts);
      getLoading().updateOdPaths(odPaths);
    } catch (Exception e) {
      LOGGER.severe(String.format("Unable to create paths for initial solution of path-based sLTM %s", getAssignmentId()));
    }
  }

  /**
   * Constructor
   * 
   * @param idGroupingToken       to use
   * @param assignmentId          to use
   * @param transportModelNetwork to use
   * @param settings              to use
   */
  public StaticLtmPathStrategy(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork, final StaticLtmSettings settings) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void performIteration() {

    // NETWORK LOADING - MODE AGNOSTIC FOR NOW
    {
      executeNetworkLoading();
    }

    // PATH CHOICE NOT YET SUPPORTED
  }

}
