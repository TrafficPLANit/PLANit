package org.planit.test.sltm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.factory.epsg.CartesianAuthorityFactory;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.network.MacroscopicNetwork;
import org.planit.network.transport.TransportModelNetwork;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.zoning.Centroid;
import org.planit.zoning.Zoning;

/**
 * Test the sLTM assignment algorithms basic functionality
 * 
 * @author markr
 *
 */
public class sLtmTest {

  private static final CoordinateReferenceSystem crs = CartesianAuthorityFactory.GENERIC_2D;

  private TransportModelNetwork transportNetwork;
  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  private Centroid centroidA;
  private Centroid centroidB;
  private Centroid centroidC;
  private Centroid centroidD;

  //@formatter:off
  @Before
  public void intialise() {
    // construct the network. 
    //
    // Create the two route network (A->D) and (C-B) with interactions at the two bottleneck nodes that lead
    // to non-convergence in Bliemer et al. (2014), but can be solved by Raadsen and Bliemer (2021)
    //
    //     0        1            2        3
    //  C  *-->-----*----->------*---->---* D
    //              |            |
    //              ^            V
    //     4       5|           6|        7
    //  B  *---<----*-----<------*----<---* A
    //
    
    
    try {
      // local CRS in meters
      GeometryFactory geoFactory = JTSFactoryFinder.getGeometryFactory();
      
      network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
      networkLayer = network.getTransportLayers().getFactory().registerNew();
      for(int nodeRowIndex = 0;nodeRowIndex<=1;++nodeRowIndex) {
        for(int nodeColIndex = 0;nodeColIndex<=3;++nodeColIndex) {
          String xmlId = String.valueOf(nodeRowIndex*3+nodeColIndex);
          Node node = networkLayer.getNodes().getFactory().registerNew();
          node.setXmlId(xmlId);
          // all nodes are spaced 1 km apart
          node.setPosition(geoFactory.createPoint(new Coordinate(nodeRowIndex*1000, nodeColIndex*1000)));
        }
      }
      
      //links
//      Node nodeA = networkLayer.getNodes().get(linkRowIndex*(gridSize+1) + linkColIndex-1);
//      Node nodeB = networkLayer.getNodes().get(linkRowIndex*(gridSize+1) + linkColIndex);
//      // all links are 1 km in length          
//      Link link = networkLayer.getLinks().getFactory().registerNew(nodeA, nodeB, 1);
//      nodeA.addEdge(link);
//      nodeB.addEdge(link);
//      LinkSegment linkSegmentAb = networkLayer.getLinkSegments().getFactory().create(link, true);
//      LinkSegment linkSegmentBa = networkLayer.getLinkSegments().getFactory().create(link, false);
//      nodeB.addEdgeSegment(linkSegmentAb);
//      nodeB.addEdgeSegment(linkSegmentBa);
//      nodeA.addEdgeSegment(linkSegmentAb);
//      nodeA.addEdgeSegment(linkSegmentBa);       
              
//      zoning = new Zoning(IdGroupingToken.collectGlobalToken(), networkLayer.getLayerIdGroupingToken());
//      Zone zoneA = zoning.odZones.getFactory().registerNew();
//      zoneA.setExternalId("A");
//      Zone zoneB = zoning.odZones.getFactory().registerNew();
//      zoneB.setExternalId("B");
//      Zone zoneC = zoning.odZones.getFactory().registerNew();
//      zoneC.setExternalId("C");
//      Zone zoneD = zoning.odZones.getFactory().registerNew();
//      zoneD.setExternalId("D");
//      Zone zoneE = zoning.odZones.getFactory().registerNew();
//      zoneE.setExternalId("E");
      
//      centroidA = zoneA.getCentroid();
//      centroidA.setPosition(geoFactory.createPoint(new Coordinate(0, 0)));
//      centroidB = zoneB.getCentroid();
//      centroidB.setPosition(geoFactory.createPoint(new Coordinate(1*1000, 4*1000)));
//      
//      zoning.odConnectoids.getFactory().registerNew(networkLayer.getNodes().get(0),  zoneA, 0);
//      zoning.odConnectoids.getFactory().registerNew(networkLayer.getNodes().get(21), zoneB, 0);
//      
//      transportNetwork = new TransportModelNetwork(network, zoning);
//      transportNetwork.integrateTransportNetworkViaConnectoids();
                
    }catch(Exception e) {
      e.printStackTrace();
      assertFalse(true);
    }
  }
  //@formatter:on

  /**
   * Test sLTM network loading on above network
   */
  @Test
  public void sLtmNetworkLoadingTest() {
    try {

      // TODO

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM network loading");
    }
  }

}
