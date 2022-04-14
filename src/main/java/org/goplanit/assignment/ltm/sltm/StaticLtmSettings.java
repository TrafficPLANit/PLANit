package org.goplanit.assignment.ltm.sltm;

import java.lang.reflect.Field;
import java.util.logging.Logger;

/**
 * POJO Settings regarding the execution of the StaticLTM network loading instance it is used on
 * 
 * @author markr
 */
public class StaticLtmSettings implements Cloneable {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmSettings.class.getCanonicalName());

  /** flag indicating of storage constraints (spillback) are disabled */
  private Boolean disableStorageConstraints = null;

  /** flag indicating if detailed logging is enabled */
  private Boolean detailedLogging = null;

  /** flag indicating tthe type of sLTM assignment to apply, bush or path based */
  private StaticLtmType sLtmType = DEFAULT_SLTM_TYPE;

  /**
   * flag indicating what to do when cost and derivative of cost on PAS alternative is equal, yet the flows are not. When false, this is considered a solution, when true an attempt
   * is made to proportionally distribute flows across PAS alternatives to obtain a unique solution. The former is faster, the latter gives a consistent result.
   */
  private Boolean enforceMaxEntropyFlowSolution = ENFORCE_FLOW_PROPORTIONAL_SOLUTION_DEFAULT;

  /** default setting for assignment is to apply an origin-based bush-based type of implementation over a path based one */
  public static StaticLtmType DEFAULT_SLTM_TYPE = StaticLtmType.DESTINATION_BUSH_BASED;

  /** default setting for enforcing a flow proportional solution when possible */
  public static boolean ENFORCE_FLOW_PROPORTIONAL_SOLUTION_DEFAULT = false;

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
   * {@inheritDoc}
   */
  @Override
  public StaticLtmSettings clone() {
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

  public Boolean isEnforceMaxEntropyFlowSolution() {
    return enforceMaxEntropyFlowSolution;
  }

  public void setEnforceMaxEntropyFlowSolution(Boolean enforceMaxEntropyFlowSolution) {
    this.enforceMaxEntropyFlowSolution = enforceMaxEntropyFlowSolution;
  }

}
