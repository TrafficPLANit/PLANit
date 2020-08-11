package org.planit.network.transport;

import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.network.virtual.Connectoid;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 * Entire transport network that is being modeled including both the physical and virtual aspects of it as well as the zoning. It acts as a wrapper unifying the two components
 * during the assignment stage
 * 
 * @author markr
 *
 */
public class TransportNetwork {

  /**
   * Holds the physical road network that is being modeled
   */
  protected final PhysicalNetwork physicalNetwork;

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
   * Constructor
   * 
   * @param physicalNetwork the PhysicalNetwork used to generate this TransportNetwork
   * @param zoning          the Zoning used to generate this TransportNetwork
   */
  public TransportNetwork(PhysicalNetwork physicalNetwork, Zoning zoning) {
    this.physicalNetwork = physicalNetwork;
    this.zoning = zoning;
  }

  /**
   * Returns the total number of edge segments available in this traffic assignment by combining the physical and non-physical link segments
   * 
   * @return total number of physical and virtual edge segments
   */
  public int getTotalNumberOfEdgeSegments() {
    return getTotalNumberOfPhysicalLinkSegments() + getTotalNumberOfConnectoidSegments();
  }

  /**
   * Returns the total number of link segments available in this transport network
   * 
   * @return the number of physical link segments in this network
   */
  public int getTotalNumberOfPhysicalLinkSegments() {
    return physicalNetwork.linkSegments.getNumberOfLinkSegments();
  }

  /**
   * Returns the total number of connectoid segments available in this transport network
   * 
   * @return the number of connectoid segments in this network
   */
  public int getTotalNumberOfConnectoidSegments() {
    return zoning.getVirtualNetwork().connectoidSegments.getNumberOfConnectoidSegments();
  }

  /**
   * Returns the total number of virtual and physical vertices in this transport network
   * 
   * @return the total number of physical and virtual vertices in this network
   */
  public int getTotalNumberOfVertices() {
    return zoning.getVirtualNetwork().centroids.getNumberOfCentroids() + physicalNetwork.nodes.getNumberOfNodes();
  }

  /**
   * Integrate physical and virtual links
   * 
   * @throws PlanItException thrown if there is an error
   */
  public void integrateConnectoidsAndLinks() throws PlanItException {
    VirtualNetwork virtualNetwork = zoning.getVirtualNetwork();
    for (Connectoid connectoid : virtualNetwork.connectoids) {
      virtualNetwork.connectoidSegments.createAndRegisterConnectoidSegment(connectoid, true);
      virtualNetwork.connectoidSegments.createAndRegisterConnectoidSegment(connectoid, false);
      connectVerticesToEdge(connectoid);
    }
    for (ConnectoidSegment connectoidSegment : virtualNetwork.connectoidSegments) {
      connectVerticesToEdgeSegment(connectoidSegment);
    }
    for (Edge edge : physicalNetwork.links) {
      connectVerticesToEdge(edge);
    }
    for (EdgeSegment linkSegment : physicalNetwork.linkSegments) {
      connectVerticesToEdgeSegment(linkSegment);
    }
  }

  /**
   * Remove the edges and edge segments on the vertices of both virtual and physical networks
   * 
   * @throws PlanItException thrown if there is an error
   */
  public void removeVirtualNetworkFromPhysicalNetwork() throws PlanItException {
    for (Connectoid connectoid : zoning.getVirtualNetwork().connectoids) {
      disconnectVerticesFromEdge(connectoid);
    }
    for (ConnectoidSegment connectoidSegment : zoning.getVirtualNetwork().connectoidSegments) {
      disconnectVerticesFromEdgeSegment(connectoidSegment);
    }
    for (Edge link : physicalNetwork.links) {
      disconnectVerticesFromEdge(link);
    }
    for (EdgeSegment linkSegment : physicalNetwork.linkSegments) {
      disconnectVerticesFromEdgeSegment(linkSegment);
    }
  }

  /**
   * Collect the physical network component of the transport network
   * 
   * @return physicalNetwork
   */
  public PhysicalNetwork getPhysicalNetwork() {
    return physicalNetwork;
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
