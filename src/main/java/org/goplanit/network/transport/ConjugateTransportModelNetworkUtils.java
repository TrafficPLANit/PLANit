package org.goplanit.network.transport;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.virtual.ConjugateVirtualNetworkLayer;
import org.goplanit.utils.network.virtual.UntypedVirtualNetwork;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;
import org.goplanit.zoning.Zoning;

import java.util.HashMap;
import java.util.Map;

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
   * edge and connectoid segments in the conjugate transport model
   *
   * @param conjugateTransportModelNetwork to use
   * @return mapping from original segments (multi key = segment, segment) to conjugate segment
   */
  public static MultiKeyMap<Object, ConjugateEdgeSegment> createOriginalSegmentsToConjugateSegmentsMapping(
          ConjugateTransportModelNetwork conjugateTransportModelNetwork) {
    var mapping = new MultiKeyMap<Object, ConjugateEdgeSegment>();

    /* virtual portion of network */
    conjugateTransportModelNetwork.getVirtualNetwork().getLayer().getConnectoidSegments().forEach(
            cs -> mapping.put(
                    new MultiKey<EdgeSegment>(
                            cs.getOriginalAdjacentEdgeSegments().first(),
                            cs.getOriginalAdjacentEdgeSegments().second()),
                    cs));
    /* physical portion of network */
    conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().forEach(
            l ->l.getLinkSegments().forEach(
                es -> mapping.put(
                        new MultiKey<EdgeSegment>(
                                es.getOriginalAdjacentEdgeSegments().first(),
                                es.getOriginalAdjacentEdgeSegments().second()),
                        es)));
    return mapping;
  }
}
