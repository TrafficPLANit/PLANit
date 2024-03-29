package org.goplanit.test.shortestpath;

import java.util.Map;
import java.util.logging.Logger;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.factory.epsg.CartesianAuthorityFactory;
import org.goplanit.algorithms.shortest.ShortestPathAStar;
import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.virtual.CentroidVertex;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the shortest path algorithms
 * 
 * @author markr
 *
 */
public class ShortestPathTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final CoordinateReferenceSystem crs = CartesianAuthorityFactory.GENERIC_2D;
  private final IdGroupingToken idToken = IdGenerator.createIdGroupingToken(ShortestPathTest.class.getCanonicalName());
  private TransportModelNetwork transportNetwork;
  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  double[] linkSegmentCosts;

  private Centroid centroidA;
  private Centroid centroidB;
  private Centroid centroidC;
  private Centroid centroidD;
  private Centroid centroidE;

  private Map<Zone, CentroidVertex> zone2CentroidVertexMapping;

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(ShortestPathTest.class);
    }
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
  }

  //@formatter:off
  @BeforeEach
  public void intialise() {
    // construct the network. The is the same network as in shortest path algorithm integration tests
    //
    //
    //
    //          col 0  col 1 
    //       
    //             10      3     21      43
    //           .------B------.------D------E     
    //           |      |      |      |      |     
    //        10 |    20|    10|     9|    40|
    //           |      |      |      |      |
    //           |   4  |  10  |  40  |  6   |
    //           .------.------.------.------.
    //           |      |      |      |      |
    //        10 |    20|     5|    30|    20|
    //           |      |      |      |      |
    //           |   8  |  40  |  15  |  20  |
    //           .------.------C------.------.
    //           |      |      |      |      |
    // lnk(25) 23|     12    47|    32|    20|
    //smt(50,51) |      |      |      |      |
    //           |  7   |  5   |  10  |  30  |       
    //           .------.------.------.------.     row 1
    //           |      |      |      |      |
    // lnk(20) 33|    31|     8|    12|    5 |
    //smt(40,41) |      |      |      |      |
    //           |  10  |  12  |  40  |   10 |    
    //           A------.------.------.------.     row 0
    //NODE      (0)   (1)    (2)    (3)    (4)
    //LINK         (0)    (1)    (2)    (3)
    //LINKSgmnt   (0,1)  (2,3)   (4,5)  (6,7)
    
    try {
      // local CRS in meters
      GeometryFactory geoFactory = JTSFactoryFinder.getGeometryFactory();
      
      int gridSize = 4;
      network = new MacroscopicNetwork(idToken);
      networkLayer = network.getTransportLayers().getFactory().registerNew();
      for(int nodeRowIndex = 0;nodeRowIndex<=gridSize;++nodeRowIndex) {
        for(int nodeColIndex = 0;nodeColIndex<=gridSize;++nodeColIndex) {
          Node node = networkLayer.getNodes().getFactory().registerNew();
          node.setXmlId(String.valueOf(node.getId()));
          // all nodes are spaced 1 km apart
          node.setPosition(geoFactory.createPoint(new Coordinate(nodeRowIndex*1000, nodeColIndex*1000)));
        }
      }
      
      //horizontal links
      for(int linkRowIndex = 0;linkRowIndex<=gridSize;++linkRowIndex) {
        for(int linkColIndex = 1;linkColIndex<=gridSize;++linkColIndex) {
          Node nodeA = networkLayer.getNodes().get(linkRowIndex*(gridSize+1) + linkColIndex-1);
          Node nodeB = networkLayer.getNodes().get(linkRowIndex*(gridSize+1) + linkColIndex);
          // all links are 1 km in length          
          var link = networkLayer.getLinks().getFactory().registerNew(nodeA, nodeB, 1, true);
          networkLayer.getLinkSegments().getFactory().registerNew(link, true, true);
          networkLayer.getLinkSegments().getFactory().registerNew(link, false, true);
        }
      }
        
      //vertical links
      for(int linkRowIndex = 1;linkRowIndex<=gridSize;++linkRowIndex) {
        for(int linkColIndex = 0;linkColIndex<=gridSize;++linkColIndex) {
          // all links are 1 km in length
          Node nodeA = networkLayer.getNodes().get((linkRowIndex-1)*(gridSize+1)+linkColIndex);
          Node nodeB = networkLayer.getNodes().get(linkRowIndex*(gridSize+1)+linkColIndex);
          var link = networkLayer.getLinks().getFactory().registerNew(nodeA, nodeB, 1, true);
          networkLayer.getLinkSegments().getFactory().registerNew(link, true, true);
          networkLayer.getLinkSegments().getFactory().registerNew(link, false, true);
        }  
      }
      
      zoning = new Zoning(idToken, networkLayer.getLayerIdGroupingToken());
      Zone zoneA = zoning.getOdZones().getFactory().registerNew();
      zoneA.setXmlId("A");
      Zone zoneB = zoning.getOdZones().getFactory().registerNew();
      zoneB.setXmlId("B");
      Zone zoneC = zoning.getOdZones().getFactory().registerNew();
      zoneC.setXmlId("C");
      Zone zoneD = zoning.getOdZones().getFactory().registerNew();
      zoneD.setXmlId("D");
      Zone zoneE = zoning.getOdZones().getFactory().registerNew();
      zoneE.setXmlId("E");
      
      centroidA = zoneA.getCentroid();
      centroidA.setPosition(geoFactory.createPoint(new Coordinate(0, 0)));
      centroidB = zoneB.getCentroid();
      centroidB.setPosition(geoFactory.createPoint(new Coordinate(1*1000, 4*1000)));
      centroidC = zoneC.getCentroid();
      centroidC.setPosition(geoFactory.createPoint(new Coordinate(2*1000, 2*1000)));
      centroidD = zoneD.getCentroid();
      centroidD.setPosition(geoFactory.createPoint(new Coordinate(3*1000, 4*1000)));
      centroidE = zoneE.getCentroid();
      centroidE.setPosition(geoFactory.createPoint(new Coordinate(4*1000, 4*1000)));
      
      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().get(0),  zoneA, 0);
      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().get(21), zoneB, 0);
      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().get(12), zoneC, 0);
      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().get(23), zoneD, 0);
      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().get(24), zoneE, 0);
      
      transportNetwork = new TransportModelNetwork(network, zoning);
      transportNetwork.integrateTransportNetworkViaConnectoids();
      zone2CentroidVertexMapping = transportNetwork.createZoneToCentroidVertexMapping(true, false);
          
      // costs
      linkSegmentCosts = new double[]
            /* horizontal segment costs */
          { 10,10, 12,12, 40,40, 10,10,
            7,7,   5,5,   10,10, 30,30,
            8,8,   40,40, 15,15, 20,20,
            4,4,   10,10, 40,40, 6,6,
            10,10, 3,3,   21,21, 43,43,
            
            /* vertical segment costs */
            33,33, 31,31, 8,8,   12,12, 5,5,
            23,23, 12,12, 47,47, 32,32, 20,20,
            10,10, 20,20, 5,5,   30,30, 20,20,
            10,10, 20,20, 10,10, 9,9,   40,40, 
            
            /* connectoid segment costs */
            0,0,   0,0,   0,0,    0,0,    0,0,
            
          };
      
      assertEquals(networkLayer.getLinkSegments().size()+zoning.getVirtualNetwork().getConnectoidSegments().size(), transportNetwork.getNumberOfEdgeSegmentsAllLayers());
      
    }catch(Exception e) {
      e.printStackTrace();
      fail("initialise");
    }
  }
//@formatter:on

  /**
   * Test Dijsktra one-to-all based on above network
   */
  @Test
  public void dijkstraOneToAllTest() {
    try {

      ShortestPathDijkstra dijkstra = new ShortestPathDijkstra(linkSegmentCosts, transportNetwork.getNumberOfVerticesAllLayers());

      ShortestPathResult result = dijkstra.executeOneToAll(zone2CentroidVertexMapping.get(centroidA.getParentZone()));

      double costAto1 = result.getCostOf(networkLayer.getNodes().get(1));
      assertEquals(10, costAto1, Precision.EPSILON_6);

      double costAto2 = result.getCostOf(networkLayer.getNodes().get(2));
      assertEquals(costAto2, 22, Precision.EPSILON_6);

      double costAto3 = result.getCostOf(networkLayer.getNodes().get(3));
      assertEquals(costAto3, 52, Precision.EPSILON_6);

      double costAto4 = result.getCostOf(networkLayer.getNodes().get(4));
      assertEquals(costAto4, 62, Precision.EPSILON_6);

      double costAto5 = result.getCostOf(networkLayer.getNodes().get(5));
      assertEquals(costAto5, 33, Precision.EPSILON_6);

      double costAto6 = result.getCostOf(networkLayer.getNodes().get(6));
      assertEquals(costAto6, 35, Precision.EPSILON_6);

      double aToCCost = result.getCostOf(zone2CentroidVertexMapping.get(centroidC.getParentZone()));
      assertEquals(aToCCost, 77.0, Precision.EPSILON_6);

      double aToBCost = result.getCostOf(zone2CentroidVertexMapping.get(centroidB.getParentZone()));
      assertEquals(aToBCost, 85.0, Precision.EPSILON_6);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing Dijsktra shortest path - one-to-all");
    }
  }

  /**
   * Test Dijsktra all-to-one based on above network
   */
  @Test
  public void dijkstraAllToOneTest() {
    try {

      ShortestPathDijkstra dijkstra = new ShortestPathDijkstra(linkSegmentCosts, transportNetwork.getNumberOfVerticesAllLayers());

      ShortestPathResult result = dijkstra.executeAllToOne(networkLayer.getNodes().get(1));

      double costAto1 = result.getCostOf(zone2CentroidVertexMapping.get(centroidA.getParentZone()));
      assertEquals(costAto1, 10, Precision.EPSILON_6);

      double cost2to1 = result.getCostOf(networkLayer.getNodes().get(2));
      assertEquals(cost2to1, 12, Precision.EPSILON_6);

      double cost3to1 = result.getCostOf(networkLayer.getNodes().get(3));
      assertEquals(cost3to1, 42, Precision.EPSILON_6);

      result = dijkstra.executeAllToOne(networkLayer.getNodes().get(2));

      double costAto2 = result.getCostOf(zone2CentroidVertexMapping.get(centroidA.getParentZone()));
      assertEquals(costAto2, 22, Precision.EPSILON_6);

      result = dijkstra.executeAllToOne(networkLayer.getNodes().get(3));

      double costAto3 = result.getCostOf(zone2CentroidVertexMapping.get(centroidA.getParentZone()));
      assertEquals(costAto3, 52, Precision.EPSILON_6);

      result = dijkstra.executeAllToOne(networkLayer.getNodes().get(4));

      double costAto4 = result.getCostOf(zone2CentroidVertexMapping.get(centroidA.getParentZone()));
      assertEquals(costAto4, 62, Precision.EPSILON_6);

      result = dijkstra.executeAllToOne(networkLayer.getNodes().get(5));

      double costAto5 = result.getCostOf(zone2CentroidVertexMapping.get(centroidA.getParentZone()));
      assertEquals(costAto5, 33, Precision.EPSILON_6);

      result = dijkstra.executeAllToOne(networkLayer.getNodes().get(6));

      double costAto6 = result.getCostOf(zone2CentroidVertexMapping.get(centroidA.getParentZone()));
      assertEquals(costAto6, 35, Precision.EPSILON_6);

      result = dijkstra.executeAllToOne(zone2CentroidVertexMapping.get(centroidC.getParentZone()));

      double aToCCost = result.getCostOf(zone2CentroidVertexMapping.get(centroidA.getParentZone()));
      assertEquals(aToCCost, 77.0, Precision.EPSILON_6);

      result = dijkstra.executeAllToOne(zone2CentroidVertexMapping.get(centroidB.getParentZone()));

      double aToBCost = result.getCostOf(zone2CentroidVertexMapping.get(centroidA.getParentZone()));
      assertEquals(aToBCost, 85.0, Precision.EPSILON_6);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing Dijsktra shortest path - all-to-one ");
    }
  }

  /**
   * Test A* with same routes and network
   */
  @Test
  public void aStarTest() {
    try {

      // each link is 1 km long. Yet smallest cost for a link is 3 in the network, so the maximum pace per km is 3 which we would use as a muliplier
      double multiplier = 3;

      ShortestPathAStar aStar = new ShortestPathAStar(linkSegmentCosts, transportNetwork.getNumberOfVerticesAllLayers(), crs, multiplier);

      final var centroidVertexA = zone2CentroidVertexMapping.get(centroidA.getParentZone());
      final var centroidVertexB = zone2CentroidVertexMapping.get(centroidB.getParentZone());
      final var centroidVertexC = zone2CentroidVertexMapping.get(centroidC.getParentZone());

      ShortestPathResult result = aStar.executeOneToOne(centroidVertexA, networkLayer.getNodes().get(1));
      double costAto1 = result.getCostOf(networkLayer.getNodes().get(1));
      assertEquals(costAto1, 10, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidVertexA, networkLayer.getNodes().get(2));
      double costAto2 = result.getCostOf(networkLayer.getNodes().get(2));
      assertEquals(costAto2, 22, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidVertexA, networkLayer.getNodes().get(3));
      double costAto3 = result.getCostOf(networkLayer.getNodes().get(3));
      assertEquals(costAto3, 52, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidVertexA, networkLayer.getNodes().get(4));
      double costAto4 = result.getCostOf(networkLayer.getNodes().get(4));
      assertEquals(costAto4, 62, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidVertexA, networkLayer.getNodes().get(5));
      double costAto5 = result.getCostOf(networkLayer.getNodes().get(5));
      assertEquals(costAto5, 33, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidVertexA, networkLayer.getNodes().get(6));
      double costAto6 = result.getCostOf(networkLayer.getNodes().get(6));
      assertEquals(costAto6, 35, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidVertexA, centroidVertexC);
      double aToCCost = result.getCostOf(centroidVertexC);
      assertEquals(aToCCost, 77.0, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidVertexA, centroidVertexB);
      double aToBCost = result.getCostOf(centroidVertexB);
      assertEquals(aToBCost, 85.0, Precision.EPSILON_6);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing aStar shortest path");
    }
  }
}
