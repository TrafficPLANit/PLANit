package org.planit.mode;

import java.util.logging.Logger;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntitiesImpl;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.ModeFactory;
import org.planit.utils.mode.Modes;
import org.planit.utils.mode.PredefinedMode;
import org.planit.utils.mode.PredefinedModeType;

/**
 * Implementation of the Modes interface to create and register modes
 * 
 * @author mark
 *
 */
public class ModesImpl extends ManagedIdEntitiesImpl<Mode> implements Modes {

  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ModesImpl.class.getCanonicalName());

  /** factory to create mode instances */
  private final ModeFactory modeFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ModesImpl(final IdGroupingToken groupId) {
    super(Mode::getId);
    this.modeFactory = new ModeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param modeFactory factory to use
   */
  public ModesImpl(ModeFactory modeFactory) {
    super(Mode::getId);
    this.modeFactory = modeFactory;
  }

  /**
   * Copy Constructor
   * 
   * @param modesImpl to copy
   */
  public ModesImpl(ModesImpl modesImpl) {
    super(modesImpl);
    this.modeFactory = modesImpl.modeFactory;
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
   * {@inheritDoc}
   */
  @Override
  public Mode getByXmlId(String xmlId) {
    return findFirst(mode -> xmlId.equals(((Mode) mode).getXmlId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModesImpl clone() {
    return new ModesImpl(this);
  }

}
