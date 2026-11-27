package org.goplanit.network.layer.macroscopic;

import java.util.function.BiConsumer;

import org.goplanit.network.layer.BannedTurnsImpl;
import org.goplanit.utils.graph.directed.BannedTurn;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurnFactory;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurns;

/**
 * Container for the banned turns of a macroscopic layer, whose factory creates banned turns that name the modes they
 * ban
 *
 * @author markr
 */
public class MacroscopicBannedTurnsImpl extends BannedTurnsImpl implements MacroscopicBannedTurns {

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public MacroscopicBannedTurnsImpl(final IdGroupingToken groupId) {
    super(groupId, MacroscopicBannedTurnFactoryImpl::new);
  }

  /**
   * Copy constructor, also creates a new factory with this as its underlying container
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy (may be null)
   */
  public MacroscopicBannedTurnsImpl(
      MacroscopicBannedTurnsImpl other, boolean deepCopy, BiConsumer<BannedTurn, BannedTurn> mapper) {
    super(other, deepCopy, mapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicBannedTurnFactory getFactory() {
    return (MacroscopicBannedTurnFactory) super.getFactory();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicBannedTurnsImpl shallowClone() {
    return new MacroscopicBannedTurnsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicBannedTurnsImpl deepClone() {
    return new MacroscopicBannedTurnsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicBannedTurnsImpl deepCloneWithMapping(BiConsumer<BannedTurn, BannedTurn> mapper) {
    return new MacroscopicBannedTurnsImpl(this, true, mapper);
  }
}
