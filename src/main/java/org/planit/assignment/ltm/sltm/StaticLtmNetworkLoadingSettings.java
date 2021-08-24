package org.planit.assignment.ltm.sltm;

import java.lang.reflect.Field;
import java.util.logging.Logger;

/**
 * POJO Settings regarding the execution of the StaticLTM network loading instance it is used on
 * 
 * @author markr
 */
public class StaticLtmNetworkLoadingSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmNetworkLoadingSettings.class.getCanonicalName());

  private Boolean disableStorageConstraints = null;

  private Boolean detailedLogging = null;

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
}
