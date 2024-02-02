package org.goplanit.assignment.ltm.sltm;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.path.ManagedDirectedPath;

import java.util.Collection;
import java.util.Iterator;

/**
 * Decorating a ManagedDirectedPathImpl instance to include hash and assigned probability in current iteration of
 * sLTM path based assignment, so it can be easily loaded onto the network.
 */
public class StaticLtmDirectedPathImpl implements StaticLtmDirectedPath {

  /**
   * Track hashcode to compare quickly if a path is equal to another path in terms of used link segments.
   * It is assumed the path does not change after creation.
   */
  private final int linkSegmentsOnlyHashCode;

  /**
   * Current path choice probability
   */
  private double currentPathChoiceProbability;

  /** the path we're decorating */
  private ManagedDirectedPath wrappedPath;

  /**
   * Constructor of this decorator
   *
   * @param pathToWrap path to decorate
   */
  public StaticLtmDirectedPathImpl(ManagedDirectedPath pathToWrap) {
    this.currentPathChoiceProbability = 0;
    this.linkSegmentsOnlyHashCode = java.util.Arrays.hashCode(
            IterableUtils.asStream(IterableUtils.toIterable(pathToWrap.iterator())).mapToLong( e -> e.getId()).toArray());
    this.wrappedPath = pathToWrap;
  }

  /**
   * Constructor of this decorator
   *
   * @param other to copy
   * @param deepCopy deep copy or not
   */
  public StaticLtmDirectedPathImpl(StaticLtmDirectedPathImpl other, boolean deepCopy) {
    this.currentPathChoiceProbability = other.currentPathChoiceProbability;
    this.linkSegmentsOnlyHashCode = other.linkSegmentsOnlyHashCode;
    this.wrappedPath = deepCopy ? other.wrappedPath.deepClone() : other.wrappedPath.shallowClone();
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
  public long getId() {
    return wrappedPath.getId();
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
  public Iterator<EdgeSegment> iterator() {
    return wrappedPath.iterator();
  }

  @Override
  public String getExternalId() {
    return wrappedPath.getExternalId();
  }

  @Override
  public void setExternalId(String externalId) {
    wrappedPath.setExternalId(externalId);
  }

  @Override
  public String getXmlId() {
    return wrappedPath.getXmlId();
  }

  @Override
  public void setXmlId(String xmlId) {
    wrappedPath.setXmlId(xmlId);
  }

  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    return wrappedPath.recreateManagedIds(tokenId);
  }

  @Override
  public long size() {
    return wrappedPath.size();
  }

  @Override
  public boolean containsSubPath(Collection<? extends EdgeSegment> subPath) {
    return wrappedPath.containsSubPath(subPath);
  }

  @Override
  public boolean containsSubPath(Iterator<? extends EdgeSegment> subPath) {
    return wrappedPath.containsSubPath(subPath);
  }

  @Override
  public EdgeSegment getFirstSegment() {
    return wrappedPath.getFirstSegment();
  }

  @Override
  public EdgeSegment getLastSegment() {
    return wrappedPath.getLastSegment();
  }
}
