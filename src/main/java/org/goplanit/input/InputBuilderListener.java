package org.goplanit.input;

import java.util.logging.Logger;

import org.goplanit.component.event.PlanitComponentEventType;
import org.goplanit.component.event.PlanitComponentListener;
import org.goplanit.component.event.PopulateComponentEvent;
import org.goplanit.component.event.PopulateDemandsEvent;
import org.goplanit.component.event.PopulateFundamentalDiagramEvent;
import org.goplanit.component.event.PopulateInitialLinkSegmentCostEvent;
import org.goplanit.component.event.PopulateNetworkEvent;
import org.goplanit.component.event.PopulatePhysicalCostEvent;
import org.goplanit.component.event.PopulateRoutedServicesEvent;
import org.goplanit.component.event.PopulateServiceNetworkEvent;
import org.goplanit.component.event.PopulateZoningEvent;

/**
 * Base input builder class that gets notified whenever traffic assignment components are to be populated or configured. It also has readers for each of the main inputs (network,
 * zoning, demands) that can be set by any derived class using specific subclasses dedicated to the specific formats of the data sources used.
 * 
 * @author markr
 *
 */
public abstract class InputBuilderListener implements PlanitComponentListener {

  //@formatter:off
  /**
   * Each input builder listener should be able to deal with at least the following Events and as such is automatically registered to listen for them when providing a derived
   * instance to a PLANit project by providing them here:
   * 
   * <ul>
   * <li>PopulateNetworkEvent for populating a newly created empty network</li>
   * <li>PopulateZoningEvent for populating a newly created empty zoning</li>
   * <li>PopulateDemandsEvent for populating a newly created empty demands</li>
   * <li>PopulatePhysicalCostEvent for populating a newly created empty physical costs</li>
   * <li>PopulateInitialLinkSegmentCostEvent for populating a newly created empty initial link segment costs</li>
   * <li>PopulateComponentEvent for all events that can be intercepted byt no dedicated event exists for (likely only for internal platform use)</li>
   * </ul>
   * 
   * @return default supported event types
   */
  @Override
  public PlanitComponentEventType[] getKnownSupportedEventTypes() {
    return new PlanitComponentEventType[] { 
        PopulateComponentEvent.EVENT_TYPE, 
        PopulateNetworkEvent.EVENT_TYPE, 
        PopulateZoningEvent.EVENT_TYPE, 
        PopulateDemandsEvent.EVENT_TYPE,
        PopulatePhysicalCostEvent.EVENT_TYPE,
        PopulateFundamentalDiagramEvent.EVENT_TYPE,
        PopulateInitialLinkSegmentCostEvent.EVENT_TYPE, 
        PopulateServiceNetworkEvent.EVENT_TYPE, 
        PopulateRoutedServicesEvent.EVENT_TYPE };
  }

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(InputBuilderListener.class.getCanonicalName());

  /**
   * Constructor
   */
  public InputBuilderListener() {
  }

}
