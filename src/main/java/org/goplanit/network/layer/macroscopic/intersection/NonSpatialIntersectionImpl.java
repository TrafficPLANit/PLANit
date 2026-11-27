package org.goplanit.network.layer.macroscopic.intersection;

import java.util.EnumSet;
import java.util.List;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.NonSpatialIntersection;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * A non-spatial intersection: one node, its approaches and how it is controlled
 *
 * @author markr
 */
public class NonSpatialIntersectionImpl extends IntersectionImpl implements NonSpatialIntersection {

  /** generated UID */
  private static final long serialVersionUID = 6250419361457262283L;

  /** id among the non-spatial intersections of the layer */
  private long nonSpatialIntersectionId;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param node its node
   * @param controlType of the intersection
   * @param type first kind of the intersection
   */
  protected NonSpatialIntersectionImpl(
      final IdGroupingToken groupId, final Node node, final IntersectionControlType controlType,
      final IntersectionType type) {
    super(groupId, List.of(node), controlType, EnumSet.of(type));
    this.nonSpatialIntersectionId = generateNonSpatialIntersectionId(groupId);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected NonSpatialIntersectionImpl(final NonSpatialIntersectionImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.nonSpatialIntersectionId = other.nonSpatialIntersectionId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getNonSpatialIntersectionId() {
    return nonSpatialIntersectionId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    this.nonSpatialIntersectionId = generateNonSpatialIntersectionId(tokenId);
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NonSpatialIntersectionImpl shallowClone() {
    return new NonSpatialIntersectionImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NonSpatialIntersectionImpl deepClone() {
    return new NonSpatialIntersectionImpl(this, true);
  }
}
