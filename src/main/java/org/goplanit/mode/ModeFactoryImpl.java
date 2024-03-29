package org.goplanit.mode;

import java.util.logging.Logger;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.ModeFactory;
import org.goplanit.utils.mode.Modes;
import org.goplanit.utils.mode.PhysicalModeFeatures;
import org.goplanit.utils.mode.PredefinedMode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.mode.UsabilityModeFeatures;

/**
 * Factory for creating modes on modes container
 * 
 * @author markr
 */
public class ModeFactoryImpl extends ManagedIdEntityFactoryImpl<Mode> implements ModeFactory {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ModeFactoryImpl.class.getCanonicalName());

  /** modes container to use */
  protected final Modes modes;

  /**
   * Constructor
   * 
   * @param groupId to use
   * @param modes   to use
   */
  protected ModeFactoryImpl(final IdGroupingToken groupId, final Modes modes) {
    super(groupId);
    this.modes = modes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PredefinedMode createPredefinedMode(IdGroupingToken groupId, final PredefinedModeType modeType) {
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
    case FERRY:
      return new FerryMode(groupId);
    default:
      LOGGER.severe(String.format("Mode type %s unknown, mode not created", modeType));
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mode registerNewCustomMode(String name, double maxSpeed, double pcu, PhysicalModeFeatures physicalFeatures, UsabilityModeFeatures usabilityFeatures) {
    final Mode newMode = new ModeImpl(
            getIdGroupingToken(), name, maxSpeed, pcu, (PhysicalModeFeaturesImpl) physicalFeatures, (UsabilityModeFeaturesImpl) usabilityFeatures);
    modes.register(newMode);
    return newMode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PredefinedMode registerNew(PredefinedModeType modeType) {
    PredefinedMode theMode = null;
    if (!modes.containsPredefinedMode(modeType)) {
      theMode = createPredefinedMode(getIdGroupingToken(), modeType);
      modes.register(theMode);
    } else {
      theMode = modes.get(modeType);
    }
    return theMode;
  }

}
