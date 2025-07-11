package org.goplanit.assignment.ltm.sltm;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CollectionUtils;
import org.goplanit.utils.misc.Pair;

/**
 * Paired Alternative Segment (PAS) implementation comprising two subpaths (segments), one of a higher cost than the other. In a PAS both subpaths start at the same vertex and end
 * at the same vertex without any intermediate links overlapping.
 * 
 * @author markr
 *
 */
public class Pas<V extends DirectedVertex, ES extends EdgeSegment> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(Pas.class.getCanonicalName());

  //todo: replace with something better, now used for easy tracking of passes during debugging
  private static final LongAdder pasIdCreator = new LongAdder();

  /** cheap PA segment s1 in downstream direction*/
  private ES[] s1;

  /** expensive PA segment s2 in downstream direction*/
  private ES[] s2;

  /** cheap path cost */
  private double s1Cost;

  /** expensive path cost */
  private double s2Cost;

  /** based on the change in cost (see cost update) between iterations, a second-derivative like measure is computed
   * that is based on the measured change in cost vs expected equilibrium cost. This adjustment factor can be applied
   * to the newly calculated proposed flow shift to improve the magnitude of the shift */
  private double proposedPasFlowShiftAdjustmentFactor = 1;

  /** allow to track a bound on the flow shift, if we know we should not overshoot */
  public double goldenRatioShiftBound = Double.MAX_VALUE;

  /** registered origin bushes */
  private final Set<RootedBush<V,ES>> registeredBushes;

  public long pasId;

  /** track status of PAS which determines how it is used */
  protected PasStatus pasStatus = PasStatus.UNKNOWN;

  /** track how often the PAS swapped its S1 segment versus how often its costs was updated
   * (cost may be updated regardless whether it is active or not) */
  protected Pair<LongAdder,LongAdder> countS1Swap = Pair.of(new LongAdder(),new LongAdder());

  /**
   * Constructor
   * 
   * @param s1 cheap subpath
   * @param s2 expensive subpath
   * @param pasId for debugging
   */
  private Pas(final ES[] s1, final ES[] s2, long pasId) {
    this.pasId = pasId;
    this.s1 = s1;
    this.s2 = s2;
    this.registeredBushes = new TreeSet<>();
  }

  /**
   * update costs of an alternative
   * 
   * @param edgeSegmentCosts to use
   * @param updateS1         Flag indicating to update cost of s1 (cheap) segment, when false update the s2
   *                         (costlier) segment
   * @return oldCost before update for given alternative
   */
  protected double updateAlternativeCost(final double[] edgeSegmentCosts, boolean updateS1) {
    ES[] alternative = updateS1 ? s1 : s2;
    double newCost = PasManager.computeCost(alternative, edgeSegmentCosts);
    double oldCost;
    if (updateS1) {
      oldCost = s1Cost;
      s1Cost = newCost;
    } else {
      oldCost = s2Cost;
      s2Cost = newCost;
    }
    return oldCost;
  }

  /**
   * Create a new PAS (factory method)
   *
   * @param <Vs> vertex type
   * @param <ESs> edge segment type
   * @param s1 to use
   * @param s2 to use
   * 
   * @return newly created PAS, or null when alternative segment(s) is/are null
   */
  protected static <Vs extends DirectedVertex, ESs extends EdgeSegment> Pas<Vs,ESs> create(
          final ESs[] s1, final ESs[] s2) {
    if (s1 == null || s2 == null) {
      LOGGER.warning("Unable to create new PAS, one or both alternative segments are null");
      return null;
    }
    pasIdCreator.increment();
    return new Pas<>(s1, s2, pasIdCreator.longValue());
  }

  /**
   * Collect the end vertex of the PAS
   * 
   * @return end vertex
   */
  public V getMergeVertex() {
    return (V) s2[s2.length - 1].getDownstreamVertex();
  }

  /**
   * Collect the start vertex of the PAS
   * 
   * @return start vertex
   */
  public V getDivergeVertex() {
    return (V) s2[0].getUpstreamVertex();
  }

  /**
   * Register origin on the PAS
   * 
   * @param bush bush to register
   * @return true when newly added, false, when already present
   */
  public boolean registerBush(final RootedBush<V,ES> bush) {
    return registeredBushes.add(bush);
  }

  /**
   * Verify if bush is registered on PAS
   * 
   * @param bush to check
   * @return true when registered, false otherwise
   */
  public boolean hasRegisteredBush(final RootedBush<V,ES> bush) {
    return registeredBushes.contains(bush);
  }

  /**
   * The registered bushes
   * 
   * @return registered bushes
   */
  public Set<? extends RootedBush<V,ES>> getRegisteredBushes() {
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
  public void removeBushes(Collection<RootedBush<V,ES>> bushes) {
    bushes.forEach(this::removeBush);
  }

  /**
   * Remove bush from this PAS
   * 
   * @param bush to remove
   */
  public void removeBush(RootedBush<V,ES> bush) {
    registeredBushes.remove(bush);
  }

  /**
   * Verify if the provided path is equal to the PAS alternative
   * 
   * @param pathToVerify to verify
   * @param lowCost      which of the two alternatives to check against
   * @return true when equal, false otherwise
   */
  public boolean isAlternativeEqual(final ES[] pathToVerify, boolean lowCost) {
    ES[] alternative = lowCost ? s1 : s2;
    return Arrays.equals(alternative, pathToVerify);
  }

  /**
   * Verify if the provided path is equal to the PAS alternative
   * 
   * @param pathToVerify to verify
   * @param lowCost      which of the two alternatives to check against
   * @return true when equal, false otherwise
   */
  public boolean isAlternativeEqual(final Collection<ES> pathToVerify, boolean lowCost) {
    ES[] alternative = lowCost ? s1 : s2;
    return CollectionUtils.equals(pathToVerify, alternative);
  }

  /**
   * See if any of the edge segments of an alternative matches the predicate
   *
   * @param pred test to apply
   * @param lowCost      when true check with low cost alternative otherwise high cost
   * @return true when match is found on any, false otherwise
   */
  public boolean anyMatch(Predicate<ES> pred, boolean lowCost) {
    ES[] alternative = lowCost ? s1 : s2;
    ES currEdgeSegment;
    for (int index = alternative.length - 1; index >= 0; --index) {
      currEdgeSegment = alternative[index];
      if(pred.test(currEdgeSegment)){
        return true;
      }
    }
    return false;
  }

  /**
   * Check if any of the set vertices is present on the indicated alternative
   *
   * @param linkSegments where we verify against set link segments
   * @param lowCost      when true check with low cost alternative otherwise high cost
   * @return true when overlapping, false otherwise
   */
  public boolean containsAny(final Collection<? extends ES> linkSegments, boolean lowCost) {
    return anyMatch(linkSegments::contains, lowCost);
  }

  /**
   * Check if any of the set link segments is present on the indicated alternative in opposite direction
   *
   * @param linkSegments where we verify against set link segments
   * @param lowCost      when true check with low cost alternative otherwise high cost
   * @return true when overlapping in opposite direction, false otherwise
   */
  public boolean containsAnyOppositeDirection(final  Collection<? extends ES> linkSegments, boolean lowCost) {
    return anyMatch(es -> linkSegments.contains((ES)es.getOppositeDirectionSegment()), lowCost);
  }

  /**
   * Check if any of the set link segments is present on either alternative
   * 
   * @param linkSegments where we verify against set link segments
   * @return true when overlapping, false otherwise
   */
  public boolean containsAny(final Collection<? extends ES> linkSegments) {
    return containsAny(linkSegments, true) || containsAny(linkSegments, false);
  }

  /**
   * Check if any of the set link segments is present on either alternative as an opposite link
   *
   * @param linkSegments where we verify against set link segments
   * @return true when overlapping in opposite direction, false otherwise
   */
  public boolean containsAnyOppositeDirection(final  Collection<? extends ES> linkSegments) {
    return containsAnyOppositeDirection(linkSegments, true)
            || containsAnyOppositeDirection(linkSegments, false);
  }

  /**
   * Check if given link segment is present on either alternative
   *
   * @param linkSegment where we verify against alternative link segments
   * @return true when present, false otherwise
   */
  public boolean containsEdgeSegment(ES linkSegment) {
    return containsEdgeSegment(linkSegment, true) || containsEdgeSegment(linkSegment, false);
  }

  /**
   * Check if given link segment is present on either high or low cost alternative
   *
   * @param linkSegment where we verify against alternative link segments
   * @param lowCost when true check against low cost alternative, otherwise the high cost alternative
   * @return true when present, false otherwise
   */
  public boolean containsEdgeSegment(ES linkSegment, boolean lowCost) {
    return anyMatch(es -> es.equals(linkSegment), lowCost);
  }

  public boolean updateCost(final double[] edgeSegmentCosts) {
    return updateCost(edgeSegmentCosts, false);
  }

  /**
   * update costs of both paths. In case the low cost path is no longer the low cost path, switch it with the
   * high cost path
   * 
   * @param edgeSegmentCosts to use
   * @param updateAdjustmentFactor when tru update the adjustment factor based on cost that cna be used to alter proposed flow shifts
   * @return true when updated costs caused a switch in what is the high and low cost path
   */
  public boolean updateCost(final double[] edgeSegmentCosts, boolean updateAdjustmentFactor) {
    double oldS1Cost = updateAlternativeCost(edgeSegmentCosts, true);
    double oldS2Cost = updateAlternativeCost(edgeSegmentCosts, false);

    // under linearised cost we could have hoped for an equilibrium cost half way between the two alternatives
    double oldCostAlternativeExpectedDelta = (oldS2Cost - oldS1Cost)/2;
    // now after an iteration and previous flow shift, we find out how far off equilibrium we still are and compute
    // a factor to apply for the interactions non-linearity that should aid us in making a better localised adjusted
    // step using this "second derivative like knowledge" that also embeds network interactions not just this PAS by
    // itself
    double s2CostRealisedDelta = Math.abs(s2Cost - oldS2Cost);
    double s2CostChangeMismatchFactor = s2CostRealisedDelta/oldCostAlternativeExpectedDelta;
    double s1CostRealisedDelta = Math.abs(s1Cost - oldS1Cost);
    double s1CostChangeMismatchFactor = s1CostRealisedDelta/oldCostAlternativeExpectedDelta;
    // if mismatch > 1 --> then we should dampen the change, if = 1, then change was good, if < 1 then we should
    // up the change.
    // We take the reciprocal of the cost that changed the most compared to the expectation (largest value) to address
    // overshooting as our primary concern, if it still changed too little, then we consider changing more based on this
    // value
    double localPasFlowShiftAdjustmentFactor = 1;
    if(updateAdjustmentFactor) {
      countS1Swap.first().increment();
      localPasFlowShiftAdjustmentFactor = 1 / Math.max(s2CostChangeMismatchFactor,s1CostChangeMismatchFactor);
    }

    boolean cheapCostAlternativeSwap = false;
    if (s1Cost > s2Cost) {
      double tempCost = s1Cost;
      s1Cost = s2Cost;
      s2Cost = tempCost;

      ES[] tempSegment = s1;
      s1 = s2;
      s2 = tempSegment;
      cheapCostAlternativeSwap = true;

      // when a swap occurs, we overshot in which case we should never allow for an adjustment over 1
      localPasFlowShiftAdjustmentFactor = Math.min(1, localPasFlowShiftAdjustmentFactor);
    }else{
      localPasFlowShiftAdjustmentFactor = Math.min(1, localPasFlowShiftAdjustmentFactor);
    }

    if(updateAdjustmentFactor){
      if(cheapCostAlternativeSwap) {
        countS1Swap.second().increment();
      }
      proposedPasFlowShiftAdjustmentFactor = localPasFlowShiftAdjustmentFactor;
      if(countS1Swap.first().intValue()<=1){
        proposedPasFlowShiftAdjustmentFactor = localPasFlowShiftAdjustmentFactor;
      }else {
        double portion = (1.0 / countS1Swap.first().intValue());
        proposedPasFlowShiftAdjustmentFactor =
                (1 - portion) * proposedPasFlowShiftAdjustmentFactor + portion * localPasFlowShiftAdjustmentFactor;
      }
    }

    return cheapCostAlternativeSwap;
  }

  /**
   * Update status of PAS explicitly
   *
   * @param pasStatus to set
   * @return previous status
   */
  public PasStatus updateStatus(final PasStatus pasStatus){
    var prevStatus = getStatus();
    this.pasStatus = pasStatus;
    return prevStatus;
  }


  public PasStatus getStatus(){
    return this.pasStatus;
  }

  /**
   * Access to stats on how often the S1 alternative swapped between the two options in relation to
   * how often this was updated.
   *
   * @return pair with first containing how often the cost was updated and second containing how often this meant that
   *  the S1 segment swapped over.
   */
  public Pair<LongAdder, LongAdder> getCountS1Swaps(){
    return countS1Swap;
  }

  /**
   * Apply consumer to each vertex on one of the cost segments
   * 
   * @param lowCostSegment when true applied to low cost segment, when false the high cost segment
   * @param vertexConsumer to apply
   */
  public void forEachVertex(boolean lowCostSegment, Consumer<? super V> vertexConsumer) {
    ES[] alternative = getAlternative(lowCostSegment);
    for (int index = 0; index < alternative.length; ++index) {
      vertexConsumer.accept((V) alternative[index].getUpstreamVertex());
    }
    vertexConsumer.accept((V) alternative[alternative.length - 1].getDownstreamVertex());
  }

  /**
   * Apply consumer to each edgeSegment on one of the cost segments
   * 
   * @param lowCostSegment      when true applied to low cost segment, when false the high cost segment
   * @param edgeSegmentConsumer to apply
   */
  public void forEachEdgeSegment(boolean lowCostSegment, Consumer<ES> edgeSegmentConsumer) {
    ES[] alternative = getAlternative(lowCostSegment);
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
  public ES getLastEdgeSegment(boolean lowCostSegment) {
    return lowCostSegment ? s1[s1.length - 1] : s2[s2.length - 1];
  }

  /**
   * Collect the first edge segment of one of the two segments
   * 
   * @param lowCostSegment when true collect for low cost segment, otherwise the high cost segment
   * @return edge segment
   */
  public ES getFirstEdgeSegment(boolean lowCostSegment) {
    return lowCostSegment ? s1[0] : s2[0];
  }

  /**
   * Access to the two alternatives that reflect the PAS
   * 
   * @param lowCostSegment when true return s1 (lowCost), otherwise s2 (highCost)
   * @return ordered edge segments representing the alternative
   */
  public ES[] getAlternative(boolean lowCostSegment) {
    return lowCostSegment ? s1 : s2;
  }

  /**
   * Returns the difference between the cost of the high cost and the low cost segment. Should always be
   * larger than zero assuming an {@link #updateCost(double[])} has been conducted to ensure the segments are
   * labelled correctly regarding which one is high and which one is low cost
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
   * @param predicate      to test on a per segment basis
   * @return edge segment that matches, null if none matches
   */
  public ES matchFirst(boolean lowCostSegment, Predicate<ES> predicate) {
    ES[] alternative = getAlternative(lowCostSegment);
    for (int index = 0; index < alternative.length; ++index) {
      if (predicate.test(alternative[index])) {
        return alternative[index];
      }
    }
    return null;
  }

  /**
   * Match first link segment of PAS segment to predicate provided, providing the next segment on the PAS as
   * an additional point of reference. In case of the last segment, the next segment is set to null.
   *
   * @param lowCostSegment when true apply on s1, otherwise on s2
   * @param predicate      to test on a per segment basis (providing next segment as second argument)
   * @return edge segment that matches, null if none matches
   */
  public ES matchFirst(boolean lowCostSegment, BiPredicate<ES, ES> predicate) {
    ES[] alternative = getAlternative(lowCostSegment);
    int index = 0;
    ES currSegment = alternative[index++];
    ES nextSegment = null;
    for (; index < alternative.length; ++index) {
      nextSegment = alternative[index];
      if (predicate.test(currSegment, nextSegment)) {
        return currSegment;
      }
      currSegment = nextSegment;
    }
    if (predicate.test(currSegment, null)) {
      return currSegment;
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
    return (int)pasId;
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
    if (this.pasId == ((Pas) obj).pasId &&
            Arrays.equals(objPas.s1, this.s1) && Arrays.equals(objPas.s2, this.s2)) {
      return true;
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(String.format(" (id: %d) -", pasId));

    Consumer<ES> consumer = (ls) -> {
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

  /**
   * Verify if the PASs has functionally equivalent alternatives to the two passed in alternatives (regardless
   * which one is tagged as high or low cost).
   *
   * @param alternative one of the alternatives
   * @param otherAlternative other alternative
   * @return true when both alternatives are matching the alternatives of this PAS
   */
  public boolean isAlternativesEqual(ES[] alternative, ES[] otherAlternative) {
    if(isAlternativeEqual(alternative, true)){
      return isAlternativeEqual(otherAlternative, false);
    }
    if(isAlternativeEqual(alternative, false)){
      return isAlternativeEqual(otherAlternative, true);
    }
    return false;
  }

  public double getProposedPasFlowShiftAdjustmentFactor(){
    return proposedPasFlowShiftAdjustmentFactor;
  }

  public double sumLengthAlternatives() {
    return
        Stream.concat(
            Arrays.stream(getAlternative(true)),
            Arrays.stream(getAlternative(false))).map(
                EdgeSegment::getLengthKm).mapToDouble(e -> e).sum();
  }
}
