package org.planit.assignment.ltm.sltm;

import java.util.logging.Logger;

import org.planit.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.OneToAllShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.ShortestPathResult;
import org.planit.assignment.ltm.sltm.loading.StaticLtmLoadingPath;
import org.planit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.planit.interactor.TrafficAssignmentComponentAccessee;
import org.planit.network.transport.TransportModelNetwork;
import org.planit.od.demand.OdDemands;
import org.planit.od.path.OdPaths;
import org.planit.od.path.OdPathsHashed;
import org.planit.path.DirectedPathFactoryImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.path.DirectedPathFactory;
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
   * @return create odPaths
   * @throws PlanItException thrown if error
   */
  private OdPaths createOdPaths(final double[] currentSegmentCosts) throws PlanItException {
    final OneToAllShortestPathAlgorithm shortestPathAlgorithm = new DijkstraShortestPathAlgorithm(currentSegmentCosts, getTransportNetwork().getNumberOfEdgeSegmentsAllLayers(),
        getTransportNetwork().getNumberOfVerticesAllLayers());
    DirectedPathFactory pathFactory = new DirectedPathFactoryImpl(getIdGroupingToken());
    OdPaths odPaths = new OdPathsHashed(getIdGroupingToken(), getTransportNetwork().getZoning().getOdZones());

    Zoning zoning = getTransportNetwork().getZoning();
    OdDemands odDemands = getOdDemands();
    for (OdZone origin : zoning.getOdZones()) {
      ShortestPathResult oneToAllResult = shortestPathAlgorithm.executeOneToAll(origin.getCentroid());
      for (OdZone destination : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        /* for positive demand on OD generate the shortest path under given costs */
        Double currOdDemand = odDemands.getValue(origin, destination);
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
  protected StaticLtmLoadingPath createNetworkLoading() {
    return new StaticLtmLoadingPath(getIdGroupingToken(), getAssignmentId(), getSettings());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtmLoadingPath getLoading() {
    return (StaticLtmLoadingPath) super.getLoading();
  }

  /**
   * Constructor
   * 
   * @param idGroupingToken       to use
   * @param assignmentId          to use
   * @param transportModelNetwork to use
   * @param settings              to use
   * @param taComponents          to use for access to user configured assignment components
   */
  public StaticLtmPathStrategy(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork, final StaticLtmSettings settings,
      final TrafficAssignmentComponentAccessee taComponents) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents);
  }

  /** create initial solution based on generating shortest paths */
  @Override
  public void createInitialSolution(double[] initialLinkSegmentCosts) {
    try {
      /* create shortest paths for each OD and place on loading */
      this.odPaths = createOdPaths(initialLinkSegmentCosts);
      getLoading().updateOdPaths(odPaths);
    } catch (Exception e) {
      LOGGER.severe(String.format("Unable to create paths for initial solution of path-based sLTM %s", getAssignmentId()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean performIteration(final Mode theMode, final double[] costsToUpdate, int iterationIndex) {

    try {
      // NETWORK LOADING - MODE AGNOSTIC FOR NOW
      executeNetworkLoading();

      /* COST UPDATE */
      boolean updateOnlyPotentiallyBlockingNodeCosts = getLoading().getActivatedSolutionScheme().equals(StaticLtmLoadingScheme.POINT_QUEUE_BASIC);
      this.executeNetworkCostsUpdate(theMode, updateOnlyPotentiallyBlockingNodeCosts, costsToUpdate);

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("Unable to complete sLTM iteration");
      if (getSettings().isDetailedLogging()) {
        e.printStackTrace();
      }
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription() {
    return "{Path-based";
  }

}
