package org.planit.mode;

import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.UsabilityModeFeatures;

/**
 * A Mode is a user class feature representing a single form of transport (car, truck etc.).
 * 
 * @author markr
 */
public class ModeImpl extends ExternalIdAbleImpl implements Mode {

  public final static Long DEFAULT_EXTERNAL_ID = Long.valueOf(1);

  /**
   * Each mode has a maximum speed indicating the maximum speed this mode can take on in the context of the network. Typically this would be chosen as the maximum speed limit
   * encountered for this mode across all road segments in the network.
   */
  private final double maxSpeed;

  /**
   * Each mode has a passenger car unit number indicating how many standard passenger cars a single unit of this mode represents
   */
  private final double pcu;

  /**
   * Name of this mode
   */
  private final String name;

  /** the physical features of this mode */
  private PhysicalModeFeatures physicalFeatures;

  /** the usability features of this mode */
  private UsabilityModeFeatures usedToFeatures;

  /**
   * Generate id for this instance
   * 
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(final IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, Mode.MODE_ID_CLASS);
  }

  /**
   * Constructor, using all defaults for non-provided parameters
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param name     the name of this mode
   * @param maxSpeed this mode takes on
   * @param pcu      the PCU value of this mode
   */
  protected ModeImpl(final IdGroupingToken groupId, final double maxSpeed, final String name, final double pcu) {
    this(groupId, name, maxSpeed, pcu, new PhysicalModeFeaturesImpl(), new UsabilityModeFeaturesImpl());
  }

  /**
   * Constructor, using all defaults for non-provided parameters
   * 
   * @param groupId,          contiguous id generation within this group for instances of this class
   * @param name              the name of this mode
   * @param maxSpeed          this mode takes on
   * @param pcu               the PCU value of this mode
   * @param physicalFeatures  physical features of the mode
   * @param usabilityFeatures usability features of the mode
   */
  protected ModeImpl(final IdGroupingToken tokenId, final String name, final double maxSpeed, final double pcu, final PhysicalModeFeatures physicalFeatures,
      final UsabilityModeFeatures usabilityFeatures) {
    super(generateId(tokenId));
    this.name = name;
    this.maxSpeed = maxSpeed;
    this.pcu = pcu;
    this.physicalFeatures = physicalFeatures;
    this.usedToFeatures = usabilityFeatures;
  }

  // getters-setters

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getMaximumSpeedKmH() {
    return maxSpeed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getPcu() {
    return pcu;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final PhysicalModeFeatures getPhysicalFeatures() {
    return physicalFeatures;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final UsabilityModeFeatures getUseFeatures() {
    return usedToFeatures;
  }

}
