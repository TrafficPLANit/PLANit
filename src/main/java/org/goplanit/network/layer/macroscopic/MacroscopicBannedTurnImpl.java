package org.goplanit.network.layer.macroscopic;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.goplanit.network.layer.BannedTurnImpl;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurn;

/**
 * A banned turn on a macroscopic layer, banned for the modes it names, or for every mode when it names none
 *
 * @author markr
 */
public class MacroscopicBannedTurnImpl extends BannedTurnImpl implements MacroscopicBannedTurn {

  /** generated UID */
  private static final long serialVersionUID = 1L;

  /** the modes the turn is banned for, by identity since recreating ids changes ids in place; empty when it is
   * banned for every mode */
  private final Set<Mode> explicitlyBannedModes;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param fromSegment from segment to use
   * @param toSegment to segment to use
   * @param explicitlyBannedModes the modes the turn is banned for; when empty it is banned for every mode
   */
  protected MacroscopicBannedTurnImpl(
      final IdGroupingToken groupId, final EdgeSegment fromSegment, final EdgeSegment toSegment,
      final Collection<Mode> explicitlyBannedModes) {
    super(groupId, fromSegment, toSegment);
    this.explicitlyBannedModes = Collections.newSetFromMap(new IdentityHashMap<>());
    this.explicitlyBannedModes.addAll(explicitlyBannedModes);
  }

  /**
   * Copy constructor. Shallow and deep copies alike hold their own set of modes, referring to the same modes
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected MacroscopicBannedTurnImpl(final MacroscopicBannedTurnImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.explicitlyBannedModes = Collections.newSetFromMap(new IdentityHashMap<>());
    this.explicitlyBannedModes.addAll(other.explicitlyBannedModes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Mode> getExplicitlyBannedModes() {
    return Collections.unmodifiableSet(explicitlyBannedModes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addExplicitlyBannedMode(Mode mode) {
    return explicitlyBannedModes.add(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeExplicitlyBannedMode(Mode mode) {
    if (explicitlyBannedModes.size() <= 1) {
      return false;
    }
    return explicitlyBannedModes.remove(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicBannedTurnImpl shallowClone() {
    return new MacroscopicBannedTurnImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicBannedTurnImpl deepClone() {
    return new MacroscopicBannedTurnImpl(this, true);
  }
}
