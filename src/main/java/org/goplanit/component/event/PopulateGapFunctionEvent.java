package org.goplanit.component.event;

import org.goplanit.component.PlanitComponentFactory;
import org.goplanit.gap.GapFunction;

/**
 * A Populate gap function event is fired when PLANit requests for a registered listener to populate the newly created gap function instance. It is assumed only a single listener
 * will populate this component and it is expected that the registration of this listener is handled by the platform rather than the user. The end user will - via the listener -
 * receive this event when implementing an input builder and registering this builder on a PLANit project for example.
 * 
 * @author markr
 *
 */
public class PopulateGapFunctionEvent extends PopulateUntypedComponentEvent {

  /** event type fired off when gap function needs to be populated */
  public static final PlanitComponentEventType EVENT_TYPE = new PlanitComponentEventType("PLANITCOMPONENT.GAPFUNCTION.POPULATE");

  /**
   * Constructor
   * 
   * @param source                of the event
   * @param gapFunctionToPopulate gap function to populate
   */
  public PopulateGapFunctionEvent(final PlanitComponentFactory<?> source, final GapFunction gapFunctionToPopulate) {
    super(EVENT_TYPE, source, gapFunctionToPopulate, null);
  }

  /**
   * collect gap function to populate
   * 
   * @return gap function
   */
  public GapFunction getGapFunctionToPopulate() {
    return (GapFunction) getComponentToPopulate();
  }

}
