package org.goplanit.network.transport;

import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.virtual.UntypedVirtualNetwork;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.zoning.Zoning;

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
    var networkLayers = theNetwork.getTransportLayers().<MacroscopicNetworkLayerImpl>getLayersOfType();
    for (var layer : networkLayers) {
      totalPhysicalLinkSegments += layer.getNumberOfLinkSegments();
    }
    return totalPhysicalLinkSegments;
  }

  /**
   * Returns the total physical vertices and centroid vertices (of od and/or transfer zones) in this transport network
   *
   * @param physicalNetwork to use
   * @param zoning to use
   * @return the total number of vertices
   */
  public static int getNumberOfVerticesAllLayers(LayeredNetwork<?, ?> physicalNetwork, Zoning zoning) {
    return zoning.getOdZones().getNumberOfCentroids() + zoning.getTransferZones().getNumberOfCentroids() + getNumberOfPhysicalNodesAllLayers(physicalNetwork);
  }

  /**
   * Returns the total number of physical nodes available in this transport network across all eligible layers
   *
   * @param theNetwork to use
   * @return the number of physical nodes in this network
   */
  public static int getNumberOfPhysicalNodesAllLayers(LayeredNetwork<?, ?> theNetwork) {
    int totalPhysicalNodes = 0;
    var networkLayers = theNetwork.getTransportLayers().<UntypedPhysicalLayer>getLayersOfType();
    for (var layer : networkLayers) {
      totalPhysicalNodes += layer.getNumberOfNodes();
    }
    return totalPhysicalNodes;
  }

}
