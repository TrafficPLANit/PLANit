package org.goplanit.test.conjugate;

import java.util.logging.Logger;

import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.ConjugateMacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test the sLTM assignment basic functionality (route choice) with a grid based network layout
 * 
 * @author markr
 *
 */
public class ConjugateNetworkTest {

  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("regularNetworkToken");
  private final IdGroupingToken conjugateTestToken = IdGenerator.createIdGroupingToken("conjugateNetworkToken");

  /** the logger */
  private static Logger LOGGER = null;

  private static double MAX_SPEED_KM_H = 60.0;

  /**
   * {@inheritDoc}
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(ConjugateNetworkTest.class);
    }
  }

  /**
   * {@inheritDoc}
   */
  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
  }

  //@formatter:off
  @BeforeEach
  public void intialise() {
    // construct the network. 
    //
    // The network is a 4X4 grid. All links have 1800 capacity per lane, for a single lane
    //
    // C_a = 1800 pcu/h (default when not set explicitly)
    // Maximum speed = 60 km/h
    //
    //
    //
    //            A''       A'''
    //        15  (7)  19  (11)  23    (15)
    //(3) * ------ * ------ * ------ *
    //    |        |        |        |
    //    | 2      | 5      | 8      | 11
    //    |    14  |   18   |   22   |
    //(2) * ------ * ------ * ------ * (14)
    //    |     (6)|    (10)|        |
    //    | 1      | 4      | 7      | 10
    //    |    13  |   17   |   21   |
    //(1) * ------ * ------ * ------ * (13)
    //    |     (5)|     (9)|        |
    //    | 0      | 3      | 6      | 9
    //    |        |        |        |
    //    * ------ * ------ * ------ *
    // (0)    12  (4)  16  (8)  20    (12)
    //  A                               A'
    //
    // note that the link segments double the ids of the links, so link 12 has a segment with id 24 and 25 for example
    
    try {
      
      network = MacroscopicNetwork.createSimpleGrid(testToken, 4, 4);
      networkLayer = network.getTransportLayers().getFirst();
      
      /* add physical link in front of attaching zone to node 0 and 12 so that we can properly deal with any queue build up there*/
      var nodeBefore0 = networkLayer.getNodes().getFactory().registerNew();
      nodeBefore0.setXmlId("before0");
      var nodeBefore12 = networkLayer.getNodes().getFactory().registerNew();
      nodeBefore12.setXmlId("before12");
      var linkBefore0 = networkLayer.getLinks().getFactory().registerNew(nodeBefore0, networkLayer.getNodes().getByXmlId("0"), 1, true);
      var linkBefore12 = networkLayer.getLinks().getFactory().registerNew(nodeBefore12, networkLayer.getNodes().getByXmlId("12"), 1, true);
      var linkSegmentsBefore0 = networkLayer.getLinkSegments().getFactory().registerNew(linkBefore0, true);
      var linkSegmentsBefore12 = networkLayer.getLinkSegments().getFactory().registerNew(linkBefore12, true);
      linkSegmentsBefore0.<MacroscopicLinkSegment>both( ls -> ls.setXmlId(""+ls.getId()));
      linkSegmentsBefore12.<MacroscopicLinkSegment>both( ls -> ls.setXmlId(""+ls.getId()));
      linkSegmentsBefore0.<MacroscopicLinkSegment>both( ls -> ls.setLinkSegmentType(networkLayer.getLinkSegmentTypes().getFirst()));
      linkSegmentsBefore12.<MacroscopicLinkSegment>both( ls -> ls.setLinkSegmentType(networkLayer.getLinkSegmentTypes().getFirst()));
      linkSegmentsBefore0.<MacroscopicLinkSegment>both( ls -> ls.setNumberOfLanes(2));
      linkSegmentsBefore12.<MacroscopicLinkSegment>both( ls -> ls.setNumberOfLanes(2));
      
      networkLayer.getLinkSegmentTypes().forEach( ls -> ls.getAccessProperties(network.getModes().getFirst()).setMaximumSpeedKmH(MAX_SPEED_KM_H /* km/h */));           
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.getOdZones().getFactory().registerNew().setXmlId("A");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A`");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A``");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A```");
           
      zoning.getOdConnectoids().getFactory().registerNew(nodeBefore0,  zoning.getOdZones().getByXmlId("A"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(nodeBefore12,  zoning.getOdZones().getByXmlId("A`"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().get(7),  zoning.getOdZones().getByXmlId("A``"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().get(11),  zoning.getOdZones().getByXmlId("A```"), 0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      Assertions.fail();
    }
  }
  //@formatter:on

  /**
   * Test the creation of conjugate network from the given original network
   */
  @Test
  public void conjugateNetworkTest() {
    try {

      var conjugateVirtualNetwork = zoning.getVirtualNetwork().createConjugate(conjugateTestToken);
      /* use a different token to ensure vertices/edges/edgesegments count from zero again, if we use the same token, they would simply continue */
      ConjugateMacroscopicNetworkLayer conjugateLayer = networkLayer.createConjugate(conjugateTestToken, conjugateVirtualNetwork);

      assertEquals(networkLayer.getLinks().size(), conjugateLayer.getConjugateNodes().size());

      /* physical conjugate network check */
      int totalEdgePairs = 0;
      for (Node node : networkLayer.getNodes()) {
        int combinations = 0;
        for (int index = node.getEdges().size() - 1; index > 0; --index) {
          combinations += index;
        }
        totalEdgePairs += combinations;
      }
      assertEquals(totalEdgePairs, conjugateLayer.getConjugateLinks().size());
      assertEquals(totalEdgePairs * 2, conjugateLayer.getConjugateLinkSegments().size());

      /*
       * virtual conjugate network check (where we add a partial dummy turn around each centroid to enter/exit the virtual network. Therefore, we have a single conjugate edge + 2
       * conjugate segments for each original connectoid edge
       */
      assertEquals(zoning.getVirtualNetwork().getConnectoidEdges().size(), conjugateVirtualNetwork.getConjugateConnectoidEdges().size());
      assertEquals(zoning.getVirtualNetwork().getConnectoidSegments().size(), conjugateVirtualNetwork.getConjugateConnectoidEdgeSegments().size());

    } catch (Exception e) {
      e.printStackTrace();
      Assertions.fail("Error when testing conjugate network creation from macroscopic network layer");
    }
  }

}
