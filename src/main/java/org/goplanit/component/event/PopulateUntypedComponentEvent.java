package org.goplanit.component.event;

import org.goplanit.component.PlanitComponent;
import org.goplanit.component.PlanitComponentFactory;
import org.goplanit.utils.event.EventImpl;

/**
 * A Populate component event is fired when PLANit requests for a registered listener to populate the provided PLANit component instance. It is assumed only a single listener will
 * populate this component and it is expected that the regisration of this listener is handled by the platform rather than the user. The end user will receive this event when
 * implementing an input builder and registering this builder on a PLANit project for example.
 * 
 * @author markr
 *
 */
public abstract class PopulateUntypedComponentEvent extends EventImpl implements PlanitComponentEvent {

  /**
   * collect PLANit component to populate
   * 
   * @return component to break
   */
  protected PlanitComponent<?> getComponentToPopulate() {
    return (PlanitComponent<?>) ((Object[]) getContent())[0];
  }

  /**
   * Collect additional content provided to be able to populate the component
   * 
   * @return additional content
   */
  protected Object[] getAdditionalContent() {
    return (Object[]) ((Object[]) getContent())[1];
  }

  /**
   * Verify if additional content is present or not
   *
   * @return true when present, false when additional content is null
   */
  protected boolean hasAdditionalContent() {
    return getAdditionalContent() != null;
  }

  /**
   * Constructor
   *
   * @param type                of the populate component event
   * @param source              of the event
   * @param componentToPopulate to populate
   */
  protected PopulateUntypedComponentEvent(final PlanitComponentEventType type, final PlanitComponentFactory<?> source, PlanitComponent<?> componentToPopulate) {
    this(type, source, componentToPopulate, null);
  }

  /**
   * Constructor
   *
   * @param type                of the populate component event
   * @param source              of the event
   * @param componentToPopulate to populate
   * @param additionalContent   for this component to be able to populate
   */
  protected PopulateUntypedComponentEvent(final PlanitComponentEventType type, final PlanitComponentFactory<?> source, PlanitComponent<?> componentToPopulate,
      Object additionalContent) {
    this(type, source, componentToPopulate, new Object[] { additionalContent });
  }

  /**
   * Constructor
   *
   * @param type                of the populate component event
   * @param source              of the event
   * @param componentToPopulate to populate
   * @param additionalContent   for this component to be able to populate
   */
  protected PopulateUntypedComponentEvent(final PlanitComponentEventType type, final PlanitComponentFactory<?> source, PlanitComponent<?> componentToPopulate,
      Object[] additionalContent) {
    super(type, source, new Object[] { componentToPopulate, additionalContent });
  }

}
