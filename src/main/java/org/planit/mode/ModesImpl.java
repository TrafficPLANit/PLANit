package org.planit.mode;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.Modes;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.PredefinedMode;
import org.planit.utils.mode.PredefinedModeType;
import org.planit.utils.mode.UsabilityModeFeatures;

/**
 * Implementation of the Modes interface to create and register modes on itself
 * 
 * @author mark
 *
 */
public class ModesImpl implements Modes {

  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ModesImpl.class.getCanonicalName());
  /**
   * Map to store modes by their Id
   */
  private final Map<Long, Mode> modeMap = new TreeMap<Long, Mode>();

  /**
   * create id's for modes based on this group token
   */
  private final IdGroupingToken groupId;

  /**
   * Constructor
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
   * {@inheritDoc}
   */
  @Override
  public Mode registerNewCustomMode(Object externalModeId, String name, double maxSpeed, double pcu, PhysicalModeFeatures physicalFeatures,
      UsabilityModeFeatures usabilityFeatures) {
    final Mode newMode = new ModeImpl(groupId, externalModeId, name, maxSpeed, pcu, physicalFeatures, usabilityFeatures);
    registerMode(newMode);
    return newMode;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public PredefinedMode registerNew(PredefinedModeType modeType) throws PlanItException {
    PredefinedMode theMode = null;
    if (!containsPredefinedMode(modeType)) {
      theMode = ModeFactory.createPredefinedMode(groupId, modeType);
      registerMode(theMode);
    } else {
      theMode = getPredefinedMode(modeType);
    }
    return theMode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return modeMap.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mode get(long id) {
    return modeMap.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsPredefinedMode(PredefinedModeType modeType) {
    return modeMap.values().stream().anyMatch(mode -> (mode instanceof PredefinedMode) && mode.getName().equals(modeType.value()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PredefinedMode getPredefinedMode(PredefinedModeType modeType) {
    return (PredefinedMode) modeMap.values().stream().dropWhile(mode -> !((mode instanceof PredefinedMode) && mode.getName().equals(modeType.value()))).findFirst().get();
  }

  /**
   * {@inheritDoc}
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
   * This method is not efficient, since it loops through all the registered modes in order to find the required time period. The equivalent method in InputBuilderListener is more
   * efficient and should be used in preference to this in Java code.
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

  /**
   * {@inheritDoc}}
   */
  @Override
  public Collection<Mode> getAll() {
    return Set.copyOf(modeMap.values());
  }

}