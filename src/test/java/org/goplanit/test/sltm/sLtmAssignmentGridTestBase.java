package org.goplanit.test.sltm;

import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.demands.Demands;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.MacroscopicNetworkUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.ZoningUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Base class for testing the sLTM assignment basic functionality (route choice) with a grid based network layout
 * 
 * @author markr
 *
 */
public class sLtmAssignmentGridTestBase {

  protected MacroscopicNetwork network;
  protected MacroscopicNetworkLayer networkLayer;
  protected Zoning zoning;

  protected static double MAX_SPEED_KM_H = 60.0;

  /**
   * Create demands object, with time period T=[0,3600] a dummy user and traveler type 
   *
   * @param testToken to use
   * @return created demands
   */
  protected Demands createDemands(IdGroupingToken testToken) {
    Demands demands = new Demands(testToken);
    demands.timePeriods.getFactory().registerNew("dummyTimePeriod", 0, 3600);
    demands.travelerTypes.getFactory().registerNew("dummyTravellerType");
    demands.userClasses.getFactory().registerNew("dummyUser", network.getModes().get(PredefinedModeType.CAR), demands.travelerTypes.getFirst());
    return demands;
  }

  //@formatter:off

  /**
   * @param testToken to use
   */
  protected void intialise4x4NetworkAndZoning(IdGroupingToken testToken) {
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
      
      network = MacroscopicNetworkUtils.createSimpleGrid(testToken, 4, 4);
      networkLayer = network.getTransportLayers().getFirst();
      
      /* add physical link in front of attaching zone to node 0 and 12 so that we can properly deal with any queue build
      up there*/
      int STUB_LENGTH_KM = 1;
      int STUB_LANES = 2;
      var linkSegmentType = networkLayer.getLinkSegmentTypes().getFirst();
      boolean CREATE_TO_STUB = true;
      boolean CREATE_FROM_STUB = true;

      networkLayer.getLinkSegmentTypes().forEach(
          ls -> ls.getAccessProperties(network.getModes().getFirst()).setMaximumSpeedKmH(MAX_SPEED_KM_H /* km/h */));
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.getOdZones().getFactory().registerNew().setXmlId("A");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A`");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A``");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A```");

      var stubBefore0 = ZoningUtils.createOdConnectoidOnNewPhysicalStub(
          zoning, zoning.getOdZones().getByXmlId("A"),0.0,
          networkLayer,  networkLayer.getNodes().getByXmlId("0"),
          STUB_LENGTH_KM, STUB_LANES, linkSegmentType,CREATE_TO_STUB,CREATE_FROM_STUB);
      var stubBefore12 = ZoningUtils.createOdConnectoidOnNewPhysicalStub(
          zoning, zoning.getOdZones().getByXmlId("A`"),0.0,
          networkLayer,  networkLayer.getNodes().getByXmlId("12"),
          STUB_LENGTH_KM, STUB_LANES, linkSegmentType,CREATE_TO_STUB,CREATE_FROM_STUB);

      stubBefore0.second().forEachSegment(ls -> ls.setXmlId(""+ls.getId()));
      stubBefore12.second().forEachSegment(ls -> ls.setXmlId(""+ls.getId()));

      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().get(7),  zoning.getOdZones().getByXmlId("A``"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().get(11),  zoning.getOdZones().getByXmlId("A```"), 0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      fail("initialise");
    }
  }

  /**
   * @param testToken to use
   */
  protected void intialise10x10NetworkAndZoning(IdGroupingToken testToken) {
    // construct the network.
    //
    // The network is a 10X10 grid. All links have 1800 capacity per lane, for a single lane
    //
    // C_a = 1800 pcu/h (default when not set explicitly)
    // Maximum speed = 60 km/h
    //
    //
    //                                          A'
    //        99  (19)  109 (29)    179   (99)
    // (9) * ------ * ------ * . . . ---- *
    //    |        |        |             |
    //    | 8      | 17     | 21          | 89
    //    :        :        :             :
    //    :        :        :             :
    //    :   93   :   103  :       173   :
    //(3) * ------ * ------ * . . . ------* (94)
    //    |    (13)|    (11)|             |
    //    | 2      | 11     | 20          | 83
    //    |   92   |   102  |        172  |
    //(2) * ------ * ------ * . . . ------* (92)
    //    |    (12)|    (10)|             |
    //    | 1      | 10     | 19          | 82
    //    |   91   |   101  |       171   |
    //(1) * ------ * ------ * . . . ------* (92)
    //    |    (11)|     (9)|             |
    //    | 0      | 9      | 18          | 81
    //    |        |        |             |
    //    * ------ * ------ * . . . ------ *
    // (0)    90  (10) 100 (21)     170     (91)
    //  A

    try {

      network = MacroscopicNetworkUtils.createSimpleGrid(testToken, 10, 10);
      networkLayer = network.getTransportLayers().getFirst();

      /* add physical link in front of attaching zone to node 0 so that we can properly deal with any queue build
      up there*/
      var nodeBefore0 = networkLayer.getNodes().getFactory().registerNew();
      nodeBefore0.setXmlId("before0");
      var linkBefore0 = networkLayer.getLinks().getFactory().registerNew(
          nodeBefore0, networkLayer.getNodes().getByXmlId("0"), 1, true);
      var linkSegmentsBefore0 = networkLayer.getLinkSegments().getFactory().registerNew(
          linkBefore0, true);
      linkSegmentsBefore0.<MacroscopicLinkSegment>both( ls -> ls.setXmlId(""+ls.getId()));
      linkSegmentsBefore0.<MacroscopicLinkSegment>both( ls -> ls.setLinkSegmentType(networkLayer.getLinkSegmentTypes().getFirst()));
      linkSegmentsBefore0.<MacroscopicLinkSegment>both( ls -> ls.setNumberOfLanes(2));

      networkLayer.getLinkSegmentTypes().forEach(
          ls -> ls.getAccessProperties(network.getModes().getFirst()).setMaximumSpeedKmH(MAX_SPEED_KM_H /* km/h */));

      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.getOdZones().getFactory().registerNew().setXmlId("A");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A`");

      zoning.getOdConnectoids().getFactory().registerNew(nodeBefore0,  zoning.getOdZones().getByXmlId("A"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(
          networkLayer.getNodes().get(99),  zoning.getOdZones().getByXmlId("A`"), 0);

    }catch(Exception e) {
      e.printStackTrace();
      fail("initialise");
    }
  }
  //@formatter:on


  public void test4x4OutflowsNoQueue(StaticLtm sLTM) {
    double outflow0 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
    double outflow1 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double outflow2 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());
    double outflow3 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("3").getLinkSegmentAb());
    double outflow4 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("4").getLinkSegmentAb());
    double outflow5 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
    double outflow6 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("6").getLinkSegmentAb());
    double outflow7 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("7").getLinkSegmentAb());
    double outflow8 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
    double outflow9 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("9").getLinkSegmentAb());
    double outflow10 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("10").getLinkSegmentAb());
    double outflow11 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("11").getLinkSegmentAb());
    double outflow12 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("12").getLinkSegmentAb());
    double outflow13 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("13").getLinkSegmentAb());
    double outflow14 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("14").getLinkSegmentAb());
    double outflow15 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("15").getLinkSegmentAb());
    double outflow20 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("20").getLinkSegmentBa());
    double outflow21 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("21").getLinkSegmentBa());
    double outflow22 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("22").getLinkSegmentBa());
    double outflow23 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("23").getLinkSegmentBa());

    assertEquals(1102, outflow0, 1);
    assertEquals(900, outflow1 , 1);
    assertEquals(697, outflow12, 1);
    assertEquals(697, outflow2 , 1);
    assertEquals(202, outflow13, 1);

    assertEquals(outflow3, outflow12, 1);
    assertEquals(outflow13 + outflow3, outflow4 , 1);
    assertEquals(outflow14 + outflow4, outflow5, 1);
    assertEquals(outflow1 - outflow2, outflow14, 1);
    assertEquals(outflow15, outflow2, 1);

    assertEquals(outflow0, outflow9,  1);
    assertEquals(outflow1, outflow10, 1);
    assertEquals(outflow2, outflow11, 1);
    assertEquals(outflow15, outflow23,1);
    assertEquals(outflow5, outflow8, 1);
    assertEquals(outflow14, outflow22,1);
    assertEquals(outflow13, outflow21,1);
    assertEquals(outflow12, outflow20,1);
  }

}
