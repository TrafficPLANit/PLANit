package org.goplanit.network.layer.macroscopic;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;

/**
 * A spatial movement: from one link segment to another, with its length and, when there is one, the ban placed on it
 * directly. Its methods change only this movement; keeping it consistent with the rest of the layer, and with the
 * container's index, is up to the layer modifier
 *
 * @author markr
 */
public class SpatialMovementImpl extends ExternalIdAbleImpl implements SpatialMovement {

  /** generated UID */
  private static final long serialVersionUID = -2284511640725312861L;

  /** the link segment it starts on */
  private MacroscopicLinkSegment linkSegmentFrom;

  /** the link segment it ends on */
  private MacroscopicLinkSegment linkSegmentTo;

  /** length in km */
  private final double lengthKm;

  /** true when a ban is placed on the movement itself */
  private boolean directBan;

  /** the modes the direct ban names, by identity since recreating ids changes ids in place; empty when there is no
   * direct ban or it bans every mode */
  private final Set<Mode> explicitlyDirectlyBannedModes;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param from the link segment it starts on
   * @param to the link segment it ends on
   * @param lengthKm from where the from segment ends to where the to segment starts
   */
  protected SpatialMovementImpl(final IdGroupingToken groupId, final MacroscopicLinkSegment from,
      final MacroscopicLinkSegment to, final double lengthKm) {
    super(IdGenerator.generateId(groupId, SPATIAL_MOVEMENT_ID_CLASS));
    this.linkSegmentFrom = from;
    this.linkSegmentTo = to;
    this.lengthKm = lengthKm;
    this.directBan = false;
    this.explicitlyDirectlyBannedModes = Collections.newSetFromMap(new IdentityHashMap<>());
  }

  /**
   * Copy constructor. Shallow and deep copies alike hold their own set of banned modes, referring to the same modes and
   * segments; re-pointing the segments to copied ones is up to the copy of the layer
   *
   * @param other to copy
   */
  protected SpatialMovementImpl(final SpatialMovementImpl other) {
    super(other);
    this.linkSegmentFrom = other.linkSegmentFrom;
    this.linkSegmentTo = other.linkSegmentTo;
    this.lengthKm = other.lengthKm;
    this.directBan = other.directBan;
    this.explicitlyDirectlyBannedModes = Collections.newSetFromMap(new IdentityHashMap<>());
    this.explicitlyDirectlyBannedModes.addAll(other.explicitlyDirectlyBannedModes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment getLinkSegmentFrom() {
    return linkSegmentFrom;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment getLinkSegmentTo() {
    return linkSegmentTo;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setLinkSegmentFrom(MacroscopicLinkSegment linkSegment) {
    this.linkSegmentFrom = linkSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setLinkSegmentTo(MacroscopicLinkSegment linkSegment) {
    this.linkSegmentTo = linkSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getLengthKm() {
    return lengthKm;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasDirectBan() {
    return directBan;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Mode> getExplicitlyDirectlyBannedModes() {
    return Collections.unmodifiableSet(explicitlyDirectlyBannedModes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void banDirectly(Collection<Mode> explicitlyBannedModes) {
    this.directBan = true;
    this.explicitlyDirectlyBannedModes.clear();
    this.explicitlyDirectlyBannedModes.addAll(explicitlyBannedModes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeDirectBan() {
    if (!directBan) {
      return false;
    }
    this.directBan = false;
    this.explicitlyDirectlyBannedModes.clear();
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    setId(IdGenerator.generateId(tokenId, getIdClass()));
    return getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SpatialMovementImpl shallowClone() {
    return new SpatialMovementImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SpatialMovementImpl deepClone() {
    return new SpatialMovementImpl(this);
  }
}
