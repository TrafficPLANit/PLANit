package org.goplanit.network.layer;

import org.goplanit.utils.graph.directed.BannedTurn;
import org.goplanit.utils.graph.directed.BannedTurnFactory;
import org.goplanit.utils.graph.directed.BannedTurns;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;

/**
 * Factory for creating movements on movement container
 * 
 * @author markr
 */
public class BannedTurnFactoryImpl extends ManagedIdEntityFactoryImpl<BannedTurn> implements BannedTurnFactory {

  /** container */
  private final BannedTurns bannedTurns;

  /**
   * Constructor
   *
   * @param groupId  to use
   * @param bannedTurns to use
   */
  protected BannedTurnFactoryImpl(final IdGroupingToken groupId, final BannedTurns bannedTurns) {
    super(groupId);
    this.bannedTurns = bannedTurns;
  }

  /**
   * The container this factory registers its banned turns on
   *
   * @return container
   */
  protected BannedTurns getBannedTurns() {
    return bannedTurns;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedTurn create(final EdgeSegment from, final EdgeSegment to) {
    return new BannedTurnImpl(getIdGroupingToken(), from, to);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedTurn registerNew(final EdgeSegment from, final EdgeSegment to) {
    final BannedTurn newBannedTurn = create(from, to);
    bannedTurns.register(newBannedTurn);
    return newBannedTurn;
  }

}
