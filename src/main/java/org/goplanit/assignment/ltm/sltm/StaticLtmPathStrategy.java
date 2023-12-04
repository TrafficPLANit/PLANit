package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathOneToAll;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingPath;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.goplanit.gap.GapFunction;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.od.path.OdMultiPaths;
import org.goplanit.od.path.OdMultiPathsHashed;
import org.goplanit.od.path.OdPaths;
import org.goplanit.od.path.OdPathsHashed;
import org.goplanit.path.ManagedDirectedPathFactoryImpl;
import org.goplanit.path.choice.PathChoice;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.ManagedDirectedPathFactory;
import org.goplanit.zoning.Zoning;

/**
 * Implementation to deal with a path based sLTM implementation
 * 
 * @author markr
 *
 */
public class StaticLtmPathStrategy extends StaticLtmAssignmentStrategy {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmPathStrategy.class.getCanonicalName());

  /** initial capacity used for list of paths per OD */
  private static final int INITIAL_PER_OD_PATH_CAPACITY = 3;

  /** odPaths to track */
  private final OdMultiPaths<List<StaticLtmDirectedPath>> odMultiPaths;

  /**
   * Create the od paths based on provided costs. Only create paths for od pairs with non-zero flow.
   * 
   * @param currentSegmentCosts costs to use for the shortest path algorithm
   * @return create odPaths
   */
  private OdPaths<StaticLtmDirectedPath> createOdPaths(final double[] currentSegmentCosts) {
    final ShortestPathOneToAll shortestPathAlgorithm = new ShortestPathDijkstra(currentSegmentCosts, getTransportNetwork().getNumberOfVerticesAllLayers());

    ManagedDirectedPathFactory pathFactory = new ManagedDirectedPathFactoryImpl(getIdGroupingToken());
    var newOdShortestPaths = new OdPathsHashed<StaticLtmDirectedPath>(getIdGroupingToken(), getTransportNetwork().getZoning().getOdZones());

    Zoning zoning = getTransportNetwork().getZoning();
    OdDemands odDemands = getOdDemands();
    for (var origin : zoning.getOdZones()) {
      var originVertex = findCentroidVertex(origin);
      var oneToAllResult = shortestPathAlgorithm.executeOneToAll(originVertex);
      for (var destination : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        /* for positive demand on OD generate the shortest path under given costs */
        Double currOdDemand = odDemands.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {
          var destinationVertex = findCentroidVertex(destination);
          var path = oneToAllResult.createPath(pathFactory, originVertex, destinationVertex);
          if (path == null) {
            LOGGER.warning(String.format("%sUnable to create path for OD (%s,%s) with non-zero demand (%.2f)", LoggingUtils.runIdPrefix(getAssignmentId()), origin.getXmlId(),
                destination.getXmlId(), currOdDemand));
            continue;
          }
          newOdShortestPaths.setValue(origin, destination, new StaticLtmDirectedPathImpl(path));
        }
      }
    }
    return newOdShortestPaths;
  }

  /**
   * Convenience access to path choice component
   * @return path choice component
   */
  protected PathChoice getPathChoice(){
    var pathChoice = (PathChoice) getTrafficAssignmentComponent(PathChoice.class);
    if(pathChoice == null){
      throw new PlanItRunTimeException("No Path choice available on sLTM Path-based approach, shouldn't happen");
    }
    return pathChoice;
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
  public StaticLtmPathStrategy(
          final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork, final StaticLtmSettings settings,
      final TrafficAssignmentComponentAccessee taComponents) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents);
    this.odMultiPaths =
            new OdMultiPathsHashed<>(getIdGroupingToken(), getTransportNetwork().getZoning().getOdZones());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createInitialSolution(double[] initialLinkSegmentCosts, int iterationIndex) {
    try {
      /* create initial paths */
      final var newOdPaths = createOdPaths(initialLinkSegmentCosts);

      /* initialise OD multi-path hash containers */
      getOdDemands().forEachNonZeroOdDemand(getTransportNetwork().getZoning().getOdZones(),
              (o, d, demand) -> {
                var odMultiPathList = new ArrayList<StaticLtmDirectedPath>(INITIAL_PER_OD_PATH_CAPACITY);
                odMultiPathList.add(newOdPaths.getValue(o, d));
                odMultiPaths.setValue(o, d, odMultiPathList);
              });

      //todo: incorrect we should no longer use the single path but multi-path. Replace with multi-path once this is in place
      //getLoading().updateOdPaths(odMultiPaths);
      getLoading().updateOdPaths(newOdPaths);
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
      /* NETWORK LOADING - MODE AGNOSTIC FOR NOW */
      executeNetworkLoading();

      /* COST UPDATE */
      boolean updateOnlyPotentiallyBlockingNodeCosts = getLoading().getActivatedSolutionScheme().equals(StaticLtmLoadingScheme.POINT_QUEUE_BASIC);
      this.executeNetworkCostsUpdate(theMode, updateOnlyPotentiallyBlockingNodeCosts, costsToUpdate);

      /* FIND NEW SHORTEST PATHS*/
      var pathChoice = getPathChoice();
      if(!pathChoice.isPathsFixed()){
        LOGGER.info("Creating new paths for path-based sLTM");
        var newOdPaths = createOdPaths(costsToUpdate);
      }


      /* PERFORM PATH CHOICE */
      //todo: execute path choice to obtain new probabilities per OD-path
      LOGGER.info("Executing path choice before next iteration for path-based sLTM");

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
    return "Path-based";
  }

  /**
   * {@inheritDoc}
   *
   * Path-based based assignment requires a form of path choice if we do more than a single iteration
   */
  @Override
  public void verifyComponentCompatibility() {
    super.verifyComponentCompatibility();

    var gapFunction = getTrafficAssignmentComponent(GapFunction.class);
    if(!hasTrafficAssignmentComponent(PathChoice.class) && gapFunction.getStopCriterion().getMaxIterations()>1){
      throw new PlanItRunTimeException("Path-based sLTM assignment has no Path Choice defined, when running multiple iterations this is a requirement");
    }
  }

}
