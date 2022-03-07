package org.goplanit.test.sltm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.ltm.sltm.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmTrafficAssignmentBuilder;
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
import org.goplanit.utils.zoning.OdZones;
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
public class sLtmAssignmentGridTest {

  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("sLtmAssignmentGridTest");

  /** the logger */
  private static Logger LOGGER = null;

  private static double MAX_SPEED_KM_H = 60.0;

  /**
   * Create demands an populate with OD DEMANDS 2000 A->A`, 2000 A->A``, and, 2000 A```->A`, 2000 A```->A``
   * 
   * @return created demands
   * @throws PlanItException thrown if error
   */
  private Demands createDemands() throws PlanItException {
    Demands demands = new Demands(testToken);
    demands.timePeriods.createAndRegisterNewTimePeriod("dummyTimePeriod", 0, 3600);
    demands.travelerTypes.createAndRegisterNew("dummyTravellerType");
    demands.userClasses.createAndRegisterNewUserClass("dummyUser", network.getModes().get(PredefinedModeType.CAR), demands.travelerTypes.getFirst());
    return demands;
  }

  /**
   * {@inheritDoc}
   */
  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLtmAssignmentGridTest.class);
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
    // The network is a 4X4 grid. All link shave 1800 capacity per lane, for a single lane
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
      networkLayer.getLinkSegmentTypes().forEach( ls -> ls.getAccessProperties(network.getModes().getFirst()).setMaximumSpeedKmH(MAX_SPEED_KM_H /* km/h */));           
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.odZones.getFactory().registerNew().setXmlId("A");
      zoning.odZones.getFactory().registerNew().setXmlId("A`");
      zoning.odZones.getFactory().registerNew().setXmlId("A``");
      zoning.odZones.getFactory().registerNew().setXmlId("A```");
           
      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().get(0),  zoning.getOdZones().getByXmlId("A"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().get(12),  zoning.getOdZones().getByXmlId("A`"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().get(7),  zoning.getOdZones().getByXmlId("A``"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(networkLayer.getNodes().get(11),  zoning.getOdZones().getByXmlId("A```"), 0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      assertFalse(true);
    }
  }
  //@formatter:on

  /**
   * Test sLTM bush-based assignment on grid based network which should result in an even spread across uncongested links in the final solution
   */
  @Test
  public void sLtmPointQueueBushBasedAssignmentNoQueueTest() {
    try {

      Demands demands = createDemands();

      /* OD DEMANDS 1800 A->A``, 1800 A->A``` */
      OdZones odZones = zoning.getOdZones();
      OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
      odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A``"), 1800.0);
      odDemands.setValue(odZones.getByXmlId("A`"), odZones.getByXmlId("A```"), 1800.0);
      demands.registerOdDemandPcuHour(demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateDetailedLogging(false);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateBushBased(true);

      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateOutput(OutputType.LINK);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(1000);
      // sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

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

      assertEquals(outflow0, 900, Precision.EPSILON_3);
      assertEquals(outflow1, outflow0 / 2, Precision.EPSILON_3);
      assertEquals(outflow2, outflow1 / 2, Precision.EPSILON_3);
      assertEquals(outflow3, outflow12, Precision.EPSILON_3);
      assertEquals(outflow4, outflow13 + outflow3, Precision.EPSILON_3);
      assertEquals(outflow5, outflow14 + outflow4, Precision.EPSILON_3);
      assertEquals(outflow12, outflow0, Precision.EPSILON_3);
      assertEquals(outflow13, outflow1, Precision.EPSILON_3);
      assertEquals(outflow14, outflow2, Precision.EPSILON_3);
      assertEquals(outflow15, outflow2, Precision.EPSILON_3);

      assertEquals(outflow9, 900, Precision.EPSILON_3);
      assertEquals(outflow10, outflow9 / 2, Precision.EPSILON_3);
      assertEquals(outflow11, outflow10 / 2, Precision.EPSILON_3);
      assertEquals(outflow20, outflow9, Precision.EPSILON_3);
      assertEquals(outflow21, outflow10, Precision.EPSILON_3);
      assertEquals(outflow22, outflow11, Precision.EPSILON_3);
      assertEquals(outflow23, outflow11, Precision.EPSILON_3);
      assertEquals(outflow6, outflow20, Precision.EPSILON_3);
      assertEquals(outflow7, outflow21 + outflow6, Precision.EPSILON_3);
      assertEquals(outflow8, outflow22 + outflow7, Precision.EPSILON_3);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment");
    }
  }

  /**
   * Test sLTM bush-based assignment on grid based network with demand causing some queues
   */
  @Test
  public void sLtmPointQueueBushBasedAssignmentWithQueueTest() {
    try {

      Demands demands = createDemands();

      /* OD DEMANDS 3600 A->A``, 3600 A->A``` */
      OdZones odZones = zoning.getOdZones();
      OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
      odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A``"), 3600.0);
      odDemands.setValue(odZones.getByXmlId("A`"), odZones.getByXmlId("A```"), 3600.0);
      demands.registerOdDemandPcuHour(demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateDetailedLogging(false);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateBushBased(true);

      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateOutput(OutputType.LINK);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(1000);
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

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

      assertEquals(outflow0, 1800, Precision.EPSILON_3);
      assertEquals(outflow1, outflow0 / 2, Precision.EPSILON_3);
      assertEquals(outflow2, outflow1 / 2, Precision.EPSILON_3);
      assertEquals(outflow3, outflow12, Precision.EPSILON_3);
      assertEquals(outflow4, outflow13 + outflow3, Precision.EPSILON_3);
      assertEquals(outflow5, outflow14 + outflow4, Precision.EPSILON_3);
      assertEquals(outflow12, outflow0, Precision.EPSILON_3);
      assertEquals(outflow13, outflow1, Precision.EPSILON_3);
      assertEquals(outflow14, outflow2, Precision.EPSILON_3);
      assertEquals(outflow15, outflow2, Precision.EPSILON_3);

      assertEquals(outflow9, 1800, Precision.EPSILON_3);
      assertEquals(outflow10, outflow9 / 2, Precision.EPSILON_3);
      assertEquals(outflow11, outflow10 / 2, Precision.EPSILON_3);
      assertEquals(outflow20, outflow9, Precision.EPSILON_3);
      assertEquals(outflow21, outflow10, Precision.EPSILON_3);
      assertEquals(outflow22, outflow11, Precision.EPSILON_3);
      assertEquals(outflow23, outflow11, Precision.EPSILON_3);
      assertEquals(outflow6, outflow20, Precision.EPSILON_3);
      assertEquals(outflow7, outflow21 + outflow6, Precision.EPSILON_3);
      assertEquals(outflow8, outflow22 + outflow7, Precision.EPSILON_3);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment");
    }
  }

}
