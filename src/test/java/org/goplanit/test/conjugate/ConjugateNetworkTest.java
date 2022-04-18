package org.goplanit.test.conjugate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.ConjugateMacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.zoning.Zoning;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("conjugateNetworkTest");

  /** the logger */
  private static Logger LOGGER = null;

  private static double MAX_SPEED_KM_H = 60.0;

  /**
   * {@inheritDoc}
   */
  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(ConjugateNetworkTest.class);
    }
  }

  /**
   * {@inheritDoc}
   */
  @After
  public void tearDown() {
    Logging.closeLogger(LOGGER);
  }

  //@formatter:off
  @Before
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
      assertFalse(true);
    }
  }
  //@formatter:on

  /**
   * Test the creation of conjugate network from the given original network
   */
  @Test
  public void conjugateNetworkTest() {
    try {

      ConjugateMacroscopicNetworkLayer conjugateLayer = networkLayer.createConjugate();

      // TODO: test contents and assert them to make sure it works

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment");
    }
  }

}
