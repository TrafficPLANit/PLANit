package org.goplanit.test.zoning;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.output.enums.SkimSubOutputType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.unit.Unit;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.zonetozone.OdDemandMatrix;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;
import org.goplanit.zoning.zonetozone.TransferZoneSkimMatrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The unit a zone to zone matrix holds its values in: the units of a PLANit model unless another is given, one that
 * does not fit a skim's type refused, kept by a copy, and a value read in another unit converted
 *
 * @author markr
 */
public class ZoneToZoneMatrixUnitTest {

  /** zoning of zones A and B */
  private Zoning zoning;

  /** zone A */
  private OdZone a;

  /** zone B */
  private OdZone b;

  /** Zones A and B, before each test */
  @BeforeEach
  public void setUp() {
    var token = IdGroupingToken.collectGlobalToken();
    var layer = new MacroscopicNetwork(token).getTransportLayers().getFactory().registerNew();
    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
    a = zoning.getOdZones().getFactory().registerNew();
    b = zoning.getOdZones().getFactory().registerNew();
  }

  /** A distance skim is in km, a time and a cost skim in hours, and OD demand in pcu per hour */
  @Test
  public void testDefaultUnits() {
    var odZones = zoning.getOdZones();
    assertEquals(Unit.KM, new OdSkimMatrix(odZones, SkimSubOutputType.DISTANCE).getUnit());
    assertEquals(Unit.HOUR, new OdSkimMatrix(odZones, SkimSubOutputType.TRAVEL_TIME).getUnit());
    assertEquals(Unit.HOUR, new OdSkimMatrix(odZones, SkimSubOutputType.COST).getUnit());
    assertEquals(Unit.KM, new TransferZoneSkimMatrix(zoning.getTransferZones(), SkimSubOutputType.DISTANCE).getUnit());
    assertEquals(Unit.PCU_HOUR, new OdDemandMatrix(odZones).getUnit());
  }

  /**
   * A unit given is held: a distance skim in metres, a time skim in minutes, and person trips without a unit; a unit
   * that does not fit the skim's type, minutes for distance or km for time, is refused
   */
  @Test
  public void testUnitGiven() {
    var odZones = zoning.getOdZones();
    assertEquals(Unit.METER, new OdSkimMatrix(odZones, SkimSubOutputType.DISTANCE, Unit.METER).getUnit());
    assertEquals(Unit.MINUTE, new OdSkimMatrix(odZones, SkimSubOutputType.TRAVEL_TIME, Unit.MINUTE).getUnit());
    assertEquals(Unit.NONE, new OdDemandMatrix(odZones, Unit.NONE).getUnit());

    assertThrows(PlanItRunTimeException.class,
        () -> new OdSkimMatrix(odZones, SkimSubOutputType.DISTANCE, Unit.MINUTE));
    assertThrows(PlanItRunTimeException.class,
        () -> new OdSkimMatrix(odZones, SkimSubOutputType.TRAVEL_TIME, Unit.KM));
    assertThrows(PlanItRunTimeException.class,
        () -> new TransferZoneSkimMatrix(zoning.getTransferZones(), SkimSubOutputType.DISTANCE, Unit.HOUR));
  }

  /** A copy holds its values in the unit of the original */
  @Test
  public void testCopyKeepsTheUnit() {
    var odZones = zoning.getOdZones();
    assertEquals(Unit.METER, new OdSkimMatrix(odZones, SkimSubOutputType.DISTANCE, Unit.METER).shallowClone().getUnit());
    assertEquals(Unit.NONE, new OdDemandMatrix(odZones, Unit.NONE).shallowClone().getUnit());
  }

  /**
   * A distance skim of 3 km from A to B is in km, not in metres; read in metres it is 3,000, in km 3; read in hours, a
   * unit it cannot be converted to, it is refused
   */
  @Test
  public void testValueConvertedOnRead() {
    var skim = new OdSkimMatrix(zoning.getOdZones(), SkimSubOutputType.DISTANCE);
    skim.setValue(a, b, 3.0);
    assertTrue(skim.isInUnit(Unit.KM));
    assertFalse(skim.isInUnit(Unit.METER));
    assertEquals(3_000.0, skim.getValueConvertedTo(a, b, Unit.METER), 1e-3);
    assertEquals(3.0, skim.getValueConvertedTo(a, b, Unit.KM), 1e-6);
    assertThrows(PlanItRunTimeException.class, () -> skim.getValueConvertedTo(a, b, Unit.HOUR));
  }
}
