package org.planit.mode;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.PredefinedMode;
import org.planit.utils.mode.PredefinedModeType;
import org.planit.utils.mode.UsabilityModeFeatures;

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
   * @param pcu               pcu value for this predefined mode
   * @param physicalFeatures  physical features of the mode
   * @param usabilityFeatures usabilitu features of the mode
   */
  protected PredefinedModeImpl(IdGroupingToken groupId, PredefinedModeType modeType, double pcu, PhysicalModeFeatures physicalFeatures, UsabilityModeFeatures usabilityFeatures) {
    super(groupId, modeType.value(), pcu, physicalFeatures, usabilityFeatures);
    this.modeType = modeType;
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

}
