package org.planit.input;

import java.util.logging.Logger;

import org.djutils.event.EventListenerInterface;
import org.planit.converter.demands.DemandsReader;
import org.planit.converter.network.NetworkReader;
import org.planit.converter.zoning.ZoningReader;

/**
 * Base input builder class that gets notified whenever traffic assignment components are to be populated or configured. It also has readers for each
 * of the main inputs (network, zoning, demands) that can be set by any derived class using specific subclasses dedicated to the specific formats of the data 
 * sources used.
 * 
 * @author markr
 *
 */
public abstract class InputBuilderListener implements EventListenerInterface {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(InputBuilderListener.class.getCanonicalName());

  /** generated UID */
  private static final long serialVersionUID = 4223028100274802893L;
  
  /** network reader used */
  private NetworkReader networkReader;
  
  /** zoning reader used */
  private ZoningReader zoningReader;
  
  /** demands reader used */
  private DemandsReader demandsReader;
  
  /** Collect network reader
   * 
   * @return network reader
   */
  protected NetworkReader getNetworkReader() {
    return networkReader;
  }

  /** Set the network reader to use
   * 
   * @param networkReader to use
   */  
  protected void setNetworkReader(NetworkReader networkReader) {
    this.networkReader = networkReader;
  }  

  /** Collect zoning reader
   * 
   * @return zoning reader
   */  
  public ZoningReader getZoningReader() {
    return zoningReader;
  }

  /** Set the zoning reader to use
   * 
   * @param zoningReader to use
   */
  public void setZoningReader(ZoningReader zoningReader) {
    this.zoningReader = zoningReader;
  }
  
  /** Collect demands reader
   * 
   * @return demands reader
   */
  public DemandsReader getDemandsReader() {
    return demandsReader;
  }
  
  /** Set the demands reader to use
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
