package org.goplanit.network.layer.macroscopic;

import java.util.Collection;
import java.util.Collections;

import org.goplanit.network.layer.BannedTurnFactoryImpl;
import org.goplanit.utils.graph.directed.BannedTurns;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurn;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurnFactory;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;

/**
 * Factory for the banned turns of a macroscopic layer. Every banned turn it creates is a {@link MacroscopicBannedTurn},
 * also those created without modes, which are banned for every mode
 *
 * @author markr
 */
public class MacroscopicBannedTurnFactoryImpl extends BannedTurnFactoryImpl implements MacroscopicBannedTurnFactory {

  /**
   * Constructor
   *
   * @param groupId to use
   * @param bannedTurns container to register on
   */
  protected MacroscopicBannedTurnFactoryImpl(final IdGroupingToken groupId, final BannedTurns bannedTurns) {
    super(groupId, bannedTurns);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicBannedTurn create(final EdgeSegment from, final EdgeSegment to) {
    return new MacroscopicBannedTurnImpl(getIdGroupingToken(), from, to, Collections.emptySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicBannedTurn registerNew(final EdgeSegment from, final EdgeSegment to) {
    return (MacroscopicBannedTurn) super.registerNew(from, to);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicBannedTurn registerNew(
      final MacroscopicLinkSegment from, final MacroscopicLinkSegment to, final Collection<Mode> explicitlyBannedModes) {
    var bannedTurn = new MacroscopicBannedTurnImpl(getIdGroupingToken(), from, to, explicitlyBannedModes);
    getBannedTurns().register(bannedTurn);
    return bannedTurn;
  }
}
