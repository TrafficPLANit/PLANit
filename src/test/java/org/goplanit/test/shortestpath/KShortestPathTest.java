package org.goplanit.test.shortestpath;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.factory.epsg.CartesianAuthorityFactory;
import org.goplanit.algorithms.shortest.ShortestPathAStar;
import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathKShortestYen;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.virtual.CentroidVertex;
import org.goplanit.utils.path.SimpleDirectedPathFactoryImpl;
import org.goplanit.utils.zoning.Centroid;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the k-shortest path algorithms
 * 
 * @author markr
 *
 */
public class KShortestPathTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final CoordinateReferenceSystem crs = CartesianAuthorityFactory.GENERIC_2D;
  private static final GeometryFactory geoFactory = JTSFactoryFinder.getGeometryFactory();

  private final IdGroupingToken idToken = IdGenerator.createIdGroupingToken(KShortestPathTest.class.getCanonicalName());
  private TransportModelNetwork transportNetwork;
  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  double[] linkSegmentCosts;

  private Centroid centroidA;
  private Centroid centroidB;

  private Map<Zone, CentroidVertex> zone2CentroidVertexMapping;

  /** util method to create nodes
   *
   * @param name of node
   * @param x xCoord converted to km by multiply with 1000
   * @param y  yCoord converted to km by multiply with 1000
   */
  private void createNode(String name, double x, double y){
    var node = networkLayer.getNodes().getFactory().registerNew();
    node.setXmlId(name);
    node.setPosition(geoFactory.createPoint(new Coordinate(x*1000, y*1000)));
  }

  /**
   * Create and register a link(segment) between the two nodes
   * @param startNode xmlId of start node
   * @param endNode xmlId of end node
   * @param lengthKm explicit length
   */
  private void createPhysicalLinkSegment(String startNode, String endNode, double lengthKm) {
    Node nodeA = networkLayer.getNodes().getByXmlId(startNode);
    Node nodeB = networkLayer.getNodes().getByXmlId(endNode);
    MacroscopicLink link = null;
    if(nodeA.hasEdge(nodeB)){
      link = (MacroscopicLink) nodeA.getEdges(nodeB).iterator().next();
    }else {
      link = networkLayer.getLinks().getFactory().registerNew(
              nodeA, nodeB, lengthKm, true);
    }
    networkLayer.getLinkSegments().getFactory().registerNew(link, true, true).setXmlId(startNode+endNode);
  }

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(KShortestPathTest.class);
    }
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
  }

  //@formatter:off
  @BeforeEach
  public void intialise() {
    // Construct the network. It is identical to Yen (example on wikipedia: https://en.wikipedia.org/wiki/Yen%27s_algorithm#cite_note-yenksp2-2
    // only we added centroid connectors before and after the origin and destination and made it geographically/spatially more accurate
    // for the A* to make sense
    //
    //   (0,0) __3_> (2,0)
    // A*--[C]/     \[D]_>_4__ (4,1)   (5,1)
    //      \      1^  _>_2___[F]__1_>[H]---B*
    //       \     |  /       2/   2_/
    //        \2_>_[E]__3_>_  V__>/
    //           (2,1)      \[G]
    //                     (4,2)
    //

    try {
      network = new MacroscopicNetwork(idToken);
      networkLayer = network.getTransportLayers().getFactory().registerNew();

      zoning = new Zoning(idToken, networkLayer.getLayerIdGroupingToken());
      Zone zoneA = zoning.getOdZones().getFactory().registerNew();
      zoneA.setXmlId("A");
      Zone zoneB = zoning.getOdZones().getFactory().registerNew();
      zoneB.setXmlId("B");

      centroidA = zoneA.getCentroid();
      centroidA.setPosition(geoFactory.createPoint(new Coordinate(0, 0)));
      centroidB = zoneB.getCentroid();
      centroidB.setPosition(geoFactory.createPoint(new Coordinate(5*1000, 1*1000)));

      createNode("C", 0,0);
      createNode("D", 2,0);
      createNode("E", 2,1);
      createNode("F", 4,1);
      createNode("G", 4,2);
      createNode("H", 5,1);

      createPhysicalLinkSegment("C","D", 3.0);
      createPhysicalLinkSegment("C","E", 2.0);
      createPhysicalLinkSegment("E","D", 1.0);
      createPhysicalLinkSegment("D","F", 4.0);
      createPhysicalLinkSegment("E","F", 2.0);
      createPhysicalLinkSegment("E","G", 3.0);
      createPhysicalLinkSegment("F","G", 2.0);
      createPhysicalLinkSegment("F","H", 1.0);
      createPhysicalLinkSegment("G","H", 2.0);

      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().getByXmlId("C"),  zoneA, 0.1);
      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().getByXmlId("H"), zoneB, 0.1);

      transportNetwork = new TransportModelNetwork(network, zoning);
      transportNetwork.integrateTransportNetworkViaConnectoids(false);
      zone2CentroidVertexMapping = transportNetwork.createZoneToCentroidVertexMapping(true, false);

      // costs
      var linkSegments = networkLayer.getLinkSegments();
      linkSegmentCosts = new double[]{
              /* physical link segments cost based on length */
              linkSegments.getByXmlId("CD").getLengthKm(),
              linkSegments.getByXmlId("CE").getLengthKm(),
              linkSegments.getByXmlId("ED").getLengthKm(),
              linkSegments.getByXmlId("DF").getLengthKm(),
              linkSegments.getByXmlId("EF").getLengthKm(),
              linkSegments.getByXmlId("EG").getLengthKm(),
              linkSegments.getByXmlId("FG").getLengthKm(),
              linkSegments.getByXmlId("FH").getLengthKm(),
              linkSegments.getByXmlId("GH").getLengthKm(),
              /* connectoid segment costs made up but so that it is non-zero and bi-directional as this is how they are created*/
              0.1, 0.1, 0.1, 0.1
          };

    }catch(Exception e) {
      e.printStackTrace();
      fail("initialise");
    }
  }
//@formatter:on

  /**
   * Test Yen's k-shortest path (one-to-one) based on above network
   */
  @Test
  public void yenKShortestTest() {
    try {

      var destinationVertex = zone2CentroidVertexMapping.get(centroidB.getParentZone());
      var originVertex = zone2CentroidVertexMapping.get(centroidA.getParentZone());

      /* prep */
      var dijkstra = new ShortestPathDijkstra(linkSegmentCosts, transportNetwork.getNumberOfVerticesAllLayers());

      // cost used is based on distance in this example, so roughly 1:1, to be conservative we assume a multiplier of 2
      final double multiplier = 2;
      var aStar = new ShortestPathAStar(linkSegmentCosts, transportNetwork.getNumberOfVerticesAllLayers(), crs, multiplier);

      int k = 8; // try to find 8 shortest paths (only 7 exist)
      var kShortest = new ShortestPathKShortestYen(dijkstra, aStar, k);
      var kShortestResult = kShortest.executeOneToOne(originVertex, destinationVertex);

      var pathFactory = new SimpleDirectedPathFactoryImpl();
      var kShortestPaths = kShortestResult.createPaths(pathFactory);

      assertEquals(7, kShortestPaths.size());
      var linkSegments = networkLayer.getLinkSegments();

      // Path 0
      kShortestResult.chooseKShortestPathIndex(0);
      double firstCost = kShortestResult.getCostToReach(destinationVertex);

      double cefhCost = linkSegments.getByXmlId("CE").getLengthKm() +
              linkSegments.getByXmlId("EF").getLengthKm() +
              linkSegments.getByXmlId("FH").getLengthKm() + 2 * 0.1;
      assertEquals(cefhCost, firstCost, Precision.EPSILON_6);

      // Path 1
      kShortestResult.chooseKShortestPathIndex(1);
      double secondCost = kShortestResult.getCostToReach(destinationVertex);
      double ceghCost = linkSegments.getByXmlId("CE").getLengthKm() +
              linkSegments.getByXmlId("EG").getLengthKm() +
              linkSegments.getByXmlId("GH").getLengthKm() + 2 * 0.1;
      assertEquals(ceghCost, secondCost, Precision.EPSILON_6);

      // Path 2
      kShortestResult.chooseKShortestPathIndex(2);
      double thirdCost = kShortestResult.getCostToReach(destinationVertex);
      double cdfhCost = linkSegments.getByXmlId("CD").getLengthKm() +
              linkSegments.getByXmlId("DF").getLengthKm() +
              linkSegments.getByXmlId("FH").getLengthKm() + 2 * 0.1;
      assertEquals(cdfhCost, thirdCost, Precision.EPSILON_6);

      // Path 3
      kShortestResult.chooseKShortestPathIndex(3);
      double fourthCost = kShortestResult.getCostToReach(destinationVertex);
      double cefghCost = cdfhCost;
      assertEquals(cefghCost, fourthCost, Precision.EPSILON_6);

      // Path 4
      kShortestResult.chooseKShortestPathIndex(4);
      double fifthCost = kShortestResult.getCostToReach(destinationVertex);
      double cedfhCost = cefghCost;
      assertEquals(cedfhCost, fifthCost, Precision.EPSILON_6);

      // Path 5
      kShortestResult.chooseKShortestPathIndex(5);
      double sixthCost = kShortestResult.getCostToReach(destinationVertex);
      double cdfghCost = linkSegments.getByXmlId("CD").getLengthKm() +
              linkSegments.getByXmlId("DF").getLengthKm() +
              linkSegments.getByXmlId("FG").getLengthKm() +
              linkSegments.getByXmlId("GH").getLengthKm() + 2 * 0.1;
      assertEquals(cdfghCost, sixthCost, Precision.EPSILON_6);

      // Path 6
      kShortestResult.chooseKShortestPathIndex(6);
      double seventhCost = kShortestResult.getCostToReach(destinationVertex);
      double cedfghCost = cdfghCost;
      assertEquals(cedfghCost, seventhCost, Precision.EPSILON_6);

      kShortestResult.chooseKShortestPathIndex(7);
      assertEquals(6,kShortestResult.getCurrentKShortestPathIndex()); // when not available truncated to largest (and warning is issued)

      /*** CHANGE ORIGIN AND SEE IF THAT WORKS ***/

      var originVertexD = network.getTransportLayers().getFirst().getNodes().getByXmlId("D");
      var kShortestResultFromD = kShortest.executeOneToOne(originVertexD, destinationVertex);

      var kShortestPathsFromD = kShortestResultFromD.createPaths(pathFactory);
      // only two options in total
      assertEquals(2, kShortestPathsFromD.size());

      // Path 0
      kShortestResultFromD.chooseKShortestPathIndex(0);
      double firstCostFromD = kShortestResultFromD.getCostToReach(destinationVertex);
      double dfhCost = linkSegments.getByXmlId("DF").getLengthKm() +
              linkSegments.getByXmlId("FH").getLengthKm() + 1 * 0.1;
      assertEquals(dfhCost, firstCostFromD, Precision.EPSILON_6);

      // Path 1
      kShortestResultFromD.chooseKShortestPathIndex(1);
      double secondCostFromD = kShortestResultFromD.getCostToReach(destinationVertex);
      double dfghCost = linkSegments.getByXmlId("DF").getLengthKm() +
              linkSegments.getByXmlId("FG").getLengthKm() +
              linkSegments.getByXmlId("GH").getLengthKm() + 1 * 0.1;
      assertEquals(dfhCost, firstCostFromD, Precision.EPSILON_6);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing Yen's K shortest path - one-to-all");
    }
  }

}
