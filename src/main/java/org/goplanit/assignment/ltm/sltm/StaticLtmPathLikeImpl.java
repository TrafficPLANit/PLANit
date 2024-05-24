package org.goplanit.assignment.ltm.sltm;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdAble;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.network.layer.physical.Movement;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.locationtech.jts.geom.Geometry;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Implementation of movement based staticLtm path like
 */
public class StaticLtmPathLikeImpl implements StaticLtmPathLike {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmPathLikeImpl.class.getCanonicalName());

  /**
   * Track hashcode to compare quickly if a path is equal to another path in terms of used link segments.
   * It is assumed the path does not change after creation.
   */
  private final int linkSegmentsOnlyHashCode;

  /**
   * Current path choice probability
   */
  private double currentPathChoiceProbability;

  /** the path */
  private Movement[] thePath;

  /**
   * Constructor of this decorator
   *
   * @param pathSegments comprising the path
   */
  public StaticLtmPathLikeImpl(Deque<EdgeSegment> pathSegments) {
    this.currentPathChoiceProbability = 0;
    this.linkSegmentsOnlyHashCode = java.util.Arrays.hashCode(
            IterableUtils.asStream(IterableUtils.toIterable(pathSegments.iterator())).mapToLong(IdAble::getId).toArray());

    this.thePath = new Movement[pathSegments.size()];
    if(thePath.length <=2){
      LOGGER.severe("Found path of length smaller than 3 link segments, this is not allowed, ignored");
    }
    var iter = pathSegments.iterator();
    int i = 0;


    EdgeSegment prevSegment = iter.next();
    while(iter.hasNext()){
      var segment = iter.next();
      // identify movement for this combo
      Movement movement = null;
      thePath[i] = movement;
      ++i;
    }
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
    return this.thePath;
  }

}
