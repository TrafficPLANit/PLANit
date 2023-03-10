package org.goplanit.network.transport;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.virtual.*;
import org.goplanit.utils.zoning.*;
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
    LOGGER.info(String.format("#OD connectoid edges: %d", getVirtualNetwork().getConnectoidEdges().size()));
    LOGGER.info(String.format("#OD connectoid segments: %d", getVirtualNetwork().getConnectoidSegments().size()));
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
   */
  protected void connectVerticesToEdge(Edge edge) {
    edge.getVertexA().addEdge(edge);
    edge.getVertexB().addEdge(edge);
  }

  /**
   * Remove Edge from both vertices
   * 
   * @param edge Edge to be removed from upstream and downstream vertices
   */
  protected void disconnectVerticesFromEdge(Edge edge) {
    edge.getVertexA().removeEdge(edge);
    edge.getVertexB().removeEdge(edge);
  }

  // Public

  /**
   * create and register the edge segments for the passed in connectoid edge, XML id set to id prefixed with "c_ab or c_ba".
   * 
   * @param connectoidSegmentFactory  to create and register on
   * @param connectoidEdge to process
   */
  protected void createAndRegisterConnectoidEdgeSegments(ConnectoidSegmentFactory connectoidSegmentFactory, ConnectoidEdge connectoidEdge) {
    var segment = connectoidSegmentFactory.registerNew(connectoidEdge, true);
    segment.setXmlId("c_ab" + segment.getId());
    segment = connectoidSegmentFactory.registerNew(connectoidEdge, false);
    segment.setXmlId("c_ba" + segment.getId());
    connectVerticesToEdge(connectoidEdge);
  }

  /**
   * Given context of centroid vertex and connectoid + access zone, we create the required connectoid edges and connected segments with the provided factories
   *
   * @param connectoidEdgeFactory factory to use
   * @param connectoidSegmentFactory factory to use
   * @param centroidVertex centroid vertex created for the access zone
   * @param accessZone at hand for the current connectoid
   * @param connectoid the connectoid at hand used to extract length to access zone
   */
  protected void createAndConnectoidEdgeAndEdgeSegments(ConnectoidEdgeFactory connectoidEdgeFactory, ConnectoidSegmentFactory connectoidSegmentFactory, CentroidVertex centroidVertex, Zone accessZone, Connectoid connectoid) {
    double connectoidLength = connectoid.getLengthKm(accessZone).orElseThrow(
        () -> new PlanItRunTimeException("unable to retrieve length for connectoid %s (id:%d)", connectoid.getXmlId(), connectoid.getId()));
    var connectoidEdge =
        connectoidEdgeFactory.registerNew(centroidVertex, connectoid.getAccessVertex(), connectoidLength);
    connectVerticesToEdge(connectoidEdge);
    createAndRegisterConnectoidEdgeSegments(connectoidSegmentFactory, connectoidEdge);
  }

  /**
   * Returns the total number of edge segments available in this traffic assignment by combining the physical and non-physical link segments
   *
   * @return total number of physical and virtual edge segments
   */
  public static int getNumberOfEdgeSegmentsAllLayers(LayeredNetwork<?, ?> theNetwork, Zoning theZoning) {
    return getNumberOfPhysicalLinkSegmentsAllLayers(theNetwork) + getNumberOfConnectoidSegments(theZoning);
  }

  /**
   * Returns the total number of connectoid segments available in this transport network
   *
   * @param theZoning to use
   * @return the number of connectoid segments in this network
   */
  public static int getNumberOfConnectoidSegments(Zoning theZoning) {
    return theZoning.getVirtualNetwork().getConnectoidSegments().size();
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
   *r
   */
  public void integrateTransportNetworkViaConnectoids(){
    LOGGER.info(String.format("Integrating physical network %d (XML id %s) with zoning %d (XML id %s)", infrastructureNetwork.getId(),
        infrastructureNetwork.getXmlId() != null ? infrastructureNetwork.getXmlId() : "N/A", zoning.getId(), zoning.getXmlId() != null ? zoning.getXmlId() : "N/A"));

    VirtualNetwork virtualNetwork = zoning.getVirtualNetwork();
    var centroidVertexFactory = virtualNetwork.getCentroidVertices().getFactory();
    var connectoidEdgeFactory = virtualNetwork.getConnectoidEdges().getFactory();
    var connectoidSegmentFactory = virtualNetwork.getConnectoidSegments().getFactory();

    Map<Zone, CentroidVertex> zone2CentroidVertexMapping = new HashMap<>();
    for (UndirectedConnectoid undirectedConnectoid : zoning.getOdConnectoids()) {
      for(var accessZone : undirectedConnectoid.getAccessZones()){
        var centroidVertex = zone2CentroidVertexMapping.get(accessZone);
        if(centroidVertex == null) {
          centroidVertex = centroidVertexFactory.registerNew(accessZone.getCentroid()); // explicit vertex for centroid related to this virtual/physical network
          zone2CentroidVertexMapping.put(accessZone, centroidVertex);
        }

        createAndConnectoidEdgeAndEdgeSegments(connectoidEdgeFactory, connectoidSegmentFactory, centroidVertex, accessZone, undirectedConnectoid);
      }
    }

    for (DirectedConnectoid directedConnectoid : zoning.getTransferConnectoids()) {
      for(var accessZone : directedConnectoid.getAccessZones()) {
        var centroidVertex = zone2CentroidVertexMapping.get(accessZone);
        if(centroidVertex == null) {
          centroidVertex = centroidVertexFactory.registerNew(accessZone.getCentroid()); // explicit vertex for centroid related to this virtual/physical network
          zone2CentroidVertexMapping.put(accessZone, centroidVertex);
        }

        var accessEdgeSegment = directedConnectoid.getAccessLinkSegment();
        var accessVertex = (Node) (accessEdgeSegment != null ? accessEdgeSegment.getDownstreamVertex() : null);
        if (accessVertex == null) {
          throw new PlanItRunTimeException("No access vertex found for directed connectoid, this shouldn't happen");
        }
        createAndConnectoidEdgeAndEdgeSegments(connectoidEdgeFactory, connectoidSegmentFactory, centroidVertex, accessZone, directedConnectoid);
      }
    }
    logInfo();
  }


    /**
     * Returns the total number of edge segments available in this traffic assignment by combining the physical and non-physical link segments
     *
     * @return total number of physical and virtual edge segments
     */
  public int getNumberOfEdgeSegmentsAllLayers() {
    return getNumberOfPhysicalLinkSegmentsAllLayers(getInfrastructureNetwork()) + getNumberOfConnectoidSegments(getZoning());
  }

  /**
   * Returns the total number of link segments available in this transport network across all eligible layers
   * 
   * @return the number of physical link segments in this network
   */
  public int getNumberOfPhysicalLinkSegmentsAllLayers() {
    return getNumberOfPhysicalLinkSegmentsAllLayers(getInfrastructureNetwork());
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
    return getNumberOfVerticesAllLayers(getInfrastructureNetwork(), zoning);
  }

  /**
   * Returns the total number of physical nodes available in this transport network across all eligible layers
   *
   * @return the number of physical nodes in this network
   */
  public int getNumberOfPhysicalNodesAllLayers() {
    return getNumberOfPhysicalNodesAllLayers(getInfrastructureNetwork());
  }

  /**
   * Remove the edges and edge segments on the vertices of both virtual and physical networks
   *
   */
  public void removeVirtualNetworkFromPhysicalNetwork() {
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

  /**
   * Create a (new) mapping from zones (transfer and or OD) to their centroid vertex.
   *
   * @param OdZones when true OdZones will be included in the mapping, not included otherwise
   * @param transferZones when true transferZones will be included in the mapping, not included otherwise
   * @return mapping that was created
   */
  public Map<Zone, CentroidVertex> createZoneToCentroidVertexMapping(boolean OdZones, boolean transferZones){
    return getZoning().getVirtualNetwork().getCentroidVertices().stream().filter(
        cVertex ->
            (OdZones && (cVertex.getParent().getParentZone() instanceof OdZone)) || (transferZones && (cVertex.getParent().getParentZone() instanceof TransferZone))).collect(
                Collectors.toMap(cVertex -> cVertex.getParent().getParentZone(), cVertex -> cVertex));
  }
}
