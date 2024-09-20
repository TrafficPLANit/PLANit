package org.goplanit.assignment.ltm.sltm;

import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.zoning.OdZone;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

  /** flag indicating of storage constraints (spillback) are disabled */
  private Boolean disableStorageConstraints = null;

  /** flag indicating if detailed logging is enabled */
  private Boolean detailedLogging = null;

  // USER SETTINGS

  /** flag indicating the type of sLTM assignment to apply, bush or path based */
  private StaticLtmType sLtmType = DEFAULT_SLTM_TYPE;

  /** gap epsilon used to identify network loading convergence on flow acceptance factors and main gap indicator for
   *  iterative schedule of sLTM in basic setup */
  private Double networkLoadingFlowAcceptanceGapEpsilon = DEFAULT_NETWORK_LOADING_GAP_EPSILON;

  /** gap epsilon which is only relevant when using extended iterative schedule of sLTM */
  private Double networkLoadingSendingFlowGapEpsilon = DEFAULT_NETWORK_LOADING_GAP_EPSILON;

  /** gap epsilon which is only relevant when using extended iterative schedule of sLTM */
  private Double networkLoadingReceivingFlowGapEpsilon = DEFAULT_NETWORK_LOADING_GAP_EPSILON;

  // ---------------------- PATH BASED ONLY OPTIONS ----------------------------------------------------
  // todo: refactor in separate implementation proper at some point

  /** disable generation of any new paths after given iteration */
  private Integer disablePathGenerationAfterIteration = DEFAULT_DISABLE_PATH_GENERATION_AFTER_ITERATION;

  private boolean relativeScalingFactorActive = DEFAULT_RELATIVE_SCALING_FACTOR_ACTIVE;

  private Integer disableRelativeScalingFactorUpdateAfterIteration = DEFAULT_DISABLE_RELATIVE_SCALING_FACTOR_AFTER_ITERATION;

  // ---------------------- BUSH BASED ONLY OPTIONS ----------------------------------------------------
  // todo: refactor in separate implementation proper at some point

  /** indicating if we allow PASs to update even if another PAS in the same iteration overlapped to some
   * extent and was updated already, when set to false convergence is more stable but slower and still may cause flip-flopping
   * without smoothing, when true it is faster but will always require smoothing to avoid flip-flopping */
  private boolean allowOverlappingPasUpdate = DEFAULT_ALLOW_OVERLAPPING_PAS_UPDATE;

  /**
   * flag indicating what to do when cost and derivative of cost on PAS alternative is equal, yet the flows are not. When false, this is considered a solution, when true an attempt
   * is made to proportionally distribute flows across PAS alternatives to obtain a unique solution. The former is faster, the latter gives a consistent result.
   */
  private Boolean enforceMaxEntropyFlowSolution = ENFORCE_FLOW_PROPORTIONAL_SOLUTION_DEFAULT;

  // PUBLIC DEFAULTS

  /** default setting for assignment is to apply a path based rather than an origin-based bush-based type of implementation*/
  //public static StaticLtmType DEFAULT_SLTM_TYPE = StaticLtmType.DESTINATION_BUSH_BASED;
  public static StaticLtmType DEFAULT_SLTM_TYPE = StaticLtmType.PATH_BASED;

  /** default setting for enforcing a flow proportional solution when possible */
  public static boolean ENFORCE_FLOW_PROPORTIONAL_SOLUTION_DEFAULT = false;

  /** default setting indicating if we allow PASs to update even if another PAS in the same iteration overlapped to some
   * extent and was updated already, when set to false convergence is more stable but slower and still may cause flip-flopping
   * without smoothing, when true it is faster but will always require smoothing to avoid flip-flopping
   */
  public static boolean DEFAULT_ALLOW_OVERLAPPING_PAS_UPDATE = true;

  /** default network loading gap epsilon to apply */
  public static double DEFAULT_NETWORK_LOADING_GAP_EPSILON = 0.001;

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
    this.sLtmType = staticLtmSettings.sLtmType;
    this.detailedLogging = staticLtmSettings.detailedLogging.booleanValue();
    this.disableStorageConstraints = staticLtmSettings.disableStorageConstraints.booleanValue();

    // bush based setting
    this.enforceMaxEntropyFlowSolution = staticLtmSettings.enforceMaxEntropyFlowSolution.booleanValue();
  }

  /**
   * Validate if all settings have been properly set and log found issues
   * 
   * @return true when valid, false otherwise
   */
  public boolean validate() {
    boolean valid = true;
    Field[] fields = this.getClass().getDeclaredFields();
    for (int index = 0; index < fields.length; ++index) {
      Field field = fields[index];
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
   * Check if a given od is marked for extended logging or not
   *
   * @param originZone origin to check
   * @param destinationZone destination to check
   * @return true when tracked, false otherwise
   */
  public boolean isTrackOdForLogging(OdZone originZone, OdZone destinationZone){
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

  ///********************
  /// BUSH BASED SETTINGS
  ///********************

  /**
   * Bush based only option
   * TODO: split out in separate settings time permitting
   * @deprecated not supported properly do not use
   * @return enforceMaxEntropyFlowSolution flag set
   */
  @Deprecated
  public Boolean isEnforceMaxEntropyFlowSolution() {
    return enforceMaxEntropyFlowSolution;
  }

  /**
   * Bush based only option
   * TODO: split out in separate settings time permitting
   * @deprecated not supported properly do not use
   * @param enforceMaxEntropyFlowSolution when true force max entropy based approach
   */
  @Deprecated
  public void setEnforceMaxEntropyFlowSolution(Boolean enforceMaxEntropyFlowSolution) {
    this.enforceMaxEntropyFlowSolution = enforceMaxEntropyFlowSolution;
  }

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

  /**
   * Bush based only option. Check setting regarding flag on overlapping pas updates
   * TODO: split out in separate settings time permitting
   * @return flag set
   */
  public boolean isAllowOverlappingPasUpdate() {
    return allowOverlappingPasUpdate;
  }

  /**
   * Bush based only option. set flag regarding overlapping pas updates
   * TODO: split out in separate settings time permitting
   * @param flag to set
   */
  public void setAllowOverlappingPasUpdate(boolean flag) {
    this.allowOverlappingPasUpdate = flag;
  }
}
