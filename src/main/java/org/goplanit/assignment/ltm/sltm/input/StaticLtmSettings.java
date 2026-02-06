package org.goplanit.assignment.ltm.sltm.input;

import org.goplanit.assignment.ltm.sltm.common.StaticLtmType;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.goplanit.utils.functionalinterface.TriPredicate;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.zoning.OdZone;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * POJO Settings regarding the execution of the StaticLTM network loading instance it is used on
 * 
 * @author markr
 */
public class StaticLtmSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmSettings.class.getCanonicalName());

  /** allow user to override the initial solution scheme to apply, no default because if not configured the default
   *  depends on whether starage constraints are activated or not.
   */
  private StaticLtmLoadingScheme initialSltmLoadingScheme = StaticLtmLoadingScheme.NONE;

  /**
   * Track ods in this container for extended logging (if any)
   */
  private Map<IdMapperType, Map<String, Set<String>>> trackedOds = new HashMap<>();

  /** tracked ods are by default active unless overwritten */
  private boolean trackedOdsActive = true;

  /** flag indicating of storage constraints (spillback) are disabled */
  private Boolean disableStorageConstraints = null;

  /** flag indicating if detailed logging is enabled */
  private Boolean detailedLogging = null;

  // USER SETTINGS

  /** user configurable stoppage predicate for bush-based outer iterations in between network loading,
   * integer is current outer iteration, double is the global network convergence gap up to current traffic
   * assignment iteration found */
  private BiPredicate<Integer,Double> outerIterationStoppagePredicate = DEFAULT_OUTER_ITERATION_PREDICATE;

  /** user configurable smoothing function for bush-based outer iterations in between network loading,
   * integer is current outer iteration should return a factor between zero and one to adjust flow shifts with */
  Function<Integer,Double> outerIterationSmoothingFunction = DEFAULT_OUTER_ITERATION_SMOOTHING_FUNCTION;

  /** user configurable stoppage predicate for bush-based inner iterations in between network loading,
   * integer in predicate is current inner iteration, double is the current PAS gap, while
   * the last double is the global gap stoppage criterion one can use as a criterion
   */
  private TriPredicate<Integer,Double, Double> innerIterationStoppagePredicate = DEFAULT_INNER_ITERATION_PREDICATE;

  /** User configurable function for smoothing the flow shift for a PAS based on the pas importance ordering.
   * provides the index of the PAS and the relative location (between zero and 1) of the PAS in the importance ordering
   * (running from least important, to most important), expects to return a double between zero and one with which to
   * smooth the flow shift to apply
   */
  private BiFunction<Integer,Double, Double> innerIterationPasImportanceSmoothingFunction =
      DEFAULT_PAS_IMPORTANCE_SMOOTHING_FUNCTION;

  /** flag that ndicates if we process PASs in ascending order when conducting flow shifts (or not, so descending).
   * When ascending we go from least important to most important. */
  private boolean pasProcessingAscendingOrder = DEFAULT_IS_PAS_ORDER_DIRECTION_ASCENDING;

  /** flag indicating the type of sLTM assignment to apply, bush or path based */
  private StaticLtmType sLtmType = DEFAULT_SLTM_TYPE;

  /** gap epsilon used to identify network loading convergence on flow acceptance factors and main gap indicator for
   *  iterative schedule of sLTM in basic setup */
  private Double networkLoadingFlowAcceptanceGapEpsilon = DEFAULT_NETWORK_LOADING_GAP_EPSILON;

  /** gap epsilon which is only relevant when using extended iterative schedule of sLTM */
  private Double networkLoadingSendingFlowGapEpsilon = DEFAULT_NETWORK_LOADING_GAP_EPSILON;

  /** gap epsilon which is only relevant when using extended iterative schedule of sLTM */
  private Double networkLoadingReceivingFlowGapEpsilon = DEFAULT_NETWORK_LOADING_GAP_EPSILON;

  /** sLTM minimum iterations to apply to loading regardless of gap */
  private Integer networkLoadingMinIterations = DEFAULT_NETWORK_LOADING_MIN_ITERATIONS;

  // ---------------------- PATH BASED ONLY OPTIONS ----------------------------------------------------
  // todo: refactor in separate implementation proper at some point

  /** disable generation of any new paths after given iteration */
  private Integer disablePathGenerationAfterIteration = DEFAULT_DISABLE_PATH_GENERATION_AFTER_ITERATION;

  private boolean relativeScalingFactorActive = DEFAULT_RELATIVE_SCALING_FACTOR_ACTIVE;

  private Integer disableRelativeScalingFactorUpdateAfterIteration = DEFAULT_DISABLE_RELATIVE_SCALING_FACTOR_AFTER_ITERATION;

  // ---------------------- BUSH BASED ONLY OPTIONS ----------------------------------------------------
  // todo: refactor in separate implementation proper at some point

  // PUBLIC DEFAULTS

  /** default setting for assignment is to apply a path based rather than an origin-based bush-based type of implementation*/
  //public static StaticLtmType DEFAULT_SLTM_TYPE = StaticLtmType.DESTINATION_BUSH_BASED;
  public static StaticLtmType DEFAULT_SLTM_TYPE = StaticLtmType.PATH_BASED;

  /**
   * Default threshold for number of outer iterations in bush-based internal loops between loading is set to:
   * (-log_10(minNetworkGapSoFar)+1) * 4, so number of decimals in global convergence gap * 4 is the maximum we allow.
   * The idea being that with an ever tighter global gap, the inner gap needs to be better as well. to avoid spending
   * time there while we are far away from convergence we make it a function of the global convergence
   */
  public static BiPredicate<Integer,Double> DEFAULT_OUTER_ITERATION_PREDICATE =
      (outerIteration, minNetworkGapAsThreshold) ->
          outerIteration > ((int) -Math.log10(minNetworkGapAsThreshold)+1) * 4;

  /**
   * Default function for smoothing the flow shift for a PAS based on the outer iteration it occurs in. We use a simple MSA
   * approach with a Polyak power function.
   */
  public static Function<Integer,Double> DEFAULT_OUTER_ITERATION_SMOOTHING_FUNCTION =
      outerIteration -> 1.0/(Math.pow(outerIteration,0.66));

  /**
   * Default threshold for number of inner iterations in bush-based internal loops between loading is set to:
   * either a 100 or stop earlier when the current PAS gap drops below the global convergence gap epsilon set by the
   * user (the latter is passed in, so it is safe to change the predicate before this epsilon is configured by the user)
   */
  public static TriPredicate<Integer,Double, Double> DEFAULT_INNER_ITERATION_PREDICATE =
      (innerIteration, currPasGap, globalGapStopCriterionEpsilon) -> {
        boolean converged = currPasGap <= globalGapStopCriterionEpsilon;
        return (innerIteration > 100 || converged);
      };

  /**
   * Default function for smoothing the flow shift for a PAS based on the pas importance ordering. For now we use
   * an exponential decay function based on the relative location (between zero and 1) of the PAS in the importance
   * ordering (running from least important, to most important).
   */
  public static BiFunction<Integer,Double, Double> DEFAULT_PAS_IMPORTANCE_SMOOTHING_FUNCTION =
      (currPasCounterIndex, perPasPercentageOfTotal) ->
          Math.min(1, (1 - (Math.pow(0.01, (currPasCounterIndex * perPasPercentageOfTotal))) + 0.01));

  /** default order for PAS processing is in ascending order, from least important to most important */
  public static boolean DEFAULT_IS_PAS_ORDER_DIRECTION_ASCENDING = true;

  /** default network loading gap epsilon to apply */
  public static double DEFAULT_NETWORK_LOADING_GAP_EPSILON = 0.001;

  /** default network loading minimum iterations to apply */
  public static int DEFAULT_NETWORK_LOADING_MIN_ITERATIONS = 1;

  /** default for disabling path generation after a certain iteration */
  public static int DEFAULT_DISABLE_PATH_GENERATION_AFTER_ITERATION = Integer.MAX_VALUE;

  /** default for activating relative scaling factor path choice ODs */
  public static boolean DEFAULT_RELATIVE_SCALING_FACTOR_ACTIVE = true;

  /** default for disabling relative scaling factor update after a certain iteration */
  public static int DEFAULT_DISABLE_RELATIVE_SCALING_FACTOR_AFTER_ITERATION = 20;

  /**
   * Provide OD pair to track extended logging for during assignment for debugging purposes.
   * (currently only supported for path based sLTM assignment)
   *
   * @param type id mapping type to use
   * @param originIdAsString origin id in idType form
   * @param destinationIdAsString origin id in idType form
   */
  protected void addTrackOdForLogging(IdMapperType type, String originIdAsString, String destinationIdAsString){
    trackedOds.putIfAbsent(type, new HashMap<>());
    var trackedOdsForType = trackedOds.get(type);
    trackedOdsForType.putIfAbsent(originIdAsString, new HashSet<>());
    trackedOdsForType.get(originIdAsString).add(destinationIdAsString);
  }

  /**
   * Constructor
   */
  public StaticLtmSettings() {
  }

  /**
   * Copy constructor
   * 
   * @param staticLtmSettings to copy
   */
  public StaticLtmSettings(StaticLtmSettings staticLtmSettings) {
    setSltmType(staticLtmSettings.getSltmType());
    setBushBasedOuterIterationStopCheck(staticLtmSettings.getBushBasedOuterIterationStopCheck());
    setBushBasedOuterIterationSmoothingFunction(staticLtmSettings.getBushBasedOuterIterationSmoothingFunction());
    setBushBasedInnerIterationStopCheck(staticLtmSettings.getBushBasedInnerIterationStopCheck());
    setBushBasedPasOrderDirectionAscending(staticLtmSettings.isBushBasedPasOrderDirectionAscending());
    setNetworkLoadingFlowAcceptanceGapEpsilon(staticLtmSettings.getNetworkLoadingFlowAcceptanceGapEpsilon());
    setNetworkLoadingReceivingFlowGapEpsilon(staticLtmSettings.getNetworkLoadingReceivingFlowGapEpsilon());
    setNetworkLoadingSendingFlowGapEpsilon(staticLtmSettings.getNetworkLoadingSendingFlowGapEpsilon());
    setDisableStorageConstraints(staticLtmSettings.isDisableStorageConstraints());
    setDetailedLogging(staticLtmSettings.isDetailedLogging());
    setActivateRelativeScalingFactor(staticLtmSettings.isActivateRelativeScalingFactor());
    setDisablePathGenerationAfterIteration(staticLtmSettings.getDisablePathGenerationAfterIteration());
    setNetworkLoadingInitialScheme(staticLtmSettings.getNetworkLoadingInitialScheme());
    setNetworkLoadingMinIterations(staticLtmSettings.getNetworkLoadingMinIterations());
  }

  /**
   * Validate if all settings have been properly set and log found issues
   * 
   * @return true when valid, false otherwise
   */
  public boolean validate() {
    boolean valid = true;
    for(Field field : this.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      try {
        if (field.get(this) == null) {
          LOGGER.severe(String.format("%s has not been set as part of sLTM network loading settings, this should not happen", field.getName()));
          valid = false;
        }
      } catch (Exception e) {
        LOGGER.severe(String.format("Unable to collect field %s from class instance %s, this should not happen", field.getName(), this.getClass().getName()));
        e.printStackTrace();
        valid = false;
      }
    }
    return valid;
  }

  /**
   * Shallow copy
   *
   * @return  shallow copy
   */
  public StaticLtmSettings shallowClone() {
    return new StaticLtmSettings(this);
  }

  public Boolean isDisableStorageConstraints() {
    return disableStorageConstraints;
  }

  public void setDisableStorageConstraints(Boolean disableStorageConstraints) {
    this.disableStorageConstraints = disableStorageConstraints;
  }

  public Boolean isDetailedLogging() {
    return detailedLogging;
  }

  public void setDetailedLogging(Boolean detailedLogging) {
    this.detailedLogging = detailedLogging;
  }

  /**
   *  Stoppage predicate for bush-based outer iteration in between network loadings,
   *  integer in predicate is current outer iteration, double is the global network convergence gap up to current traffic
   *  assignment iteration found
   *
   * @param outerIterationPredicate to apply as stoppage check
   */
  public void setBushBasedOuterIterationStopCheck(BiPredicate<Integer,Double> outerIterationPredicate){
    this.outerIterationStoppagePredicate = outerIterationPredicate;
  }

  /**
   *  Stoppage predicate for bush-based outer iteration in between network loadings,
   *  integer in predicate is current outer iteration, double is the global network convergence gap up to current traffic
   *  assignment iteration found
   *
   * @return outerIterationPredicate applied as stoppage check
   */
  public BiPredicate<Integer,Double> getBushBasedOuterIterationStopCheck(){
    return this.outerIterationStoppagePredicate;
  }

  /**
   * Set outer iteration smoothing function based on the outer iteration index provided
   *
   * @param outerIterationSmoothingFunction integer is current iteration index, should return smoothing factor between
   *                                       zero and one
   */
  public void setBushBasedOuterIterationSmoothingFunction(Function<Integer,Double> outerIterationSmoothingFunction){
    this.outerIterationSmoothingFunction = outerIterationSmoothingFunction;
  }

  /** Outer iteration smoothing function based on the outer iteration index provided, when applied the returned value
   * is the factor applied to reduce the amount of flow shifted.
   *
   * @return function where integer is current iteration index, should return smoothing factor between
   *        zero and one
   */
  public Function<Integer,Double> getBushBasedOuterIterationSmoothingFunction(){
    return this.outerIterationSmoothingFunction;
  }

  /**
   *  Stoppage predicate for bush-based outer iteration in between network loadings,
   *  integer in predicate is current inner iteration,
   *  double is the current PAS gap, while
   *  the last double is the global gap stoppage criterion one can use as a criterion
   *
   *
   * @param innerIterationPredicate to apply as stoppage check
   */
  public void setBushBasedInnerIterationStopCheck(TriPredicate<Integer,Double, Double> innerIterationPredicate){
    this.innerIterationStoppagePredicate = innerIterationPredicate;
  }

  /**
   *  Stoppage predicate for bush-based inner iterations in between network loadings,
   *  integer in predicate is current inner iteration, double is the current PAS gap, whil
   *  the last double is the global gap stoppage criterion one can use as a criterion
   *
   * @return innerIterationPredicate applied as stoppage check
   */
  public TriPredicate<Integer,Double, Double> getBushBasedInnerIterationStopCheck(){
    return this.innerIterationStoppagePredicate;
  }

  /**
   * Set smoothing function based on the PAS importance in the ordered PAS list
   *
   * @param pasImportanceSmoothingFunction integer is current PAS index, double is the relative location frm the start
   *                                       of the ordered PAS list running between zero and 1. function should return
   *                                       smoothing factor between zero and one as well
   */
  public void setBushBasedPasImportanceSmoothingFunction(
      BiFunction<Integer,Double, Double> pasImportanceSmoothingFunction){
    this.innerIterationPasImportanceSmoothingFunction = pasImportanceSmoothingFunction;
  }

  /**
   * get smoothing function based on the PAS importance in the ordered PAS list
   *
   * @return bi function with integer is current PAS index, double is the relative location frm the start
   *         of the ordered PAS list running between zero and 1. function should return
   *         smoothing factor between zero and one as well
   */
  public BiFunction<Integer,Double, Double> getBushBasedPasImportanceSmoothingFunction(){
    return innerIterationPasImportanceSmoothingFunction;
  }

  /**
   * Indicate if we process PASs in ascending order when conducting flow shifts (or not, so descending).
   * When ascending we go from least important to most important.
   *
   * @param ascendingOrder flag
   */
  public void setBushBasedPasOrderDirectionAscending(Boolean ascendingOrder){
    this.pasProcessingAscendingOrder = ascendingOrder;
  }

  /**
   * Check if we process PASs in ascending order when conducting flow shifts (or not, so descending).
   * When ascending we go from least important to most important.
   *
   * @return current ascendingOrder flag
   */
  public Boolean isBushBasedPasOrderDirectionAscending(){
    return this.pasProcessingAscendingOrder;
  }

  public Boolean isBushBased() {
    return this.sLtmType != StaticLtmType.PATH_BASED;
  }

  public void setSltmType(StaticLtmType type) {
    this.sLtmType = type;
  }
  
  public StaticLtmType getSltmType() {
    return this.sLtmType;
  }

  public Double getNetworkLoadingFlowAcceptanceGapEpsilon() {
    return networkLoadingFlowAcceptanceGapEpsilon;
  }

  public void setNetworkLoadingFlowAcceptanceGapEpsilon(Double networkLoadingFlowAcceptanceGapEpsilon) {
    this.networkLoadingFlowAcceptanceGapEpsilon = networkLoadingFlowAcceptanceGapEpsilon;
  }

  public Double getNetworkLoadingSendingFlowGapEpsilon() {
    return networkLoadingSendingFlowGapEpsilon;
  }

  public void setNetworkLoadingSendingFlowGapEpsilon(Double networkLoadingSendingFlowGapEpsilon) {
    this.networkLoadingSendingFlowGapEpsilon = networkLoadingSendingFlowGapEpsilon;
  }

  public Double getNetworkLoadingReceivingFlowGapEpsilon() {
    return networkLoadingReceivingFlowGapEpsilon;
  }

  public void setNetworkLoadingReceivingFlowGapEpsilon(Double networkLoadingReceivingFlowGapEpsilon) {
    this.networkLoadingReceivingFlowGapEpsilon = networkLoadingReceivingFlowGapEpsilon;
  }

  public Integer getNetworkLoadingMinIterations() {
    return networkLoadingMinIterations;
  }

  public void setNetworkLoadingMinIterations(Integer networkLoadingMinIterations) {
    this.networkLoadingMinIterations = networkLoadingMinIterations;
  }

  /**
   * Get the chosen  initial SLTM loading scheme to start with. If not explicitly overwritten this method returns
   * {@code  StaticLtmLoadingScheme.NONE}, otherwise the value set. If not overwritten the loading will choose the appropriate
   * initial scheme depending on configuration.
   *
   * @return loading scheme if set explicitly
   */
  public StaticLtmLoadingScheme getNetworkLoadingInitialScheme() {
    return initialSltmLoadingScheme;
  }

  /**
   * Set a specific initial SLTM loading scheme to start with. Should be compatible with the storage constraints approach
   * chosen, i.e., you cannot set it to point queue when storage constraints are active.
   *
   * @param initialSltmLoadingScheme scheme to start with
   */
  public void setNetworkLoadingInitialScheme(StaticLtmLoadingScheme initialSltmLoadingScheme) {
    this.initialSltmLoadingScheme = initialSltmLoadingScheme;
  }

  /**
   * Provide OD pair to track extended logging for during assignment for debugging purposes.
   * (currently only supported for path based sLTM assignment)
   *
   * @param originId origin id in idType form
   * @param destinationId origin id in idType form
   */
  public void addTrackOdForLoggingById(Integer originId, Integer destinationId){
    addTrackOdForLogging(IdMapperType.ID, String.valueOf(originId), String.valueOf(destinationId));
  }

  /**
   * Provide OD pair to track extended logging for during assignment for debugging purposes.
   * (currently only supported for path based sLTM assignment)
   *
   * @param originId origin id in idType form
   * @param destinationId origin id in idType form
   */
  public void addTrackOdForLoggingByXmlId(String originId, String destinationId){
    addTrackOdForLogging(IdMapperType.XML, originId, destinationId);
  }

  /**
   * Provide OD pair to track extended logging for during assignment for debugging purposes.
   * (currently only supported for path based sLTM assignment)
   *
   * @param originId origin id in idType form
   * @param destinationId origin id in idType form
   */
  public void addTrackOdForLoggingByExternalId(String originId, String destinationId){
    addTrackOdForLogging(IdMapperType.EXTERNAL_ID, originId, destinationId);
  }


  /**
   * Check if any ods are marked for extended logging
   *
   * @return true if present, false otherwise
   */
  public boolean hasTrackOdsForLogging(){
    return !trackedOds.isEmpty();
  }

  /**
   * Check if a given od is marked for extended logging or not (will return false of tracking of ods is
   * disabled)
   *
   * @param originZone origin to check
   * @param destinationZone destination to check
   * @return true when tracked, false otherwise
   */
  public boolean isTrackOdForLogging(OdZone originZone, OdZone destinationZone){
    if(!trackedOdsActive){
      return false;
    }
    /* try each id type which has registered zones, if available... */
    for(var entry : trackedOds.entrySet()){
      var odsForType = entry.getValue();
      var originIdForType = originZone.getIdAsString(entry.getKey());
      if(!odsForType.containsKey(originIdForType)){
        continue;
      }

      /* verify id od pair is registered */
      var destinationIdForType = destinationZone.getIdAsString(entry.getKey());
      if(odsForType.get(originIdForType).contains(destinationIdForType)){
        return true;
      }
    }
    return false;
  }

  /**
   * Check if a given destination is marked for extended logging or not. (will return false of tracking of ods is
   * disabled)
   *
   * @param destinationZone destination to check
   * @return true when tracked, false otherwise
   */
  public boolean isTrackDestinationForLogging(OdZone destinationZone){
    if(!trackedOdsActive){
      return false;
    }
    /* try each id type which has registered zones, if available... */
    for(var entry : trackedOds.entrySet()){
      var odsForType = entry.getValue();
      /* verify id destination is registered for any origin */
      var destinationIdForType = destinationZone.getIdAsString(entry.getKey());
      if(odsForType.values().stream().flatMap(Collection::stream).anyMatch(dId -> dId.equals(destinationIdForType))){
        return true;
      }
    }
    return false;
  }

  /**
   * enable or disable tracking of ods (temporarily)
   *
   * @param activate flag to set
   */
  public void enableTrackedOds(boolean activate){
    this.trackedOdsActive = activate;
  }


  ///********************
  /// BUSH BASED SETTINGS
  ///********************

  ///********************
  /// PATH BASED SETTINGS
  ///********************

  /**
   * Path based only option
   * TODO: split out in separate settings time permitting
   * @return disablePathGenerationAfterIteration iteration number used
   */
  public Integer getDisablePathGenerationAfterIteration() {
    return disablePathGenerationAfterIteration;
  }

  /**
   * Path based only option. choose iteration after which we disable path generation
   * TODO: split out in separate settings time permitting
   * @param disablePathGenerationAfterIteration choose iteration after which we disable path generation
   */
  public void setDisablePathGenerationAfterIteration(Integer disablePathGenerationAfterIteration) {
    this.disablePathGenerationAfterIteration = disablePathGenerationAfterIteration;
  }

  /**
   * Path based only option. Choose whether scaling factor of path choice is to be made relative to the minimum cost
   * on each OD
   * TODO: split out in separate settings time permitting
   * @param flag to set
   */
  public void setActivateRelativeScalingFactor(Boolean flag){
    this.relativeScalingFactorActive = flag;
  }

  /**
   * Path based only option. Check whether scaling factor of path choice is to be made relative to the minimum cost
   * on each OD
   * TODO: split out in separate settings time permitting
   * @return flag set
   */
  public Boolean isActivateRelativeScalingFactor(){
    return relativeScalingFactorActive;
  }

  /**
   * Path based only option. If relative scaling factor is active, disable updating it each iteration after the given iteration
   * TODO: split out in separate settings time permitting
   * @param disableRelativeScalingFactorUpdateAfterIteration iteration to disable relative scaling factor update after
   */
  public void setDisableRelativeScalingFactorUpdateAfterIteration(Integer disableRelativeScalingFactorUpdateAfterIteration){
    this.disableRelativeScalingFactorUpdateAfterIteration = disableRelativeScalingFactorUpdateAfterIteration;
  }

  /**
   * Path based only option. Check setting regarding when relative scaling factor update would be disabled if active
   * TODO: split out in separate settings time permitting
   * @return iteration set
   */
  public Integer getDisableRelativeScalingFactorUpdateAfterIteration(){
    return this.disableRelativeScalingFactorUpdateAfterIteration;
  }

}
