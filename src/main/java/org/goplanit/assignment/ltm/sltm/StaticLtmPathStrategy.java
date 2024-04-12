package org.goplanit.assignment.ltm.sltm;

import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathOneToAll;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingPath;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.goplanit.choice.ChoiceModel;
import org.goplanit.choice.logit.BoundedMultinomialLogit;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.PathBasedGapFunction;
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
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.ManagedDirectedPathFactory;
import org.goplanit.utils.path.PathUtils;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.logging.Logger;
import java.util.stream.IntStream;

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

  /** List of filters in form of predicates to apply when checking if a newly created path is eligible for inclusion in the set */
  List<BiPredicate<ManagedDirectedPath, Collection<? extends ManagedDirectedPath>>> sLtmPathFilters = new ArrayList<>();

  /** standard filter on newly created paths checking the path is not equal to any existing path in the set.
   * Note we assume that all provided paths are of type StaticLtmDirectedPath, if not the call will crash */
  private static final BiPredicate<ManagedDirectedPath, Collection<? extends ManagedDirectedPath>> NEW_PATH_NOT_EQUAL_TO_EXISTING_PATHS =
          (newPath, existingOdPaths) -> existingOdPaths.stream().noneMatch(
                  existingPath -> ((StaticLtmDirectedPath)existingPath).getLinkSegmentsOnlyHashCode() == ((StaticLtmDirectedPath)newPath).getLinkSegmentsOnlyHashCode());

  /**
   * Initialise the sLTM compatible path filters combining the user defined and sLTM default filters
   */
  private void initialiseSltmPathFilters() {
    final var stochasticPathChoice = (StochasticPathChoice) getPathChoice(); // only type of path choice which is verified (if present), so safe to cast
    if(stochasticPathChoice == null) {
      throw new PlanItRunTimeException("stochastic path choice not available in sLTM path based strategy, expected it to be");
    }

    /* We copy the generically configured filters and combine them with the pre-emptive sLTM specific filters (checking on hash code
     * which is only available in sTLM specific path implementation. Hence, we front-load that filter and then supplement with remaining
     * user-defined filters
     */
    sLtmPathFilters.clear();
    sLtmPathFilters.add(NEW_PATH_NOT_EQUAL_TO_EXISTING_PATHS);
    if(stochasticPathChoice.getPathFilter().hasFilters()) {
      stochasticPathChoice.getPathFilter().forEach( f -> sLtmPathFilters.add(f));
    }
  }

  /**
   * Update gap. gap function where we update the GAP based on path cost discrepancy following Bliemer et al 2014 gap function
   *
   * @param gapFunction   to use
   * @param minPathCostIndex index to path with lowest perceived cost
   * @param perceivedPathCosts the perceived costs of each od path
   * @param perceivedCostUpperBound upper bound to apply to any perceived cost. Truncate to this value if perceived costs exceeds the bound
   * @param absolutePathCosts the absolute costs of each od path
   * @param odPaths list of od paths
   * @param odDemand total OdDemand
   */
  protected void updateGap(
          final PathBasedGapFunction gapFunction,
          final int minPathCostIndex,
          final double[] perceivedPathCosts,
          final double perceivedCostUpperBound,
          final double[] absolutePathCosts,
          final Collection<StaticLtmDirectedPath> odPaths,
          double odDemand) {

    double minPerceivedCost = perceivedPathCosts[minPathCostIndex];
    if(minPerceivedCost == 0.0){
      // in case this is zero (can happen in some exp transformed logit models where it is multiplied with demand), replace
      // with absolute cost this is generally a conservative estimate for the lower bound. Ideally this is never needed though
      // so if this occurs often consider investigating why low cost paths end up with zero demand and/or zero perceived costs.
      minPerceivedCost = absolutePathCosts[minPathCostIndex];
      throw new PlanItRunTimeException("NOT PERMITTED ANYMORE -- see if this still happens, replacing with absolute costs does not work when we allow for negative min perceived costs");
    }

    //gapFunction.increaseMinimumPathCosts(minPerceivedCost, odDemand); // traditional approach of min cost in denominator
    int index = 0;
    for(var path : odPaths){
      double pathCost = index == minPathCostIndex ? minPerceivedCost : perceivedPathCosts[index];
      // Adopting relative gap from rasmussen where the dnominator is NOT based on min cost but just the current cost. This avoids having really large gaps
      // in earlier iterations while it converges to the same final gap at convergence regardless
      gapFunction.increaseMinimumPathCosts(Math.min(perceivedCostUpperBound, pathCost), odDemand * path.getPathChoiceProbability());

      // numerator
      gapFunction.increaseAbsolutePathGap(Math.min(perceivedCostUpperBound, pathCost), odDemand * path.getPathChoiceProbability(), minPerceivedCost);
      ++index;
    }
  }

  /**
   * Create the od paths based on provided costs. Only create paths for od pairs with non-zero flow.
   * 
   * @param currentSegmentCosts costs to use for the shortest path algorithm
   * @return newly created odPaths
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
   *  Per Od update of path probabilities based on available cost information. Assuming link additive costs and stochastic path choice
   *  approach.
   *
   * @param origin the origin
   * @param destination the destination
   * @param odPaths paths available for OD
   * @param newPathAdded flag indicating if a new path was appended to odPaths for this iteration
   * @param currLinkSegmentsCosts absolute costs per link
   * @param dCostDFlow derivative of cost towards flow at present on a per link basis
   * @param stochasticPathChoice SUE path choice to be applied
   * @param smoothing smoothing to be applied to found newton step
   * @param gapFunction to track gap for this iteration to be updated for this OD
   * @param demand the od demand across all paths of this od
   */
  private void updateOdPathProbabilities(
          OdZone origin, OdZone destination, List<StaticLtmDirectedPath> odPaths, boolean newPathAdded,
          double[] currLinkSegmentsCosts, double[] dCostDFlow,
          StochasticPathChoice stochasticPathChoice, Smoothing smoothing, PathBasedGapFunction gapFunction,
          double demand){

    if(odPaths.size() == 1){
      return;
    }

    // required since we cannot guarantee all demands are above 1 in the end result
    boolean applyExpTransform = true;

    //1. get absolute and perceived costs for all paths
    double[] currAbsolutePathCosts = PathUtils.computeEdgeSegmentAdditiveValues(odPaths, currLinkSegmentsCosts);
    double[] currCostRelatedPathProbabilities = odPaths.stream().map( p -> p.getPathChoiceProbability()).mapToDouble(v -> v).toArray();
    double[] currPerceivedPathCosts = stochasticPathChoice.computePerceivedPathCosts(currAbsolutePathCosts, currCostRelatedPathProbabilities, demand, applyExpTransform);

    // sort values and construct indices of sorted values. We do this to base our pairwise probability shifts on
    var currPerceivedPathCostsSortedIndices = IntStream.range(0,currPerceivedPathCosts.length).boxed().sorted(
            Comparator.comparingDouble(i -> currPerceivedPathCosts[i])).mapToInt(e -> e).toArray();

    //8. update gap as it currently stands before commencing new iteration with new probabilities
    int lowestCostPathIndex = currPerceivedPathCostsSortedIndices[0];
    int highestCostPathIndex = currPerceivedPathCostsSortedIndices[currPerceivedPathCosts.length - 1];
    double perceivedCostUpperBoundForGap = currPerceivedPathCosts[highestCostPathIndex];
    // in case the proposed demand for the path drops to zero (0 probability) it may be that it falls outside of the bounds of
    // the logit model if we apply a bounded model. In that case the gap is infinite, which is unhelpful. Instead we approach the bound
    // leading to a measurable gap instead.
    if(stochasticPathChoice.getChoiceModel().getComponentType().equals(ChoiceModel.BOUNDED_MNL)){
      var bMnl = ((BoundedMultinomialLogit) stochasticPathChoice.getChoiceModel());
      var maxAbsCostWithNonZeroProbability = currAbsolutePathCosts[lowestCostPathIndex] + bMnl.getDelta() - bMnl.getDelta()/100.0;
      perceivedCostUpperBoundForGap =
              bMnl.computePerceivedCostGivenReferenceCost(
                      maxAbsCostWithNonZeroProbability, currAbsolutePathCosts[lowestCostPathIndex], demand, applyExpTransform);
    }

    updateGap(gapFunction, lowestCostPathIndex, currPerceivedPathCosts, perceivedCostUpperBoundForGap, currAbsolutePathCosts, odPaths, demand);

    if(getSettings().hasTrackOdsForLogging() && getSettings().isTrackOdForLogging(origin, destination)){
      LOGGER.info(String.format("-------------------- [ Origin (%s) Destination (%s) ]-------------------------------", origin.getIdsAsString(), destination.getIdsAsString()));
      LOGGER.info(String.format("absolute costs:                 %s", Arrays.toString(currAbsolutePathCosts)));
      LOGGER.info(String.format("perceived costs:                %s", Arrays.toString(currPerceivedPathCosts)));
      LOGGER.info(String.format("perceived costs (no transform): %s", Arrays.toString(stochasticPathChoice.computePerceivedPathCosts(currAbsolutePathCosts, currCostRelatedPathProbabilities, demand, !applyExpTransform))));
      LOGGER.info(String.format("probabilities:                  %s", Arrays.toString(currCostRelatedPathProbabilities)));
    }

    int pairIndex = 0;
    List<StaticLtmDirectedPath> pathsToRemove = new ArrayList<>(1);
    while(pairIndex <= (odPaths.size()/2)-1){
      // go through this pairwise

      //2. identify low and highest cost path and their respective absolute and perceived costs
      int lowCostPathIndex = currPerceivedPathCostsSortedIndices[pairIndex];
      int highCostPathIndex = currPerceivedPathCostsSortedIndices[currPerceivedPathCosts.length - pairIndex - 1];
      var lowCostPath = odPaths.get(lowCostPathIndex);

      var highCostPath = odPaths.get(highCostPathIndex);
      double lowCostPathCurrPerceivedCost = currPerceivedPathCosts[lowCostPathIndex];
      double highCostPathCurrPerceivedCost = currPerceivedPathCosts[highCostPathIndex];

      //3. determine link based derivatives to inform step size for both low and high cost path (on link level) - absolute cost component only
      double lowCostPathDAbsoluteCostDFlow = PathUtils.computeEdgeSegmentAdditiveValues(lowCostPath, dCostDFlow);
      double highCostPathDAbsoluteCostDFlow = PathUtils.computeEdgeSegmentAdditiveValues(highCostPath, dCostDFlow); // high cost path based sum of dCostdFlow

      // 4. identify non-overlapping links between low and high cost as flow shifts only impact those links
      //todo: make configurable as this is a costly exercise and for large networks setting this to false can improve convergence as steps becomes more conservative by definition
      boolean onlyConsiderNonOverlappingLinks = false;
      if(onlyConsiderNonOverlappingLinks){
        int[] overlappingIndices = PathUtils.getOverlappingPathLinkIndices(lowCostPath, highCostPath);
        for(var overlappingLinkIndex : overlappingIndices){
          lowCostPathDAbsoluteCostDFlow -= dCostDFlow[overlappingLinkIndex];
          highCostPathDAbsoluteCostDFlow -= dCostDFlow[overlappingLinkIndex];
        }
      }
      // BOUND ENFORCEMENT - Part I
      // For linear free flow branches of an FD,the dcost/dflow on uncongested links is zero -> when high cost non-overlapping path solely comprise such links
      // this results in very high steps. To somewhat soften this and reduce likelihood of overstepping (flip-flopping), we enforce that the dcost/dflow of the high cost
      // path is at least is as high as the lowest cost path's dcost dflow as a lower bound
      highCostPathDAbsoluteCostDFlow = Math.max(lowCostPathDAbsoluteCostDFlow,highCostPathDAbsoluteCostDFlow);

      // 5. determine path based derivatives dcost/dflow (on perceived cost) utilising absolute cost derivatives and functional form of SUE function
      double[] dpCostdFlows = {highCostPathDAbsoluteCostDFlow, lowCostPathDAbsoluteCostDFlow};
      double[] absCosts = {currAbsolutePathCosts[highCostPathIndex], currAbsolutePathCosts[lowCostPathIndex]};
      double highCostPathDenominator = stochasticPathChoice.getChoiceModel().computeDPerceivedCostDFlow(
              dpCostdFlows, absCosts, 0 /*high cost */, highCostPath.getPathChoiceProbability() * demand, applyExpTransform);
      // low cost path based dPerceivedCost/dFlow this required the derivative of the perceived cost related to the applied path choice model
      double lowCostPathDenominator = stochasticPathChoice.getChoiceModel().computeDPerceivedCostDFlow(
              dpCostdFlows,  absCosts, 1 /*low cost */,lowCostPath.getPathChoiceProbability() * demand, applyExpTransform);
      // BOUND ENFORCEMENT - Part II
      // derivative of low cost path should never be steeper than that of the high-cost path (if it exists). In certain edge cases this may occur due to very low demands. In that case
      // we truncate to the high cost derivative as an upper bound
      if(highCostPathDenominator > 0) {
        lowCostPathDenominator = Math.min(lowCostPathDenominator, highCostPathDenominator);
      }

      var currHighCostDemand = highCostPath.getPathChoiceProbability() * demand;
      var currLowCostDemand = lowCostPath.getPathChoiceProbability() * demand;

      //6. NEWTON STEP: analytical equilibration of two paths based on their current cost and first derivative to determine flows/probabilities for i+1
      //   (adapted from Olga Perederieieva (2015) thesis)
      double newtonStepDenominator = highCostPathDenominator + lowCostPathDenominator;
      if(newtonStepDenominator < 0){
        LOGGER.severe("Negative step denominator, should never happen!");
        newtonStepDenominator = 0.0;
      }
      //   cost_high - step * dCost_high/d_Flow_high = cost_low - step * dCost_low/d_Flow_low
      //   rewrite towards step: step =  (cost_high - cost_low)/((dCost_high/d_Flow_high)+(dCost_low/d_Flow_low))
      double newtonStep = Math.min(currHighCostDemand,
              newtonStepDenominator != 0.0 ?
                      (highCostPathCurrPerceivedCost - lowCostPathCurrPerceivedCost) / newtonStepDenominator :
                      currHighCostDemand);

      // 7. Apply smoothing to step (capped at overall demand in case no derivatives were available for both options)
      var proposedLowCostDemand = currLowCostDemand + newtonStep;
      var proposedHighCostDemand = currHighCostDemand - newtonStep;

      if(origin.getId() == 0 && destination.getId() == 2){
        LOGGER.info(String.format(" [%d] -> [%d] ---- demand (%.8f, %.8f) ----Step proposed: %.8f, Step applied: %.8f)", highCostPathIndex, lowCostPathIndex,currLowCostDemand, currHighCostDemand, newtonStep, smoothing.execute(currLowCostDemand, proposedLowCostDemand)-currLowCostDemand));
      }

      double newLowCostPathProbability = Math.min(1, smoothing.execute(currLowCostDemand, proposedLowCostDemand)/demand);
      double newHighCostPathProbability = Math.max(0, smoothing.execute(currHighCostDemand, proposedHighCostDemand)/demand);

      //9. - prune path set if expectation is that high-cost path has become so unattractive it is not/negligibly used
      //     required as really low used paths may become unstable in their perceived costs due to low flow causing convergence problems
      if(newHighCostPathProbability < stochasticPathChoice.getRemovePathPobabilityThreshold()){
        pathsToRemove.add(highCostPath);
        newLowCostPathProbability += newHighCostPathProbability;
        newHighCostPathProbability = 0.0;
      }

      //10. update new probabilities for i + 1 iteration
      {
        // update probabilities applied, so they are available for the next iteration
        lowCostPath.setPathChoiceProbability(newLowCostPathProbability);
        highCostPath.setPathChoiceProbability(newHighCostPathProbability);
      }

      ++pairIndex;
    }

    pathsToRemove.forEach(p -> odPaths.remove(p));

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

    // initialise path filtering setup
    initialiseSltmPathFilters();
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

      /* DERIVATIVES per link segment (so we can construct newton step) */
      double[] dCostDFlow = this.constructLinkBasedDCostDFlow(theMode, updateOnlyPotentiallyBlockingNodeCosts);

      // prep
      final var smoothing = getSmoothing();
      final var gapFunction = (PathBasedGapFunction) getTrafficAssignmentComponent(GapFunction.class);

      final var stochasticPathChoice = (StochasticPathChoice) getPathChoice(); // only type of path choice which is verified (if present), so safe to cast
      if(stochasticPathChoice == null) {
        return false;
      }

      /* EXPAND OD PATH SETS WHEN ELIGIBLE NEW PATH FOUND */
      final OdPaths<StaticLtmDirectedPath> newOdPaths = (iterationIndex < 50) ? createOdPaths(costsToUpdate) : null;

      getOdDemands().forEachNonZeroOdDemand(getTransportNetwork().getZoning().getOdZones(),
              (o, d, demand) -> {

                var odPaths =  odMultiPaths.getValue(o, d);

                boolean newPathAdded = false;
                if(newOdPaths != null) {
                  var newOdPath = newOdPaths.getValue(o, d);

                  /* FILTER to see if new path is indeed eligible */
                  if(sLtmPathFilters.stream().allMatch(p -> p.test(newOdPath, odPaths))){
                    // valid new path, add to set
                    odPaths.add(newOdPath);
                    newPathAdded = true;
                  }
                }

                /* redistribute flows given current OD pathset */
                this.updateOdPathProbabilities(
                        o, d, odPaths, newPathAdded,
                        costsToUpdate, dCostDFlow,
                        stochasticPathChoice, smoothing, gapFunction, demand);
              });
      if(getSettings().isDetailedLogging()){
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

    /* gap function check */
    PlanItRunTimeException.throwIf(!(gapFunction instanceof PathBasedGapFunction),
            "Static LTM with paths requires path based gap function, but found %s", gapFunction.getClass().getCanonicalName());

    var pathChoice = getPathChoice();
    if(pathChoice==null && gapFunction.getStopCriterion().getMaxIterations()>1){
      throw new PlanItRunTimeException("Path-based sLTM assignment has no Path Choice defined, when running multiple iterations this is a requirement");
    }

    if(pathChoice!=null && !(pathChoice instanceof StochasticPathChoice)){
      throw new PlanItRunTimeException("Path-based sLTM assignment currently only supports Stochastic Path Choice, but found %s", pathChoice.getComponentType());
    }

  }

}
