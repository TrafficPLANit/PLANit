package org.planit.mode;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;

/**
 * A Mode is a user class feature representing a single form of transport (car, truck etc.).
 * 
 * @author markr
 */
public class ModeImpl implements Mode {

  private final long DEFAULT_EXTERNAL_ID = 1;

  // Protected

  /**
   * Each mode has a passenger car unit number indicating how many standard passenger cars a single unit of this mode represents
   */
  private final double pcu;

  /**
   * Id value of this mode
   */
  private final long id;

  /**
   * External Id of this mode
   */
  private Object externalId;

  /**
   * Name of this mode
   */
  private final String name;

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param name     the name of this mode
   * @param pcu      the PCU value of this mode
   */

  protected ModeImpl(final IdGroupingToken groupId, final String name, final double pcu) {
    this.id = IdGenerator.generateId(groupId, Mode.class);
    this.externalId = DEFAULT_EXTERNAL_ID;
    this.name = name;
    this.pcu = pcu;
  }

  /**
   * Constructor
   * 
   * @param groupId,   contiguous id generation within this group for instances of this class
   * @param externalId the externalId of this mode
   * @param name       the name of this mode
   * @param pcu        the PCU value of this mode
   */
  protected ModeImpl(final IdGroupingToken groupId, final Object externalId, final String name, final double pcu) {
    this.id = IdGenerator.generateId(groupId, ModeImpl.class);
    this.externalId = externalId;
    this.name = name;
    this.pcu = pcu;
  }

  // getters-setters

  @Override
  public double getPcu() {
    return pcu;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public Object getExternalId() {
    return externalId;
  }

  @Override
  public boolean hasExternalId() {
    return (externalId != null);
  }

  /**
   * Compare based on id
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(Mode o) {
    return (int) (this.getId() - o.getId());
  }

}
