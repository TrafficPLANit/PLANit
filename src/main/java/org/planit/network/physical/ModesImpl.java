package org.planit.network.physical;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Modes;

/**
 * Implementation of the Modes interface to create and register modes on itself
 * 
 * @author mark
 *
 */
public class ModesImpl implements Modes {
  /**
   * Map to store modes by their Id
   */
  private final Map<Long, Mode> modeMap = new TreeMap<Long, Mode>();
  
  /**
   * create id's for modes based on this group token
   */
  private final IdGroupingToken groupId;
  
  /** Constructor 
   * 
   * @param groupId to generated id's within this group
   */
  public ModesImpl(IdGroupingToken groupId) {
    this.groupId = groupId;
  }

  /**
   * Add mode to the internal container
   *
   * @param mode to be registered in this network
   * @return mode, in case it overrides an existing mode, the removed mode is returned
   */
  protected Mode registerMode(final Mode mode) {
    return modeMap.put(mode.getId(), mode);
  }

  /**
   * Iterator over available modes
   */
  @Override
  public Iterator<Mode> iterator() {
    return modeMap.values().iterator();
  }

  /**
   * Create and register new mode
   *
   * @param externalModeId the external mode id for the mode
   * @param name           of the mode
   * @param pcu            value for the mode
   * @return new mode created
   */
  @Override  
  public Mode registerNew(final long externalModeId, final String name, final double pcu) {
    final Mode newMode = new ModeImpl(groupId, externalModeId, name, pcu);
    registerMode(newMode);
    return newMode;
  }

  /**
   * Return number of registered modes
   *
   * @return number of registered modes
   */
  @Override  
  public int size() {
    return modeMap.size();
  }

  /**
   * Return a Mode by its id
   * 
   * @param id the id of the Mode
   * @return the specified mode
   * 
   */
  @Override  
  public Mode get(long id) {
    return modeMap.get(id);
  }

  /**
   * Collect the first registered mode
   * 
   * @return first registered mode if any
   */
  @Override  
  public Mode getFirst() {
    return get(0);
  }

  /**
   * Retrieve a Mode by its external Id
   * 
   * This method has the option to convert the external Id parameter into a long value, to find the mode when mode objects use long values for external ids.
   * 
   * @param externalId    the external Id of the specified mode
   * @param convertToLong if true, the external Id is converted into a long before beginning the search
   * @return the retrieved mode, or null if no mode was found
   */
  @Override  
  public Mode getByExternalId(Object externalId, boolean convertToLong) {
    if (convertToLong) {
      try {
        long value = Long.valueOf(externalId.toString());
        return getByExternalId(value);
      } catch (NumberFormatException e) {
        // do nothing - if conversion to long is not possible, use general method instead
      }
    }
    return getByExternalId(externalId);
  }

  /**
   * Retrieve a Mode by its external Id
   * 
   * This method is not efficient, since it loops through all the registered modes in order to find the required time period. The equivalent method in InputBuilderListener is
   * more efficient and should be used in preference to this in Java code.
   * 
   * @param externalId the external Id of the specified mode
   * @return the retrieved mode, or null if no mode was found
   */
  public Mode getByExternalId(Object externalId) {
    for (Mode mode : modeMap.values()) {
      if (mode.getExternalId().equals(externalId)) {
        return mode;
      }
    }
    return null;
  }
}
