package org.goplanit.assignment.ltm.sltm.util;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegmentUtils;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdAble;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.PathUtils;
import org.locationtech.jts.geom.Geometry;

import javax.annotation.Nonnull;
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

  /** the path definition  */
//  private Movement[] thePath;
  private Deque<? extends EdgeSegment> thePath;

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
   * @param thePath to base path on
   */
  //protected StaticLtmDirectedPathImpl(IdGroupingToken groupingToken, Movement[] movements){
  protected StaticLtmDirectedPathImpl(IdGroupingToken groupingToken, Deque<? extends EdgeSegment> thePath){
    super(generateId(groupingToken));

    this.thePath = thePath;

    this.currentPathChoiceProbability = 0;

    if(thePath != null) {
      this.linkSegmentsOnlyHashCode = java.util.Arrays.hashCode(thePath.stream().mapToLong(IdAble::getId).toArray());
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
  @SuppressWarnings("unchecked")
  protected StaticLtmDirectedPathImpl(StaticLtmDirectedPathImpl other, boolean deepCopy) {
    super(other);
    this.currentPathChoiceProbability = other.currentPathChoiceProbability;
    this.linkSegmentsOnlyHashCode = other.linkSegmentsOnlyHashCode;
    try {
      this.thePath = other.thePath.getClass().getConstructor(Collection.class).newInstance(other.thePath);
    } catch (Exception e) {
      throw new RuntimeException("Cannot copy Deque of the path", e);
    }
  }

  @Override
  public void setPathChoiceProbability(double probability){
    if(Double.compare(probability, Double.NaN) == 0){
      LOGGER.warning(String.format(
              "Probability for path %s cannot be NaN, setting to 0 instead, check validity of procedure",
          getIdsAsString()));
      probability = 0.0;
    }
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

//  @Override
//  public Movement[] getMovements() {
//    return thePath;
//  }

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
  @SuppressWarnings("unchecked")
  @Nonnull
  public Iterator<EdgeSegment> iterator() {
    return (Iterator<EdgeSegment>) thePath.iterator();
  }

  @Override
  public long size() {
    return thePath.size();
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
    return thePath.getFirst();
  }

  @Override
  public EdgeSegment getLastSegment() {
    return thePath.getLast();
  }

  @Override
  public Geometry createGeometry() {
    return EdgeSegmentUtils.createGeometryFrom(iterator());
  }

  @Override
  public String toString(){
    var sb = new StringBuilder();
    // id
    sb.append("Path [").append(getIdsAsString()).append("] - segments: ");
    // edge segments
    this.forEach(es -> sb.append("(").append(es.getIdsAsString()).append(") "));
    return sb.toString();
  }
}
