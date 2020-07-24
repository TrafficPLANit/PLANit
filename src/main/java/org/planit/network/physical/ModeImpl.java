package org.planit.network.physical;

import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.Mode;

/**
 * A Mode is a user class feature representing a single form of transport (car,
 * truck etc.).
 * 
 * @author markr
 */
public class ModeImpl implements Mode {

  private final long DEFAULT_EXTERNAL_ID = 1;

  // Protected

  /**
   * Each mode has a passenger car unit number indicating how many standard
   * passenger cars a single unit of this mode represents
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
   * @param parent of this mode
   * @param name the name of this mode
   * @param pcu the PCU value of this mode
   */

  public ModeImpl(Object parent, String name, double pcu) {
    this.id = IdGenerator.generateId(parent, Mode.class);
    this.externalId = DEFAULT_EXTERNAL_ID;
    this.name = name;
    this.pcu = pcu;
  }

  /**
   * Constructor
   * 
   * @param parent of this mode 
   * @param externalId the externalId of this mode
   * @param name the name of this mode
   * @param pcu the PCU value of this mode
   */
  public ModeImpl(Object parent, Object externalId, String name, double pcu) {
    this.id = IdGenerator.generateId(parent, ModeImpl.class);
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
