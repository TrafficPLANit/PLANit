package org.goplanit.mode;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.mode.PhysicalModeFeatures;
import org.goplanit.utils.mode.PredefinedMode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.mode.UsabilityModeFeatures;

/**
 * A mode that has predefined fixed values so it can easily be understood, or mapped
 * 
 * @author markr
 *
 */
public class PredefinedModeImpl extends ModeImpl implements PredefinedMode {

  /**
   * the predefined mode type of this mode
   */
  private final PredefinedModeType modeType;

  /**
   * A predefined mode
   * 
   * @param groupId           groupId for id generation
   * @param modeType          predefined mode type used
   * @param maxSpeed          the maximum speed for this predefined mode type
   * @param pcu               pcu value for this predefined mode
   * @param physicalFeatures  physical features of the mode
   * @param usabilityFeatures usabilitu features of the mode
   */
  protected PredefinedModeImpl(
          IdGroupingToken groupId,
          PredefinedModeType modeType,
          double maxSpeed,
          double pcu,
          PhysicalModeFeatures physicalFeatures,
          UsabilityModeFeatures usabilityFeatures) {
    super(groupId, modeType.value(), maxSpeed, pcu, (PhysicalModeFeaturesImpl) physicalFeatures, (UsabilityModeFeaturesImpl) usabilityFeatures);
    this.modeType = modeType;
    /* Xml id is always its predefined mode type value (name) */
    setXmlId(modeType.value());
  }

  /**
   * Copy constructor
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected PredefinedModeImpl(final PredefinedModeImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.modeType = other.modeType;
  }

  /**
   * Collect the predefined mode type of this mode
   * 
   * @return predefined mode type
   */
  @Override
  public PredefinedModeType getPredefinedModeType() {
    return modeType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString(){
    return modeType.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PredefinedModeImpl shallowClone() {
    return new PredefinedModeImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PredefinedModeImpl deepClone() {
    return new PredefinedModeImpl(this, true);
  }
}
