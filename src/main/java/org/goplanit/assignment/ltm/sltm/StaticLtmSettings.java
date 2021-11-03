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

  /** flag indicating to apply bush based assignment, or path based, default is true meaning bush based */
  private Boolean bushBased = BUSH_BASED_DEFAULT;

  /**
   * Constructor
   */
  public StaticLtmSettings() {
  }

  /**
   * Copy constructor
   * 
   * @param staticLtmSettings
   */
  public StaticLtmSettings(StaticLtmSettings staticLtmSettings) {
    this.bushBased = staticLtmSettings.bushBased.booleanValue();
    this.detailedLogging = staticLtmSettings.detailedLogging.booleanValue();
    this.disableStorageConstraints = staticLtmSettings.disableStorageConstraints.booleanValue();
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
    return bushBased;
  }

  public void setBushBased(Boolean flag) {
    this.bushBased = flag;
  }

  /** default setting for assignment is to apply a bush-based type of implementation over a path based one */
  public static boolean BUSH_BASED_DEFAULT = true;

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
   * {@inhericDoc}
   */
  @Override
  public StaticLtmSettings clone() {
    return new StaticLtmSettings(this);
  }
}
