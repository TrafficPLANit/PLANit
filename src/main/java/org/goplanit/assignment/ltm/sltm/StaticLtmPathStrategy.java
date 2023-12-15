package org.goplanit.assignment.ltm.sltm;

import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathOneToAll;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingPath;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.goplanit.choice.ChoiceModel;
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
import org.goplanit.path.choice.StochasticPathChoice;
import org.goplanit.sdinteraction.smoothing.IterationBasedSmoothing;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.path.ManagedDirectedPathFactory;
import org.goplanit.utils.path.PathUtils;
import org.goplanit.zoning.Zoning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
          var sLtmPath = new StaticLtmDirectedPathImpl(path);
          sLtmPath.setPathCost(oneToAllResult.getCostOf(destinationVertex));
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

      /* initialise OD multi-path hash containers and set path probability to 1*/
      getOdDemands().forEachNonZeroOdDemand(getTransportNetwork().getZoning().getOdZones(),
              (o, d, demand) -> {
                var odMultiPathList = new ArrayList<StaticLtmDirectedPath>(INITIAL_PER_OD_PATH_CAPACITY);
                var initialOdPath = newOdPaths.getValue(o, d);
                initialOdPath.setPathChoiceProbability(1);  // set initial probability to 100%
                odMultiPathList.add(initialOdPath);         // add to path set
                odMultiPaths.setValue(o, d, odMultiPathList);
              });

      getLoading().updateOdPaths(odMultiPaths);
    } catch (Exception e) {
      LOGGER.severe(String.format("Unable to create paths for initial solution of path-based sLTM %s", getAssignmentId()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean performIteration(final Mode theMode, final double[] prevCosts, final double[] costsToUpdate, int iterationIndex) {

    try {
      /* NETWORK LOADING - MODE AGNOSTIC FOR NOW */
      executeNetworkLoading();

      /* COST UPDATE */
      boolean updateOnlyPotentiallyBlockingNodeCosts = getLoading().getActivatedSolutionScheme().equals(StaticLtmLoadingScheme.POINT_QUEUE_BASIC);
      this.executeNetworkCostsUpdate(theMode, updateOnlyPotentiallyBlockingNodeCosts, costsToUpdate);

      final var stochasticPathChoice = (StochasticPathChoice) getPathChoice(); // currently only type of path choice which is verified (if present), so safe to cast

      if(stochasticPathChoice != null) {

        // if simple smoothing, prep here, if path based, prep later
        final var smoothing = getSmoothing();
        final boolean applyOdIndependentSmoothing = smoothing instanceof IterationBasedSmoothing;

        /* EXPAND OD PATH SETS WHEN ELIGIBLE NEW PATH FOUND */
        var newOdPaths = createOdPaths(costsToUpdate);
        getOdDemands().forEachNonZeroOdDemand(getTransportNetwork().getZoning().getOdZones(),
                (o, d, demand) -> {
                  var newOdPath = newOdPaths.getValue(o, d);
                  boolean newPathAdded = false;
                  var odPaths = odMultiPaths.getValue(o, d);
                  if (odPaths.stream().noneMatch(existingPath -> existingPath.getLinkSegmentsOnlyHashCode() == newOdPath.getLinkSegmentsOnlyHashCode())) {
                    // new path, add to set
                    odPaths.add(newOdPath);
                    newPathAdded = true;
                  }

                    // TODO: THIS IS NOT RIGHT YET -> FIRST START SIMPLE AND COMPUTE FOR HIGHEST AND LOWEST COST PATH ITS PERCEIVED COST OF PREVIOUS ITERATION AND CURRENT
                    // TODO: ITERATION, BOTH ARE KNOWN BOTH PREVIOUS ALSO REQUIRES THE PREVIOUS PROBABILITY RATHER THAN JUST THE CURRENT! THIS IS MISSING FROM THE PATH
                    // TODO: THEN USE THE BELOW BUT EVERYTHING IS SHIFTED ONE ITERATION, SO INSTEAD OF USING THE NEWLY CALCULATED PROBABILITIES AND CURRENT, WE USE CURRENT
                    // TODO: AND THE PREVIOUS ETC. IF WE DO SO, THE BELOW SHOULD BE WORKABLE



//                  double[] pathProbabilitiesForOldCosts = stochasticPathChoice.computePathProbabilities(odPaths, prevCosts);
//                  // path cost update + old path cost in array format
//                  double[] currAbsolutePathCosts = PathUtils.computeEdgeSegmentAdditivePathCost(odPaths, costsToUpdate);
//                  double[] newPathProbabilities = stochasticPathChoice.computePathProbabilities(odPaths, currAbsolutePathCosts);
//
//                  //todo: This is incorrect, we can't use this because they will be exactly equal always when using the new probabilities
//                  // instead we should use the existing difference in perceived cost (with the existing probabilities/flows)
//                  // and these should be compared to those in the iteration before,
//                  double[] perceivedPathCosts = stochasticPathChoice.computePerceivedPathCosts(currAbsolutePathCosts, newPathProbabilities, demand);
//
//                  int highCostPathIndex = ArrayUtils.findMaxValueIndex(perceivedPathCosts);
//                  var highCostPath = odPaths.get(highCostPathIndex);
//                  int lowCostPathIndex = newPathAdded ? odPaths.size()-1 : ArrayUtils.findMinValueIndex(perceivedPathCosts);
//                  var lowCostPath = odPaths.get(lowCostPathIndex);
//
//                  // compute dPerceivedCost/dProbability for lowest vs highest cost path
//                  double oldPerceivedCostHighCostPath = stochasticPathChoice.getChoiceModel().computePerceivedCost(highCostPath.getPathCost(), highCostPath.getPathChoiceProbability() * demand, true);
//
//                  double dPerceivedPathCostsHighCostPath = perceivedPathCosts[highCostPathIndex] - oldPerceivedCostHighCostPath;
//                  double dProbabilityHighCostPath = newPathProbabilities[highCostPathIndex] - highCostPath.getPathChoiceProbability();
//                  double highCostPathdCdP = 0;
//                  if(dPerceivedPathCostsHighCostPath*dProbabilityHighCostPath<0){
//                    // sign is pointing in wrong direction, meaning that due to network interactions, an increase in flow led to a decrease in cost
//                    // in this case we can't use this, instead we'll apply  a flat (zero) impact as a best guess without resorting to link level derivatives
//                    // todo: if this happens often and we do not get good performance, we should consider using link level derivatives instead as these always have the correct sign
//                    LOGGER.warning("Conflicting descent direction, flatlining derivative, consider moving to link based approach if this happens often");
//                  }else {
//                    highCostPathdCdP = dPerceivedPathCostsHighCostPath/dProbabilityHighCostPath;
//                  }
//
//                  double dPerceivedPathCostsLowCostPath = 0;
//                  double dProbabilityLowCostPath = 0;
//                  double lowCostPathdCdP = 0;
//                  if(newPathAdded) {
//                    // if new path, then no (easy) path-based derivative of cost towards probability/flow can be created. So instead, of
//                    // doing computationally complicated things, we instead simply adopt the derivative of the high cost path, which is expected to be a
//                    // conservative estimate to start with, after the initial attempt, it will have a derivative in the next iteration so it will quickly become
//                    // more accurate
//                    lowCostPathdCdP = highCostPathdCdP;
//                  }else{
//                    double oldPerceivedCostLowCostPath = stochasticPathChoice.getChoiceModel().computePerceivedCost(lowCostPath.getPathCost(), lowCostPath.getPathChoiceProbability() * demand, true);
//                    dPerceivedPathCostsLowCostPath = perceivedPathCosts[lowCostPathIndex] - oldPerceivedCostLowCostPath;
//                    dProbabilityLowCostPath = newPathProbabilities[lowCostPathIndex] - lowCostPath.getPathChoiceProbability();
//                    if(dPerceivedPathCostsHighCostPath*dProbabilityHighCostPath>0){
//                      lowCostPathdCdP = dPerceivedPathCostsLowCostPath/dProbabilityLowCostPath;
//                    }
//                  }

//                  /* Newton method (in original deterministic path equilibrium flow format)
//                   *
//                   *                C_high - C_low
//                   * Delta_FLOW =  ----------------
//                   *                SUM_all_links_on_either_path_but_not_both( dC_link/dFLOW_link)
//                   *
//                   * In this context we track dC/dF on a path level instead of per link for computational efficiency, we use a proxy in case the path is new
//                   * (and no path information from previous iteration is available). We substitute the flow for probabilities.
//                   * Also, we use perceived costs rather than absolute costs here since we're equilibrating the perceived costs, not absolute costs.
//                   */
//                  double newtonMethodProbabilityShift =
//                          (perceivedPathCosts[highCostPathIndex] - perceivedPathCosts[lowCostPathIndex])
//                          /(lowCostPathdCdP + highCostPathdCdP);
//
//                  // Apply probability shift, but no more than we have available:
//                  lowCostPath.setPathChoiceProbability(
//                          lowCostPath.getPathChoiceProbability() + Math.min(newtonMethodProbabilityShift,  highCostPath.getPathChoiceProbability()));
//                  highCostPath.setPathChoiceProbability(
//                          highCostPath.getPathChoiceProbability() - Math.min(newtonMethodProbabilityShift,  highCostPath.getPathChoiceProbability()));
                });


        LOGGER.info("Creating new paths for path-based sLTM");



        /* PERFORM PATH CHOICE */
        //todo: execute path choice to obtain new probabilities per OD-path
        LOGGER.info("Executing path choice before next iteration for path-based sLTM");
      }

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
    var pathChoice = getPathChoice();
    if(pathChoice==null && gapFunction.getStopCriterion().getMaxIterations()>1){
      throw new PlanItRunTimeException("Path-based sLTM assignment has no Path Choice defined, when running multiple iterations this is a requirement");
    }

    if(pathChoice!=null && !(pathChoice instanceof StochasticPathChoice)){
      throw new PlanItRunTimeException("Path-based sLTM assignment currently only supports Stochastic Path Choice, but found %s", pathChoice.getComponentType());
    }

  }

}
