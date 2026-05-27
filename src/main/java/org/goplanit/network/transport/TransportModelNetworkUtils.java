package org.goplanit.network.transport;

import com.google.common.collect.Streams;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.MacroscopicNetworkUtils;
import org.goplanit.network.UntypedPhysicalNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.CompiledRelationIndex;
import org.goplanit.utils.network.layer.physical.Movement;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.virtual.UntypedVirtualNetwork;
import org.goplanit.zoning.Zoning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

public class TransportModelNetworkUtils {

  /**
   * Returns the total number of edge segments available in this traffic assignment by combining the
   * physical and non-physical link segments
   *
   * @param theNetwork to use
   * @param theZoning to use
   * @return total number of physical and virtual edge segments
   */
  public static int getNumberOfEdgeSegmentsAllLayers(LayeredNetwork<?, ?> theNetwork, Zoning theZoning) {
    return getNumberOfPhysicalLinkSegmentsAllLayers(theNetwork) + getNumberOfConnectoidSegments(theZoning);
  }

  /**
   * Returns the total number of connectoid segments available in the zoning's virtual network
   *
   * @param theZoning to use
   * @return the number of connectoid segments
   */
  public static int getNumberOfConnectoidSegments(Zoning theZoning) {
    return getNumberOfConnectoidSegments(theZoning.getVirtualNetwork());
  }

  /**
   * Returns the total number of connectoid segments available based on virtual network
   *
   * @param virtualNetwork to use
   * @return the number of connectoid segments in this network
   */
  public static int getNumberOfConnectoidSegments(UntypedVirtualNetwork<?> virtualNetwork) {
    return virtualNetwork.getLayer().getConnectoidSegments().size();
  }

  /**
   * Returns the total number of link segments available in this physical layered network across all eligible layers
   *
   * @param theNetwork to use
   * @return the number of physical link segments in this network
   */
  public static int getNumberOfPhysicalLinkSegmentsAllLayers(LayeredNetwork<?, ?> theNetwork) {
    int totalPhysicalLinkSegments = 0;
    var networkLayers = theNetwork.getTransportLayers().<UntypedPhysicalLayer<?,?,?>>getLayersOfType();
    for (var layer : networkLayers) {
      totalPhysicalLinkSegments += (int) layer.getNumberOfLinkSegments();
    }
    return totalPhysicalLinkSegments;
  }

  /**
   * Returns the total physical vertices and centroid vertices (of od and/or transfer zones) in this transport network
   *
   * @param physicalNetwork to use
   * @param virtualNetwork to use
   * @return the total number of vertices
   */
  public static int getNumberOfVerticesAllLayers(
          UntypedPhysicalNetwork<?, ?> physicalNetwork, UntypedVirtualNetwork<?> virtualNetwork) {
    return virtualNetwork.getLayer().getVertices().size() + getNumberOfPhysicalNodesAllLayers(physicalNetwork);
  }

  /**
   * Create id mapping in raw array where based on id of any vertex (node, centroid) in the physical and virtual
   * network. If a mismatch is found (any gaps) a warning is issued
   *
   * @param physicalNetwork to use
   * @param virtualNetwork to use
   * @return raw array of vertices by id
   */
  public static DirectedVertex[] createIdIndexedVerticesAllLayers(
          UntypedPhysicalNetwork<?, ?> physicalNetwork, UntypedVirtualNetwork<?>  virtualNetwork) {
    int numberOfVertices = getNumberOfVerticesAllLayers(physicalNetwork, virtualNetwork);
    DirectedVertex[] indexedVertices = new DirectedVertex[numberOfVertices];
    LongAdder count = new LongAdder();
    Consumer<DirectedVertex> lambda = v -> {
      indexedVertices[(int)v.getId()] = v;
      count.increment();
    };

    // ids for vertex internal id should be unique and adjacent across entire transport network, if so this works
    virtualNetwork.getLayer().getVertices().forEach(lambda);
    physicalNetwork.getTransportLayers().stream().flatMap( l -> l.getNodes().stream()).forEach(lambda);

    if( (int)count.longValue() != numberOfVertices){
      throw new PlanItRunTimeException("vertex internal ids across virtual and physical network are not unique, unable" +
              "to created id indexed vertex array transport network wide");
    }
    return indexedVertices;
  }

  /**
   * Returns the total number of physical nodes available in this transport network across all eligible layers
   *
   * @param theNetwork to use
   * @return the number of physical nodes in this network
   */
  public static int getNumberOfPhysicalNodesAllLayers(LayeredNetwork<?, ?> theNetwork) {
    int totalPhysicalNodes = 0;
    var networkLayers = theNetwork.getTransportLayers().<UntypedPhysicalLayer<?,?,?>>getLayersOfType();
    for (var layer : networkLayers) {
      totalPhysicalNodes += (int)layer.getNumberOfNodes();
    }
    return totalPhysicalNodes;
  }

  /**
   * Create a mapping from the original network permissible turns (segment,segment combination) excluding turn bans
   * to a unique index in compiled way so we can attach data to each permissible turn indexed by the combination of
   * the two segments. When banned or a u-turn the returned index is -1.
   *
   * @param transportModelNetwork to use
   * @return compiled index mapping
   */
  public static CompiledRelationIndex createCompiledMovementIdMapping(
      TransportModelNetwork<?,?> transportModelNetwork){

    // ------------------------------------------------------------
    // Step 1: banned movements (original style, no streams)
    // ------------------------------------------------------------
    Map<EdgeSegment, List<EdgeSegment>> bannedByEntryExit = new HashMap<>();
    Streams.concat(
            transportModelNetwork.getVirtualNetwork().getLayer().getMovements().stream(),
            transportModelNetwork.getInfrastructureNetwork().getTransportLayers().stream().flatMap(
                l -> l.getMovements().stream())
        ).filter(Movement::isBanned)
        .forEach(m -> bannedByEntryExit
            .computeIfAbsent(m.getSegmentFrom(), k -> new java.util.ArrayList<>())
            .add(m.getSegmentTo())
        );
    List<EdgeSegment> allSegments = Streams.concat(
        transportModelNetwork.getVirtualNetwork().getLayer().getConnectoidSegments().stream(),
        transportModelNetwork.getInfrastructureNetwork().getTransportLayers().stream()
            .flatMap(l -> l.getLinkSegments().stream())
    ).collect(java.util.stream.Collectors.toList());


    // Determine max original segment ID
    int maxSegmentId = -1;
    for (EdgeSegment ls : allSegments) {
      maxSegmentId = Math.max(maxSegmentId, (int)ls.getId());
    }

    // Count number of outgoing segments per original in-segment
    // since each conjugate segment is a turn, each instance means, one outgoing segment for the incoming segment
    int[] numExitsPerIncomingSegment = new int[maxSegmentId + 1];
    for (EdgeSegment ls : allSegments) {
      List<EdgeSegment> bannedOut = bannedByEntryExit.get(ls);
      for (var exit : ls.getDownstreamVertex().getExitEdgeSegments()) {
        if (ls.equals(exit)) {
          continue;
        }

        if (bannedOut != null && bannedOut.contains(exit)) {
          continue;
        }

        numExitsPerIncomingSegment[(int) ls.getId()]++;
      }
    }

    // -----------------------------
    // Allocate arrays
    // -----------------------------
    long[][] outgoingByIn = new long[maxSegmentId + 1][];
    long[][] movementByIn = new long[maxSegmentId + 1][];
    for (int i = 0; i <= maxSegmentId; i++) {
      if (numExitsPerIncomingSegment[i] > 0) {
        outgoingByIn[i] = new long[numExitsPerIncomingSegment[i]];
        movementByIn[i] = new long[numExitsPerIncomingSegment[i]];
      }
    }

    // -----------------------------
    // Fill arrays
    // -----------------------------

    int[] cursor = new int[maxSegmentId + 1];
    int nextMovementId = 0;

    for (EdgeSegment ls : allSegments) {
      var inId = (int) ls.getId();
      List<EdgeSegment> bannedOut = bannedByEntryExit.get(ls);
      for (var exit : ls.getDownstreamVertex().getExitEdgeSegments()) {
        if (ls.equals(exit)) {
          continue;
        }

        if (bannedOut != null && bannedOut.contains(exit)) {
          continue;
        }

        int pos = cursor[inId]++;
        outgoingByIn[inId][pos] = exit.getId();
        movementByIn[inId][pos] = nextMovementId++;
      }
    }

    // -----------------------------
    // Create CompiledRelationIndex
    // -----------------------------
    return new CompiledRelationIndex(
        outgoingByIn,
        movementByIn,
        nextMovementId
    );
  }

  /**
   * Based on a given transport model network, generate an id grouping token for the conjugate version of this network
   * embedding information about the original in the description
   *
   * @param transportModelNetwork to use as reference
   * @return created token
   */
  public static IdGroupingToken generateDerivedConjugateIdGroupingToken(
          TransportModelNetwork<?,?> transportModelNetwork) {
    return MacroscopicNetworkUtils.generateDerivedConjugateIdGroupingToken(
            (MacroscopicNetwork) transportModelNetwork.getInfrastructureNetwork());
  }


}
