package org.goplanit.network.transport;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.UntypedPhysicalNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.network.layer.physical.MovementsImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.network.layer.physical.Movement;
import org.goplanit.utils.network.layer.physical.Movements;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.virtual.*;
import org.goplanit.utils.zoning.*;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Entire transport network that is being modeled including both the physical and virtual aspects of it as well as the zoning.
 * It acts as a wrapper unifying the two components during the assignment stage.
 * <p>
 *   It also tracks movements if the user desired to generate those
 * </p>
 * 
 * @author markr
 *
 */
public class TransportModelNetworkImpl implements TransportModelNetwork{

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(TransportModelNetworkImpl.class.getCanonicalName());

  /**
   * log info on transport model network assuming it has integrated virtual and physical network it reports
   * on the connectoid edges and segments to do so.
   */
  private void logInfo() {
    getVirtualNetwork().logInfo("");
    if(!movements.isEmpty()){
      LOGGER.info(String.format("#Movements: %d", getMovements().size()));
    }
  }

  /**
   * Holds the infrastructure road network that is being modelled
   */
  protected final UntypedPhysicalNetwork<?, ?> infrastructureNetwork;

  /**
   * Holds the zoning structure and virtual transport network interfacing with the physical network
   */
  protected final Zoning zoning;

  /**
   * Optional container to fill with all permissible movements in the transport network
   */
  protected final Movements movements;

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
   * @param connectoidEdgeFactory    factory to use
   * @param connectoidSegmentFactory factory to use
   * @param centroidVertex           centroid vertex created for the access zone
   * @param accessZone               at hand for the current connectoid
   * @param connectoid               the connectoid at hand used to extract length to access zone
   * @param geoTools                 to use for geometry creation
   */
  protected void createAndRegisterConnectoidEdgeAndEdgeSegments(
      ConnectoidEdgeFactory connectoidEdgeFactory, ConnectoidSegmentFactory connectoidSegmentFactory, CentroidVertex centroidVertex, Zone accessZone, Connectoid connectoid, PlanitJtsCrsUtils geoTools) {
    double connectoidLength = connectoid.getLengthKm(accessZone).orElseThrow(
        () -> new PlanItRunTimeException("unable to retrieve length for connectoid %s (id:%d)", connectoid.getXmlId(), connectoid.getId()));
    var connectoidEdge =
        connectoidEdgeFactory.registerNew(centroidVertex, connectoid.getAccessVertex(), connectoidLength);
    connectVerticesToEdge(connectoidEdge);
    createAndRegisterConnectoidEdgeSegments(connectoidSegmentFactory, connectoidEdge);

    /* populate geometry as well */
    populateConnectoidGeometry(connectoidEdge, geoTools);
  }

  /**
   * Populate connectoid edge with geometry, simple line between the two vertices when centroid is present, otherwise
   * to closest project point on geometry of parent zone to signify connectoid (visually)
   *
   * @param connectoidEdge          to create geometry for
   * @param geoTools                to use for geometry creation
   * @return true when successful, false otherwise
   */
  private boolean populateConnectoidGeometry(ConnectoidEdge connectoidEdge, PlanitJtsCrsUtils geoTools) {
    var centroidVertex = connectoidEdge.getCentroidVertex();
    var parentCentroid = centroidVertex.getParent();

    boolean connectoidHasGeometry = connectoidEdge.hasGeometry();
    /* when centroid is present and it makes sense to use for the geometry, use as is */
    if(!connectoidHasGeometry && parentCentroid.hasPosition()){
      connectoidHasGeometry = connectoidEdge.populateBasicGeometry(true);
    }

    var parentZone = parentCentroid!=null ? parentCentroid.getParentZone() : null;
    if(!connectoidHasGeometry && parentZone!=null && parentZone.hasGeometry()){
      /* possible that no centroid is present making it impossible to create basic geometry (vertex-vertex), instead use
       * zone information */
      var zoneGeometry = parentZone.getGeometry();

      /* point -> use point geometry and create simple line, after populating centroid vertex location */
      if(zoneGeometry instanceof Point){
        centroidVertex.setPosition((Point)zoneGeometry);
        return connectoidEdge.populateBasicGeometry(true);
      }
      /* polygon -> convert to line string */
      if(zoneGeometry instanceof Polygon){
        zoneGeometry = PlanitJtsUtils.createLineString(((Polygon)zoneGeometry).getExteriorRing().getCoordinates());
      }

      /* not a line string -> not supported yet */
      if(!(zoneGeometry instanceof LineString)){
        return false;
      }

      /* should not happen, but avoid null pointer */
      if(connectoidEdge.getNonCentroidVertex()==null ||  !connectoidEdge.getNonCentroidVertex().hasPosition()){
        return false;
      }

      /* line string -> find closest projected point and use that as "centroid" vertex location */
      var projectedLocation = geoTools.getClosestProjectedLinearLocationOnLineString(
          connectoidEdge.getNonCentroidVertex().getPosition().getCoordinate(), (LineString)zoneGeometry);
      var closestPointOnZoneGeometry = PlanitJtsUtils.createPoint(projectedLocation.getCoordinate(zoneGeometry));
      connectoidEdge.setGeometry(PlanitJtsUtils.createLineString(
          closestPointOnZoneGeometry.getCoordinate(),
          connectoidEdge.getNonCentroidVertex().getPosition().getCoordinate()));
      connectoidHasGeometry = true;
    }
    return connectoidHasGeometry;
  }

  /**
   * Constructor
   *
   * @param infrastructureNetwork the network used to generate this TransportNetwork
   * @param zoning                the Zoning used to generate this TransportNetwork
   */
  public TransportModelNetworkImpl(UntypedPhysicalNetwork<?, ?> infrastructureNetwork, Zoning zoning) {
    this.infrastructureNetwork = infrastructureNetwork;
    this.zoning = zoning;
    this.movements = new MovementsImpl(infrastructureNetwork.getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransportModelNetworkImpl integrateTransportNetworkViaConnectoids(boolean resetAndRecreateManagedIds){
    LOGGER.info(String.format("Integrating physical network %d (XML id %s) with zoning %d (XML id %s)", infrastructureNetwork.getId(),
        infrastructureNetwork.getXmlId() != null ? infrastructureNetwork.getXmlId() : "N/A", zoning.getId(), zoning.getXmlId() != null ? zoning.getXmlId() : "N/A"));

    VirtualNetwork virtualNetwork = zoning.getVirtualNetwork();
    if(resetAndRecreateManagedIds){
      LOGGER.info("Recreating internal contiguous ids for network and zoning");
      infrastructureNetwork.recreateManagedIds();
      virtualNetwork.recreateManagedIds(false); // reset of underlying managed id class (edge,vertex) already happened in network
    }

    var centroidVertexFactory = virtualNetwork.getLayer().getVertices().getFactory();
    var connectoidEdgeFactory = virtualNetwork.getLayer().getConnectoidEdges().getFactory();
    var connectoidSegmentFactory = virtualNetwork.getLayer().getConnectoidSegments().getFactory();

    var geoTools = new PlanitJtsCrsUtils(getInfrastructureNetwork().getCoordinateReferenceSystem());

    Map<Zone, CentroidVertex> zone2CentroidVertexMapping = new HashMap<>();
    for (UndirectedConnectoid undirectedConnectoid : zoning.getOdConnectoids()) {
      for(var accessZone : undirectedConnectoid.getAccessZones()){
        var centroidVertex = zone2CentroidVertexMapping.get(accessZone);
        if(centroidVertex == null) {
          centroidVertex = centroidVertexFactory.registerNew(accessZone.getCentroid()); // explicit vertex for centroid related to this virtual/physical network
          zone2CentroidVertexMapping.put(accessZone, centroidVertex);
        }

        createAndRegisterConnectoidEdgeAndEdgeSegments(
            connectoidEdgeFactory, connectoidSegmentFactory, centroidVertex, accessZone, undirectedConnectoid, geoTools);
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
        createAndRegisterConnectoidEdgeAndEdgeSegments(
            connectoidEdgeFactory, connectoidSegmentFactory, centroidVertex, accessZone, directedConnectoid, geoTools);
      }
    }
    logInfo();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfVerticesAllLayers() {
    // todo: move to interface if conjugate implementation also uses this
    return TransportModelNetworkUtils.getNumberOfVerticesAllLayers(getInfrastructureNetwork(), zoning);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeVirtualNetworkFromPhysicalNetwork(boolean resetManagedIds) {
    // todo: move to interface if conjugate implementation has same implementation
    for (ConnectoidEdge connectoidEdge : getVirtualNetwork().getLayer().getConnectoidEdges()) {
      disconnectVerticesFromEdge(connectoidEdge);
    }

    /* clear out contents */
    if(resetManagedIds){
      getVirtualNetwork().reset();
    }else{
      getVirtualNetwork().clear();
    }

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UntypedPhysicalNetwork<?, ?> getInfrastructureNetwork() {
    return infrastructureNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VirtualNetwork getVirtualNetwork() {
    return zoning.getVirtualNetwork();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Zoning getZoning() {
    return zoning;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void generatePermissibleMovements() {
    //todo: if conjugate implementation also uses this move to interface
    movements.reset();
    for(var layer : getInfrastructureNetwork().getTransportLayers()){
      for(var node : layer.getNodes()){
        for(var entrySegment : node.getEntryEdgeSegments()){
          for(var exitSegment : node.getExitLinkSegments()){

            // never allow u-turn movement
            if(entrySegment.hasOppositeDirectionSegment() && entrySegment.getOppositeDirectionSegment() == exitSegment){
              continue;
            }

            movements.getFactory().registerNew(entrySegment, exitSegment);
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   *   todo: should ideally check if it has been integrated with the zoning/virtual network already
   *    for now assume this is the case, so we can integrate immediately here
   * </p>
   */
  @Override
  public ConjugateTransportModelNetwork createConjugate() {
    var conjugateTransportModelNetwork = new ConjugateTransportModelNetwork(this);

    // since conjugate network is always created new no need to recreate ids
    conjugateTransportModelNetwork.integrateTransportNetworkViaConnectoids(false);
    return conjugateTransportModelNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Movements getMovements(){
    return movements;
  }

}
