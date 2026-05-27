package org.goplanit.network.transport;

import com.google.common.collect.Streams;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.CompiledRelationMapping;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;

import java.lang.reflect.Array;
import java.util.stream.Stream;

/**
 * Utilities for conjugate transport model networks
 *
 * @author markr
 */
public class ConjugateTransportModelNetworkUtils {


  /** Dummy constructor */
  private ConjugateTransportModelNetworkUtils(){}

  /**
   * Create an inverted mapping from the original network turns (segment,segment combination) to the conjugate
   * edge and connectoid segments in the conjugate transport model and store it in efficient compiled index
   *
   * @param conjugateTransportModelNetwork to use
   * @return mapping from original segments (multi key = segment, segment) to conjugate segment
   */
  public static CompiledRelationMapping<ConjugateEdgeSegment> createOriginalSegmentsToConjugateSegmentIdMapping(
      ConjugateTransportModelNetwork conjugateTransportModelNetwork){

    // currently we do not support conjugate (banned) movements, to avoid issues, we chekc if they exists and if so
    // we throw
    if(conjugateTransportModelNetwork.getVirtualNetwork().getLayer().hasMovements() ||
        conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().stream().anyMatch(
            UntypedPhysicalLayer::hasMovements)){
      throw new PlanItRunTimeException("We do not yet support banned movements in conjugate network i.c.w. creating" +
          "a mapping from pairs of segments to a conjugate segment");
    }

    var allConjugateSegmentsStream = Streams.concat(
        conjugateTransportModelNetwork.getVirtualNetwork().getLayer().getConnectoidSegments().stream(),
        conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().stream().flatMap(
            l -> l.getLinkSegments().stream().map( ls -> (ConjugateEdgeSegment) ls)));

    // Determine max original segment ID
    int[] maxOriginalSegmentId = new int[1];
    maxOriginalSegmentId[0] = -1;
    allConjugateSegmentsStream.forEach( cs -> {
      EdgeSegment in = cs.getOriginalAdjacentEdgeSegments().first();
      EdgeSegment out = cs.getOriginalAdjacentEdgeSegments().second();
      maxOriginalSegmentId[0] = Math.max(maxOriginalSegmentId[0], in!=null ? (int) in.getId() : -1);
      maxOriginalSegmentId[0] = Math.max(maxOriginalSegmentId[0], out != null ? (int) out.getId(): -1);
    });

    // re-instate as exhausted from before
    allConjugateSegmentsStream = Streams.concat(
        conjugateTransportModelNetwork.getVirtualNetwork().getLayer().getConnectoidSegments().stream(),
        conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().stream().flatMap(
            l -> l.getLinkSegments().stream().map( ls -> (ConjugateEdgeSegment) ls)));

    // Count number of outgoing segments per original in-segment
    // since each conjugate segment is a turn, each instance means, one outgoing segment for the incoming segment
    int[] countsPerInSegment = new int[maxOriginalSegmentId[0] + 1];
    allConjugateSegmentsStream.forEach( cs -> {
      var inSegment = cs.getOriginalAdjacentEdgeSegments().first();
      if(inSegment == null){
        return; // can be null for virtual conjugate segments exiting from centroid equivalent for example
      }
      if(cs.getOriginalAdjacentEdgeSegments().second() == null){
        return; // can be null for virtual conjugate segments entering centroid equivalent for example
      }
      int inId = (int) inSegment.getId();
      countsPerInSegment[inId]++;
    });

    // -----------------------------
    // Allocate arrays
    // -----------------------------
    long[][] outgoingByIn = new long[maxOriginalSegmentId[0] + 1][];
    ConjugateEdgeSegment[][] idsByIn = new ConjugateEdgeSegment[maxOriginalSegmentId[0] + 1][];
    for (int i = 0; i <= maxOriginalSegmentId[0]; i++) {
      if (countsPerInSegment[i] > 0) {
        outgoingByIn[i] = new long[countsPerInSegment[i]];
        idsByIn[i] = new ConjugateEdgeSegment[countsPerInSegment[i]];
      }
    }

    // -----------------------------
    // Fill arrays
    // -----------------------------
    int[] indexWithinOriginalIncomingForOut = new int[maxOriginalSegmentId[0] + 1];
    long[] maxConjugateId = new long[1];
    maxConjugateId[0] = 0;

    // re-instate as exhausted from before
    allConjugateSegmentsStream = Streams.concat(
        conjugateTransportModelNetwork.getVirtualNetwork().getLayer().getConnectoidSegments().stream(),
        conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().stream().flatMap(
            l -> l.getLinkSegments().stream().map( ls -> (ConjugateEdgeSegment) ls)));
    // NOW DO THE MAPPING
    allConjugateSegmentsStream.forEach( cs -> {
      var inSegment = cs.getOriginalAdjacentEdgeSegments().first();
      if(inSegment == null){
        return; // can be null for virtual conjugate segments exiting from centroid equivalent for example
      }
      int inId = (int) inSegment.getId();
      if(outgoingByIn[inId] == null){
        return; // can be null when no outgoing segments, for example when ending at destination equivalent
      }
      int pos = indexWithinOriginalIncomingForOut[inId]++;
      // out segment id at correct pos
      outgoingByIn[inId][pos] = cs.getOriginalAdjacentEdgeSegments().second().getId();
      // store the actual conjugate id as final result
      idsByIn[inId][pos] = cs;

      if (cs.getId() > maxConjugateId[0]) {
        maxConjugateId[0] = cs.getId();
      }
    });

    // -----------------------------
    // Create CompiledRelationIndex
    // -----------------------------
    return new CompiledRelationMapping<>(
        ConjugateEdgeSegment.class, outgoingByIn, idsByIn, maxConjugateId[0] +1);
  }
}


