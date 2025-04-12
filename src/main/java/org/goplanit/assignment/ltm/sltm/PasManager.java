package org.goplanit.assignment.ltm.sltm;

import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.algorithms.shortest.ShortestPathSearchUtils;
import org.goplanit.algorithms.shortest.ShortestSearchType;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CollectionUtils;
import org.goplanit.utils.reflection.ReflectionUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Container class for tracking all unique PASs indexed by their relevant reference vertex
 * 
 * @author markr
 *
 */
public class PasManager<V extends DirectedVertex, ES extends EdgeSegment> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PasManager.class.getCanonicalName());

  /**
   * reduced cost multiplier, empirical calibrated value to use as threshold to consider shifting flow on an origin
   * matching with a PAS, such that
   * reducedCost_max_bush_PAS_path - reducedCost_min_bush_PAS_path > mu * reducedCost_min_network_PAS_path.
   * 0.5 based on Bar-Gera (2010)
   */
  private static final double MU = 0.5;

  /**
   * Flow Effective factor nu, empirically calibrated value to use as threshold to consider shifting flow on an
   * origin matching with a PAS, such that
   * max_cost_PAS_path_flow - max_cost_PAS_path_flow > nu * min_network_PAS_path_flow. 0.25 based on Xie and Xie (2015)
   */
  private static final double NU = 0.25;

  /**
   * Map storing all currently in use PASs by their reference vertex
   */
  private final Map<V, Collection<Pas<V,ES>>> activePassByVertex;

  /**
   * Map storing all previously used, but currently unused PASs by their reference vertex
   */
  private final Map<V, Collection<Pas<V,ES>>> inactivePassByVertex;
  
  /** flag indicating if we store PASs by their downstream merge, or upstream diverge */
  private final boolean registerByDiverge;
  
  /** lambda to obtain reference vertex to use to store PAS in container */
  private final Function<Pas<V,ES>, V> getReferenceVertex;

  /** a comparator to compare PASs based on the reduced cost between their high and low cost segments */
  public static final Comparator<Pas<?, ?>> PAS_REDUCED_COST_COMPARATOR;

  /** a comparator to compare PASs based on the reduced cost per KM between their high and low cost segments */
  public static final Comparator<Pas<?, ?>> PAS_REDUCED_COST_PER_KM_COMPARATOR;
  
  static {
    /*
     * compare by normalised reduced cost in descending order (from high reduced cost to low reduced cost), use very
     * high precision to make sure very small cost differences are still considered as much as possible.
     */
    PAS_REDUCED_COST_COMPARATOR = (p1, p2) -> {
      if (Precision.greater(p1.getReducedCost(), p2.getReducedCost(), Precision.EPSILON_15)) {
        return -1;
      } else if (Precision.smaller(p1.getReducedCost(), p2.getReducedCost(), Precision.EPSILON_15)) {
        return 1;
      } else {
        return 0;
      }
    };

    /* Normalised cost version to ensure that small PASs are not disadvantaged compared to overlapping larger PASs since the
     * smaller the PAS the better the convergence so if anything they should be favoured and processed earlier
     */
    PAS_REDUCED_COST_PER_KM_COMPARATOR = (p1, p2) -> {
      if (Precision.greater(p1.getNormalisedReducedCost(), p2.getNormalisedReducedCost(), Precision.EPSILON_15)) {
        return -1;
      } else if (Precision.smaller(p1.getNormalisedReducedCost(), p2.getNormalisedReducedCost(), Precision.EPSILON_15)) {
        return 1;
      } else {
        return 0;
      }
    };
  }

  /** flag for detailed logging */
  private boolean detailedLogging = DETAILED_LOGGING;

  /** Based on the reference vertex relative to the PAS we obtain the correct vertex from a list of edge segments that are assumed to represent a PAS alternative
   * 
   * @param alternative to collect from
   * @return found reference vertex
   */
  private V getReferenceVertexFromAlternative(List<ES> alternative) {
    V referenceVertex = null;
    if(this.registerByDiverge) {
      referenceVertex = (V) alternative.get(0).getUpstreamVertex();
    }else {
      referenceVertex = (V) alternative.get(alternative.size() - 1).getDownstreamVertex();
    }
    return referenceVertex;
  }
  
  /** Based on the reference vertex relative to the PAS we obtain the correct vertex from a list of edge segments that
   * are assumed to represent a PAS alternative
   * 
   * @param alternative to collect from
   * @return found reference vertex
   */
  private V getReferenceVertexFromAlternative(ES[] alternative) {
    if(alternative == null){
      return null;
    }
    V referenceVertex = null;
    if(this.registerByDiverge) {
      referenceVertex = (V) alternative[0].getUpstreamVertex();
    }else {
      referenceVertex = (V) alternative[alternative.length-1].getDownstreamVertex();
    }
    return referenceVertex;
  }

  /**
   * find the first PAS of the PASs considered and which if we would extend the bush
   * with its least cost alternative would improve to the point it is considered effective enough compared to the
   * upper bound (reduced cost) improvement provided as well as that the bush has sufficient flow on the high-cost
   * alternative of the PAS such that it can improve sufficiently by shifting flow towards the new low cost segment.
   * If this all holds the PAS is selected and returned. We select the first PAS we can find that matches the criteria.
   *
   * @param bush                  to find suitable PAS for
   * @param pass                  to consider
   * @param flowAcceptanceFactors to use (required to assess flow effectiveness in capacitated context)
   * @param reducedCost           the upper bound on the improvement that is known for this merge vertex
   * @param checkEffectiveness when true enforce effectiveness checks, otherwise do not
   * @return pas found, null if no suitable candidates exist
   */
  @SuppressWarnings("unchecked")
  private Pas<V,ES> findFirstSuitablePas(
          final RootedBush<V,ES> bush,
          Collection<Pas<V,ES>> pass,
          double[] flowAcceptanceFactors,
          double reducedCost,
          boolean checkEffectiveness) {

    if (pass == null) {
      return null;
    }

    Pas<V,ES> matchedPas = null;
    for (var pas : pass) {
      if (pas.hasRegisteredBush(bush)) {
        continue;
      }

      /* check if PAS is attached upstream to bush - even if it is just with the vertex */
      boolean pasPotentialMatch = false;
      for (var pasFirstExitSegment : pas.getDivergeVertex().getExitEdgeSegments()) {
        if (bush.contains((ES) pasFirstExitSegment)) {
          pasPotentialMatch = true;
          break;
        }
      }

      if(!pasPotentialMatch) {
        continue;
      }

      pasPotentialMatch = false;
      /* check if PAS is attached downstream to bush - even if it is just with the vertex */
      for (var pasLastEntrySegment : pas.getMergeVertex().getEntryEdgeSegments()) {
        if (bush.contains((ES) pasLastEntrySegment)) {
          pasPotentialMatch = true;
          break;
        }
      }

      if (!pasPotentialMatch) {
        continue;
      }

      /* PAS start/end node are on bush, now check if it is effective in reducing cost/shifting flow */
      if (checkEffectiveness && !isPasEffectiveForBush(pas, bush, flowAcceptanceFactors, reducedCost)) {
        continue;
      }
      /* when not checking on effectiveness we at least must check it has flow */
      if(!checkEffectiveness && hasZeroFlow(pas.getAlternative(false), bush, flowAcceptanceFactors)){
        continue;
      }

      /* deemed effective, now ensure it does not introduce cycles given current state of the bush */
      if (bush.determineIntroduceCycle(pas.getAlternative(true)) != null) {
        continue;
      }

      matchedPas = pas;
      break;

    }
    return matchedPas;
  }

  /**
   * Find PAS that exactly matches the provides alternative segments. Identical to
   * {@link #findMatchingActivePas(List, List)}
   *
   * @param alternative1 alternative segment of PAS
   * @param alternative2 alternative segment of PAS
   * @param active when true consider active PASs for a match, otherwise consider inactive PASs for a match
   * @return the matching PAS, null otherwise
   */
  private Pas<V,ES> findMatchingPas(final ES[] alternative1, final ES[] alternative2, boolean active) {
    if (alternative1 == null || alternative2 == null) {
      LOGGER.severe("one or more alternatives of potential PAS are null");
      return null;
    }

    var potentialPass = active ?
            getActivePassByReferenceVertex(getReferenceVertexFromAlternative(alternative1)) :
            getInactivePassByReferenceVertex(getReferenceVertexFromAlternative(alternative1));
    if (potentialPass == null) {
      return null;
    }

    for (var potentialPas : potentialPass) {
      if(potentialPas.isAlternativesEqual(alternative1,alternative2)){
        return potentialPas;
      }
    }

    return null;
  }

  /** default for detailed logging flag */
  public static final boolean DETAILED_LOGGING = false;

  /**
   * Verify if extending a bush with the given PAS given the reduced cost found, it would be effective in improving
   * the bush. This is verified by
   * <p>
   * reducedCost = bush_min_path_cost - PAS_min_path_cost, then it is considered effective if
   * (PAS_max_path_cost - PAS_min_path_cost) exceeds mu*bushReducedCost.
   * <p>
   * Formulation based on Bar-Gera (2010). IDea is that if the PAS has little difference between high and low cost,
   * we can't shift much flow to improve and it is less attractive. This is ok if the reduced cost, i.e., the maximum
   * improvement given the current state of the network, is also low, but when the best option (which might not
   * exactly follow this PAS) is much better than what this PAS offers, we regard this PAS as not cost-effective and
   * ignore it as a viable option.
   *
   * @param alternativeHighCost to use
   * @param alternativeLowCost to use
   * @param reducedCost to use
   * @return true when considered effective, false otherwise
   */
  public static boolean isCostEffective(double alternativeHighCost, double alternativeLowCost, double reducedCost) {
    return Precision.greater(alternativeHighCost - alternativeLowCost, MU * reducedCost);
  }

  /**
   * Use the accepted flow on the bush from start-to-end of PAS high cost segment and make sure it exceeds NU * total
   * accepted flow (on the bush at hand) on the final edge segment of the PAS high cost segment. This is an adaptation
   * from Bar-Gera who uses the minimum across all high cost segments on the bush, but since we are capacitated this
   * won't be representative. So instead we use the portion of the total flow on the final segment that belongs to the
   * high-cost sub-path present on the bush instead.
   * <p>
   * the rationale here is that we should only consider the PAS as effective for this bush, i.e., consider it for
   * inclusion - if a decent amount of flow leading to the end point of this PAS comes from the high cost segment of
   * this PAS which would allow for a decent chunk of the flow to be shifted to the low cost segment. If not,
   * it would not improve this bush much if we would consider it.
   * </p>
   * <p>
   *   UPDATE OCT 2024: found that using accepted flow on last segment is not a good choice due to alphas along the way
   *   We can achieve our objective using sending flows at the start of the PAS instead and compare it against the
   *   portion that follows the pas in full. This is the same concept, but one that makes way more sense in a
   *   capacitated environment
   * </p>
   *
   * @param <Vs> type of vertex on bush
   * @param <ESs> type of edge segment on bush
   * @param pasHighCostAlternative  PAS high cost alternative to check for a bush
   * @param bush the accepted flow found on the bush traversing the high cost PAS and reaching the end
   *             vertex (including final alpha) of the PAS
   * @param nlFlowAcceptanceFactors the accepted flow found passing through the final vertex of the PAS from the origin
   *                              of the bush, i.e., all sub-paths to this vertex
   * @return true when considered effective, false otherwise
   */
  @SuppressWarnings("unchecked")
  public static <Vs extends DirectedVertex,ESs extends EdgeSegment> boolean isFlowEffective(
          ESs[] pasHighCostAlternative, RootedBush<Vs,ESs> bush, double[] nlFlowAcceptanceFactors) {
    // NEW based on sending flow
    double s2FullSubPathSendingFlowOnBush = bush.determineSubPathSendingFlow(
            pasHighCostAlternative, nlFlowAcceptanceFactors);
    /* general usage of high cost initial segment in bush (irrespective whether the flow follows the high-cost path)*/
    var s2InitialEdgeSegment = pasHighCostAlternative[0];
    double s2InitialSegmentSendingFlowOnBush = bush.getSendingFlowPcuH(s2InitialEdgeSegment);
    return Precision.greater(s2FullSubPathSendingFlowOnBush, NU * Math.min(100, s2InitialSegmentSendingFlowOnBush));
  }

  /**
   * Verify if the alternative has zero flow on bush
   *
   * @param alternative to check
   * @param bush bush to use
   * @param nlFlowAcceptanceFactors to use
   * @return true when flow is present, false otherwise
   * @param <Vs> type of vertex
   * @param <ESs> type of segment
   */
  public static <Vs extends DirectedVertex,ESs extends EdgeSegment> boolean hasZeroFlow(
          ESs[] alternative, RootedBush<Vs,ESs> bush, double[] nlFlowAcceptanceFactors) {
    return !Precision.positive(bush.determineSubPathSendingFlow(alternative, nlFlowAcceptanceFactors));
  }

  /**
   * Verify if PAS is considered effective (enough) to improve the provided bush. This is verified by being both
   * {@link #isCostEffective(double, double, double)} and
   * {@link #isFlowEffective(EdgeSegment[], RootedBush, double[])}
   *
   * @param <Vs> type of vertex on bush
   * @param <ESs> type of edge segment on bush
   * @param pas                   to use
   * @param bush            to use
   * @param nlFlowAcceptanceFactors to use
   * @param reducedCost           to use
   * @return true when considered effective, false otherwise
   */
  public static <Vs extends DirectedVertex,ESs extends EdgeSegment> boolean isPasEffectiveForBush(
          Pas<Vs,ESs> pas, RootedBush<Vs,ESs> bush, double[] nlFlowAcceptanceFactors, double reducedCost) {
    /* Verify if low-cost PAS alternative is effective (enough) in improving the bush within the identified upper bound
    of the reduced cost */
    return isPasEffectiveForBush(
            pas.getAlternative(false),
            pas.getAlternativeHighCost(),
            pas.getAlternativeLowCost(),
            bush,
            nlFlowAcceptanceFactors,
            reducedCost);
  }

  /**
   * Verify if PAS is considered effective (enough) to improve the provided bush. This is verified by being both
   * {@link #isCostEffective(double, double, double)} and
   * {@link #isFlowEffective(EdgeSegment[], RootedBush, double[])}
   *
   * @param <Vs> type of vertex on bush
   * @param <ESs> type of edge segment on bush
   * @param pasHighCostAlternative  to use
   * @param highCostAlternativeCost to use
   * @param lowCostAlternativeCost to use
   * @param bush            to use
   * @param nlFlowAcceptanceFactors to use
   * @param reducedCost           to use
   * @return true when considered effective, false otherwise
   */
  public static <Vs extends DirectedVertex,ESs extends EdgeSegment> boolean isPasEffectiveForBush(
          ESs[] pasHighCostAlternative,
          double highCostAlternativeCost,
          double lowCostAlternativeCost,
          RootedBush<Vs,ESs> bush,
          double[] nlFlowAcceptanceFactors,
          double reducedCost) {
    /* Verify if low-cost PAS alternative is effective (enough) in improving the bush within the identified upper bound
    of the reduced cost */
    return isCostEffective(highCostAlternativeCost, lowCostAlternativeCost, reducedCost)
            && isFlowEffective(pasHighCostAlternative, bush, nlFlowAcceptanceFactors);
  }

  /**
   * Extract a subpath in the form of a raw edge segment array in downstream direction based on the shortest path result
   * provided. Since the path tree is in reverse direction, the array is filled from the back, i.e.,if there is spare
   * capacity the front of the array would be empty.
   *
   * @param <Vs> vertex type to use
   * @param <ESs> edge segment type to use
   * @param closestToSearchRoot       vertex in relation to searchResult tree chosen root
   * @param furthestFromSearchRoot    vertex in relation to searchResult tree chosen root
   * @param searchResultTree          to extract path from, tree's direction is automatically accounted for
   * @param edgeSegmentArrayToPopulate to use for the array population which should be at least as long as the path
   *                                   that is to be extracted into it
   * @param truncateArray             flag indicating to truncate the subpath array in case the front of the array is
   *                                  not fully used due to the existence of spare capacity
   * @return created array in downstream direction, null if no path could be found
   */
  @SuppressWarnings("unchecked")
  @Deprecated
  public static <Vs extends DirectedVertex,ESs extends EdgeSegment> ESs[] createSubPathArrayFrom(
      final Vs closestToSearchRoot,
      final Vs furthestFromSearchRoot,
      final ShortestPathResult searchResultTree,
      ESs[] edgeSegmentArrayToPopulate,
      boolean truncateArray) {

    // Note: result tree is traversed in reversed order of the search itself
    // 1) one-to-all (search not inverted) --> traverse result tree backwards from destination to origin
    //                                     --> extract path needs to be added in reverse to go in travel direction
    // 2) all-to-one (search inverted) --> traverse tree backwards from origin to destination
    //                                 --> extract path already in correct travel direction

    ESs currEdgeSegment = null;
    int arrayLength = edgeSegmentArrayToPopulate.length;
    Vs currVertex = furthestFromSearchRoot;
    boolean searchInverted = searchResultTree.getSearchType().isInverted();
    
    /* run from end to start backward while adding in reverse to final array, unless search was inverted, then we go
    from start to end */
    int index = searchInverted ? 0 : arrayLength - 1;
    do {

      currEdgeSegment = (ESs) searchResultTree.getNextEdgeSegmentForVertex(currVertex);
      edgeSegmentArrayToPopulate[index] = currEdgeSegment;
      if (currEdgeSegment == null) {
        LOGGER.warning(String.format("Unable to extract subpath from start vertex %s to end vertex %s, no incoming " +
                "edge segment available at intermediate vertex %s",
            closestToSearchRoot.getXmlId(), furthestFromSearchRoot.getXmlId(), currVertex.getXmlId()));
        return null;
      }
      currVertex = (Vs) searchResultTree.getNextVertexForEdgeSegment(currEdgeSegment);
      
      if (searchInverted) {
        ++index;
      } else {
        --index;
      }      
    } while (!currVertex.idEquals(closestToSearchRoot));

    if (truncateArray) {
      if (!searchInverted && index > 0){
        return Arrays.copyOfRange(edgeSegmentArrayToPopulate, index+1, edgeSegmentArrayToPopulate.length);
      }else if(searchInverted && index < arrayLength) {
        return Arrays.copyOfRange(edgeSegmentArrayToPopulate, 0, index);
      }
    }
    return edgeSegmentArrayToPopulate;
  }

  /**
   * Extract a subpath in the form of a raw edge segment array in downstream direction based on the breadth-first (BF)
   * search result provided. This search result is expected to be constructed from the regular shortest path result
   * which direction depends on the search type. the BF search results are expected to be provided in the SAME direction
   * as the search itself (unlike shortestXResults which are in the opposite direction), i.e., if the search was
   * one-to-all (not inverted) then the bf results are also provided in the downstream direction, whereas all-to-one is
   * in the opposite direction.
   *
   * @param <Vs> vertex type to use
   * @param <ESs> edge segment type to use
   * @param closestToSearchRoot         vertex in relation to searchResult tree
   * @param furthestFromSearchRoot      vertex in relation to searchResult tree
   * @param shortestSearchType          shortestSearchType used to obtain inverted search result, i.e., when on-to-all
   *                                    inverted search result is in downstream direction, when all-to-one in upstream
   *                                    direction
   * @param invertedBfSearchResultTree  to extract path from, tree is in inverted direction compared to regular search
   *                                    tree result, i.e., one-to-all search result is normally in upstream direction,
   *                                    here it is in downstream direction etc.
   * @param edgeSegmentArrayToPopulate to use for the array population which should be at least as long as the path
   *                                   that is to be extracted into it
   * @param truncateArray               flag indicating to truncate the subpath array in case the back of the array is
   *                                    not fully used due to the existence of spare capacity
   * @return created array always in downstream direction, null if no path could be found
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  public static <Vs extends DirectedVertex, ESs extends EdgeSegment> ESs[] createSubPathArrayFrom(
        Vs closestToSearchRoot,
        Vs furthestFromSearchRoot,
        ShortestSearchType shortestSearchType,
        Map<? extends Vs, ? extends ESs> invertedBfSearchResultTree,
        ESs[] edgeSegmentArrayToPopulate,
        boolean truncateArray) {

    /*
     * depending on the original search direction, i.e., the direction of the to-be extract segments, we revert the way
     * we add them to the resulting array to obtain the correct final direction of edge segments in downstream direction
     */
    boolean searchInverted = shortestSearchType.isInverted();
    
    int arrayLength = edgeSegmentArrayToPopulate.length;
    ESs currEdgeSegment = null;
    
    /* search utils yields lambda based on search type for searching, not result traversal, we traverse results, so we
     * should invert. However, our provided results are inverted already, so double inversion makes that we should
     * not invert */
    var getNextVertex =
        ShortestPathSearchUtils.getVertexFromEdgeSegmentLambda(shortestSearchType);
    
    /* run from end to start backward while adding in reverse to final array, unless search was inverted, then we go
    from start to end */
    int index = 0;
    Vs currVertex = closestToSearchRoot;
    if (searchInverted) {
      index = arrayLength-1;
    }
    
    boolean nextAvailable = true;
    do {
      currEdgeSegment = invertedBfSearchResultTree.get(currVertex);
      edgeSegmentArrayToPopulate[index] = currEdgeSegment;
      if (currEdgeSegment == null) {
        LOGGER.warning(String.format("Unable to extract sub-path between vertex (%s) and vertex (%s), no edge " +
                        "segment available at intermediate vertex (%s)",
            closestToSearchRoot.getIdsAsString(), furthestFromSearchRoot.getIdsAsString(), currVertex.getIdsAsString()));
        return null;
      }
      currVertex = (Vs) getNextVertex.apply(currEdgeSegment);
      
      if (searchInverted) {
        nextAvailable = --index >= 0;
      } else {
        nextAvailable = ++index < arrayLength;
      }      
      
    } while (!currVertex.idEquals(furthestFromSearchRoot) && nextAvailable);

    if (!currVertex.idEquals(furthestFromSearchRoot)) {
      LOGGER.warning(String.format("Unable to create sub-path array between node (%s) and node (%s) from given " +
              "pathTree", closestToSearchRoot.getIdsAsString(), furthestFromSearchRoot.getIdsAsString()));
      return null;
    }

    if (truncateArray && nextAvailable) {
      if(searchInverted) {
        /* inverted, truncate start */
        return Arrays.copyOfRange(edgeSegmentArrayToPopulate, index + 1, arrayLength);
      }else {
        /* regular, truncate end */
        return Arrays.copyOfRange(edgeSegmentArrayToPopulate, 0, index);
      }      
    }

    return edgeSegmentArrayToPopulate;
  }

  /**
   * Compute costs for an array of edge segments
   *
   * @param edgeSegments to compute cost for
   * @param edgeSegmentCosts costs per edge segment
   * @return total cost
   */
  public static double computeCost(EdgeSegment[] edgeSegments, final double[] edgeSegmentCosts){
    double cost = 0;
    for (int index = 0; index < edgeSegments.length; ++index) {
      cost += edgeSegmentCosts[(int) edgeSegments[index].getId()];
    }
    return cost;
  }

  /**
   * Constructor
   * 
   * @param registerByDiverge when true store PASs by (most upstream) diverge vertex, otherwise by their
   *                          (most downstream) merge vertex
   */
  public PasManager(boolean registerByDiverge) {
    
    this.registerByDiverge = registerByDiverge;
    if(registerByDiverge) {
      this.getReferenceVertex = Pas::getDivergeVertex;
    }else {
      this.getReferenceVertex = Pas::getMergeVertex;
    }
    
    this.activePassByVertex = new HashMap<>();
    this.inactivePassByVertex = new HashMap<>();
  }

  /**
   * create a new PAS for the given cheap and expensive paired alternative segments (subpaths) and register the origin
   * bush on it that was responsible for creating it
   * 
   * @param bush responsible for triggering the creation of this PAS
   * @param s1         cheap alternative segment
   * @param s2         expensive alternative segment
   * @return createdPas
   */
  public Pas<V,ES> createAndRegisterNewPas(final RootedBush<V,ES> bush, final ES[] s1, final ES[] s2) {
    var refVertex = getReferenceVertexFromAlternative(s1);
    var unusedPassForRefVertex = inactivePassByVertex.get(refVertex);

    Pas<V,ES> newPas;
    if(unusedPassForRefVertex != null &&
            unusedPassForRefVertex.stream().anyMatch( p -> p.isAlternativesEqual(s1, s2))){
      LOGGER.warning("Trying to register a new Pas that already existed in unused form, using existing variant");
      newPas = unusedPassForRefVertex.stream().filter(p -> p.isAlternativesEqual(s1, s2)).findFirst().get();
    }else{
      newPas = Pas.create(s1, s2);
    }

    if (newPas == null) {
      return null;
    }

    newPas.registerBush(bush);
    activePassByVertex.putIfAbsent(this.getReferenceVertex.apply(newPas), new ArrayList<>());
    activePassByVertex.get(this.getReferenceVertex.apply(newPas)).add(newPas);
    return newPas;
  }

  /**
   * Reactivate a currently inactive PAS
   *
   * @param inactivePas to reactivate
   */
  public void reactivatePas(Pas<V, ES> inactivePas) {
    var refVertex = getReferenceVertex.apply(inactivePas);
    if(activePassByVertex.get(refVertex).contains(inactivePas)){
      LOGGER.warning("Unable to reactive PAS (%s) as it is already active, shouldn't happen");
      return;
    }
    if(!inactivePassByVertex.get(refVertex).contains(inactivePas)){
      LOGGER.warning("Unable to reactivate PAS (%s) as it is unknown as an inactive PAS, shouldn't happen");
      return;
    }
    activePassByVertex.putIfAbsent(refVertex, new ArrayList<>());
    activePassByVertex.get(refVertex).add(inactivePas);
    inactivePassByVertex.get(refVertex).remove(inactivePas);
  }

  /**
   * create a new PAS for the given cheap and expensive paired alternative segments (subpaths) and register the origin
   * bush on it that was responsible for creating it
   * 
   * @param bush responsible for triggering the creation of this PAS
   * @param s1         cheap alternative segment
   * @param s2         expensive alternative segment
   * @return createdPas
   */
  public Pas<V,ES> createAndRegisterNewPas(
          final RootedBush<V,ES> bush, final Collection<ES> s1, final Collection<ES> s2) {
    return createAndRegisterNewPas(bush, CollectionUtils.toArray(s1), CollectionUtils.toArray(s2));
  }

  /**
   * Deactivate the PAS
   * 
   * @param pas           to remove
   * @param logDeactivatedPas when true log deactived pas, when false do not
   */
  public void deactivatePas(final Pas<V,ES> pas, boolean logDeactivatedPas) {
    var refVertex = this.getReferenceVertex.apply(pas);
    // remove from active pass
    activePassByVertex.get(this.getReferenceVertex.apply(pas)).remove(pas);
    // remove all bushes from PAS if any are remaining
    pas.removeAllRegisteredBushes();
    // track as unused pass which may later be activated again
    inactivePassByVertex.putIfAbsent(refVertex, new ArrayList<>());
    inactivePassByVertex.get(refVertex).add(pas);

    // larger networks cause massive logging, switched off for now
//    if (logDeactivatedPas) {
//      LOGGER.info(String.format("Deactivated existing PAS: %s", pas.toString()));
//    }
  }

  /**
   * Collect all active PASs that share the same reference vertex.
   * 
   * @param referenceVertex to collect for
   * @return found active PAS matches, null if none
   */
  public Collection<Pas<V,ES>> getActivePassByReferenceVertex(final V referenceVertex) {
    return activePassByVertex.get(referenceVertex);
  }

  /**
   * Collect all inactive PASs that share the same reference vertex.
   *
   * @param referenceVertex to collect for
   * @return found inactive PAS matches, null if none
   */
  public Collection<Pas<V,ES>> getInactivePassByReferenceVertex(final V referenceVertex) {
    return inactivePassByVertex.get(referenceVertex);
  }

  /**
   * Identical to {@link #findMatchingActivePas(EdgeSegment[], EdgeSegment[])}
   *
   * @param alternative1 alternative segment of PAS
   * @param alternative2 alternative segment of PAS
   * @return the matching PAS, null otherwise
   */
  @SuppressWarnings("unchecked")
  public Pas<V,ES> findMatchingActivePas(final List<ES> alternative1, final List<ES> alternative2) {
    var alt1Array = ReflectionUtils.createTypedArrayInstance(
            (Class<ES>)alternative1.get(0).getClass(),alternative1.size());
    var alt2Array = ReflectionUtils.createTypedArrayInstance(
            (Class<ES>) alternative2.get(0).getClass(),alternative2.size());
    return findMatchingActivePas(alternative1.toArray(alt1Array), alternative2.toArray(alt2Array));
  }

  /**
   * Find PAS that exactly matches the provides alternative segments. Identical to
   * {@link #findMatchingActivePas(List, List)}
   * 
   * @param alternative1 alternative segment of PAS
   * @param alternative2 alternative segment of PAS
   * @return the matching PAS, null otherwise
   */
  public Pas<V,ES> findMatchingActivePas(final ES[] alternative1, final ES[] alternative2) {
    boolean searchActivePass = true;
    return findMatchingPas(alternative1, alternative2, searchActivePass);
  }

  /**
   * Find inactive PAS that exactly matches the provides alternative segments. Identical to
   * {@link #findMatchingActivePas(List, List)}
   *
   * @param alternative1 alternative segment of PAS
   * @param alternative2 alternative segment of PAS
   * @return the matching PAS, null otherwise
   */
  public Pas<V,ES> findMatchingInactivePas(final ES[] alternative1, final ES[] alternative2) {
    boolean searchActivePass = true;
    return findMatchingPas(alternative1, alternative2, !searchActivePass);
  }

  /**
   * Verify if any active PAS at given reference vertex is used by this origin bush.
   * 
   * @param bush  to test for
   * @param referenceVertex to test for
   * @return true when PAS is used by origin bush ending at this vertex, false otherwise
   */
  public boolean isRegisteredOnAnyActivePasAtReferenceVertex(
          final RootedBush<V,ES> bush, final V referenceVertex) {
    /* verify potential PASs */
    var potentialPass = getActivePassByReferenceVertex(referenceVertex);
    if (potentialPass == null) {
      return false;
    }

    for (var pas : potentialPass) {
      if (pas.hasRegisteredBush(bush)) {
        return true;
      }
    }

    return false;
  }

  /**
   * find the first ACTIVE PAS which if we would extend the bush with its least cost alternative would improve
   * to the point it is considered effective enough compared to the upper bound (reduced cost) improvement
   * provided as well as that the bush has sufficient flow on the high-cost alternative of the PAS such
   * that it can improve sufficiently by shifting flow towards the new low cost segment.
   * If this all holds the PAS is selected and returned. We select the first PAS we can find that matches
   * the criteria.
   *
   * @param bush            to find suitable PAS for
   * @param referenceVertex           to use
   * @param flowAcceptanceFactors to use (required to assess flow effectiveness in capacitated context)
   * @param reducedCost           the upper bound on the improvement that is known for this merge vertex
   * @param checkEffectiveness when true enforce effectiveness checks, otherwise do not
   * @return pas found, null if no suitable candidates exist
   */
  @SuppressWarnings("unchecked")
  public Pas<V,ES> findFirstSuitableActivePas(
          final RootedBush<V,ES> bush,
          final V referenceVertex,
          double[] flowAcceptanceFactors,
          double reducedCost,
          boolean checkEffectiveness) {

    /* check suitable PASs from pool of active PASs */
    return findFirstSuitablePas(
            bush,
            getActivePassByReferenceVertex(referenceVertex),
            flowAcceptanceFactors,
            reducedCost,
            checkEffectiveness);
  }

  /**
   * find the first INACTIVE PAS which if we would extend the bush with its least cost alternative would improve
   * to the point it is considered effective enough compared to the upper bound (reduced cost) improvement
   * provided as well as that the bush has sufficient flow on the high-cost alternative of the PAS such
   * that it can improve sufficiently by shifting flow towards the new low cost segment.
   * If this all holds the PAS is selected and returned. We select the first PAS we can find that matches
   * the criteria.
   *
   * @param bush            to find suitable PAS for
   * @param referenceVertex           to use
   * @param flowAcceptanceFactors to use (required to assess flow effectiveness in capacitated context)
   * @param reducedCost           the upper bound on the improvement that is known for this merge vertex
   * @param checkEffectiveness when true enforce effectiveness checks, otherwise do not
   * @return pas found, null if no suitable candidates exist
   */
  @SuppressWarnings("unchecked")
  public Pas<V,ES> findFirstSuitableInactivePas(
          final RootedBush<V,ES> bush,
          final V referenceVertex,
          double[] flowAcceptanceFactors,
          double reducedCost,
          boolean checkEffectiveness) {
      return findFirstSuitablePas(
              bush,
              getInactivePassByReferenceVertex(referenceVertex),
              flowAcceptanceFactors,
              reducedCost,
              checkEffectiveness);
  }

  /**
   * Update costs on all registered active PASs
   * 
   * @param linkSegmentCosts to use
   */
  public void updateActivePassCosts(final double[] linkSegmentCosts) {
    for (Collection<Pas<V,ES>> pass : activePassByVertex.values()) {
      updatePassCosts(pass, linkSegmentCosts, true);
    }
  }

  /**
   * Update costs on all registered inactive PASs
   *
   * @param linkSegmentCosts to use
   */
  public void updateInactivePassCosts(final double[] linkSegmentCosts) {
    for (Collection<Pas<V,ES>> pass : inactivePassByVertex.values()) {
      updatePassCosts(pass, linkSegmentCosts, false);
    }
  }

  /**
   * Update cost for a selection of PASs only
   * 
   * @param pass             collection of specific PASs to update
   * @param linkSegmentCosts to use
   */
  public void updatePassCosts(Collection<Pas<V,ES>> pass, double[] linkSegmentCosts, boolean updateAdjustmentFactor) {
    for (var pas : pass) {
      pas.updateCost(linkSegmentCosts, updateAdjustmentFactor);
    }
  }

  /**
   * Construct a priority queue based on the active PASs reduced cost, i.e., difference between their high and low
   * cost segments in descending order.
   *
   * @param pasComparator to use
   * @return sorted PAS queue in descending order, i.e., highest reduced cost first
   */
  public Collection<Pas<V,ES>> getActivePassSortedByReducedCost(Comparator<Pas<V,ES>> pasComparator) {
    var sortedList = new ArrayList<Pas<V,ES>>((int) getNumberOfActivePass());
    forEachActivePas(sortedList::add);
    sortedList.sort(pasComparator);
    return sortedList;
  }

  /**
   * Loop over all active Pass
   * 
   * @param pasConsumer to apply
   */
  public void forEachActivePas(Consumer<Pas<V,ES>> pasConsumer) {
    activePassByVertex.forEach((v, pc) -> {
      pc.forEach(pasConsumer);
    });
  }

  /**
   * Loop over all inactive Pass
   *
   * @param pasConsumer to apply
   */
  public void forEachInactivePas(Consumer<Pas<V,ES>> pasConsumer) {
    inactivePassByVertex.forEach((v, pc) -> {
      pc.forEach(pasConsumer);
    });
  }

  /**
   * Number of active PASs registered
   * 
   * @return number of PASs registered
   */
  public long getNumberOfActivePass() {
    long numPass = 0;
    for (var pass : activePassByVertex.values()) {
      numPass += pass.size();
    }
    return numPass;
  }

  /**
   * Remove bushes from active Pass on which the bush is currently registered and the predicate provided holds
   *
   * @param bush to check
   * @param pasPredicate to apply
   * @param removeUnusedPass flag to use
   * @return number of pass from which the bush has been removed
   */
  public int removeBushFromActivePasIf(
          RootedBush<V,ES> bush, Predicate<Pas<V,ES>> pasPredicate, boolean removeUnusedPass) {
    int countRemovals = 0;
    List<Pas<V,ES>> passWithoutBush = null; //todo: make set when pas is comparable
    for (var pass : activePassByVertex.values()) {
      for( var pas : pass){
        if(pas.hasRegisteredBush(bush)){
          if(pasPredicate.test(pas)) {
            pas.removeBush(bush);
            ++countRemovals;
          }
        }

        // clean-up PASs if they have no bushes anymore
        if(removeUnusedPass && !pas.hasRegisteredBushes()){
          if (passWithoutBush == null) {
            passWithoutBush = new ArrayList<>();
          }
          passWithoutBush.add(pas);
        }
      }
    }
    if(passWithoutBush!=null && !passWithoutBush.isEmpty()){
      passWithoutBush.forEach(p -> deactivatePas(p, isDetailedLogging()));
    }
    return countRemovals;
  }

  /* GETTERS - SETTERS */

  public boolean isDetailedLogging() {
    return detailedLogging;
  }

  public void setDetailedLogging(boolean detailedLogging) {
    this.detailedLogging = detailedLogging;
  }

  public void reset() {
    this.inactivePassByVertex.clear();
    this.activePassByVertex.clear();
  }
}
