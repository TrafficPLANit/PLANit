package org.goplanit.network.transport;

import java.util.Collection;
import java.util.logging.Logger;

import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.network.virtual.VirtualNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.goplanit.utils.network.virtual.ConnectoidEdgeFactory;
import org.goplanit.utils.network.virtual.ConnectoidSegmentFactory;
import org.goplanit.utils.zoning.DirectedConnectoid;
import org.goplanit.utils.zoning.UndirectedConnectoid;
import org.goplanit.zoning.Zoning;

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
  protected final LayeredNetwork<?, ?> infrastructureNetwork;

  /**
   * Holds the zoning structure and virtual transport network interfacing with the physical network
   */
  protected final Zoning zoning;

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
   * create and register the edge segments for the passed in connectoid edges, XML id set to id prefixed with "c".
   * 
   * @param virtualNetwork  to create and register on
   * @param connectoidEdges to process
   * @throws PlanItException thrown if error
   */
  protected void createAndRegisterConnectoidEdgeSegments(VirtualNetwork virtualNetwork, Collection<ConnectoidEdge> connectoidEdges) throws PlanItException {

    ConnectoidSegmentFactory connectoidSegmentFactory = virtualNetwork.getConnectoidSegments().getFactory();
    for (ConnectoidEdge connectoidEdge : connectoidEdges) {
      var segment = connectoidSegmentFactory.registerNew(connectoidEdge, true);
      segment.setXmlId("c" + segment.getId());
      segment = connectoidSegmentFactory.registerNew(connectoidEdge, false);
      segment.setXmlId("c" + segment.getId());
      connectVerticesToEdge(connectoidEdge);
    }
  }

  /**
   * Constructor
   * 
   * @param infrastructureNetwork the network used to generate this TransportNetwork
   * @param zoning                the Zoning used to generate this TransportNetwork
   */
  public TransportModelNetwork(LayeredNetwork<?, ?> infrastructureNetwork, Zoning zoning) {
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
    for (UndirectedConnectoid undirectedConnectoid : zoning.getOdConnectoids()) {
      /* undirected connectoid (virtual) edge between zone centroid and access node */
      Collection<ConnectoidEdge> connectoidEdges = connectoidEdgeFactory.registerNew(undirectedConnectoid);
      for (ConnectoidEdge connectoidEdge : connectoidEdges) {
        connectVerticesToEdge(connectoidEdge);
      }
      createAndRegisterConnectoidEdgeSegments(virtualNetwork, connectoidEdges);

    }
    for (DirectedConnectoid directedConnectoid : zoning.getTransferConnectoids()) {
      /* directed connectoid (virtual) edge between zone centroid and access link segment's downstream node */
      Collection<ConnectoidEdge> connectoidEdges = connectoidEdgeFactory.registerNew(directedConnectoid);
      for (ConnectoidEdge connectoidEdge : connectoidEdges) {
        connectVerticesToEdge(connectoidEdge);
      }
      createAndRegisterConnectoidEdgeSegments(virtualNetwork, connectoidEdges);
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
    return zoning.getOdZones().getNumberOfCentroids() + zoning.getTransferZones().getNumberOfCentroids() + getNumberOfPhysicalNodesAllLayers();
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

    /* clear out contents */
    zoning.getVirtualNetwork().clear();
  }

  /**
   * Collect the physical network component of the transport network
   * 
   * @return physicalNetwork
   */
  public LayeredNetwork<?, ?> getInfrastructureNetwork() {
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
