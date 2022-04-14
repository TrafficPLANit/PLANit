package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.algorithms.shortest.ShortestPathSearchUtils;
import org.goplanit.algorithms.shortest.ShortestSearchType;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;

/**
 * Container class for tracking all unique PASs indexed by their last (merge) vertex
 * 
 * @author markr
 *
 */
public class PasManager {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PasManager.class.getCanonicalName());

  /**
   * reduced cost multiplier, empirical calibrated value to use as threshold to consider shifting flow on an origin matching with a PAS, such that reducedCost_max_bush_PAS_path -
   * reducedCost_min_bush_PAS_path > mu * reducedCost_min_network_PAS_path. 0.5 based on Bar-Gera (2010)
   */
  private static final double MU = 0.5;

  /**
   * Flow Effective factor nu, empirically calibrated value to use as threshold to consider shifting flow on an origin matching with a PAS, such that max_cost_PAS_path_flow -
   * max_cost_PAS_path_flow > nu * min_network_PAS_path_flow. 0.25 based on Xie and Xie (2015)
   */
  private static final double NU = 0.25;

  /**
   * Map storing all PASs by their reference vertex
   */
  private Map<DirectedVertex, Collection<Pas>> passByVertex;
  
  /** flag indicating if we store PASs by their downstream merge, or upstream diverge */
  private final boolean registerByDiverge;
  
  /** lambda to obtain reference vertex to use to store PAS in container */
  private Function<Pas, DirectedVertex> getReferenceVertex;

  /** a comparator to compare PASs based on the reduced cost between their high and low cost segments */
  private static final Comparator<Pas> PAS_REDUCED_COST_COMPARATOR;
  
  static {
    /*
     * compare by normalised reduced cost in descending order (from high reduced cost to low reduced cost), use very high precision to make sure very small cost differences are
     * still considered as much as possible. We use normalised cost to ensure that small PASs are not disadvantaged compared to overlapping larger PASs since the smaller the PAS
     * the better the convergence so if anything they should be favoured and processed earlier
     */
    PAS_REDUCED_COST_COMPARATOR = new Comparator<Pas>() {
      @Override
      public int compare(Pas p1, Pas p2) {
        if (Precision.greater(p1.getReducedCost(), p2.getReducedCost(), Precision.EPSILON_15)) {
          return -1;
        } else if (Precision.smaller(p1.getReducedCost(), p2.getReducedCost(), Precision.EPSILON_15)) {
          return 1;
        } else {
          return 0;
        }
      }
    };    
  }

  /** flag for detailed logging */
  private boolean detailedLogging = DETAILED_LOGGING;

  /**
   * Use the accepted flow on the bush from start-to-end of PAS high cost segment and make sure it exceeds NU * total accepted flow (on the bush at hand) on the final edge segment
   * of the PAS high cost segment. This is an adaptation from Bar-Gera who uses the minimum across all high cost segments on the bush, but since we are capacitated this won't be
   * representative. So instead we use the portion of the total flow on the final segment that belongs to the high-cost sub-path present on the bush instead.
   * <p>
   * the rationale here is that we should only consider the PAS as effective for this bush, i.e., consider it for inclusion - if a decent amount of flow leading to the end point of
   * this PAS comes from the high cost segment of this PAS which would allow for a decent chunk of the flow to be shifted to the low cost segment. If not, it would not improve this
   * bush much if we would consider it.
   * 
   * @param pas                   under consideration for a bush
   * @param originBush            the accepted flow found on the bush traversing the high cost PAS and reaching the end vertex (including final alpha) of the PAS
   * @param flowAcceptanceFactors the accepted flow found passing through the final vertex of the PAS from the origin of the bush, i.e., all sub-paths to this vertex
   * @return true when considered effective, false otherwise
   */
  private boolean isFlowEffective(Pas pas, RootedBush originBush, double[] flowAcceptanceFactors) {
    boolean lowCostPath = true;
    /* usage of high cost segment on bush */
    double s2SubPathAcceptedFlowOnBush = pas.computeOverlappingAcceptedFlow(originBush, !lowCostPath, flowAcceptanceFactors);
    /* usage of segment arriving at merge vertex in bush */
    EdgeSegment s2LastEdgeSegment = pas.getLastEdgeSegment(!lowCostPath);
    double s2LastSegmentSendingFlowOnBush = originBush.getSendingFlowPcuH(s2LastEdgeSegment);
    double s2LastSegmentAcceptedFlowOnBush = s2LastSegmentSendingFlowOnBush * flowAcceptanceFactors[(int) s2LastEdgeSegment.getId()];

    return Precision.greater(s2SubPathAcceptedFlowOnBush, NU * s2LastSegmentAcceptedFlowOnBush);
  }

  /**
   * Verify if PAS is considered effective (enough) to improve the provided bush. This is verified by being both {@link #isCostEffective(Pas, double)} and
   * {@link #isFlowEffective(Pas, RootedBush, double[])}
   * 
   * @param pas                   to use
   * @param originBush            to use
   * @param flowAcceptanceFactors to use
   * @param reducedCost           to use
   * @return true when considered effective, false otherwise
   */
  private boolean isPasEffectiveForBush(Pas pas, RootedBush originBush, double[] flowAcceptanceFactors, double reducedCost) {
    /* Verify if low-cost PAS alternative is effective (enough) in improving the bush within the identified upper bound of the reduced cost */
    return isCostEffective(pas.getAlternativeHighCost(), pas.getAlternativeLowCost(), reducedCost) && isFlowEffective(pas, originBush, flowAcceptanceFactors);
  }
  
  /** Based on the reference vertex relative to the PAS we obtain the correct vertex from a list of edge segments that are assumed to represent a PAS alternative
   * 
   * @param alternative to collect from
   * @return found reference vertex
   */
  private DirectedVertex getReferenceVertexFromAlternative(List<EdgeSegment> alternative) {
    DirectedVertex referenceVertex = null;
    if(this.registerByDiverge) {
      referenceVertex = alternative.get(0).getUpstreamVertex();
    }else {
      referenceVertex = alternative.get(alternative.size() - 1).getDownstreamVertex();
    }
    return referenceVertex;
  }
  
  /** Based on the reference vertex relative to the PAS we obtain the correct vertex from a list of edge segments that are assumed to represent a PAS alternative
   * 
   * @param alternative to collect from
   * @return found reference vertex
   */
  private DirectedVertex getReferenceVertexFromAlternative(EdgeSegment[] alternative) {
    DirectedVertex referenceVertex = null;
    if(this.registerByDiverge) {
      referenceVertex = alternative[0].getUpstreamVertex();
    }else {
      referenceVertex = alternative[alternative.length-1].getDownstreamVertex();
    }
    return referenceVertex;
  }

  /** default for detailed logging flag */
  public static final boolean DETAILED_LOGGING = false;

  /**
   * Verify if extending a bush with the given PAS given the reduced cost found, it would be effective in improving the bush. This is verified by
   * <p>
   * reducedCost = bush_min_path_cost - PAS_min_path_cost, then <br\> it is considered effective if <br\> (PAS_max_path_cost - PAS_min_path_cost) > mu*bushReducedCost.
   * <p>
   * Formulation based on Bar-Gera (2010). IDea is that if the PAS has little difference between high and low cost, we can't shift much flow to improve and it is less attractive.
   * This is ok if the reduced cost, i.e., the maximum improvement given the current state of the network, is also low, but when the best option (which might not exactly follow
   * this PAS) is much better than what this PAS offers, we regard this PAS as not cost-effective and ignore it as a viable option.
   * 
   * @return true when considered effective, false otherwise
   */
  public static boolean isCostEffective(double alternativeHighCost, double alternativeLowCost, double reducedCost) {
    return Precision.greater(alternativeHighCost - alternativeLowCost, MU * reducedCost);
  }  

  /**
   * Extract a subpath in the form of a raw edge segment array in downstream direction based on the shortest path result provided. Since the path tree is in reverse direction, the
   * array is filled from the back, i.e.,if there is spare capacity the front of the array would be empty.
   * 
   * @param closestToSearchRoot       vertex in relation to searchResult tree
   * @param furthestFromSearchRoot    vertex in relation to searchResult tree
   * @param searchResultTree          to extract path from, tree's direction is automatically accounted for
   * @param arrayLength               to use for the to be created array which should be at least as long as the path that is to be extracted
   * @param truncateArray             flag indicating to truncate the subpath array in case the front of the array is not fully used due to the existence of spare capacity
   * @return created array in downstream direction, null if no path could be found
   */
  public static EdgeSegment[] createSubpathArrayFrom(final DirectedVertex closestToSearchRoot, final DirectedVertex furthestFromSearchRoot, final ShortestPathResult searchResultTree, int arrayLength,
      boolean truncateArray) {

    EdgeSegment currEdgeSegment = null;
    EdgeSegment[] edgeSegmentArray = new EdgeSegment[arrayLength];
    DirectedVertex currVertex = furthestFromSearchRoot;

    /*
     * depending on the search direction, i.e., the direction of the to-be extract segments, we revert the way we add them to the resulting array to obtain the correct final
     * direction of edge segments in downstream direction
     */
    boolean searchInverted = searchResultTree.getSearchType().isInverted();
    
    /* run from end to start backward while adding in reverse to final array, unless search was inverted, then we go from start to end */
    int index = arrayLength - 1;
    if (searchInverted) {
      index = 0;
    }

    do {

      currEdgeSegment = searchResultTree.getNextEdgeSegmentForVertex(currVertex);
      edgeSegmentArray[index] = currEdgeSegment;
      if (currEdgeSegment == null) {
        LOGGER.warning(String.format("Unable to extract subpath from start vertex %s to end vertex %s, no incoming edge segment available at intermediate vertex %s",
            closestToSearchRoot.getXmlId(), furthestFromSearchRoot.getXmlId(), currVertex.getXmlId()));
        return null;
      }
      currVertex = searchResultTree.getNextVertexForEdgeSegment(currEdgeSegment);
      
      if (searchInverted) {
        ++index;
      } else {
        --index;
      }      
    } while (!currVertex.idEquals(closestToSearchRoot));

    if (truncateArray) {
      if (!searchInverted && index > 0){
        return Arrays.copyOfRange(edgeSegmentArray, index+1, edgeSegmentArray.length);
      }else if(searchInverted && index < arrayLength) {
        return Arrays.copyOfRange(edgeSegmentArray, 0, index);
      }
    }
    return edgeSegmentArray;
  }

  /**
   * Extract a subpath in the form of a raw edge segment array in downstream direction based on the breadth-first (BF) search result provided. This search result is expected to be constructed from the regular shortest path result 
   * which direction depends on the search type. the BF search results are expected to be provided in the SAME direction as the search itself (unlike shortestXResults which are in the opposite direction), i.e., if the search was one-to-all (not inverted)
   * then the bf results are also provided in the downstream direction, whereas all-to-one is in the opposite direction.
   * 
   * @param closestToSearchRoot         vertex in relation to searchResult tree
   * @param furthestFromSearchRoot      vertex in relation to searchResult tree
   * @param shortestSearchType          shortestSearchType used to obtain inverted search result, i.e., when on-to-all inverted search result is in downstream direction, when all-to-one in upstream direction
   * @param invertedBfSearchResultTree  to extract path from, tree is in inverted direction compared to regular search tree result, i.e., one-to-all search result is normally in upstream direction, here it is in downstream direction etc.
   * @param arrayLength                 to use for the to be created array which should be at least as long as the path that is to be extracted
   * @param truncateArray               flag indicating to truncate the subpath array in case the back of the array is not fully used due to the existence of spare capacity
   * @return created array always in downstream direction, null if no path could be found
   */
  public static EdgeSegment[] createSubpathArrayFrom(DirectedVertex closestToSearchRoot, DirectedVertex furthestFromSearchRoot, ShortestSearchType shortestSearchType, Map<DirectedVertex, EdgeSegment> invertedBfSearchResultTree, int arrayLength,
      boolean truncateArray) {

    /*
     * depending on the original search direction, i.e., the direction of the to-be extract segments, we revert the way we add them to the resulting array to obtain the correct final
     * direction of edge segments in downstream direction
     */
    boolean searchInverted = shortestSearchType.isInverted();
    
    EdgeSegment[] edgeSegmentArray = new EdgeSegment[arrayLength];
    EdgeSegment currEdgeSegment = null;
    
    /* search utils yields lambda based on search type for searching, not result traversal, we traverse results, so we should invert. However, our provided results are inverted already, so double inversion makes that we should not invert */
    var getNextVertex = ShortestPathSearchUtils.getVertexFromEdgeSegmentLambda(shortestSearchType);
    
    /* run from end to start backward while adding in reverse to final array, unless search was inverted, then we go from start to end */
    int index = 0;
    DirectedVertex currVertex = closestToSearchRoot;
    if (searchInverted) {
      index = arrayLength-1;
    }
    
    boolean nextAvailable = true;
    do {
      currEdgeSegment = invertedBfSearchResultTree.get(currVertex);
      edgeSegmentArray[index] = currEdgeSegment;
      if (currEdgeSegment == null) {
        LOGGER.warning(String.format("Unable to extract subpath between vertices (%s, %s), no edge segment available at intermediate vertex %s",
            closestToSearchRoot.getXmlId(), furthestFromSearchRoot.getXmlId(), currVertex.getXmlId()));
        return null;
      }
      currVertex = getNextVertex.apply(currEdgeSegment);
      
      if (searchInverted) {
        nextAvailable = --index >= 0;
      } else {
        nextAvailable = ++index < arrayLength;
      }      
      
    } while (!currVertex.idEquals(furthestFromSearchRoot) && nextAvailable);

    if (!currVertex.idEquals(furthestFromSearchRoot)) {
      LOGGER.warning(String.format("Unable to create subpath array between nodes (%s, %s) from given pathTree", closestToSearchRoot.toString(), furthestFromSearchRoot.toString()));
      return null;
    }

    if (truncateArray && nextAvailable) {
      if(searchInverted) {
        /* inverted, truncate start */
        return Arrays.copyOfRange(edgeSegmentArray, index + 1, arrayLength);
      }else {
        /* regular, truncate end */
        return Arrays.copyOfRange(edgeSegmentArray, 0, index); 
      }      
    }

    return edgeSegmentArray;
  }

  /**
   * Constructor
   * 
   * @param registerByDiverge when true store PASs by (most upstream) diverge vertex, otherwise by their (most downstream) merge vertex 
   */
  public PasManager(boolean registerByDiverge) {
    
    this.registerByDiverge = registerByDiverge;
    if(registerByDiverge) {
      this.getReferenceVertex = p -> p.getDivergeVertex();  
    }else {
      this.getReferenceVertex = p -> p.getMergeVertex();
    }
    
    this.passByVertex = new HashMap<DirectedVertex, Collection<Pas>>();
  }

  /**
   * create a new PAS for the given cheap and expensive paired alternative segments (subpaths) and register the origin bush on it that was responsible for creating it
   * 
   * @param bush responsible for triggering the creation of this PAS
   * @param s1         cheap alternative segment
   * @param s2         expensive alternative segment
   * @return createdPas
   */
  public Pas createAndRegisterNewPas(final RootedBush bush, final EdgeSegment[] s1, final EdgeSegment[] s2) {
    Pas newPas = Pas.create(s1, s2);
    if (newPas == null) {
      return null;
    }

    newPas.registerBush(bush);
    passByVertex.putIfAbsent(this.getReferenceVertex.apply(newPas), new ArrayList<Pas>());
    passByVertex.get(this.getReferenceVertex.apply(newPas)).add(newPas);
    return newPas;
  }

  /**
   * create a new PAS for the given cheap and expensive paired alternative segments (subpaths) and register the origin bush on it that was responsible for creating it
   * 
   * @param bush responsible for triggering the creation of this PAS
   * @param s1         cheap alternative segment
   * @param s2         expensive alternative segment
   * @return createdPas
   */
  public Pas createAndRegisterNewPas(final RootedBush bush, final Collection<EdgeSegment> s1, final Collection<EdgeSegment> s2) {
    return createAndRegisterNewPas(bush, s1.toArray(new EdgeSegment[s1.size()]), s2.toArray(new EdgeSegment[s2.size()]));
  }

  /**
   * Remove the PAS from the manager
   * 
   * @param pas           to remove
   * @param logRemovedPas when true log removed pas, when false do not
   */
  public void removePas(final Pas pas, boolean logRemovedPas) {
    passByVertex.get(this.getReferenceVertex.apply(pas)).remove(pas);
    if (logRemovedPas) {
      LOGGER.info(String.format("Removed existing PAS: %s", pas.toString()));
    }
  }

  /**
   * Collect all PASs that share the same reference vertex. Make sure this vertex is in line with the manager's chosen reference vertex (diverge or merge vertex of the PAS container key)
   * 
   * @param referenceVertex to collect for
   * @return found PAS matches, null if none
   */
  public Collection<Pas> getPassByReferenceVertex(final DirectedVertex referenceVertex) {
    return passByVertex.get(referenceVertex);
  }

  /**
   * Find PAS that exactly matches the provides alternative segments. Identical to {@link #findExistingPas(EdgeSegment[], EdgeSegment[])}
   * 
   * @param alternative1 alternative segment of PAS
   * @param alternative2 alternative segment of PAS
   * @return the matching PAS, null otherwise
   */
  public Pas findExistingPas(final List<EdgeSegment> alternative1, final List<EdgeSegment> alternative2) {
    if (alternative1 == null || alternative2 == null) {
      LOGGER.severe("one or more alternatives of potential PAS are null");
      return null;
    }
    if (alternative1.isEmpty() || alternative2.isEmpty()) {
      LOGGER.severe("one or more alternatives of potential PAS are empty");
      return null;
    }

    var potentialPass = getPassByReferenceVertex(getReferenceVertexFromAlternative(alternative1));
    if (potentialPass == null) {
      return null;
    }

    for (Pas potentialPas : potentialPass) {
      if (potentialPas.isAlternativeEqual(alternative1, true) && potentialPas.isAlternativeEqual(alternative2, false)) {
        return potentialPas;
      } else if (potentialPas.isAlternativeEqual(alternative2, true) && potentialPas.isAlternativeEqual(alternative1, false)) {
        return potentialPas;
      }
    }

    return null;
  }

  /**
   * Find PAS that exactly matches the provides alternative segments. Identical to {@link #findExistingPas(ArrayList, ArrayList)}
   * 
   * @param alternative1 alternative segment of PAS
   * @param alternative2 alternative segment of PAS
   * @return the matching PAS, null otherwise
   */
  public Pas findExistingPas(final EdgeSegment[] alternative1, final EdgeSegment[] alternative2) {
    if (alternative1 == null || alternative2 == null) {
      LOGGER.severe("one or more alternatives of potential PAS are null");
      return null;
    }

    var potentialPass = getPassByReferenceVertex(getReferenceVertexFromAlternative(alternative1));
    if (potentialPass == null) {
      return null;
    }

    for (Pas potentialPas : potentialPass) {
      if (potentialPas.isAlternativeEqual(alternative1, true) && potentialPas.isAlternativeEqual(alternative2, false)) {
        return potentialPas;
      } else if (potentialPas.isAlternativeEqual(alternative2, true) && potentialPas.isAlternativeEqual(alternative1, false)) {
        return potentialPas;
      }
    }

    return null;
  }

  /**
   * Verify if any PAS at given reference vertex is used by this origin bush.
   * 
   * @param bush  to test for
   * @param referenceVertex to test for
   * @return true when PAS is used by origin bush ending at this vertex, false otherwise
   */
  public boolean isRegisteredOnAnyPasAtReferenceVertex(final RootedBush bush, final DirectedVertex referenceVertex) {
    /* verify potential PASs */
    var potentialPass = getPassByReferenceVertex(referenceVertex);
    if (potentialPass == null) {
      return false;
    }

    for (Pas pas : potentialPass) {
      if (pas.hasRegisteredBush(bush)) {
        return true;
      }
    }

    return false;
  }

  /**
   * find the first PAS which has the given merge vertex as end vertex and which if we would extend the bush with its least cost alternative would improve to the point it is
   * considered effective enough compared to the upper bound (reduced cost) improvement provided as well as that the bush has sufficient flow on the high cost alternative of the
   * PAS such that it can improve sufficiently by shifting flow towards the new low cost segment. If this all holds the PAS is selected and returned. We select the first PAS we can
   * find that matches this criteria.
   * 
   * @param bush            to find suitable PAS for
   * @param referenceVertex           to use
   * @param flowAcceptanceFactors to use (required to assess flow effectiveness in capacitated context)
   * @param reducedCost           the upper bound on the improvement that is known for this merge vertex
   * @return pas found, null if no suitable candidates exist
   */
  public Pas findFirstSuitableExistingPas(final RootedBush bush, final DirectedVertex referenceVertex, double[] flowAcceptanceFactors, double reducedCost) {

    /* verify potential PASs */
    var potentialPass = getPassByReferenceVertex(referenceVertex);
    if (potentialPass == null) {
      return null;
    }

    Pas matchedPas = null;
    for (Pas pas : potentialPass) {
      if (pas.hasRegisteredBush(bush)) {
        continue;
      }
      
      /* check if PAS is attached upstream to bush - even if it is just with the vertex */
      boolean pasPotentialMatch = false;
      for (var pasFirstExitSegment : pas.getDivergeVertex().getExitEdgeSegments()) {
        if (bush.containsEdgeSegment(pasFirstExitSegment)) {
          pasPotentialMatch = true;
          break;
        }
      }
      
      if(!pasPotentialMatch) {
        continue;
      }
      
      /* check if PAS is attached downstream to bush - even if it is just with the vertex */
      for (var pasLastEntrySegment : pas.getMergeVertex().getEntryEdgeSegments()) {
        if (bush.containsEdgeSegment(pasLastEntrySegment)) {
          pasPotentialMatch = true;
          break;
        }
      }      

      if (!pasPotentialMatch) {
        continue;
      }

      /* PAS start/end node are on bush, now check if it is effective in reducing cost/shifting flow */
      if (!isPasEffectiveForBush(pas, bush, flowAcceptanceFactors, reducedCost)) {
        continue;
      }

      /* deemed effective, now ensure it does not introduce cycles */
      if (bush.determineIntroduceCycle(pas.getAlternative(true)) != null) {
        continue;
      }

      matchedPas = pas;
      break;

    }
    return matchedPas;
  }

  /**
   * Update costs on all registered PASs
   * 
   * @param linkSegmentCosts to use
   */
  public void updateCosts(final double[] linkSegmentCosts) {
    for (Collection<Pas> pass : passByVertex.values()) {
      updateCosts(pass, linkSegmentCosts);
    }
  }

  /**
   * Update cost for a selection of PASs only
   * 
   * @param pass             collection of specific PASs to update
   * @param linkSegmentCosts to use
   */
  public void updateCosts(Collection<Pas> pass, double[] linkSegmentCosts) {
    for (Pas pas : pass) {
      pas.updateCost(linkSegmentCosts);
    }
  }

  /**
   * Construct a priority queue based on the PASs reduced cost, i.e., difference between their high and low cost segments in descending order.
   * 
   * @return sorted PAS queue in descending order, i.e., highest reduced cost first
   */
  public Collection<Pas> getPassSortedByReducedCost() {
    var sortedList = new ArrayList<Pas>((int) getNumberOfPass());
    forEachPas((pas) -> sortedList.add(pas));
    Collections.sort(sortedList, PAS_REDUCED_COST_COMPARATOR);
    return sortedList;
  }

  /**
   * Loop over all Pass
   * 
   * @param pasConsumer to apply
   */
  public void forEachPas(Consumer<Pas> pasConsumer) {
    passByVertex.forEach((v, pc) -> {
      pc.forEach(pasConsumer);
    });
  }

  /**
   * Number of PASs registered
   * 
   * @return number of PASs registered
   */
  public long getNumberOfPass() {
    long numPass = 0;
    for (var pass : passByVertex.values()) {
      numPass += pass.size();
    }
    return numPass;

  }

  /* GETTERS - SETTERS */

  public boolean isDetailedLogging() {
    return detailedLogging;
  }

  public void setDetailedLogging(boolean detailedLogging) {
    this.detailedLogging = detailedLogging;
  }

}
