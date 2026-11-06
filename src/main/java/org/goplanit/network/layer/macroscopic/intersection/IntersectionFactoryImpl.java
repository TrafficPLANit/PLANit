package org.goplanit.network.layer.macroscopic.intersection;

import java.util.Collection;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionFactory;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersections;
import org.goplanit.utils.network.layer.macroscopic.intersection.NonSpatialIntersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * Factory for intersections of both kinds, either created for the caller to complete and register, or created and
 * registered at once
 *
 * @author markr
 */
public class IntersectionFactoryImpl extends ManagedIdEntityFactoryImpl<Intersection> implements IntersectionFactory {

  /** container to register intersections on */
  private final Intersections intersections;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   * @param intersections to register on
   */
  protected IntersectionFactoryImpl(final IdGroupingToken groupId, final Intersections intersections) {
    super(groupId);
    this.intersections = intersections;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NonSpatialIntersection createNonSpatial(
      Node node, IntersectionControlType controlType, IntersectionType type) {
    return new NonSpatialIntersectionImpl(getIdGroupingToken(), node, controlType, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SpatialIntersection createSpatial(
      Collection<Node> memberNodes, IntersectionControlType controlType, Collection<IntersectionType> types) {
    return new SpatialIntersectionImpl(getIdGroupingToken(), memberNodes, controlType, types);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Intersection register(Intersection intersection) {
    intersections.register(intersection);
    return intersection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NonSpatialIntersection registerNewNonSpatial(
      Node node, IntersectionControlType controlType, IntersectionType type) {
    var intersection = createNonSpatial(node, controlType, type);
    register(intersection);
    return intersection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SpatialIntersection registerNewSpatial(
      Collection<Node> memberNodes, IntersectionControlType controlType, Collection<IntersectionType> types) {
    var intersection = createSpatial(memberNodes, controlType, types);
    register(intersection);
    return intersection;
  }
}
