package org.goplanit.assignment.ltm.sltm;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathOneToAll;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingPath;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.goplanit.choice.ChoiceModel;
import org.goplanit.choice.logit.BoundedMultinomialLogit;
import org.goplanit.cost.CostUtils;
import org.goplanit.demands.Demands;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.PathBasedGapFunction;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.od.path.OdMultiPaths;
import org.goplanit.od.path.OdMultiPathsHashed;
import org.goplanit.od.path.OdPaths;
import org.goplanit.od.path.OdPathsHashed;
import org.goplanit.od.skim.OdSkimMatrix;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.path.choice.PathChoice;
import org.goplanit.path.choice.StochasticPathChoice;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.Movement;
import org.goplanit.utils.od.OdData;
import org.goplanit.utils.od.OdHashedImpl;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.PathUtils;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.OdZones;
import org.goplanit.zoning.Zoning;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiPredicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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

  // when false, seems to work better with MNL, but others bounded/weibit not tested properly, true should be more stable
  // but makes duality gap more difficult to interpret as it is all exp transformed
  private static final boolean APPLY_EXP_TRANSFORM = false;

  /** initial capacity used for list of paths per OD */
  private static final int INITIAL_PER_OD_PATH_CAPACITY = 3;

  /** List of filters in form of predicates to apply when checking if a newly created path is eligible for inclusion in the set */
  List<BiPredicate<ManagedDirectedPath, Collection<? extends ManagedDirectedPath>>> sLtmPathFilters = new ArrayList<>();

  /** when relative scaling factors are used, they are stored here, so they can be fed to the choice model at the
   * appropriate time */
  protected final Map<Mode, OdData<Double>> odRelativeScalingFactorsByMode = new TreeMap<>();

  /** standard filter on newly created paths checking the path is not equal to any existing path in the set.
   * Note we assume that all provided paths are of type StaticLtmDirectedPath, if not the call will crash */
  private static final BiPredicate<ManagedDirectedPath, Collection<? extends ManagedDirectedPath>> NEW_PATH_NOT_EQUAL_TO_EXISTING_PATHS =
          (newPath, existingOdPaths) -> existingOdPaths.stream().noneMatch(
                  existingPath -> ((StaticLtmDirectedPath)existingPath).getLinkSegmentsOnlyHashCode() == ((StaticLtmDirectedPath)newPath).getLinkSegmentsOnlyHashCode());

  /**
   * Convenience method for logging tracked Od paths, only log when od is tracked
   *
   * @param origin at hand
   * @param destination at hand
   * @param path path to log if tracked
   */
  private void logTrackedOdPath(OdZone origin, OdZone destination, StaticLtmDirectedPath path) {
    if(getSettings().hasTrackOdsForLogging() && getSettings().isTrackOdForLogging(origin, destination)) {
      LOGGER.info(String.format("-------------------- [ Origin (%s) Destination (%s) ]-------------------------------", origin.getIdsAsString(), destination.getIdsAsString()));
      LOGGER.info(String.format("new path added - path nodes: %s", IterableUtils.asStream(path).map(e -> "(" + e.getDownstreamVertex().getIdsAsString() + ")").collect(Collectors.joining(","))));
    }
  }

  /**
   * Create an OD cost skim
   *
   * @param mode mode to create skim for
   * @param iterationData data to use, containing link segment costs
   * @return created kim matrix
   */
  private OdSkimMatrix createOdCostSkimMatrix(Mode mode, StaticLtmSimulationData iterationData) {
    var odPaths = getOdMultiPaths(mode);
    var odZones = getTransportNetwork().getZoning().getOdZones();
    var skimMatrix = new OdSkimMatrix(odZones, OdSkimSubOutputType.COST);
    var linkSegmentCosts = iterationData.getLinkSegmentTravelTimePcuH(mode);

    odZones.forEachOriginDestination( (o,d) -> {
      var currOdPaths = odPaths.getValue(o,d);

      /* cost unknown if no paths exist */
      // todo: create shortest path between OD based on latest cost if this is desirable
      if(currOdPaths == null || currOdPaths.isEmpty()){
        skimMatrix.setValue(o,d, Double.NaN);
        return;
      }

      double[] currAbsolutePathCosts = PathUtils.computeEdgeSegmentAdditiveValues(currOdPaths, linkSegmentCosts);
      double[] currCostRelatedPathProbabilities = currOdPaths.stream().map(StaticLtmDirectedPath::getPathChoiceProbability).mapToDouble(v -> v).toArray();

      double weightedOdCost = ArrayUtils.dotProduct(currAbsolutePathCosts, currCostRelatedPathProbabilities, currAbsolutePathCosts.length);
      skimMatrix.setValue(o,d, weightedOdCost);
    });
    return skimMatrix;
  }

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

    gapFunction.increaseMinimumPathCosts(minPerceivedCost, odDemand); // traditional approach of min cost in denominator
    int index = 0;
    for(var path : odPaths){
      double pathCost = index == minPathCostIndex ? minPerceivedCost : perceivedPathCosts[index];
      var pathCostToUse = Math.min(perceivedCostUpperBound, pathCost);
      var odPathDemandToUse = odDemand * path.getPathChoiceProbability();

      // Adopting relative gap from rasmussen where the denominator is NOT based on min cost but just the current perceived cost. This avoids having really large gaps
      // in earlier iterations while it converges to the same final gap at convergence regardless
      //gapFunction.increaseMinimumPathCosts(pathCostToUse, odPathDemandToUse);

      // numerator
      gapFunction.increaseAbsolutePathGap(pathCostToUse, odPathDemandToUse, minPerceivedCost);
      ++index;
    }
  }

  /**
   * Create the relative scaling factors for each OD based on the cheapest cost path for that OD
   * <p>
   *   requires at least one od path per od with demand to exist
   * </p>
   *
   * @param mode related to the provided costs
   * @param linkSegmentCosts costs to use for constructing the path costs
   */
  private void updateOdRelativeScalingFactors(Mode mode, double[] linkSegmentCosts) {
    final var sueChoiceModel = ((StochasticPathChoice) getPathChoice()).getChoiceModel();
    if(sueChoiceModel.getComponentType() == ChoiceModel.WEIBIT){
      LOGGER.info(String.format("Skip construction of relative scaling factors for mode %s (%s) - Weibit choice model not compatible with this approach", mode.getName(), mode.getIdsAsString()));
      return;
    }

    final var modeOdMultiPaths = getOdMultiPaths(mode);
    if(odRelativeScalingFactorsByMode.containsKey(mode)){
      LOGGER.warning(String.format(
              "Expected relative scaling factors for mode (%s) to not already been populated, overwriting pre-existing entry"));
    }
    var relativeScalingFactors = new OdHashedImpl<>(getIdGroupingToken(), Double.class, getTransportNetwork().getZoning().getOdZones());

    final int maxRelScalingFactorUnderExpTransform = 20;
    final LongAdder countTruncatedRelativeScalingFactors = new LongAdder();

    /* populate OD multi-path container with single-path and set path probability to 1*/
    getOdDemands(mode).forEachNonZeroOdDemand(getTransportNetwork().getZoning().getOdZones(),
            (o, d, demand) -> {
              var odPaths = modeOdMultiPaths.getValue(o, d);
              double relativeScalingFactor = ((StochasticPathChoice) getPathChoice()).getChoiceModel().getScalingFactor();

              if(odPaths.size()>1) {
                var odPathCosts = PathUtils.computeEdgeSegmentAdditiveValues(modeOdMultiPaths.getValue(o, d), linkSegmentCosts);
                relativeScalingFactor = sueChoiceModel.computeRelativeScalingFactorGivenMinimumAlternativeCost(odPathCosts);

                if (APPLY_EXP_TRANSFORM && relativeScalingFactor > maxRelScalingFactorUnderExpTransform) {
                  relativeScalingFactor = maxRelScalingFactorUnderExpTransform;
                  countTruncatedRelativeScalingFactors.increment();
                }
              }
              relativeScalingFactors.setValue(o, d, (double) Math.max(1,Math.round(relativeScalingFactor)));

            });

    odRelativeScalingFactorsByMode.put(mode, relativeScalingFactors);
    if(getSettings().isDetailedLogging()) {
      LOGGER.info(String.format("Constructed relative scaling factors for mode %s (%s)", mode.getName(), mode.getIdsAsString()));
      if(countTruncatedRelativeScalingFactors.longValue() > 0) {
        LOGGER.warning(String.format("Truncated %d OD relative scaling factors due to max value of %d, to avoid exponent calculation overflow",
                countTruncatedRelativeScalingFactors.longValue(), maxRelScalingFactorUnderExpTransform));
      }
    }
  }

  /**
   * Create the initial OD paths and container based on the cheapest cost path for that OD
   *
   * @param mode related to the provided costs
   * @param odZones in the zoning
   * @param initialLinkSegmentCosts costs to use for constructing the path costs
   */
  private void createInitialOdPaths(Mode mode, OdZones odZones, double[] initialLinkSegmentCosts) {
    try {

      /* register multi-paths container on loading with initial paths included*/
      var odMultiPathsForMode = new OdMultiPathsHashed<StaticLtmDirectedPath, ArrayList<StaticLtmDirectedPath>>(
              getIdGroupingToken(), ArrayList.class, odZones);
      getLoading().setOdMultiPaths(mode, odMultiPathsForMode);

      /* create initial single-path for each OD */
      final var newOdPaths = createOdPaths(mode, initialLinkSegmentCosts);

      /* populate OD multi-path container with single-path and set path probability to 1*/
      getOdDemands(mode).forEachNonZeroOdDemand(getTransportNetwork().getZoning().getOdZones(),
              (o, d, demand) -> {
                var odMultiPathList = new ArrayList<StaticLtmDirectedPath>(INITIAL_PER_OD_PATH_CAPACITY);
                var initialOdPath = newOdPaths.getValue(o, d);
                initialOdPath.setPathChoiceProbability(1); // set current probability to 100%
                odMultiPathList.add(initialOdPath);         // add to path set
                odMultiPathsForMode.setValue(o, d, odMultiPathList);
                logTrackedOdPath(o, d, initialOdPath);

              });

    } catch (Exception e) {
      LOGGER.severe(String.format("Unable to create paths for initial solution of path-based sLTM %s", getAssignmentId()));
    }
  }

  /**
   * Create the od paths based on provided costs. Only create paths for od pairs with non-zero flow.
   *
   * @param mode related to the provided costs
   * @param currentSegmentCosts costs to use for the shortest path algorithm
   * @return newly created odPaths
   */
  private OdPaths<StaticLtmDirectedPath> createOdPaths(Mode mode, final double[] currentSegmentCosts) {
    final ShortestPathOneToAll shortestPathAlgorithm =
        new ShortestPathDijkstra(currentSegmentCosts, getTransportNetwork().getNumberOfVerticesAllLayers());

    StaticLtmDirectedPathFactory pathFactory =
        new StaticLtmDirectedPathFactory(getIdGroupingToken(), getSegmentToMovementMapping() );

    var newOdShortestPaths = new OdPathsHashed<>(
            getIdGroupingToken(), StaticLtmDirectedPath.class, getTransportNetwork().getZoning().getOdZones());

    Zoning zoning = getTransportNetwork().getZoning();
    OdDemands odDemands = getOdDemands(mode);
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
          var sltmPath = oneToAllResult.createPath(pathFactory, originVertex, destinationVertex);
          if (sltmPath == null) {
            LOGGER.warning(String.format("%sUnable to create path for OD [ o - (%s), d - (%s)] with non-zero demand (%.2f)",
                    LoggingUtils.runIdPrefix(getAssignmentId()), origin.getIdsAsString(), destination.getIdsAsString(), currOdDemand));
            oneToAllResult.createPath(pathFactory, originVertex, destinationVertex);
            continue;
          }
          newOdShortestPaths.setValue(origin, destination, sltmPath);
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
          OdZone origin,
          OdZone destination,
          List<StaticLtmDirectedPath> odPaths,
          boolean newPathAdded,
          double[] currLinkSegmentsCosts,
          double[] dCostDFlow,
          StochasticPathChoice stochasticPathChoice,
          Smoothing smoothing,
          PathBasedGapFunction gapFunction,
          double demand){

    if(odPaths.size() == 1 && !(getSettings().hasTrackOdsForLogging() && getSettings().isTrackOdForLogging(origin, destination))){
      return;
    }

    //1. get absolute and perceived costs for all paths
    double[] currAbsolutePathCosts = PathUtils.computeEdgeSegmentAdditiveValues(odPaths, currLinkSegmentsCosts);
    double[] currCostRelatedPathProbabilities =
            odPaths.stream().map(StaticLtmDirectedPath::getPathChoiceProbability).mapToDouble(v -> v).toArray();
    double[] currPerceivedPathCosts = stochasticPathChoice.computePerceivedPathCosts(
            currAbsolutePathCosts, currCostRelatedPathProbabilities, demand, APPLY_EXP_TRANSFORM);

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
                      maxAbsCostWithNonZeroProbability, currAbsolutePathCosts[lowestCostPathIndex], demand, APPLY_EXP_TRANSFORM);
    }

    updateGap(gapFunction, lowestCostPathIndex, currPerceivedPathCosts, perceivedCostUpperBoundForGap, currAbsolutePathCosts, odPaths, demand);

    if(getSettings().hasTrackOdsForLogging() && getSettings().isTrackOdForLogging(origin, destination)){
      LOGGER.info(String.format("-------------------- [ Origin (%s) Destination (%s) ]-------------------------------", origin.getIdsAsString(), destination.getIdsAsString()));
      LOGGER.info(String.format("absolute costs:                 %s", Arrays.toString(currAbsolutePathCosts)));
      LOGGER.info(String.format("perceived costs:                %s", Arrays.toString(currPerceivedPathCosts)));
      LOGGER.info(String.format("perceived costs (no transform): %s", Arrays.toString(stochasticPathChoice.computePerceivedPathCosts(currAbsolutePathCosts, currCostRelatedPathProbabilities, demand, !APPLY_EXP_TRANSFORM))));
      LOGGER.info(String.format("probabilities:                  %s", Arrays.toString(currCostRelatedPathProbabilities)));
      LOGGER.info(String.format("scaling factor:                 %.2f", stochasticPathChoice.getChoiceModel().getScalingFactor()));
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
      //todo: make configurable as this is a costly exercise yet for situations with paths sharing bottlenecks close to origin it is important to consider
      boolean onlyConsiderNonOverlappingLinks = true;
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
              dpCostdFlows, absCosts, 0 /*high cost */, highCostPath.getPathChoiceProbability() * demand, APPLY_EXP_TRANSFORM);
      // low cost path based dPerceivedCost/dFlow this required the derivative of the perceived cost related to the applied path choice model
      double lowCostPathDenominator = stochasticPathChoice.getChoiceModel().computeDPerceivedCostDFlow(
              dpCostdFlows,  absCosts, 1 /*low cost */,lowCostPath.getPathChoiceProbability() * demand, APPLY_EXP_TRANSFORM);
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
      }else if(Double.isNaN(newtonStepDenominator) || Double.isInfinite(newtonStepDenominator)){
        LOGGER.severe(String.format("step denominator is %.2f, should never happen!", newtonStepDenominator));

        stochasticPathChoice.getChoiceModel().computeDPerceivedCostDFlow(
                dpCostdFlows, absCosts, 0 /*high cost */, highCostPath.getPathChoiceProbability() * demand, APPLY_EXP_TRANSFORM);

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

      if(getSettings().hasTrackOdsForLogging() && getSettings().isTrackOdForLogging(origin,destination)){
        LOGGER.info(String.format(" [%d] -> [%d] ---- highcost path: abscost/dflow = %.8f, lowcost path abscost/dflow = %.8f)", highCostPathIndex, lowCostPathIndex, highCostPathDAbsoluteCostDFlow, lowCostPathDAbsoluteCostDFlow));
        LOGGER.info(String.format(" [%d] -> [%d] ---- highcost path: perccost/dflow = %.8f, lowcost path perccost/dflow = %.8f)", highCostPathIndex, lowCostPathIndex, highCostPathDenominator, lowCostPathDenominator));
        LOGGER.info(String.format(" [%d] -> [%d] ---- demand (%.8f, %.8f) ----Step proposed: %.8f, Step applied: %.8f)", highCostPathIndex, lowCostPathIndex, currLowCostDemand, currHighCostDemand, newtonStep, smoothing.execute(currLowCostDemand, proposedLowCostDemand)-currLowCostDemand));
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

    pathsToRemove.forEach(odPaths::remove);

  }

  /**
   * Convenience access to path choice component
   * @return path choice component
   */
  protected PathChoice getPathChoice(){
    return getTrafficAssignmentComponent(PathChoice.class);
  }

  /** create a path based network loading for this solution scheme
   *
   * @param segmentPair2MovementMap mapping from entry/exit segment (dual key) to movement, use to covert turn flows
   *  to splitting rate data format
   * @return created path based loading
   */
  @Override
  protected StaticLtmLoadingPath createNetworkLoading(MultiKeyMap<Object, Movement> segmentPair2MovementMap) {
    return new StaticLtmLoadingPath(getIdGroupingToken(), getAssignmentId(), segmentPair2MovementMap, getSettings());
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

    // initialise path filtering setup
    initialiseSltmPathFilters();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateTimePeriod(TimePeriod timePeriod, Set<Mode> modes, Demands demands) {
    super.updateTimePeriod(timePeriod, modes, demands);
    odRelativeScalingFactorsByMode.clear(); // re-initialise every iteration
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createInitialSolution(Mode mode, OdZones odZones, double[] initialLinkSegmentCosts, int iterationIndex) {

    createInitialOdPaths(mode, odZones, initialLinkSegmentCosts);

    if(getSettings().isActivateRelativeScalingFactor()) {
      updateOdRelativeScalingFactors(mode, initialLinkSegmentCosts);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean performIteration(
          final Mode mode, final double[] prevCosts, final double[] costsToUpdate, final StaticLtmSimulationData simulationData) {

    // prep
    final var smoothing = getSmoothing();
    final var gapFunction = (PathBasedGapFunction) getTrafficAssignmentComponent(GapFunction.class);
    final var stochasticPathChoice = (StochasticPathChoice) getPathChoice(); // only type of path choice which is verified (if present), so safe to cast
    if(stochasticPathChoice == null) {
      return false;
    }
    final var choiceModel = stochasticPathChoice.getChoiceModel();
    final var relativeScalingFactors = odRelativeScalingFactorsByMode.get(mode);
    final var originalChoiceModelScalingFactor = choiceModel.getScalingFactor();

    boolean success = true;
    try {
      /* NETWORK LOADING - MODE AGNOSTIC FOR NOW */
      executeNetworkLoading(mode);

      /* COST UPDATE */
      boolean updateOnlyPotentiallyBlockingNodeCosts = getLoading().getActivatedSolutionScheme().equals(StaticLtmLoadingScheme.POINT_QUEUE_BASIC);
      {
        if(simulationData.isInitialCostsAppliedInFirstIteration(mode) && simulationData.isFirstIteration()){
          /* initial costs will be inconsistent with loading performed in first iteration, recalculate all link segment costs for free flow conditions first
           * and then for those that need tracking override with flow based costs */
          CostUtils.populateModalFreeFlowPhysicalLinkSegmentCosts(
                  mode, getInfrastructureNetwork().getLayerByMode(mode).getLinkSegments(), costsToUpdate);
        }
        this.executeNetworkCostsUpdate(mode, updateOnlyPotentiallyBlockingNodeCosts, costsToUpdate);
      }

      /* DERIVATIVES per link segment (so we can construct Newton step) */
      double[] dCostDFlow = this.constructLinkBasedDCostDFlow(mode, updateOnlyPotentiallyBlockingNodeCosts);

      // RELATIVE SCALING FACTOR update
      {
        if(getSettings().isActivateRelativeScalingFactor()) {
          int lastIterationToUpdate = getSettings().getDisableRelativeScalingFactorUpdateAfterIteration();
          if(simulationData.getIterationIndex() < lastIterationToUpdate){
            odRelativeScalingFactorsByMode.clear();
            updateOdRelativeScalingFactors(mode, costsToUpdate);
          }else if(simulationData.getIterationIndex() == lastIterationToUpdate){
            LOGGER.info("Stopping relative scaling factor update retaining most recent for remainder of simulation, cut-off reached");
          }
        }
      }

      /* EXPAND OD PATH SETS WHEN ELIGIBLE NEW PATH FOUND */
      boolean stopPathGeneration = simulationData.getIterationIndex() > getSettings().getDisablePathGenerationAfterIteration();
      final OdPaths<StaticLtmDirectedPath> newOdPaths = stopPathGeneration ? null : createOdPaths(mode, costsToUpdate);
      final var odMultiPathsForMode = getOdMultiPaths(mode);
      final LongAdder numNewPaths = new LongAdder();
      getOdDemands(mode).forEachNonZeroOdDemand(getTransportNetwork().getZoning().getOdZones(),
              (o, d, demand) -> {

                var odPaths =  odMultiPathsForMode.getValue(o, d);

                boolean newPathAdded = false;
                if(newOdPaths != null) {
                  var newOdPath = newOdPaths.getValue(o, d);

                  /* FILTER to see if new path is indeed eligible */
                  if(sLtmPathFilters.stream().allMatch(p -> p.test(newOdPath, odPaths))){
                    // valid new path, add to set
                    odPaths.add(newOdPath);
                    newPathAdded = true;
                    numNewPaths.increment();

                    logTrackedOdPath(o, d, newOdPath);
                  }
                }

                double odScalingFactor =
                        relativeScalingFactors!= null ? relativeScalingFactors.getValue(o, d) : choiceModel.getScalingFactor();
                choiceModel.setScalingFactor(odScalingFactor);

                /* redistribute flows given current OD pathset */
                this.updateOdPathProbabilities(
                        o, d, odPaths,
                        newPathAdded,
                        costsToUpdate,
                        dCostDFlow,
                        stochasticPathChoice,
                        smoothing,
                        gapFunction,
                        demand);
              });
      if(getSettings().isDetailedLogging()){
        if(!stopPathGeneration){
          LOGGER.info(String.format("Added %d new paths", numNewPaths.longValue()));
        }
        LOGGER.info(String.format("Iteration path choice update complete", numNewPaths.longValue()));
      }

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("Unable to complete sLTM iteration");
      if (getSettings().isDetailedLogging()) {
        e.printStackTrace();
      }
      success = false;
    }finally {
      choiceModel.setScalingFactor(originalChoiceModelScalingFactor);
    }
    return success;
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

  /**
   * After each iteration this method can be used to construct on-the-fly skim matrices
   *
   * @param odSkimOutputType the type of skim
   * @param mode             mode to create for
   * @param iterationData   data to use such as link segment travel times
   * @return created skim matrix, null if not supported
   */
  @Override
  public OdSkimMatrix createOdSkimMatrix(OdSkimSubOutputType odSkimOutputType, Mode mode, StaticLtmSimulationData iterationData) {
    switch (odSkimOutputType){
      case COST:
        return createOdCostSkimMatrix(mode, iterationData);
      default:
        LOGGER.severe(String.format(
                "Unknown OD skim type to create in sLTM path-based fr mode (%s), ignored", mode.getIdsAsString() ));
    }
    return null;
  }

  /**
   * Access to current od multi-paths for a given mode
   *
   * @param mode to use
   * @return the current od multi-paths for the given mode
   */
  public OdMultiPaths<StaticLtmDirectedPath,? extends List<StaticLtmDirectedPath>> getOdMultiPaths(Mode mode){
    return getLoading().getOdMultiPaths(mode);
  }


}
