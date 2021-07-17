package org.planit.component.event;

import org.planit.component.PlanitComponent;
import org.planit.component.PlanitComponentFactory;

/**
 * A Populate component event for components for which no dedicated derived event is created and provides access to the component to populate and additional content as is without
 * additional type or contextual information.
 * 
 * @author markr
 *
 */
public class PopulateComponentEvent extends PopulateUntypedComponentEvent {

  /** event type fired off when edge has been broken */
  public static final PlanitComponentEventType EVENT_TYPE = new PlanitComponentEventType("PLANITCOMPONENT.GENERIC.POPULATE");

  /**
   * Constructor
   *
   * @param type                of the populate component event
   * @param source              of the event
   * @param componentToPopulate to populate
   * @param additionalContent   for this component to be able to populate
   */
  public PopulateComponentEvent(final PlanitComponentFactory<?> source, PlanitComponent<?> componentToPopulate, Object[] additionalContent) {
    super(EVENT_TYPE, source, componentToPopulate, additionalContent);
  }

  /**
   * collect PLANit component to populate
   * 
   * @return component to break
   */
  public PlanitComponent<?> getComponentToPopulate() {
    return super.getComponentToPopulate();
  }

  /**
   * Collect additional content provided to be able to populate the component
   * 
   * @return additional content
   */
  public Object[] getAdditionalContent() {
    return super.getAdditionalContent();
  }
}
