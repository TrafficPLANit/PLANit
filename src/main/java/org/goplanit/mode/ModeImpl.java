package org.goplanit.mode;

import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PhysicalModeFeatures;
import org.goplanit.utils.mode.UsabilityModeFeatures;

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
  private PhysicalModeFeaturesImpl physicalFeatures;

  /** the usability features of this mode */
  private UsabilityModeFeaturesImpl usedToFeatures;

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
   * @param groupId  contiguous id generation within this group for instances of this class
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
   * @param tokenId           contiguous id generation within this group for instances of this class
   * @param name              the name of this mode
   * @param maxSpeed          this mode takes on
   * @param pcu               the PCU value of this mode
   * @param physicalFeatures  physical features of the mode
   * @param usabilityFeatures usability features of the mode
   */
  protected ModeImpl(final IdGroupingToken tokenId, final String name, final double maxSpeed, final double pcu, final PhysicalModeFeaturesImpl physicalFeatures,
      final UsabilityModeFeaturesImpl usabilityFeatures) {
    super(generateId(tokenId));
    this.name = name;
    this.maxSpeed = maxSpeed;
    this.pcu = pcu;
    this.physicalFeatures = physicalFeatures;
    this.usedToFeatures = usabilityFeatures;
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ModeImpl(final ModeImpl other, boolean deepCopy) {
    super(other);
    this.name = other.name;
    this.maxSpeed = other.maxSpeed;
    this.pcu = other.pcu;
    this.physicalFeatures = deepCopy ? new PhysicalModeFeaturesImpl(other.physicalFeatures) : other.physicalFeatures;
    this.usedToFeatures   = deepCopy ? new UsabilityModeFeaturesImpl(other.usedToFeatures)  : other.usedToFeatures;
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

  /**
   * {@inheritDoc}
   */
  @Override
  public ModeImpl shallowClone() {
    return new ModeImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModeImpl deepClone() {
    return new ModeImpl(this, true);
  }

}
