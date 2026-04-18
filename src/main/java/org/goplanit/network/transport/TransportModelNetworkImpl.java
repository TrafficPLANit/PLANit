package org.goplanit.network.transport;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.UntypedPhysicalNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.*;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.graph.CentroidVertexFactory;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;
import org.goplanit.utils.network.virtual.physical.ConnectoidLink;
import org.goplanit.utils.network.virtual.physical.ConnectoidLinkFactory;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegmentFactory;
import org.goplanit.utils.zoning.*;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
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

  //protected

  /**
   * Construct readable but unique Xml id for connectoid based on zone and contextual information
   *
   * @param accessZone               at hand for the current connectoid
   * @param connectoidEdge           the connectoid at hand used to extract length to access zone
   * @param fromSource               when true connectoid goes from source into network, otherwise it is a sink
   * @return xml id
   */
  protected String constructConnectoidXmlId(
          Zone accessZone, ConnectoidLink connectoidEdge, boolean fromSource){
    // e.g., xmlId= "D2_1" is the first connectoid edge connected to zone 2 its destination vertex
    var sb = new StringBuilder();
    if(fromSource) {
      sb.append("O");
    } else {
      sb.append("D");
    }
    // use XML id but if not present indicate internal id is used.
    sb.append(accessZone.hasXmlId() ? accessZone.getXmlId() : accessZone.getId());
    // supplement with index on how manieth connectoid it is for the zone
    sb.append("_").append(connectoidEdge.getCentroidVertex().getEdges().size());
    return sb.toString();
  }

  /**
   * create and register the edge segment for the passed in connectoid edge, XML id set to id prefixed with
   * "c_ab or c_ba".
   *
   * @param connectoidSegmentFactory  to create and register on
   * @param connectoidLink to process
   * @param directionAb direction to create
   * @param xmlId to set
   */
  protected void createAndRegisterConnectoidEdgeSegment(
      ConnectoidSegmentFactory connectoidSegmentFactory,
      ConnectoidLink connectoidLink,
      boolean directionAb,
      String xmlId) {

    var segment = connectoidSegmentFactory.registerNew(connectoidLink, directionAb);
    segment.setXmlId(xmlId);
    connectVerticesToEdge(connectoidLink);
  }

  /**
   * Given context of centroid vertex and connectoid + access zone, we create the required connectoid edge and
   * single connectoid segments with the provided factories in the required direction.
   *
   * @param connectoidLinkFactory    factory to use
   * @param connectoidSegmentFactory factory to use
   * @param centroidVertex           centroid vertex created for the access zone
   * @param accessZone               at hand for the current connectoid
   * @param type                     type of connectoid zone entry
   * @param connectoid               the connectoid at hand used to extract length to access zone
   * @param geoTools                 to use for geometry creation
   * @param fromSource               when true create link and segment away from provided centroid vertex, otherwise
   *                                 towards it
   */
  protected void createAndRegisterConnectoidLinkAndEdgeSegment(
      ConnectoidLinkFactory connectoidLinkFactory,
      ConnectoidSegmentFactory connectoidSegmentFactory,
      CentroidVertex centroidVertex,
      Zone accessZone,
      ZoneConnectoidType type,
      Connectoid<?> connectoid,
      PlanitJtsCrsUtils geoTools,
      boolean fromSource) {

    double connectoidLength = connectoid.getAccessZoneEntry(accessZone, type).getLengthKm().orElseThrow(
        () -> new PlanItRunTimeException(
                "Unable to retrieve length for connectoid %s (id:%d)", connectoid.getXmlId(), connectoid.getId()));
    var connectoidEdge =
        connectoidLinkFactory.registerNew(centroidVertex, connectoid.getAccessVertex(), connectoidLength);
    connectVerticesToEdge(connectoidEdge);
    connectoidEdge.setXmlId(constructConnectoidXmlId(accessZone, connectoidEdge, fromSource));
    // since sink/source centroid vertices will only ever have one segment per edge, we reuse the xmlId of the
    // edge since it is more descriptive and still unique within the type of object
    createAndRegisterConnectoidEdgeSegment(
            connectoidSegmentFactory, connectoidEdge, fromSource, connectoidEdge.getXmlId());

    /* populate geometry as well */
    populateConnectoidGeometry(connectoidEdge, geoTools);
  }

  /**
   * Create connectoid links/segments for transfer connectoids. We create a single link and segment in indicated
   * direction for each unique access segment of a connectoids zone/type combination. We do so because we assume
   * that the type imposes restrictions on the allowed modes and as such is allowed to have different lengths specified
   * and therefore must be kept separate from other types. within this, we collate all unique access segment across all
   * modes and create a single link/segment.
   *
   * @param isSource flag whether we do source/sink
   * @param zone2CentroidVertexMappingToPopulate mapping to update if centroid of zone processed for connectoid
   *                                             not already tracked
   * @param centroidVertexFactory factory for centroid vertices
   * @param cLinkFactory factory for connectoid links
   * @param cSegmentFactory factory for connectoid segments
   * @param geoTools geo tools to use
   */
  protected void createTransferConnectoidVirtualLinksAndSegments(
      boolean isSource,
      Map<Zone, CentroidVertex> zone2CentroidVertexMappingToPopulate,
      CentroidVertexFactory centroidVertexFactory,
      ConnectoidLinkFactory cLinkFactory,
      ConnectoidSegmentFactory cSegmentFactory,
      PlanitJtsCrsUtils geoTools){

    Predicate<DirectedConnectoid> accessNodeLocationFilter = null;
    if(isSource){
      accessNodeLocationFilter = DirectedConnectoid::isAccessNodeUpstreamOfSegments;
    }else{
      accessNodeLocationFilter = DirectedConnectoid::isAccessNodeDownstreamOfSegments;
    }

    zoning.getTransferConnectoids().stream().filter(accessNodeLocationFilter).forEach(
        c -> {
          var accessVertex = c.getAccessVertex();
          if (accessVertex == null) {
            throw new PlanItRunTimeException("No access vertex found for directed connectoid, this shouldn't happen");
          }

          // populate mapping for source vertices/centroids and vreate lniks/segments
          c.getAccessZoneStream().forEach(accessZone -> {
            zone2CentroidVertexMappingToPopulate.computeIfAbsent(
                accessZone, theZ -> centroidVertexFactory.registerNew(theZ.getCentroid()));

            var sourceVertex = zone2CentroidVertexMappingToPopulate.get(accessZone);
            var entriesForZoneByType = c.getAccessZoneEntriesByType(accessZone);
            if(entriesForZoneByType== null || entriesForZoneByType.isEmpty()){
              return;
            }

            entriesForZoneByType.forEach((currType, value) -> {
              //all source entries filtered to current zone/type combo - for now we disregard modes on the
              // virtual links/segments. For all unique access segments known we create source virtual links
              Predicate<DirectedConnectoidAccessZoneEntry> zoneTypeFilter =
                  e -> e.getAccessZone() == accessZone &&
                      e.getType().equals(currType);

              c.getAccessLinkSegmentsStream(zoneTypeFilter).forEach(ls -> {
                createAndRegisterConnectoidLinkAndEdgeSegment(
                    cLinkFactory, cSegmentFactory, sourceVertex, accessZone, currType, c, geoTools, isSource);
              }); //  link segments
            }); // type/entries
          }); // zones
    }); //connectoids
}

  // Public

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
    LOGGER.info(String.format("Integrating physical network (%s) with zoning (%s)",
            infrastructureNetwork.getIdsAsString(),
            zoning.getIdsAsString()));

    VirtualNetwork virtualNetwork = zoning.getVirtualNetwork();
    if(resetAndRecreateManagedIds){
      LOGGER.info("Recreating internal contiguous ids for network and zoning");
      infrastructureNetwork.recreateManagedIds();
      // reset of underlying managed id class (edge,vertex) already happened in network
      virtualNetwork.recreateManagedIds(false);
    }

    var centroidVertexFactory = virtualNetwork.getLayer().getVertices().getFactory();
    var cLinkFactory = virtualNetwork.getLayer().getConnectoidLinks().getFactory();
    var cSegmentFactory = virtualNetwork.getLayer().getConnectoidSegments().getFactory();
    var geoTools = new PlanitJtsCrsUtils(getInfrastructureNetwork().getCoordinateReferenceSystem());

    // create two centroid vertices per centroid one for the source and one for the sink
    //  for origin-destination zones these two should not be connected to eac other to avoid traffic through centroids..
    // todo: migrate to transfer connectoid approach which is more elegant once structure of od connectoids is updated
    Map<Zone, CentroidVertex> zone2SourceCentroidVertexMapping = new HashMap<>();
    Map<Zone, CentroidVertex> zone2SinkCentroidVertexMapping = new HashMap<>();
    for (UndirectedConnectoid uConnectoid : zoning.getOdConnectoids()) {
      for(var zoneIdToTypeEntriesMapEntry : uConnectoid.getAccessZoneEntriesByType().entrySet()){
        var accessZone = zoning.getZone(zoneIdToTypeEntriesMapEntry.getKey());
        for(var typeZoneConnectoidEntries : zoneIdToTypeEntriesMapEntry.getValue().entrySet()){
          var type = typeZoneConnectoidEntries.getKey();
          var sourceVertex = zone2SourceCentroidVertexMapping.computeIfAbsent(
              accessZone, z -> centroidVertexFactory.registerNew(z.getCentroid()));
          var sinkVertex = zone2SinkCentroidVertexMapping.computeIfAbsent(
              accessZone, z -> centroidVertexFactory.registerNew(z.getCentroid()));
          boolean fromSource = true;
          createAndRegisterConnectoidLinkAndEdgeSegment(
              cLinkFactory, cSegmentFactory, sourceVertex, accessZone, type, uConnectoid, geoTools, fromSource);
          fromSource = false;
          createAndRegisterConnectoidLinkAndEdgeSegment(
              cLinkFactory, cSegmentFactory, sinkVertex, accessZone, type, uConnectoid, geoTools, fromSource);
        }
      }
    }

    //  .. for transfer zones this may depend on how the transfers are modeled, also we keep it simple for now by not
    //  creating separate links/segments based on mode access, but rely on the connectoid entry segment entries for that
    // to be picked up.

    // source connectoids first, then sinks
    boolean isSource = true;
    createTransferConnectoidVirtualLinksAndSegments(
        isSource, zone2SourceCentroidVertexMapping, centroidVertexFactory, cLinkFactory, cSegmentFactory, geoTools);
    isSource = false;
    createTransferConnectoidVirtualLinksAndSegments(
        isSource, zone2SinkCentroidVertexMapping, centroidVertexFactory, cLinkFactory, cSegmentFactory, geoTools);
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

    // creating the conjugate transport model will trigger a complete creation/initialisation of this network
    // at the moment the result is a complete integrated conjugate version of the original.
    @SuppressWarnings("unchecked")
    var conjugateTransportModelNetwork = new ConjugateTransportModelNetwork(idToken,
        (TransportModelNetwork<MacroscopicNetwork,VirtualNetwork>)(TransportModelNetwork<?,?>) this);

    return conjugateTransportModelNetwork;
  }

}
