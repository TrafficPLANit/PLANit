package org.planit.mode;

import java.util.HashMap;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.Modes;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.PredefinedMode;
import org.planit.utils.mode.PredefinedModeType;
import org.planit.utils.mode.UsabilityModeFeatures;
import org.planit.utils.wrapper.LongMapWrapperImpl;

/**
 * Implementation of the Modes interface to create and register modes on itself
 * 
 * @author mark
 *
 */
public class ModesImpl extends LongMapWrapperImpl<Mode> implements Modes {

  @SuppressWarnings("unused")

  private static final Logger LOGGER = Logger.getLogger(ModesImpl.class.getCanonicalName());

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
    super(new HashMap<Long, Mode>(), Mode::getId);
    this.groupId = groupId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mode registerNewCustomMode(String name, double maxSpeed, double pcu, PhysicalModeFeatures physicalFeatures, UsabilityModeFeatures usabilityFeatures) {
    final Mode newMode = new ModeImpl(groupId, name, maxSpeed, pcu, physicalFeatures, usabilityFeatures);
    register(newMode);
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
      register(theMode);
    } else {
      theMode = get(modeType);
    }
    return theMode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsPredefinedMode(PredefinedModeType modeType) {
    return getMap().values().stream().anyMatch(mode -> (mode instanceof PredefinedMode) && mode.getName().equals(modeType.value()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PredefinedMode get(PredefinedModeType modeType) {
    return (PredefinedMode) getMap().values().stream().dropWhile(mode -> !((mode instanceof PredefinedMode) && mode.getName().equals(modeType.value()))).findFirst().get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mode getFirst() {
    return get(0);
  }

  /**
   * Retrieve a Mode by its xml Id
   * 
   * This method is not efficient, since it loops through all the registered modes in order to find the required mode. The equivalent method in InputBuilderListener is more
   * efficient and should be used in preference to this in Java code.
   * 
   * @param xmlId the XML Id of the specified mode
   * @return the retrieved mode, or null if no mode was found
   */
  @Override
  public Mode getByXmlId(String xmlId) {
    return findFirst(mode -> xmlId.equals(((Mode) mode).getXmlId()));
  }

}
