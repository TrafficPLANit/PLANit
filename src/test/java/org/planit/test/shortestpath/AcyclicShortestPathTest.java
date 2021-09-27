package org.planit.test.shortestpath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.factory.epsg.CartesianAuthorityFactory;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.graph.directed.acyclic.ACyclicSubGraph;
import org.planit.graph.directed.acyclic.ACyclicSubGraphImpl;
import org.planit.network.MacroscopicNetwork;
import org.planit.network.transport.TransportModelNetwork;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.directed.DirectedEdge;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.zoning.Centroid;
import org.planit.utils.zoning.Zone;
import org.planit.zoning.Zoning;

/**
 * Test the acyclic min-maxpath algorithms and topological sorting
 * 
 * @author markr
 *
 */
public class AcyclicShortestPathTest {

  private static final CoordinateReferenceSystem crs = CartesianAuthorityFactory.GENERIC_2D;

  private TransportModelNetwork transportNetwork;
  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  double[] linkSegmentCosts;

  private Centroid centroidA;
  private Centroid centroidB;

  //@formatter:off
  @Before
  public void intialise() {
    // construct the network. The is the same network as in shortest path algorithm integration tests
    //
    //
    //
    //          col 0  col 1   col 2 
    //       
    //               4     5
    //           .--->--.-->---B       row 2
    //           |      |      |  
    //          9^    10^    11^  
    //           |      |      |  
    //           |  2   |   3  |         
    //           .-->---.--->--.        row 1
    //           |      |      | 
    //           ^      ^      ^ 
    //          6|     7|     8| 
    //           |  0   |  1   |     
    //           A--->--.-->---.        row 0
    //NODE      (0)   (1)    (2)     
    //LINK         (0)    (1)    
    //LINKSgmnt   (0,-)  (1,-)   
    
    try {
      // local CRS in meters
      GeometryFactory geoFactory = JTSFactoryFinder.getGeometryFactory();
      
      int gridSize = 2;
      network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
      networkLayer = network.getTransportLayers().getFactory().registerNew();
      for(int nodeRowIndex = 0;nodeRowIndex<=gridSize;++nodeRowIndex) {
        for(int nodeColIndex = 0;nodeColIndex<=gridSize;++nodeColIndex) {
          Node node = networkLayer.getNodes().getFactory().registerNew();
          String xmlId = String.valueOf(node.getId());          
          node.setXmlId(xmlId);
          // all nodes are spaced 1 km apart
          node.setPosition(geoFactory.createPoint(new Coordinate(nodeRowIndex*1000, nodeColIndex*1000)));
        }
      }
      
      //horizontal links
      for(int linkRowIndex = 0;linkRowIndex<=gridSize;++linkRowIndex) {
        for(int linkColIndex = 1;linkColIndex<=gridSize;++linkColIndex) {
          Node nodeA = networkLayer.getNodes().get(linkRowIndex*(gridSize+1) + linkColIndex-1);
          Node nodeB = networkLayer.getNodes().get(linkRowIndex*(gridSize+1) + linkColIndex);
          // all links are 1 km in length and move from left to right         
          Link link = networkLayer.getLinks().getFactory().registerNew(nodeA, nodeB, 1, true);
          networkLayer.getLinkSegments().getFactory().registerNew(link, true, true);
        }
      }
        
      //vertical links
      for(int linkRowIndex = 1;linkRowIndex<=gridSize;++linkRowIndex) {
        for(int linkColIndex = 0;linkColIndex<=gridSize;++linkColIndex) {
          // all links are 1 km in length
          Node nodeA = networkLayer.getNodes().get((linkRowIndex-1)*(gridSize+1)+linkColIndex);
          Node nodeB = networkLayer.getNodes().get(linkRowIndex*(gridSize+1)+linkColIndex);
          Link link = networkLayer.getLinks().getFactory().registerNew(nodeA, nodeB, 1, true);
          networkLayer.getLinkSegments().getFactory().registerNew(link, true, true);
        }  
      }
      
      zoning = new Zoning(IdGroupingToken.collectGlobalToken(), networkLayer.getLayerIdGroupingToken());
      Zone zoneA = zoning.odZones.getFactory().registerNew();
      zoneA.setXmlId("A");
      Zone zoneB = zoning.odZones.getFactory().registerNew();
      zoneB.setXmlId("B");      
      
      centroidA = zoneA.getCentroid();
      centroidA.setPosition(geoFactory.createPoint(new Coordinate(0, 0)));
      centroidB = zoneB.getCentroid();
      centroidB.setPosition(geoFactory.createPoint(new Coordinate(2*1000, 2*1000)));      
      
      zoning.odConnectoids.getFactory().registerNew(networkLayer.getNodes().get(0), zoneA, 0);
      zoning.odConnectoids.getFactory().registerNew(networkLayer.getNodes().get(8), zoneB, 0);      
      
      transportNetwork = new TransportModelNetwork(network, zoning);
      transportNetwork.integrateTransportNetworkViaConnectoids();
          
      // costs
      linkSegmentCosts = new double[]
            /* horizontal segment costs */
          { 0,1,2,3,4,5,
            
            /* vertical segment costs */
            6,7,8,9,10,11, 
            
            /* connectoid segment costs */
            0,0,0,0
            
          };
      
      assertEquals(networkLayer.getLinkSegments().size()+zoning.getVirtualNetwork().getConnectoidSegments().size(), transportNetwork.getNumberOfEdgeSegmentsAllLayers());
      
    }catch(Exception e) {
      e.printStackTrace();
      assertFalse(true);
    }
  }
//@formatter:on

  /**
   * Test topological sorting on above network
   */
  @Test
  public void topologicalSortingTest() {
    try {

      Centroid root = zoning.getOdZones().getByXmlId("A").getCentroid();
      Centroid destination = zoning.getOdZones().getByXmlId("B").getCentroid();
      long totalEdgeSegments = transportNetwork.getNumberOfEdgeSegmentsAllLayers();
      ACyclicSubGraph<DirectedVertex, ?, ?> acyclicSubGraph = new ACyclicSubGraphImpl<DirectedVertex, DirectedEdge, EdgeSegment>(network.getNetworkGroupingTokenId(),
          (int) totalEdgeSegments, root);

      /* add all physical link segments */
      for (MacroscopicLinkSegment linkSegment : networkLayer.getLinkSegments()) {
        acyclicSubGraph.addEdgeSegment(linkSegment);
      }

      /* only add outgoing connectoid segment of origin and incoming connectoid segment of destination */
      acyclicSubGraph.addEdgeSegment(root.getExitEdgeSegments().iterator().next());
      acyclicSubGraph.addEdgeSegment(destination.getEntryEdgeSegments().iterator().next());

      Collection<DirectedVertex> topologicalOrder = acyclicSubGraph.topologicalSort();
      assertNotNull(topologicalOrder);

      Set<Long> processed = new HashSet<Long>();
      for (DirectedVertex vertex : topologicalOrder) {

        // vertex 0 should occur before 1,3
        if (vertex.getId() == 1 || vertex.getId() == 3) {
          assertTrue(processed.contains(0l));
        }

        // vertex 1 should occur before 2,5
        if (vertex.getId() == 2 || vertex.getId() == 5) {
          assertTrue(processed.contains(1l));
        }

        // vertex 3 should occur before 4,6
        if (vertex.getId() == 4 || vertex.getId() == 6) {
          assertTrue(processed.contains(3l));
        }

        // vertex 4 should occur before 5,7
        if (vertex.getId() == 5 || vertex.getId() == 7) {
          assertTrue(processed.contains(4l));
        }

        // vertex 6 should occur before 7
        if (vertex.getId() == 7) {
          assertTrue(processed.contains(6l));
        }

        // vertex 5 and 7 should occur before 8
        if (vertex.getId() == 8) {
          assertTrue(processed.contains(5l));
          assertTrue(processed.contains(7l));
        }

        processed.add(vertex.getId());
      }

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing Dijsktra shortest path");
    }
  }

}
