package org.goplanit.assignment.ltm.sltm;

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

  /** flag indicating of storage constraints (spillback) are disabled */
  private Boolean disableStorageConstraints = null;

  /** flag indicating if detailed logging is enabled */
  private Boolean detailedLogging = null;

  /** gap epsilon used to identify network loading convergence on flow acceptance factors and main gap indicator for
   *  iterative schedule of sLTM in basic setup */
  private Double networkLoadingFlowAcceptanceGapEpsilon = DEFAULT_NETWORK_LOADING_GAP_EPSILON;

  /** gap epsilon which is only relevant when using extended iterative schedule of sLTM */
  private Double networkLoadingSendingFlowGapEpsilon = DEFAULT_NETWORK_LOADING_GAP_EPSILON;

  /** gap epsilon which is only relevant when using extended iterative schedule of sLTM */
  private Double networkLoadingReceivingFlowGapEpsilon = DEFAULT_NETWORK_LOADING_GAP_EPSILON;

  /**
   * Track ods in this container for extended logging (if any)
   */
  private Map<IdMapperType, Map<String, Set<String>>> trackedOds = new HashMap<>();


  /** flag indicating the type of sLTM assignment to apply, bush or path based */
  private StaticLtmType sLtmType = DEFAULT_SLTM_TYPE;

  /**
   * flag indicating what to do when cost and derivative of cost on PAS alternative is equal, yet the flows are not. When false, this is considered a solution, when true an attempt
   * is made to proportionally distribute flows across PAS alternatives to obtain a unique solution. The former is faster, the latter gives a consistent result.
   */
  private Boolean enforceMaxEntropyFlowSolution = ENFORCE_FLOW_PROPORTIONAL_SOLUTION_DEFAULT;

  /** default setting for assignment is to apply a path based rather than an origin-based bush-based type of implementation*/
  //public static StaticLtmType DEFAULT_SLTM_TYPE = StaticLtmType.DESTINATION_BUSH_BASED;
  public static StaticLtmType DEFAULT_SLTM_TYPE = StaticLtmType.PATH_BASED;

  /** default setting for enforcing a flow proportional solution when possible */
  public static boolean ENFORCE_FLOW_PROPORTIONAL_SOLUTION_DEFAULT = false;

  /** default network loading gap epsilon to apply */
  public static double DEFAULT_NETWORK_LOADING_GAP_EPSILON = 0.001;

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

  public Boolean isEnforceMaxEntropyFlowSolution() {
    return enforceMaxEntropyFlowSolution;
  }

  public void setEnforceMaxEntropyFlowSolution(Boolean enforceMaxEntropyFlowSolution) {
    this.enforceMaxEntropyFlowSolution = enforceMaxEntropyFlowSolution;
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

}
