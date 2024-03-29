package org.goplanit.mode;

import java.util.function.BiConsumer;
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
   * Copy Constructor, also creates a new factory with reference to this container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   */
  public ModesImpl(ModesImpl other, boolean deepCopy, BiConsumer<Mode,Mode> mapper) {
    super(other, deepCopy, mapper);
    this.modeFactory = new ModeFactoryImpl(other.modeFactory.getIdGroupingToken(), this);
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
    return (PredefinedMode) getMap().values().stream().dropWhile(mode -> !((mode instanceof PredefinedMode) && mode.getName().equals(modeType.value()))).findFirst().orElse(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mode getByXmlId(String xmlId) {
    return firstMatch(mode -> xmlId.equals(mode.getXmlId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModesImpl shallowClone() {
    return new ModesImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModesImpl deepClone() {
    return new ModesImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModesImpl deepCloneWithMapping(BiConsumer<Mode,Mode> mapper) {
    return new ModesImpl(this, true, mapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModeFactory getFactory() {
    return modeFactory;
  }

}
