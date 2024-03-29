package org.goplanit.component;

import java.io.Serializable;
import java.util.Map;

import org.goplanit.component.event.PlanitComponentEvent;
import org.goplanit.component.event.PlanitComponentEventType;
import org.goplanit.component.event.PlanitComponentListener;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * PLANit components are the main building blocks to create PLANit applications with.
 * <p>
 * Each component is also a listener for PlanitComponentEvents, upon creation by the component factory each component is automatically registered for these events (at high
 * priority). This allows each component to initialise itself in a flexible way before any user or builder would make changes to its default configuration via the same or other
 * events. It is not mandatory to implement any actions upon receiving such events. Therefore this base class implements an empty notify method which may be overridden by derived
 * implementations if desired.
 *
 * @author markr
 *
 */
public abstract class PlanitComponent<T extends PlanitComponent<T> & Serializable> implements ExternalIdAble, PlanitComponentListener {

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
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected PlanitComponent(PlanitComponent<T> other, boolean deepCopy) {
    this.planitComponentType = other.planitComponentType;
    this.tokenId = other.tokenId;

    // clone because idImpl is not a member but is a composite and part of PlanitComponent itself
    this.idImpl = deepCopy ? other.idImpl.deepClone() : other.idImpl.shallowClone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract PlanitComponent<T> shallowClone();

  /**
   * deep clone on planit component should attempt to also update internal dependencies between deep cloned containers where possible
   */
  @Override
  public abstract PlanitComponent<T> deepClone();

  /**
   * All components should be able to reset going back to some representative initial state
   */
  public abstract void reset();

  /**
   * Each component may override this default which indicates it does not support any events, meaning that the component is not notified on any planitcomponent events. Derived
   * classes may override to register for specific events if needed
   * 
   * @return default supported event types (none)
   */
  @Override
  public PlanitComponentEventType[] getKnownSupportedEventTypes() {
    return new PlanitComponentEventType[] {};
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
   * Act upon the provided event. In this base class an empty implementation is provided indicating that it is not madnatory to act upon these events.
   * 
   * @param event the component is registered for
   */
  @Override
  public void onPlanitComponentEvent(PlanitComponentEvent event) throws PlanItException {
    // DO NOTHING IN BASE IMPLEMENTATION - OPTIONAL FOR DERIVED IMPLEMENTATIONS
  }

  /**
   * Provide all the settings of this particular component by name and value, where each value is a string representation of the underlying entity
   * 
   * @return name-value map of all (user configurable) settings
   */
  public abstract Map<String, String> collectSettingsAsKeyValueMap();

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
}
