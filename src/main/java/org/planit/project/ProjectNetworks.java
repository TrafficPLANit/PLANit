package org.planit.project;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.planit.network.TransportLayerNetwork;

/**
 * Internal class for registered physical networks
 *
 */
public class ProjectNetworks {
  
  /**
   * The physical networks registered on this project
   */
  protected final TreeMap<Long, TransportLayerNetwork<?,?>> infrastructureNetworkMap;  
  
  /**
   * Constructor
   */
  protected ProjectNetworks() {
    infrastructureNetworkMap = new TreeMap<Long, TransportLayerNetwork<?,?>>();
  }

  /**
   * Returns a List of infrastructure based networks
   *
   * @return List of networks
   */
  public List<TransportLayerNetwork<?,?>> toList() {
    return new ArrayList<TransportLayerNetwork<?,?>>(infrastructureNetworkMap.values());
  }

  /**
   * Get infrastructure network by id
   *
   * @param id the id of the network
   * @return the retrieved network
   */
  public TransportLayerNetwork<?,?> get(final long id) {
    return infrastructureNetworkMap.get(id);
  }

  /**
   * Get the number of networks
   *
   * @return the number of networks in the project
   */
  public int size() {
    return infrastructureNetworkMap.size();
  }

  /**
   * Check if infrastructure networks have already been registered
   *
   * @return true if registered networks exist, false otherwise
   */
  public boolean isEmpty() {
    return !infrastructureNetworkMap.isEmpty();
  }

  /**
   * Collect the first network that is registered (if any). Otherwise return null
   * 
   * @return first network that is registered if none return null
   */
  public TransportLayerNetwork<?,?> getFirst() {
    return isEmpty() ? infrastructureNetworkMap.firstEntry().getValue() : null;
  }

  /** Register a network
   * 
   * @param infrastructureNetwork to register
   */
  public void register(TransportLayerNetwork<?, ?> infrastructureNetwork) {
    infrastructureNetworkMap.put(infrastructureNetwork.getId(), infrastructureNetwork);    
  }
}
