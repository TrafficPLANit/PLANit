package org.planit.trafficassignment;

import java.io.Serializable;

import org.djutils.event.EventProducer;

/**
 * Traffic assignment components are the main building blocks to conduct traffic
 * assignment on
 *
 * @author markr
 *
 */
public abstract class TrafficAssignmentComponent<T extends TrafficAssignmentComponent<T> & Serializable> extends
    EventProducer {

  /** generated UID */
  private static final long serialVersionUID = -3940841069228367177L;

  /**
   * Traffic component type used to identify the component uniquely. If not
   * provided to the constructor the class name is used
   */
  protected final String trafficComponentType;

  /**
   * Constructor
   */
  protected TrafficAssignmentComponent() {
    this.trafficComponentType = this.getClass().getCanonicalName();
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
  public abstract long getId();

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
