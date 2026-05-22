package org.goplanit.network.transport;

import com.google.common.collect.Streams;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.CompiledRelationMapping;

import java.lang.reflect.Array;

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
      maxOriginalSegmentId[0] = Math.max(maxOriginalSegmentId[0], (int) in.getId());
      maxOriginalSegmentId[0] = Math.max(maxOriginalSegmentId[0], (int) out.getId());
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
      int inId = (int) cs.getOriginalAdjacentEdgeSegments().first().getId();
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
      int inId = (int) cs.getOriginalAdjacentEdgeSegments().first().getId();
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
        ConjugateEdgeSegment.class, outgoingByIn, idsByIn, maxConjugateId[0]);
  }
}


