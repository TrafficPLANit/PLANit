package org.goplanit.assignment.ltm.sltm;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CollectionUtils;

/**
 * Paired Alternative Segment (PAS) implementation comprising two subpaths (segments), one of a higher cost than the other. In a PAS both subpaths start at the same vertex and end
 * at the same vertex without any intermediate links overlapping.
 * 
 * @author markr
 *
 */
public class Pas {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(Pas.class.getCanonicalName());

  //todo: replace with something better, now used for easy tracking of passes during debugging
  private static final LongAdder pasIdCreator = new LongAdder();

  /** cheap PA segment s1 in downstream direction*/
  private EdgeSegment[] s1;

  /** expensive PA segment s2 in downstream direction*/
  private EdgeSegment[] s2;

  /** cheap path cost */
  private double s1Cost;

  /** expensive path cost */
  private double s2Cost;

  /** registered origin bushes */
  private final Set<RootedLabelledBush> registeredBushes;

  protected long pasId;

  /**
   * Constructor
   * 
   * @param s1 cheap subpath
   * @param s2 expensive subpath
   * @param pasId for debugging
   */
  private Pas(final EdgeSegment[] s1, final EdgeSegment[] s2, long pasId) {
    this.pasId = pasId;
    this.s1 = s1;
    this.s2 = s2;
    this.registeredBushes = new HashSet<>();
  }

  /**
   * update costs of an alternative
   * 
   * @param edgeSegmentCosts to use
   * @param updateS1         Flag indicating to update cost of s1 (cheap) segment, when false update the s2 (costlier) segment
   */
  protected void updateCost(final double[] edgeSegmentCosts, boolean updateS1) {

    EdgeSegment[] alternative = updateS1 ? s1 : s2;
    double cost = 0;
    for (int index = 0; index < alternative.length; ++index) {
      cost += edgeSegmentCosts[(int) alternative[index].getId()];
    }

    if (updateS1) {
      s1Cost = cost;
    } else {
      s2Cost = cost;
    }
  }

  /**
   * Create a new PAS (factory method)
   * 
   * @param s1 to use
   * @param s2 to use
   * 
   * @return newly created PAS, or null when alternative segment(s) is/are null
   */
  protected static Pas create(final EdgeSegment[] s1, final EdgeSegment[] s2) {
    if (s1 == null || s2 == null) {
      LOGGER.warning("Unable to create new PAS, one or both alternative segments are null");
      return null;
    }
    pasIdCreator.increment();
    return new Pas(s1, s2, pasIdCreator.longValue());
  }

  /**
   * Collect the end vertex of the PAS
   * 
   * @return end vertex
   */
  public DirectedVertex getMergeVertex() {
    return s2[s2.length - 1].getDownstreamVertex();
  }

  /**
   * Collect the start vertex of the PAS
   * 
   * @return start vertex
   */
  public DirectedVertex getDivergeVertex() {
    return s2[0].getUpstreamVertex();
  }

  /**
   * Register origin on the PAS
   * 
   * @param bush bush to register
   * @return true when newly added, false, when already present
   */
  public boolean registerBush(final RootedLabelledBush bush) {
//    if(pasId == 5 ){
//      int bla = 4;
//    }
    return registeredBushes.add(bush);
  }

  /**
   * Verify if bush is registered on PAS
   * 
   * @param bush to check
   * @return true when registered, false otherwise
   */
  public boolean hasRegisteredBush(final RootedLabelledBush bush) {
    return registeredBushes.contains(bush);
  }

  /**
   * The registered bushes
   * 
   * @return registered bushes
   */
  public Set<RootedLabelledBush> getRegisteredBushes() {
    return registeredBushes;
  }

  /**
   * Verify if PAS (still) has origins registered on it
   * 
   * @return true when origins are present, false otherwise
   */
  public boolean hasRegisteredBushes() {
    return !registeredBushes.isEmpty();
  }

  /**
   * Remove all currently registered bushes from PAS
   */
  public void removeAllRegisteredBushes() {
    registeredBushes.clear();
  }

  /**
   * Remove bushes from this PAS
   * 
   * @param bushes to remove
   */
  public void removeBushes(List<RootedLabelledBush> bushes) {
    bushes.forEach((bush) -> removeBush(bush));
  }

  /**
   * Remove bush from this PAS
   * 
   * @param bush to remove
   */
  public void removeBush(RootedLabelledBush bush) {
    registeredBushes.remove(bush);
  }

  /**
   * Check if bush is overlapping with one of the alternatives, and if it is how much sending flow this sub-path currently represents
   * 
   * @param bush                             to verify
   * @param lowCost                          when true check with low cost alternative otherwise high cost
   * @param linkSegmentFlowAcceptanceFactors to use to obtain accepted flow along subpath, where the flow at the start of the high cost segment is used as starting demand
   * @return when non-negative the segment is overlapping with the PAS, where the value indicates the accepted flow on this sub-path for the bush (with sendinf flow at start as
   *         base demand)
   */
  public double computeOverlappingAcceptedFlow(RootedLabelledBush bush, boolean lowCost, double[] linkSegmentFlowAcceptanceFactors) {
    EdgeSegment[] alternative = lowCost ? s1 : s2;
    return bush.computeSubPathAcceptedFlow(getDivergeVertex(), getMergeVertex(), alternative, linkSegmentFlowAcceptanceFactors);
  }

  /**
   * check if shortest path tree is overlapping with one of the alternatives
   * 
   * @param pathSearchResult to verify
   * @param lowCost      when true check with low cost alternative otherwise high cost
   * @return true when overlapping, false otherwise
   */
  public boolean isOverlappingWith(ShortestPathResult pathSearchResult, boolean lowCost) {
    EdgeSegment[] alternative = lowCost ? s1 : s2;
    EdgeSegment currEdgeSegment = null;
    EdgeSegment matchingEdgeSegment = null;
        
    if(pathSearchResult.isInverted()) {
      /* when search type (and result) is in inverted direction, the result is traversed in downstream direction, i.e., match from first to last */      
      for (int index = 0; index < alternative.length; ++index) {
        currEdgeSegment = alternative[index];
        matchingEdgeSegment = pathSearchResult.getNextEdgeSegmentForVertex(currEdgeSegment.getUpstreamVertex());
        if (!currEdgeSegment.idEquals(matchingEdgeSegment)) {
          return false;
        }
      }
    }else {
      /* when search type (and result) is in regular direction, the result is traversed in upstream direction, i.e., match from last to first */
      for (int index = alternative.length - 1; index >= 0; --index) {
        currEdgeSegment = alternative[index];
        matchingEdgeSegment = pathSearchResult.getNextEdgeSegmentForVertex(currEdgeSegment.getDownstreamVertex());
        if (!currEdgeSegment.idEquals(matchingEdgeSegment)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Verify if the provided path is equal to the PAS alternative
   * 
   * @param pathToVerify to verify
   * @param lowCost      which of the two alternatives to check against
   * @return true when equal, false otherwise
   */
  public boolean isAlternativeEqual(final EdgeSegment[] pathToVerify, boolean lowCost) {
    EdgeSegment[] alternative = lowCost ? s1 : s2;
    return Arrays.equals(alternative, pathToVerify);
  }

  /**
   * Verify if the provided path is equal to the PAS alternative
   * 
   * @param pathToVerify to verify
   * @param lowCost      which of the two alternatives to check against
   * @return true when equal, false otherwise
   */
  public boolean isAlternativeEqual(final Collection<EdgeSegment> pathToVerify, boolean lowCost) {
    EdgeSegment[] alternative = lowCost ? s1 : s2;
    return CollectionUtils.equals(pathToVerify, alternative);
  }

  /**
   * See if any of the edge segments of an alternative matches the predicate
   *
   * @param pred test to apply
   * @param lowCost      when true check with low cost alternative otherwise high cost
   * @return true when match is found on any, false otherwise
   */
  public boolean anyMatch(Predicate<EdgeSegment> pred, boolean lowCost) {
    EdgeSegment[] alternative = lowCost ? s1 : s2;
    EdgeSegment currEdgeSegment;
    for (int index = alternative.length - 1; index >= 0; --index) {
      currEdgeSegment = alternative[index];
      if(pred.test(currEdgeSegment)){
        return true;
      }
    }
    return false;
  }

  /**
   * Check if any of the set link segments is present on the indicated alternative
   * 
   * @param linkSegments where we verify against set link segments
   * @param lowCost      when true check with low cost alternative otherwise high cost
   * @return true when overlapping, false otherwise
   */
  public boolean containsAny(final Collection<EdgeSegment> linkSegments, boolean lowCost) {
    return anyMatch(linkSegments::contains, lowCost);
  }

  /**
   * Check if any of the set link segments is present on the indicated alternative in opposite direction
   *
   * @param linkSegments where we verify against set link segments
   * @param lowCost      when true check with low cost alternative otherwise high cost
   * @return true when overlapping in opposite direction, false otherwise
   */
  public boolean containsAnyOppositeDirection(final  Collection<EdgeSegment> linkSegments, boolean lowCost) {
    return anyMatch(es -> linkSegments.contains(es.getOppositeDirectionSegment()), lowCost);
  }

  /**
   * Check if any of the set link segments is present on either alternative
   * 
   * @param linkSegments where we verify against set link segments
   * @return true when overlapping, false otherwise
   */
  public boolean containsAny(final Collection<EdgeSegment> linkSegments) {
    return containsAny(linkSegments, true) || containsAny(linkSegments, false);
  }

  /**
   * Check if any of the set link segments is present on either alternative as an opposite link
   *
   * @param linkSegments where we verify against set link segments
   * @return true when overlapping in opposite direction, false otherwise
   */
  public boolean containsAnyOppositeDirection(final  Collection<EdgeSegment> linkSegments) {
    return containsAnyOppositeDirection(linkSegments, true) || containsAnyOppositeDirection(linkSegments, false);
  }

  /**
   * update costs of both paths. In case the low cost path is no longer the low cost path, switch it with the high cost path
   * 
   * @param edgeSegmentCosts to use
   * @return true when updated costs caused a switch in what is the high and low cost path
   */
  public boolean updateCost(final double[] edgeSegmentCosts) {
    updateCost(edgeSegmentCosts, true);
    updateCost(edgeSegmentCosts, false);

    if (s1Cost > s2Cost) {
      double tempCost = s1Cost;
      s1Cost = s2Cost;
      s2Cost = tempCost;

      EdgeSegment[] tempSegment = s1;
      s1 = s2;
      s2 = tempSegment;
      return true;
    }
    return false;
  }

  /**
   * Apply consumer to each vertex on one of the cost segments
   * 
   * @param lowCostSegment when true applied to low cost segment, when false the high cost segment
   * @param vertexConsumer to apply
   */
  public void forEachVertex(boolean lowCostSegment, Consumer<DirectedVertex> vertexConsumer) {
    EdgeSegment[] alternative = getAlternative(lowCostSegment);
    for (int index = 0; index < alternative.length; ++index) {
      vertexConsumer.accept(alternative[index].getUpstreamVertex());
    }
    vertexConsumer.accept(alternative[alternative.length - 1].getDownstreamVertex());
  }

  /**
   * Apply consumer to each edgeSegment on one of the cost segments
   * 
   * @param lowCostSegment      when true applied to low cost segment, when false the high cost segment
   * @param edgeSegmentConsumer to apply
   */
  public void forEachEdgeSegment(boolean lowCostSegment, Consumer<EdgeSegment> edgeSegmentConsumer) {
    EdgeSegment[] alternative = getAlternative(lowCostSegment);
    for (int index = 0; index < alternative.length; ++index) {
      edgeSegmentConsumer.accept(alternative[index]);
    }
  }

  /**
   * get cost of high cost alternative segment
   * 
   * @return cost
   */
  public double getAlternativeHighCost() {
    return s2Cost;
  }

  /**
   * get cost of high cost alternative segment
   * 
   * @return cost
   */
  public double getAlternativeLowCost() {
    return s1Cost;
  }

  /**
   * Collect the last edge segment of one of the two segments
   * 
   * @param lowCostSegment when true collect for low cost segment, otherwise the high cost segment
   * @return edge segment
   */
  public EdgeSegment getLastEdgeSegment(boolean lowCostSegment) {
    return lowCostSegment ? s1[s1.length - 1] : s2[s2.length - 1];
  }

  /**
   * Collect the first edge segment of one of the two segments
   * 
   * @param lowCostSegment when true collect for low cost segment, otherwise the high cost segment
   * @return edge segment
   */
  public EdgeSegment getFirstEdgeSegment(boolean lowCostSegment) {
    return lowCostSegment ? s1[0] : s2[0];
  }

  /**
   * Access to the two alternatives that reflect the PAS
   * 
   * @param lowCostSegment when true return s1 (lowCost), otherwise s2 (highCost)
   * @return ordered edge segments representing the alternative
   */
  public EdgeSegment[] getAlternative(boolean lowCostSegment) {
    return lowCostSegment ? s1 : s2;
  }

  /**
   * Returns the difference between the cost of the high cost and the low cost segment. Should always be larger than zero assuming an {@link #updateCost(double[])} has been
   * conducted to ensure the segments are labelled correctly regarding which one is high and which one is low cost
   * 
   * @return s2Cost - s2Cost
   */
  public double getReducedCost() {
    return s2Cost - s1Cost;
  }

  /**
   * Returns the difference between the cost of the high cost and the low cost segment normalised based on the length
   * of the alternatives. Should always be larger than zero.
   * 
   * @return (s2Cost - s2Cost)/(length of s1 + s2)
   */
  public double getNormalisedReducedCost() {
    return (s2Cost - s1Cost) / (s1.length + s2.length);
  }

  /**
   * Match first link segment of PAS segment to predicate provided
   * 
   * @param lowCostSegment when true apply on s1, otherwise on s2
   * @param predicate      to test
   * @return edge segment that matches, null if none matches
   */
  public EdgeSegment matchFirst(boolean lowCostSegment, Predicate<EdgeSegment> predicate) {
    EdgeSegment[] alternative = getAlternative(lowCostSegment);
    for (int index = 0; index < alternative.length; ++index) {
      if (predicate.test(alternative[index])) {
        return alternative[index];
      }
    }
    return null;
  }

  /**
   * Verify if the current known cost for the PAS is considered equal under the given epsilon
   * 
   * @param epsilon to use
   * @return true when abs(costS1-costS2) smaller or equal than epsilon
   */
  public boolean isCostEqual(double epsilon) {
    return Precision.equal(s2Cost, s1Cost, epsilon);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(s1, s2);
  }

  /**
   * A PAS equals another pas if the alternative segments are the same. The registered origins or current cost are not considered in this equality test
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Pas)) {
      return false;
    }

    if (obj == this) {
      return true;
    }

    var objPas = (Pas) obj;
    if (Arrays.equals(objPas.s1, this.s1) && Arrays.equals(objPas.s2, this.s2)) {
      return true;
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(String.format("(%d)", pasId));

    Consumer<EdgeSegment> consumer = (ls) -> {
      if (ls == null) {
        LOGGER.warning("edgeSegment null on PAS alternative, shouldn't happen");
        sb.append("null,");
        return;
      }
      sb.append(ls.getXmlId() != null ? ls.getXmlId() : String.valueOf(ls.getId()) + "*").append(",");
    };

    sb.append("s1: [");
    if (s1 != null && s1.length > 0) {
      Arrays.stream(s1).forEach(consumer);
      sb.replace(sb.length() - 1, sb.length(), "");
    }
    sb.append("] s2: [");
    if (s2 != null && s2.length > 0) {
      Arrays.stream(s2).forEach(consumer);
      sb.replace(sb.length() - 1, sb.length(), "");
    }
    sb.append("]");
    return sb.toString();
  }

}
