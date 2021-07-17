package org.planit.component;

import java.io.Serializable;

import org.planit.utils.id.ExternalIdAble;
import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * PLANit components are the main building blocks to create PLANit applications with.
 *
 * @author markr
 *
 */
public abstract class PlanitComponent<T extends PlanitComponent<T> & Serializable> implements ExternalIdAble {

  /** store id information */
  private final ExternalIdAbleImpl idImpl;

  /**
   * id generation using this token will be contiguous and unique for each instance of this class
   */
  private IdGroupingToken tokenId;

  /**
   * Traffic component type used to identify the component uniquely. If not provided to the constructor the class name is used
   */
  private final String planitComponentType;

  /**
   * Constructor
   * 
   * @param tokenId,   contiguous id generation using this same token for instances of this class
   * @param classType, the class type this instance belongs to and we are generating an id for
   */
  protected PlanitComponent(IdGroupingToken tokenId, Class<?> classType) {
    // actual instance class
    this.planitComponentType = this.getClass().getCanonicalName();
    // the groupId would generally be the token of the project or the assignment as it owns the components
    // the class type would be the super class of the all instances for which we want contiguous ids
    this.tokenId = tokenId;
    this.idImpl = new ExternalIdAbleImpl(IdGenerator.generateId(tokenId, classType));
  }

  /**
   * Copy constructor
   * 
   * @param other, to copy
   */
  protected PlanitComponent(PlanitComponent<T> other) {
    this.planitComponentType = other.planitComponentType;
    this.tokenId = other.tokenId;
    this.idImpl = other.idImpl.clone();
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
  public boolean equals(Object obj) {
    return idEquals(obj);
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public long getId() {
    return idImpl.getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getExternalId() {
    return idImpl.getExternalId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setExternalId(String externalId) {
    idImpl.setExternalId(externalId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getXmlId() {
    return idImpl.getXmlId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setXmlId(String xmlId) {
    idImpl.setXmlId(xmlId);
  }

  // Public

  /**
   * Collect the component type of this instance
   * 
   * @return PLANit component type
   */
  public String getComponentType() {
    return planitComponentType;
  }

  /**
   * Collect the id grouping token used to generate ids for entities of this class.
   * 
   * @return id grouping token
   */
  public IdGroupingToken getIdGroupingToken() {
    return tokenId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract PlanitComponent<T> clone();
}
