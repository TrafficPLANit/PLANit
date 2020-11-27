package org.planit.assignment;

import java.io.Serializable;

import org.djutils.event.EventProducer;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.Idable;

/**
 * Traffic assignment components are the main building blocks to conduct traffic assignment on
 *
 * @author markr
 *
 */
public abstract class TrafficAssignmentComponent<T extends TrafficAssignmentComponent<T> & Serializable> extends EventProducer implements Idable {

  /** generated UID */
  private static final long serialVersionUID = -3940841069228367177L;

  /**
   * unique identifier for this traffic component
   */
  protected final long id;

  /**
   * id generation using this token will be contiguous and unique for each instance of this class
   */
  protected IdGroupingToken groupId;

  /**
   * Traffic component type used to identify the component uniquely. If not provided to the constructor the class name is used
   */
  protected final String trafficComponentType;

  /**
   * Constructor
   * 
   * @param groupId,   contiguous id generation within this group for instances of this class
   * @param classType, the class type this instance belongs to and we are generating an id for
   */
  protected TrafficAssignmentComponent(IdGroupingToken groupId, Class<?> classType) {
    // actual instance class
    this.trafficComponentType = this.getClass().getCanonicalName();
    // the groupId would generally be the token of the project or the assignment as it owns the components
    // the class type would be the super class of the all instances for which we want contiguous ids
    this.groupId = groupId;
    this.id = IdGenerator.generateId(groupId, classType);
  }

  // Public

  public String getTrafficComponentType() {
    return trafficComponentType;
  }

  /**
   * All traffic components must have a unique id
   * 
   * @return id of traffic assignment component
   */
  @Override
  public long getId() {
    return id;
  }

  /**
   * Collect the id grouping token used to generate ids for entities of this class.
   * 
   * @return id grouping token
   */
  public IdGroupingToken getIdGroupingtoken() {
    return groupId;
  }

  /**
   * the source id whenever this instance fires an event is simply this
   * 
   * @return this instance as source id
   */
  @Override
  public Serializable getSourceId() {
    return this;
  }

}
