package org.goplanit.mode;

import java.util.logging.Logger;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.ModeFactory;
import org.goplanit.utils.mode.Modes;
import org.goplanit.utils.mode.PredefinedMode;
import org.goplanit.utils.mode.PredefinedModeType;

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
    super(Mode::getId, Mode.MODE_ID_CLASS);
    this.modeFactory = new ModeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param modeFactory factory to use
   */
  public ModesImpl(ModeFactory modeFactory) {
    super(Mode::getId, Mode.MODE_ID_CLASS);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public ModeFactory getFactory() {
    return modeFactory;
  }

}
