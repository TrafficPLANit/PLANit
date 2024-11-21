package org.goplanit.network.transport;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.UntypedPhysicalNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.virtual.*;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;
import org.goplanit.utils.network.virtual.physical.ConnectoidLink;
import org.goplanit.utils.network.virtual.physical.ConnectoidLinkFactory;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegmentFactory;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.DirectedConnectoid;
import org.goplanit.utils.zoning.UndirectedConnectoid;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Transport network implementation that is being modeled including both the physical and virtual aspects of it as
 * well as the zoning. It acts as a wrapper unifying the two components during the assignment stage.
 * <p>
 *   It also tracks movements if the user desired to generate those
 * </p>
 * 
 * @author markr
 *
 */
public class TransportModelNetworkImpl
    extends UntypedTransportModelNetwork<UntypedPhysicalNetwork<?, ?>, VirtualNetwork> {

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

  // Public

  /**
   * create and register the edge segments for the passed in connectoid edge, XML id set to id prefixed with "c_ab or c_ba".
   *
   * @param connectoidSegmentFactory  to create and register on
   * @param connectoidLink to process
   */
  protected void createAndRegisterConnectoidEdgeSegments(
      ConnectoidSegmentFactory connectoidSegmentFactory, ConnectoidLink connectoidLink) {
    var segment = connectoidSegmentFactory.registerNew(connectoidLink, true);
    segment.setXmlId("c_ab" + segment.getId());
    segment = connectoidSegmentFactory.registerNew(connectoidLink, false);
    segment.setXmlId("c_ba" + segment.getId());
    connectVerticesToEdge(connectoidLink);
  }

  /**
   * Given context of centroid vertex and connectoid + access zone, we create the required connectoid edges and connected segments with the provided factories
   *
   * @param connectoidLinkFactory    factory to use
   * @param connectoidSegmentFactory factory to use
   * @param centroidVertex           centroid vertex created for the access zone
   * @param accessZone               at hand for the current connectoid
   * @param connectoid               the connectoid at hand used to extract length to access zone
   * @param geoTools                 to use for geometry creation
   */
  protected void createAndRegisterConnectoidLinkAndEdgeSegments(
      ConnectoidLinkFactory connectoidLinkFactory,
      ConnectoidSegmentFactory connectoidSegmentFactory,
      CentroidVertex centroidVertex,
      Zone accessZone,
      Connectoid connectoid,
      PlanitJtsCrsUtils geoTools) {

    double connectoidLength = connectoid.getLengthKm(accessZone).orElseThrow(
        () -> new PlanItRunTimeException("unable to retrieve length for connectoid %s (id:%d)", connectoid.getXmlId(), connectoid.getId()));
    var connectoidEdge =
        connectoidLinkFactory.registerNew(centroidVertex, connectoid.getAccessVertex(), connectoidLength);
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
  private boolean populateConnectoidGeometry(ConnectoidDirectedEdge connectoidEdge, PlanitJtsCrsUtils geoTools) {
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
  public TransportModelNetworkImpl(
      UntypedPhysicalNetwork<?, ?> infrastructureNetwork, Zoning zoning) {
    super(infrastructureNetwork, zoning, zoning.getVirtualNetwork());
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
    var connectoidLinkFactory = virtualNetwork.getLayer().getConnectoidLinks().getFactory();
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

        createAndRegisterConnectoidLinkAndEdgeSegments(
            connectoidLinkFactory, connectoidSegmentFactory, centroidVertex, accessZone, undirectedConnectoid, geoTools);
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
        createAndRegisterConnectoidLinkAndEdgeSegments(
            connectoidLinkFactory, connectoidSegmentFactory, centroidVertex, accessZone, directedConnectoid, geoTools);
      }
    }
    logInfo();
    return this;
  }

  /**
   * {@inheritDoc}
   * <p>
   *   todo: should ideally check if it has been integrated with the zoning/virtual network already
   *    for now assume this is the case, so we can integrate immediately here
   * </p>
   */
  @Override
  public ConjugateTransportModelNetwork createConjugate(final IdGroupingToken idToken) {
    if(!(getInfrastructureNetwork() instanceof MacroscopicNetwork)){
      LOGGER.warning("Unsupported infrastructure network type create conjugate network for, only " +
          "Macroscopic networks currently supported");
    }
    if(getVirtualNetwork() == null){
      LOGGER.warning("Virtual network must be available to be able to create conjugate but found null");
    }
    @SuppressWarnings("unchecked")
    var conjugateTransportModelNetwork = new ConjugateTransportModelNetwork(idToken,
        (TransportModelNetwork<MacroscopicNetwork,VirtualNetwork>)(TransportModelNetwork<?,?>) this);

    // since conjugate network is always created new no need to recreate ids
    conjugateTransportModelNetwork.integrateTransportNetworkViaConnectoids(false);
    return conjugateTransportModelNetwork;
  }

}
