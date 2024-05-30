package org.goplanit.test.sltm;

import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.ltm.sltm.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmTrafficAssignmentBuilder;
import org.goplanit.assignment.ltm.sltm.StaticLtmType;
import org.goplanit.demands.Demands;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.od.demand.OdDemandMatrix;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.zoning.OdZones;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

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
  protected void intialiseNetworkAndZoning(IdGroupingToken testToken) {
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
      fail("initialise");
    }
  }
  //@formatter:on


  public void testOutflowsNoQueue(StaticLtm sLTM) {
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

    assertEquals(900, outflow0, Precision.EPSILON_3);
    assertEquals(outflow0 / 2, outflow1 , Precision.EPSILON_3);
    assertEquals(outflow1 / 2, outflow2 , Precision.EPSILON_3);
    assertEquals(outflow3, outflow12, Precision.EPSILON_3);
    assertEquals(outflow13 + outflow3, outflow4 , Precision.EPSILON_3);
    assertEquals(outflow14 + outflow4, outflow5,  Precision.EPSILON_3);
    assertEquals(outflow12, outflow0, Precision.EPSILON_3);
    assertEquals(outflow13, outflow1, Precision.EPSILON_3);
    assertEquals(outflow14, outflow2, Precision.EPSILON_3);
    assertEquals(outflow15, outflow2, Precision.EPSILON_3);

    assertEquals(900, outflow9,  Precision.EPSILON_3);
    assertEquals(outflow9 / 2, outflow10 , Precision.EPSILON_3);
    assertEquals(outflow10 / 2, outflow11,  Precision.EPSILON_3);
    assertEquals(outflow20, outflow9, Precision.EPSILON_3);
    assertEquals(outflow21, outflow10, Precision.EPSILON_3);
    assertEquals(outflow22, outflow11, Precision.EPSILON_3);
    assertEquals(outflow23, outflow11, Precision.EPSILON_3);
    assertEquals(outflow6, outflow20, Precision.EPSILON_3);
    assertEquals(outflow21 + outflow6, outflow7,  Precision.EPSILON_3);
    assertEquals(outflow22 + outflow7, outflow8,  Precision.EPSILON_3);
  }

}
