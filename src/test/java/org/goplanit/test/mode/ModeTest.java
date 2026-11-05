package org.goplanit.test.mode;

import org.goplanit.mode.ModeFeaturesFactory;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.DriverType;
import org.goplanit.utils.mode.MotorisationModeType;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.mode.TrackModeType;
import org.goplanit.utils.mode.UseOfModeType;
import org.goplanit.utils.mode.VehicularModeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests checking a mode against a predefined mode type, and whether one of the people in it drives it
 *
 * @author markr
 */
public class ModeTest {

  private MacroscopicNetwork network;

  @BeforeEach
  public void setUp() {
    IdGenerator.reset();
    network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
  }

  @AfterEach
  public void tearDown() {
    IdGenerator.reset();
  }

  /**
   * A predefined mode is its own predefined mode type and no other
   */
  @Test
  public void isPredefinedModeTypeMatchesOnlyItsOwnType() {
    var bicycle = network.getModes().getFactory().registerNew(PredefinedModeType.BICYCLE);

    assertTrue(bicycle.isPredefinedModeType(PredefinedModeType.BICYCLE));
    assertFalse(bicycle.isPredefinedModeType(PredefinedModeType.CAR));
  }

  /**
   * A custom mode is no predefined mode type, also not the custom one
   */
  @Test
  public void customModeIsNoPredefinedModeType() {
    var scooter = network.getModes().getFactory().registerNewCustomMode("scooter", 25, 0.5, null, null);

    assertFalse(scooter.isPredefinedModeType(PredefinedModeType.CUSTOM));
    assertFalse(scooter.isPredefinedModeType(PredefinedModeType.BICYCLE));
  }

  /**
   * A predefined mode's usability features say who drives it as its type does, and it is motorised and driven by one
   * of the people travelling in it exactly when it is a car, car share, car high occupancy, motorbike or goods vehicle,
   * not a bicycle, which is not motorised
   */
  @Test
  public void predefinedModeTakesItsDriverTypeFromItsType() {
    var selfDrivenMotorised = EnumSet.of(PredefinedModeType.CAR, PredefinedModeType.CAR_SHARE,
        PredefinedModeType.CAR_HIGH_OCCUPANCY, PredefinedModeType.MOTOR_BIKE, PredefinedModeType.GOODS_VEHICLE,
        PredefinedModeType.HEAVY_GOODS_VEHICLE, PredefinedModeType.LARGE_HEAVY_GOODS_VEHICLE);
    for (var modeType : PredefinedModeType.getPredefinedModeTypesWithout(PredefinedModeType.CUSTOM)) {
      var mode = network.getModes().getFactory().registerNew(modeType);
      assertEquals(modeType.getDriverType(), mode.getUseFeatures().getDriverType(), modeType.name());
      assertEquals(selfDrivenMotorised.contains(modeType), mode.isSelfDrivenMotorisedMode(), modeType.name());
    }
  }

  /**
   * A custom mode is motorised and driven by one of the people travelling in it only when its features say both: not
   * when someone else or nobody drives it, as in an automated private car, not when it is not motorised, and not when
   * who drives it is not known, whatever its use
   */
  @Test
  public void customModeIsSelfDrivenMotorisedWhenItsFeaturesSaySo() {
    var factory = network.getModes().getFactory();
    var motorisedRoad = ModeFeaturesFactory.createPhysicalFeatures(
        VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.ROAD);

    var van = factory.registerNewCustomMode("van", 100, 1.5, motorisedRoad,
        ModeFeaturesFactory.createUsabilityFeatures(UseOfModeType.PRIVATE, DriverType.TRAVELLER));
    var automatedCar = factory.registerNewCustomMode("automated car", 100, 1, motorisedRoad,
        ModeFeaturesFactory.createUsabilityFeatures(UseOfModeType.PRIVATE, DriverType.NONE));
    var shuttle = factory.registerNewCustomMode("shuttle", 80, 2, motorisedRoad,
        ModeFeaturesFactory.createUsabilityFeatures(UseOfModeType.PUBLIC, DriverType.OPERATOR));
    var carDriverNotKnown = factory.registerNewCustomMode("car, driver not known", 100, 1, motorisedRoad,
        ModeFeaturesFactory.createUsabilityFeatures(UseOfModeType.PRIVATE));
    var cargoBike = factory.registerNewCustomMode("cargo bike", 20, 0.5, ModeFeaturesFactory.createPhysicalFeatures(
            VehicularModeType.VEHICLE, MotorisationModeType.NON_MOTORISED, TrackModeType.ROAD),
        ModeFeaturesFactory.createUsabilityFeatures(UseOfModeType.PRIVATE, DriverType.TRAVELLER));
    var scooter = factory.registerNewCustomMode("scooter", 25, 0.5, null, null);

    assertTrue(van.isSelfDrivenMotorisedMode());
    assertFalse(automatedCar.isSelfDrivenMotorisedMode());
    assertFalse(shuttle.isSelfDrivenMotorisedMode());
    assertFalse(carDriverNotKnown.isSelfDrivenMotorisedMode());
    assertFalse(cargoBike.isSelfDrivenMotorisedMode());
    assertFalse(scooter.isSelfDrivenMotorisedMode());
  }
}
