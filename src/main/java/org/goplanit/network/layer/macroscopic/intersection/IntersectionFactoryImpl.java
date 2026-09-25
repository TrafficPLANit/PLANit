package org.goplanit.network.layer.macroscopic.intersection;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionFactory;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersections;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * Factory for intersections, either created for the caller to complete and register, or created and registered at
 * once
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
  public Intersection create(Node node, IntersectionControlType controlType, IntersectionType type) {
    return new IntersectionImpl(getIdGroupingToken(), node, controlType, type);
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
  public Intersection registerNew(Node node, IntersectionControlType controlType, IntersectionType type) {
    return register(create(node, controlType, type));
  }
}
