package org.goplanit.network.transport;

import java.util.Collection;
import java.util.logging.Logger;

import org.goplanit.network.TransportLayerNetwork;
import org.goplanit.network.layer.MacroscopicNetworkLayerImpl;
import org.goplanit.network.virtual.VirtualNetwork;
import org.goplanit.zoning.Zoning;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.goplanit.utils.network.virtual.ConnectoidEdgeFactory;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.network.virtual.ConnectoidSegmentFactory;
import org.goplanit.utils.zoning.DirectedConnectoid;
import org.goplanit.utils.zoning.UndirectedConnectoid;

/**
 * Entire transport network that is being modeled including both the physical and virtual aspects of it as well as the zoning. It acts as a wrapper unifying the two components
 * during the assignment stage
 * 
 * @author markr
 *
 */
public class TransportModelNetwork {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(TransportModelNetwork.class.getCanonicalName());

  /**
   * log info on transport model network assuming it has integrated virtual and physical network it reports on the connectoid edges and segments to do so
   */
  private void logInfo() {
    LOGGER.info(String.format("#od connectoid edges: %d", getVirtualNetwork().getConnectoidEdges().size()));
    LOGGER.info(String.format("#od connectoid segments: %d", getVirtualNetwork().getConnectoidSegments().size()));
  }

  /**
   * Holds the infrastructure road network that is being modelled
   */
  protected final TransportLayerNetwork<?, ?> infrastructureNetwork;

  /**
   * Holds the zoning structure and virtual transport network interfacing with the physical network
   */
  protected final Zoning zoning;

  /**
   * Add edge segment to the incoming or outgoing set of edge segments for the related vertices
   * 
   * @param edgeSegment EdgeSegment to be added to upstream and downstream vertices
   * @throws PlanItException thrown if there is an error
   */
  protected void connectVerticesToEdgeSegment(EdgeSegment edgeSegment) throws PlanItException {
    edgeSegment.getUpstreamVertex().addEdgeSegment(edgeSegment);
    edgeSegment.getDownstreamVertex().addEdgeSegment(edgeSegment);
  }

  /**
   * Remove edge segment from the incoming or outgoing set of edge segments for the related vertices
   * 
   * @param edgeSegment the EdgeSegment object to be removed from the network
   * @throws PlanItException thrown if there is an error
   */
  protected void disconnectVerticesFromEdgeSegment(EdgeSegment edgeSegment) throws PlanItException {
    edgeSegment.getUpstreamVertex().removeEdgeSegment(edgeSegment);
    edgeSegment.getDownstreamVertex().removeEdgeSegment(edgeSegment);
  }

  /**
   * Add Edge to both vertices
   * 
   * @param edge Edge to be added to upstream and downstream vertices
   * @throws PlanItException thrown if there is an error
   */
  protected void connectVerticesToEdge(Edge edge) throws PlanItException {
    edge.getVertexA().addEdge(edge);
    edge.getVertexB().addEdge(edge);
  }

  /**
   * Remove Edge from both vertices
   * 
   * @param edge Edge to be removed from upstream and downstream vertices
   * @throws PlanItException thrown if there is an error
   */
  protected void disconnectVerticesFromEdge(Edge edge) throws PlanItException {
    edge.getVertexA().removeEdge(edge);
    edge.getVertexB().removeEdge(edge);
  }

  // Public

  /**
   * create and register the edge segments for the passed in connectoid edges
   * 
   * @param virtualNetwork  to create and register on
   * @param connectoidEdges to process
   * @throws PlanItException thrown if error
   */
  protected void createAndRegisterConectoidEdgeSegments(VirtualNetwork virtualNetwork, Collection<ConnectoidEdge> connectoidEdges) throws PlanItException {

    ConnectoidSegmentFactory connectoidSegmentFactory = virtualNetwork.getConnectoidSegments().getFactory();
    for (ConnectoidEdge connectoidEdge : connectoidEdges) {
      connectoidSegmentFactory.registerNew(connectoidEdge, true);
      connectoidSegmentFactory.registerNew(connectoidEdge, false);
      connectVerticesToEdge(connectoidEdge);
    }
  }

  /**
   * Constructor
   * 
   * @param infrastructureNetwork the network used to generate this TransportNetwork
   * @param zoning                the Zoning used to generate this TransportNetwork
   */
  public TransportModelNetwork(TransportLayerNetwork<?, ?> infrastructureNetwork, Zoning zoning) {
    this.infrastructureNetwork = infrastructureNetwork;
    this.zoning = zoning;
  }

  /**
   * Integrate physical and virtual links within od zones (undirected connectoid access node and centroid)
   * 
   * @throws PlanItException thrown if there is an error
   */
  public void integrateTransportNetworkViaConnectoids() throws PlanItException {
    LOGGER.info(String.format("Integrating physical network %d (xml id %s) with zoning %d (XML id %s)", infrastructureNetwork.getId(),
        infrastructureNetwork.getXmlId() != null ? infrastructureNetwork.getXmlId() : "N/A", zoning.getId(), zoning.getXmlId() != null ? zoning.getXmlId() : "N/A"));

    VirtualNetwork virtualNetwork = zoning.getVirtualNetwork();
    ConnectoidEdgeFactory connectoidEdgeFactory = virtualNetwork.getConnectoidEdges().getFactory();
    for (UndirectedConnectoid undirectedConnectoid : zoning.odConnectoids) {
      /* undirected connectoid (virtual) edge between zone centroid and access node */
      Collection<ConnectoidEdge> connectoidEdges = connectoidEdgeFactory.registerNew(undirectedConnectoid);
      createAndRegisterConectoidEdgeSegments(virtualNetwork, connectoidEdges);

    }
    for (DirectedConnectoid directedConnectoid : zoning.transferConnectoids) {
      /* directed connectoid (virtual) edge between zone centroid and access link segment's downstream node */
      Collection<ConnectoidEdge> connectoidEdges = connectoidEdgeFactory.registerNew(directedConnectoid);
      createAndRegisterConectoidEdgeSegments(virtualNetwork, connectoidEdges);
    }
    for (ConnectoidSegment connectoidSegment : virtualNetwork.getConnectoidSegments()) {
      connectVerticesToEdgeSegment(connectoidSegment);
    }

    logInfo();
  }

  /**
   * Returns the total number of edge segments available in this traffic assignment by combining the physical and non-physical link segments
   * 
   * @return total number of physical and virtual edge segments
   */
  public int getNumberOfEdgeSegmentsAllLayers() {
    return getNumberOfPhysicalLinkSegmentsAllLayers() + getNumberOfConnectoidSegments();
  }

  /**
   * Returns the total number of link segments available in this transport network across all eligible layers
   * 
   * @return the number of physical link segments in this network
   */
  public int getNumberOfPhysicalLinkSegmentsAllLayers() {
    int totalPhysicalLinkSegments = 0;
    Collection<MacroscopicNetworkLayerImpl> networkLayers = getInfrastructureNetwork().getTransportLayers().<MacroscopicNetworkLayerImpl>getLayersOfType();
    for (MacroscopicNetworkLayerImpl layer : networkLayers) {
      totalPhysicalLinkSegments += layer.getNumberOfLinkSegments();
    }
    return totalPhysicalLinkSegments;
  }

  /**
   * Returns the total number of connectoid segments available in this transport network
   * 
   * @return the number of connectoid segments in this network
   */
  public int getNumberOfConnectoidSegments() {
    return zoning.getVirtualNetwork().getConnectoidSegments().size();
  }

  /**
   * Returns the total physical vertices and centroid vertices (of od and/or transfer zones) in this transport network
   * 
   * @return the total number of vertices
   */
  public int getNumberOfVerticesAllLayers() {
    return zoning.odZones.getNumberOfCentroids() + zoning.transferZones.getNumberOfCentroids() + getNumberOfPhysicalNodesAllLayers();
  }

  /**
   * Returns the total number of physical nodes available in this transport network across all eligible layers
   * 
   * @return the number of physical nodes in this network
   */
  @SuppressWarnings("rawtypes")
  public int getNumberOfPhysicalNodesAllLayers() {
    int totalPhysicalNodes = 0;
    Collection<UntypedPhysicalLayer> networkLayers = getInfrastructureNetwork().getTransportLayers().<UntypedPhysicalLayer>getLayersOfType();
    for (UntypedPhysicalLayer layer : networkLayers) {
      totalPhysicalNodes += layer.getNumberOfNodes();
    }
    return totalPhysicalNodes;
  }

  /**
   * Remove the edges and edge segments on the vertices of both virtual and physical networks
   * 
   * @throws PlanItException thrown if there is an error
   */
  public void removeVirtualNetworkFromPhysicalNetwork() throws PlanItException {
    for (ConnectoidEdge connectoidEdge : zoning.getVirtualNetwork().getConnectoidEdges()) {
      disconnectVerticesFromEdge(connectoidEdge);
    }
    for (ConnectoidSegment connectoidSegment : zoning.getVirtualNetwork().getConnectoidSegments()) {
      disconnectVerticesFromEdgeSegment(connectoidSegment);
    }

    /* clear out contents */
    zoning.getVirtualNetwork().clear();
  }

  /**
   * Collect the physical network component of the transport network
   * 
   * @return physicalNetwork
   */
  public TransportLayerNetwork<?, ?> getInfrastructureNetwork() {
    return infrastructureNetwork;
  }

  /**
   * Collect the virtual network component of the transport network
   * 
   * @return virtualNetwork
   */
  public VirtualNetwork getVirtualNetwork() {
    return zoning.getVirtualNetwork();
  }

  /**
   * Collect the zoning structure
   * 
   * @return zoning
   */
  public Zoning getZoning() {
    return zoning;
  }

}