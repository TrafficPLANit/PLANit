package org.goplanit.test.sltm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.ltm.sltm.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmTrafficAssignmentBuilder;
import org.goplanit.demands.Demands;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.od.demand.OdDemandMatrix;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypes;
import org.goplanit.utils.network.layer.physical.Links;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.Nodes;
import org.goplanit.utils.zoning.OdZones;
import org.goplanit.zoning.Zoning;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Test the sLTM network loading algorithms basic functionality (no route choice)
 * 
 * @author markr
 *
 */
public class sLtmNetworkLoadingTest {

  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("sLtmNetworkLoadingTest");

  /** the logger */
  private static Logger LOGGER = null;

  /**
   * {@inheritDoc}
   */
  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLtmNetworkLoadingTest.class);
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
      
      network = new MacroscopicNetwork(testToken);
      network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
      networkLayer = network.getTransportLayers().getFactory().registerNew(network.getModes().get(PredefinedModeType.CAR));
      int index=0;
      for(int nodeRowIndex = 0;nodeRowIndex<=1;++nodeRowIndex) {
        for(int nodeColIndex = 0;nodeColIndex<=3;++nodeColIndex,++index) {
          String xmlId = String.valueOf(index);
          Node node = networkLayer.getNodes().getFactory().registerNew();
          node.setXmlId(xmlId);
          // all nodes are spaced 1 km apart
          node.setPosition(geoFactory.createPoint(new Coordinate(nodeRowIndex*1000, nodeColIndex*1000)));
        }
      }
      
      Nodes nodes = networkLayer.getNodes();
      Links links = networkLayer.getLinks();
      //links
      links.getFactory().registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("1"), 1, true).setXmlId("0");;
      links.getFactory().registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("2"), 1, true).setXmlId("1");
      links.getFactory().registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("3"), 1, true).setXmlId("2");     
      links.getFactory().registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("6"), 1, true).setXmlId("3");
      links.getFactory().registerNew(nodes.getByXmlId("7"), nodes.getByXmlId("6"), 1, true).setXmlId("4");      
      links.getFactory().registerNew(nodes.getByXmlId("6"), nodes.getByXmlId("5"), 1, true).setXmlId("5");
      links.getFactory().registerNew(nodes.getByXmlId("5"), nodes.getByXmlId("4"), 1, true).setXmlId("6");         
      links.getFactory().registerNew(nodes.getByXmlId("5"), nodes.getByXmlId("1"), 1, true).setXmlId("7");
      
      
      MacroscopicLinkSegmentTypes linkTypes = networkLayer.getLinkSegmentTypes();
      linkTypes.getFactory().registerNew("MainType", 2000, 180, network.getModes().getFirst()).setXmlId("MainType");
      linkTypes.getFactory().registerNew("BottleNeckType", 250, 180, network.getModes().getFirst()).setXmlId("BottleNeckType");
      
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("0"), linkTypes.getByXmlId("MainType"), true, true);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("1"), linkTypes.getByXmlId("MainType"), true, true);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("2"), linkTypes.getByXmlId("BottleNeckType"), true, true);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("3"), linkTypes.getByXmlId("MainType"), true, true);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("4"), linkTypes.getByXmlId("MainType"), true, true);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("5"), linkTypes.getByXmlId("MainType"), true, true);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("6"), linkTypes.getByXmlId("BottleNeckType"), true, true);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("7"), linkTypes.getByXmlId("MainType"), true, true);        
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.odZones.getFactory().registerNew().setXmlId("A");
      zoning.odZones.getFactory().registerNew().setXmlId("B");
      zoning.odZones.getFactory().registerNew().setXmlId("C");
      zoning.odZones.getFactory().registerNew().setXmlId("D");
           
      zoning.odConnectoids.getFactory().registerNew(nodes.getByXmlId("7"),  zoning.getOdZones().getByXmlId("A"), 0);
      zoning.odConnectoids.getFactory().registerNew(nodes.getByXmlId("4"),  zoning.getOdZones().getByXmlId("B"), 0);
      zoning.odConnectoids.getFactory().registerNew(nodes.getByXmlId("0"),  zoning.getOdZones().getByXmlId("C"), 0);
      zoning.odConnectoids.getFactory().registerNew(nodes.getByXmlId("3"),  zoning.getOdZones().getByXmlId("D"), 0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      assertFalse(true);
    }
  }
  //@formatter:on

  /**
   * Test sLTM network loading on above network using a path absed approach
   */
  @Test
  public void sLtmPointQueueNetworkLoadingPathTest() {
    try {

      Demands demands = new Demands(testToken);
      demands.timePeriods.createAndRegisterNewTimePeriod("dummyTimePeriod", 0, 3600);
      demands.travelerTypes.createAndRegisterNew("dummyTravellerType");
      demands.userClasses.createAndRegisterNewUserClass("dummyUser", network.getModes().get(PredefinedModeType.CAR), demands.travelerTypes.getFirst());

      /* OD DEMANDS 1000 A->C, 1000 C->B */
      OdZones odZones = zoning.getOdZones();
      OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
      odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("D"), 1000.0);
      odDemands.setValue(odZones.getByXmlId("C"), odZones.getByXmlId("B"), 1000.0);
      demands.registerOdDemandPcuHour(demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateDetailedLogging(true);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateBushBased(false);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).getGapFunction().getStopCriterion().setMaxIterations(1);

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.execute();

      // TODO: path based sLTM assignment does not work yet, this is why gap computation gives severe warning. Fix this

      // TODO: add assertions to check validity of outcome explicitly

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM network loading");
    }
  }

}
