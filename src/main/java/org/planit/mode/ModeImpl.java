package org.planit.mode;

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
public class ModeImpl implements Mode {

  public final static Long DEFAULT_EXTERNAL_ID = Long.valueOf(1);

  /**
   * Id value of this mode
   */
  private final long id;

  /**
   * External Id of this mode
   */
  private String externalId;

  /**
   * xml Id of this mode
   */
  private String xmlId;

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
  protected ModeImpl(final IdGroupingToken groupId, final String name, final double maxSpeed, final double pcu, final PhysicalModeFeatures physicalFeatures,
      final UsabilityModeFeatures usabilityFeatures) {
    this.id = IdGenerator.generateId(groupId, Mode.class);
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
  public long getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getExternalId() {
    return externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getXmlId() {
    return xmlId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setXmlId(String xmlId) {
    this.xmlId = xmlId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasExternalId() {
    return (externalId != null);
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
  public int hashCode() {
    return idHashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    return idEquals(o);
  }

}
