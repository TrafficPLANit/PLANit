package org.goplanit.assignment.ltm.sltm;

import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegmentUtils;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdAble;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.network.layer.physical.Movement;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.PathUtils;
import org.locationtech.jts.geom.Geometry;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Decorating a ManagedDirectedPathImpl instance to include hash and assigned probability in current iteration of
 * sLTM path based assignment, so it can be easily loaded onto the network.
 */
public class StaticLtmDirectedPathImpl extends ExternalIdAbleImpl implements StaticLtmDirectedPath {

  private static final Logger LOGGER = Logger.getLogger(StaticLtmDirectedPathImpl.class.getCanonicalName());

  /**
   * Track hashcode to compare quickly if a path is equal to another path in terms of used link segments.
   * It is assumed the path does not change after creation.
   */
  private final int linkSegmentsOnlyHashCode;

  /**
   * Current path choice probability
   */
  private double currentPathChoiceProbability;

  /** the movement based path definition  */
  private Movement[] thePath;

  /**
   * Generate an id for this instance
   *
   * @param groupId to use
   * @return created id
   */
  protected static long generateId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, ManagedDirectedPath.PATH_ID_CLASS);
  }

  /**
   * Empty path constructor
   * @param idGroupingToken to use
   */
  public StaticLtmDirectedPathImpl(IdGroupingToken idGroupingToken) {
    this(idGroupingToken, null);
  }

  /**
   * Constructor
   *
   * @param groupingToken grouping token
   * @param movements to base path on
   */
  protected StaticLtmDirectedPathImpl(IdGroupingToken groupingToken, Movement[] movements){
    super(generateId(groupingToken));

    this.thePath = movements;

    this.currentPathChoiceProbability = 0;

    if(movements != null) {
      this.linkSegmentsOnlyHashCode = java.util.Arrays.hashCode(
          IterableUtils.asStream(
              IterableUtils.toIterable(new StaticLtmDirectedPathIterator(thePath))).mapToLong(IdAble::getId).toArray());
    }else{
      this.linkSegmentsOnlyHashCode = 0;
    }
  }

  /**
   * Constructor of this decorator
   *
   * @param other to copy
   * @param deepCopy deep copy or not
   */
  protected StaticLtmDirectedPathImpl(StaticLtmDirectedPathImpl other, boolean deepCopy) {
    super(other);
    this.currentPathChoiceProbability = other.currentPathChoiceProbability;
    this.linkSegmentsOnlyHashCode = other.linkSegmentsOnlyHashCode;
    this.thePath = Arrays.copyOf(other.thePath, other.thePath.length);
  }

  @Override
  public void setPathChoiceProbability(double probability){
    this.currentPathChoiceProbability = probability;
  }

  @Override
  public double getPathChoiceProbability(){
    return this.currentPathChoiceProbability;
  }

  @Override
  public int getLinkSegmentsOnlyHashCode(){
    return this.linkSegmentsOnlyHashCode;
  }

  @Override
  public Movement[] getMovements() {
    return thePath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StaticLtmDirectedPathImpl shallowClone() {
    return new StaticLtmDirectedPathImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StaticLtmDirectedPathImpl deepClone() {
    return new StaticLtmDirectedPathImpl(this, true);
  }

  @Override
  public StaticLtmDirectedPathIterator iterator() {
    return new StaticLtmDirectedPathIterator(thePath);
  }

  @Override
  public long size() {
    return thePath.length+1;
  }

  @Override
  public boolean containsSubPath(Collection<? extends EdgeSegment> subPath) {
    return containsSubPath(subPath.iterator());
  }

  @Override
  public boolean containsSubPath(Iterator<? extends EdgeSegment> subPath) {
    return PathUtils.containsSubPath(iterator(), subPath);
  }

  @Override
  public EdgeSegment getFirstSegment() {
    return thePath[0].getSegmentFrom();
  }

  @Override
  public EdgeSegment getLastSegment() {
    return thePath[thePath.length-1].getSegmentTo();
  }

  @Override
  public Geometry createGeometry() {
    return EdgeSegmentUtils.createGeometryFrom(iterator());
  }
}
