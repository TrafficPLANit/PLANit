package org.planit.input;

import java.util.logging.Logger;

import org.planit.component.event.PlanitComponentEventType;
import org.planit.component.event.PlanitComponentListener;
import org.planit.component.event.PopulateComponentEvent;
import org.planit.component.event.PopulateDemandsEvent;
import org.planit.component.event.PopulateInitialLinkSegmentCostEvent;
import org.planit.component.event.PopulateNetworkEvent;
import org.planit.component.event.PopulatePhysicalCostEvent;
import org.planit.component.event.PopulateZoningEvent;
import org.planit.converter.demands.DemandsReader;
import org.planit.converter.network.NetworkReader;
import org.planit.converter.zoning.ZoningReader;

/**
 * Base input builder class that gets notified whenever traffic assignment components are to be populated or configured. It also has readers for each of the main inputs (network,
 * zoning, demands) that can be set by any derived class using specific subclasses dedicated to the specific formats of the data sources used.
 * 
 * @author markr
 *
 */
public abstract class InputBuilderListener implements PlanitComponentListener {

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
    return new PlanitComponentEventType[] { PopulateComponentEvent.EVENT_TYPE, PopulateNetworkEvent.EVENT_TYPE, PopulateZoningEvent.EVENT_TYPE, PopulateDemandsEvent.EVENT_TYPE,
        PopulatePhysicalCostEvent.EVENT_TYPE, PopulateInitialLinkSegmentCostEvent.EVENT_TYPE };
  }

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(InputBuilderListener.class.getCanonicalName());

  /** network reader used */
  private NetworkReader networkReader;

  /** zoning reader used */
  private ZoningReader zoningReader;

  /** demands reader used */
  private DemandsReader demandsReader;

  /**
   * Collect network reader
   * 
   * @return network reader
   */
  protected NetworkReader getNetworkReader() {
    return networkReader;
  }

  /**
   * Set the network reader to use
   * 
   * @param networkReader to use
   */
  protected void setNetworkReader(NetworkReader networkReader) {
    this.networkReader = networkReader;
  }

  /**
   * Collect zoning reader
   * 
   * @return zoning reader
   */
  public ZoningReader getZoningReader() {
    return zoningReader;
  }

  /**
   * Set the zoning reader to use
   * 
   * @param zoningReader to use
   */
  public void setZoningReader(ZoningReader zoningReader) {
    this.zoningReader = zoningReader;
  }

  /**
   * Collect demands reader
   * 
   * @return demands reader
   */
  public DemandsReader getDemandsReader() {
    return demandsReader;
  }

  /**
   * Set the demands reader to use
   * 
   * @param demandsReader to use
   */
  public void setDemandsReader(DemandsReader demandsReader) {
    this.demandsReader = demandsReader;
  }

  /**
   * Constructor
   */
  public InputBuilderListener() {
  }

}
