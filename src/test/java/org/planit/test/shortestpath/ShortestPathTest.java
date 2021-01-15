package org.planit.test.shortestpath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.factory.epsg.CartesianAuthorityFactory;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.algorithms.shortestpath.AStarShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.ShortestPathResult;
import org.planit.network.macroscopic.physical.MacroscopicNetwork;
import org.planit.network.transport.TransportNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.math.Precision;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.Zone;

/**
 * Test the shortest path algorithms
 * 
 * @author markr
 *
 */
public class ShortestPathTest {

  private static final CoordinateReferenceSystem crs = CartesianAuthorityFactory.GENERIC_2D;

  private TransportNetwork transportNetwork;
  private MacroscopicNetwork network;
  private Zoning zoning;

  double[] linkSegmentCosts;

  private Centroid centroidA;
  private Centroid centroidB;
  private Centroid centroidC;
  private Centroid centroidD;
  private Centroid centroidE;

  //@formatter:off
  @Before
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
      network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
      for(int nodeRowIndex = 0;nodeRowIndex<=gridSize;++nodeRowIndex) {
        for(int nodeColIndex = 0;nodeColIndex<=gridSize;++nodeColIndex) {
          String externalId = String.valueOf(nodeRowIndex*gridSize+nodeColIndex);
          Node node = network.nodes.registerNew();
          node.setExternalId(externalId);
          // all nodes are spaced 1 km apart
          node.setPosition(geoFactory.createPoint(new Coordinate(nodeRowIndex*1000, nodeColIndex*1000)));
        }
      }
      
      //horizontal links
      for(int linkRowIndex = 0;linkRowIndex<=gridSize;++linkRowIndex) {
        for(int linkColIndex = 1;linkColIndex<=gridSize;++linkColIndex) {
          Node nodeA = network.nodes.get(linkRowIndex*(gridSize+1) + linkColIndex-1);
          Node nodeB = network.nodes.get(linkRowIndex*(gridSize+1) + linkColIndex);
          // all links are 1 km in length          
          Link link = network.links.registerNew(nodeA, nodeB, 1);
          nodeA.addEdge(link);
          nodeB.addEdge(link);
          LinkSegment linkSegmentAb = network.linkSegments.create(link, true);
          LinkSegment linkSegmentBa = network.linkSegments.create(link, false);
          nodeB.addEdgeSegment(linkSegmentAb);
          nodeB.addEdgeSegment(linkSegmentBa);
          nodeA.addEdgeSegment(linkSegmentAb);
          nodeA.addEdgeSegment(linkSegmentBa);
        }
      }
        
      //vertical links
      for(int linkRowIndex = 1;linkRowIndex<=gridSize;++linkRowIndex) {
        for(int linkColIndex = 0;linkColIndex<=gridSize;++linkColIndex) {
          // all links are 1 km in length
          Node nodeA = network.nodes.get((linkRowIndex-1)*(gridSize+1)+linkColIndex);
          Node nodeB = network.nodes.get(linkRowIndex*(gridSize+1)+linkColIndex);
          Link link = network.links.registerNew(nodeA, nodeB, 1);
          nodeA.addEdge(link);
          nodeB.addEdge(link);
          LinkSegment linkSegmentAb = network.linkSegments.create(link, true);
          LinkSegment linkSegmentBa = network.linkSegments.create(link, false);
          nodeB.addEdgeSegment(linkSegmentBa);
          nodeB.addEdgeSegment(linkSegmentAb);
          nodeA.addEdgeSegment(linkSegmentAb);
          nodeA.addEdgeSegment(linkSegmentBa);
        }  
      }
      
      zoning = new Zoning(IdGroupingToken.collectGlobalToken(), network.getNetworkIdGroupingToken());
      Zone zoneA = zoning.zones.createAndRegisterNewZone();
      zoneA.setExternalId("A");
      Zone zoneB = zoning.zones.createAndRegisterNewZone();
      zoneB.setExternalId("B");
      Zone zoneC = zoning.zones.createAndRegisterNewZone();
      zoneC.setExternalId("C");
      Zone zoneD = zoning.zones.createAndRegisterNewZone();
      zoneD.setExternalId("D");
      Zone zoneE = zoning.zones.createAndRegisterNewZone();
      zoneE.setExternalId("E");
      
      centroidA = zoning.getVirtualNetwork().centroids.registerNewCentroid(zoneA);
      centroidA.setPosition(geoFactory.createPoint(new Coordinate(0, 0)));
      centroidB = zoning.getVirtualNetwork().centroids.registerNewCentroid(zoneB);
      centroidB.setPosition(geoFactory.createPoint(new Coordinate(1*1000, 4*1000)));
      centroidC = zoning.getVirtualNetwork().centroids.registerNewCentroid(zoneC);
      centroidC.setPosition(geoFactory.createPoint(new Coordinate(2*1000, 2*1000)));
      centroidD = zoning.getVirtualNetwork().centroids.registerNewCentroid(zoneD);
      centroidD.setPosition(geoFactory.createPoint(new Coordinate(3*1000, 4*1000)));
      centroidE = zoning.getVirtualNetwork().centroids.registerNewCentroid(zoneE);
      centroidE.setPosition(geoFactory.createPoint(new Coordinate(4*1000, 4*1000)));
      
      zoning.getVirtualNetwork().connectoids.registerNewConnectoid(centroidA, network.nodes.get(0), 0);
      zoning.getVirtualNetwork().connectoids.registerNewConnectoid(centroidB, network.nodes.get(21), 0);
      zoning.getVirtualNetwork().connectoids.registerNewConnectoid(centroidC, network.nodes.get(12), 0);
      zoning.getVirtualNetwork().connectoids.registerNewConnectoid(centroidD, network.nodes.get(23), 0);
      zoning.getVirtualNetwork().connectoids.registerNewConnectoid(centroidE, network.nodes.get(24), 0);
      
      transportNetwork = new TransportNetwork(network, zoning);
      transportNetwork.integrateConnectoidsAndLinks();
          
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
      
      assertEquals(network.linkSegments.size()+zoning.getVirtualNetwork().connectoidSegments.getNumberOfConnectoidSegments(), transportNetwork.getTotalNumberOfEdgeSegments());
      
    }catch(Exception e) {
      e.printStackTrace();
      assertFalse(true);
    }
  }
//@formatter:on

  /**
   * Test Dijsktra based on above network
   */
  @Test
  public void dijkstraTest() {
    try {

      DijkstraShortestPathAlgorithm dijkstra = new DijkstraShortestPathAlgorithm(linkSegmentCosts, transportNetwork.getTotalNumberOfEdgeSegments(),
          transportNetwork.getTotalNumberOfVertices());

      ShortestPathResult result = dijkstra.executeOneToAll(centroidA);

      double costAto1 = result.getCostToReach(network.nodes.get(1));
      assertEquals(costAto1, 10, Precision.EPSILON_6);

      double costAto2 = result.getCostToReach(network.nodes.get(2));
      assertEquals(costAto2, 22, Precision.EPSILON_6);

      double costAto3 = result.getCostToReach(network.nodes.get(3));
      assertEquals(costAto3, 52, Precision.EPSILON_6);

      double costAto4 = result.getCostToReach(network.nodes.get(4));
      assertEquals(costAto4, 62, Precision.EPSILON_6);

      double costAto5 = result.getCostToReach(network.nodes.get(5));
      assertEquals(costAto5, 33, Precision.EPSILON_6);

      double costAto6 = result.getCostToReach(network.nodes.get(6));
      assertEquals(costAto6, 35, Precision.EPSILON_6);

      double aToCCost = result.getCostToReach(centroidC);
      assertEquals(aToCCost, 77.0, Precision.EPSILON_6);

      double aToBCost = result.getCostToReach(centroidB);
      assertEquals(aToBCost, 85.0, Precision.EPSILON_6);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing Dijsktra shortest path");
    }
  }

  /**
   * Test A* with same routes and network
   */
  @Test
  public void aStarTest() {
    try {

      // each link is 1 km long. Yet smallest cost for a link is 3 in the network, so the minimum cost multiplier per km is 3
      double multiplier = 3;

      AStarShortestPathAlgorithm aStar = new AStarShortestPathAlgorithm(linkSegmentCosts, transportNetwork.getTotalNumberOfVertices(), crs, multiplier);

      ShortestPathResult result = aStar.executeOneToOne(centroidA, network.nodes.get(1));
      double costAto1 = result.getCostToReach(network.nodes.get(1));
      assertEquals(costAto1, 10, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidA, network.nodes.get(2));
      double costAto2 = result.getCostToReach(network.nodes.get(2));
      assertEquals(costAto2, 22, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidA, network.nodes.get(3));
      double costAto3 = result.getCostToReach(network.nodes.get(3));
      assertEquals(costAto3, 52, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidA, network.nodes.get(4));
      double costAto4 = result.getCostToReach(network.nodes.get(4));
      assertEquals(costAto4, 62, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidA, network.nodes.get(5));
      double costAto5 = result.getCostToReach(network.nodes.get(5));
      assertEquals(costAto5, 33, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidA, network.nodes.get(6));
      double costAto6 = result.getCostToReach(network.nodes.get(6));
      assertEquals(costAto6, 35, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidA, centroidC);
      double aToCCost = result.getCostToReach(centroidC);
      assertEquals(aToCCost, 77.0, Precision.EPSILON_6);

      result = aStar.executeOneToOne(centroidA, centroidB);
      double aToBCost = result.getCostToReach(centroidB);
      assertEquals(aToBCost, 85.0, Precision.EPSILON_6);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing aStar shortest path");
    }
  }
}
