package org.goplanit.assignment.ltm.sltm;

import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathOneToAll;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingPath;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.LinkBasedRelativeDualityGapFunction;
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
import java.util.List;
import java.util.logging.Logger;

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
   * Update gap. Unconventional gap function where we update the GAP based on path cost discrepancy.
   * Consider implementing alternative time permitting
   * <p>
   * minimumCost low cost path perceived cost   : low cost * SUM(low cost sending flow, high cost sending flow)
   * measuredCost: low cost sending flow * perceived cost + high cost sending flow * perceived high cost
   * <p>
   * Sum the above over all low/high cost path alternatives. Note that we only consider highest and lowest cost path here, so sum does not
   * add up to total network cost.
   *
   * @param gapFunction   to use
   * @param lowestCostPath the low-cost path
   * @param lowestCostPerceivedCost the low-cost path perceived costs
   * @param highestCostPath the high-cost path
   * @param highestCostPerceivedCost the high-cost path perceived costs
   * @param odDemand total OdDemand
   */
  protected void updateGap(
          final LinkBasedRelativeDualityGapFunction gapFunction,
          final StaticLtmDirectedPath lowestCostPath,
          double lowestCostPerceivedCost,
          final StaticLtmDirectedPath highestCostPath,
          double highestCostPerceivedCost,
          double odDemand) {
    gapFunction.increaseConvexityBound(
            lowestCostPerceivedCost * (odDemand * (highestCostPath.getPathChoiceProbability() + lowestCostPath.getPathChoiceProbability())));
    gapFunction.increaseMeasuredCost(odDemand * lowestCostPath.getPathChoiceProbability() * lowestCostPerceivedCost);
    gapFunction.increaseMeasuredCost(odDemand * highestCostPath.getPathChoiceProbability() * highestCostPerceivedCost);
  }

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
          newOdShortestPaths.setValue(origin, destination, sLtmPath);
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
                initialOdPath.setPathChoiceProbability(1); // set current probability to 100%
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
      /* construct derivatives per link segment, so we can construct newton step */
      double[] dCostDFlow = this.constructLinkBasedDCostDFlow(theMode, updateOnlyPotentiallyBlockingNodeCosts);

      final var stochasticPathChoice = (StochasticPathChoice) getPathChoice(); // currently only type of path choice which is verified (if present), so safe to cast

      /* determine path choice for next iteration */
      if(stochasticPathChoice != null) {

        // if simple smoothing, prep here, if path based, prep later
        final var smoothing = getSmoothing();
        final var gapFunction = (LinkBasedRelativeDualityGapFunction) getTrafficAssignmentComponent(GapFunction.class);
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

                  double[] currAbsolutePathCosts = PathUtils.computeEdgeSegmentAdditiveValues(odPaths, costsToUpdate);
                  double[] currCostRelatedPathProbabilities = odPaths.stream().map( p -> p.getPathChoiceProbability()).mapToDouble(v -> v).toArray();
                  double[] currPerceivedPathCosts = stochasticPathChoice.computePerceivedPathCosts(currAbsolutePathCosts, currCostRelatedPathProbabilities, demand);

                  //1. get i-1 iteration perceived cost for LOW and HIGH cost paths
                  int lowCostPathIndex = newPathAdded ? odPaths.size()-1 : ArrayUtils.findMinValueIndex(currPerceivedPathCosts);
                  int highCostPathIndex = ArrayUtils.findMaxValueIndex(currPerceivedPathCosts);
                  var lowCostPath = odPaths.get(lowCostPathIndex);
                  var highCostPath = odPaths.get(highCostPathIndex);

                  double lowCostPathCurrPerceivedCost = currPerceivedPathCosts[lowCostPathIndex];
                  double highCostPathCurrPerceivedCost = currPerceivedPathCosts[highCostPathIndex];

                  final var choiceModel = stochasticPathChoice.getChoiceModel();

                  // low and high cost path based sum of dAbsoluteCostdFlow (so not perceived derivatives yet, just the absolute cost component of it)
                  double lowCostPathDAbsoluteCostDFlow = PathUtils.computeEdgeSegmentAdditiveValues(lowCostPath, dCostDFlow);
                  double highCostPathDAbsoluteCostDFlow = PathUtils.computeEdgeSegmentAdditiveValues(highCostPath, dCostDFlow); // high cost path based sum of dCostdFlow

                  //todo: make configurable as this is a costly exercise (or if implemented more efficiently will cost more memory. Also unlikely to have an impact
                  // in many cases as bottlenecks are less likely to be overlapping.
                  boolean onlyConsiderNonOverlappingLinks = true;
                  if(onlyConsiderNonOverlappingLinks){
                    int[] overlappingIndices = PathUtils.getOverlappingPathLinkIndices(lowCostPath, highCostPath);
                    for(var overlappingLinkIndex : overlappingIndices){
                      lowCostPathDAbsoluteCostDFlow -= dCostDFlow[overlappingLinkIndex];
                      highCostPathDAbsoluteCostDFlow -= dCostDFlow[overlappingLinkIndex];
                    }
                  }

                  // high cost path based dPerceivedCost/dFlow this required the derivative of the perceived cost related to the applied path choice model
                  double highCostPathDenominator = choiceModel.computeDPerceivedCostDFlow(
                          highCostPathDAbsoluteCostDFlow, currAbsolutePathCosts[highCostPathIndex], highCostPath.getPathChoiceProbability() * demand, true);
                  // low cost path based dPerceivedCost/dFlow this required the derivative of the perceived cost related to the applied path choice model
                  double lowCostPathDenominator = choiceModel.computeDPerceivedCostDFlow(
                          lowCostPathDAbsoluteCostDFlow,  currAbsolutePathCosts[lowCostPathIndex], lowCostPath.getPathChoiceProbability() * demand, true);

                  //7. determine newton step to determine flows/probabilities for i+1
                  double newtonStepDenominator = highCostPathDenominator + lowCostPathDenominator;
                  //   cost_high - step * dCost_high/d_Flow_high = cost_low - step * dCost_low/d_Flow_low
                  //   rewrite towards step: step =  (cost_high - cost_low)/((dCost_high/d_Flow_high)+(dCost_low/d_Flow_low))
                  double newtonStep =
                            (highCostPathCurrPerceivedCost - lowCostPathCurrPerceivedCost) / newtonStepDenominator;

                  // todo apply smoothing instead of multiplying by 0.25 (revert to always being iteration based for now I would think + implement fixed smoothing step option to use)
                  double newLowCostPathProbability = Math.min(1, ((lowCostPath.getPathChoiceProbability() * demand)   + 0.25 * newtonStep)/demand);
                  double newHighCostPathProbability = Math.max(0, ((highCostPath.getPathChoiceProbability() * demand) - 0.25 * newtonStep)/demand);

                  //7. prep for i + 1 iteration
                  {
                    // update probabilities applied, so they are available for the next iteration
                    lowCostPath.setPathChoiceProbability(newLowCostPathProbability);
                    highCostPath.setPathChoiceProbability(newHighCostPathProbability);
                  }

                  //6. update gap
                  // todo should be based on all paths, not just the ones changed
                  updateGap(gapFunction, lowCostPath, lowCostPathCurrPerceivedCost, highCostPath, highCostPathCurrPerceivedCost, demand);
                });
        LOGGER.info("Created new paths and updated path choice for path-based sLTM");
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
