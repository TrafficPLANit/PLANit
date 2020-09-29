package org.planit.mode;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.PredefinedMode;
import org.planit.utils.mode.PredefinedModeType;

/**
 * factory class to instantiate different (pre-specified) mode types. You can of course create your own modes. However, using the pre-specified modes makes it easier to interpret
 * and exchange projects/applications using these modes.
 * 
 * @author markr
 *
 */
public class ModeFactory {

  /**
   * create a predefined mode instance
   * 
   * @param groupId  the is generation token
   * @param modeType predefined mode type to create
   * 
   * @return predefined mode instance
   * @throws PlanItException thrown if error
   */
  public static PredefinedMode createPredefinedMode(IdGroupingToken groupId, final PredefinedModeType modeType) throws PlanItException {
    switch (modeType) {
    case BICYCLE:
      return new BicycleMode(groupId);
    case BUS:
      return new BusMode(groupId);
    case CAR:
      return new CarMode(groupId);
    case CAR_SHARE:
      return new CarShareMode(groupId);
    case CAR_HIGH_OCCUPANCY:
      return new CarHighOccupancyMode(groupId);
    case GOODS_VEHICLE:
      return new GoodsMode(groupId);
    case HEAVY_GOODS_VEHICLE:
      return new HeavyGoodsMode(groupId);
    case LARGE_HEAVY_GOODS_VEHICLE:
      return new LargeHeavyGoodsMode(groupId);
    case LIGHTRAIL:
      return new LightRailMode(groupId);
    case MOTOR_BIKE:
      return new MotorBikeMode(groupId);
    case PEDESTRIAN:
      return new PedestrianMode(groupId);
    case SUBWAY:
      return new SubwayMode(groupId);
    case TRAIN:
      return new TrainMode(groupId);
    case TRAM:
      return new TramMode(groupId);
    default:
      throw new PlanItException(String.format("mode type %s unknown", modeType));
    }
  }

}
