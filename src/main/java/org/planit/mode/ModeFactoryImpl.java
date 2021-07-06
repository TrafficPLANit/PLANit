package org.planit.mode;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedId;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.ModeFactory;
import org.planit.utils.mode.Modes;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.PredefinedMode;
import org.planit.utils.mode.PredefinedModeType;
import org.planit.utils.mode.UsabilityModeFeatures;

/**
 * Factory for creating modes on modes container
 * 
 * @author markr
 */
public class ModeFactoryImpl extends ManagedIdEntityFactoryImpl<Mode> implements ModeFactory {

  /** modes container to use */
  protected final Modes modes;

  /**
   * Constructor
   * 
   * @param groupId to use
   * @param edges   to use
   */
  protected ModeFactoryImpl(final IdGroupingToken groupId, final Modes modes) {
    super(groupId);
    this.modes = modes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PredefinedMode createPredefinedMode(IdGroupingToken groupId, final PredefinedModeType modeType) throws PlanItException {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Mode registerUniqueCopyOf(ManagedId mode) {
    Mode copy = createUniqueCopyOf(mode);
    modes.register(copy);
    return copy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mode registerNewCustomMode(String name, double maxSpeed, double pcu, PhysicalModeFeatures physicalFeatures, UsabilityModeFeatures usabilityFeatures) {
    final Mode newMode = new ModeImpl(getIdGroupingToken(), name, maxSpeed, pcu, physicalFeatures, usabilityFeatures);
    modes.register(newMode);
    return newMode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PredefinedMode registerNew(PredefinedModeType modeType) throws PlanItException {
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
