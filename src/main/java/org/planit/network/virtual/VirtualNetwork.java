package org.planit.network.virtual;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.Connectoid;
import org.planit.utils.network.virtual.ConnectoidSegment;
import org.planit.utils.network.virtual.Zone;

/**
 * Model free virtual network which is part of the zoning and holds all the virtual infrastructure connecting the zones to the physical road network.
 * 
 * @author markr
 */
public class VirtualNetwork {

  // INNER CLASSES

  /**
   * Internal class for all Connectoid specific code
   *
   */
  public class Connectoids implements Iterable<Connectoid> {

    /**
     * Add connectoid to the internal container
     * 
     * If new connectoid overrides an existing connectoid, the removed connectoid is returned
     * 
     * @param connectoid the connectoid to be registered
     * @return connectoid added
     */
    protected Connectoid registerConnectoid(Connectoid connectoid) {
      return connectoidMap.put(connectoid.getId(), connectoid);
    }

    /**
     * Create new connectoid to from a specified centroid to a specified node
     * 
     * @param centroid   centroid at one end of the connectoid
     * @param node       node at other end of the connectoid
     * @param length     length of connectiod
     * @param externalId external Id of connectoid
     * @return Connectoid object created and registered
     * @throws PlanItException thrown if there is an error
     */
    public Connectoid registerNewConnectoid(Centroid centroid, Node node, double length, Object externalId) throws PlanItException {
      Connectoid newConnectoid = new ConnectoidImpl(groupId, centroid, node, length, externalId);
      registerConnectoid(newConnectoid);
      return newConnectoid;
    }

    /**
     * Create new connectoid to from a specified centroid to a specified node
     * 
     * @param centroid centroid at one end of the connectoid
     * @param node     node at other end of the connectoid
     * @param length   length of connectiod
     * @return Connectoid object created and registered
     * @throws PlanItException thrown if there is an error
     */
    public Connectoid registerNewConnectoid(Centroid centroid, Node node, double length) throws PlanItException {
      Connectoid newConnectoid = new ConnectoidImpl(groupId, centroid, node, length);
      registerConnectoid(newConnectoid);
      return newConnectoid;
    }

    /**
     * Get connectoid by id
     * 
     * @param id the id of this connectoid
     * @return the retrieved connectoid
     */
    public Connectoid getConnectoid(long id) {
      return connectoidMap.get(id);
    }

    /**
     * Return number of connectoids on the network
     * 
     * @return the number of connectoids
     */
    public int getNumberOfConnectoids() {
      return connectoidMap.size();
    }

    /**
     * Collect the connectoids available through an iterator
     */
    @Override
    public Iterator<Connectoid> iterator() {
      return connectoidMap.values().iterator();
    }

  }

  /**
   * Internal class for non-physical LinkSegment specific code, i.e. connectoid segments (physical link segments are placed in the network)
   *
   */
  public class ConnectoidSegments implements Iterable<ConnectoidSegment> {

    /**
     * Register a connectid segment on the zoning
     * 
     * @param connectoidSegment ConnectoidSegment to be registered
     * @return the registered connectoid segment
     */
    protected ConnectoidSegment registerConnectoidSegment(ConnectoidSegment connectoidSegment) {
      return connectoidSegmentMap.put(connectoidSegment.getId(), connectoidSegment);
    }

    /**
     * Iterator for all connectoid segments available
     */
    @Override
    public Iterator<ConnectoidSegment> iterator() {
      return connectoidSegmentMap.values().iterator();
    }

    /**
     * Create and register connectoid segment in AB direction on virtual network
     * 
     * @param parentConnectoid the connectoid which will contain this connectoid segment
     * @param directionAB      direction of travel
     * @return created connectoid segment
     * @throws PlanItException thrown if there is an error
     */
    public ConnectoidSegment createAndRegisterConnectoidSegment(Connectoid parentConnectoid, boolean directionAB) throws PlanItException {
      ConnectoidSegment connectoidSegment = new ConnectoidSegmentImpl(groupId, parentConnectoid, directionAB);
      parentConnectoid.registerConnectoidSegment(connectoidSegment, directionAB);
      registerConnectoidSegment(connectoidSegment);
      return connectoidSegment;
    }

    /**
     * Get connectoid segment by id
     * 
     * @param id the id of this connectoid segment
     * @return retrieved ConnectoidSegment object
     */
    public ConnectoidSegment getConnectoidSegment(long id) {
      return connectoidSegmentMap.get(id);
    }

    /**
     * Return the number of registered connectoid segments
     * 
     * @return the number of registered connectoid segments
     */
    public int getNumberOfConnectoidSegments() {
      return connectoidSegmentMap.size();
    }

  }

  /**
   * Internal class for all Centroid specific code
   *
   */
  public class Centroids implements Iterable<Centroid> {

    /**
     * Add centroid to the internal container
     * 
     * If centroid overrides an existing centroid, the removed centroid is returned
     * 
     * @param centroid centroid to be registered
     * @return registered centroid
     */
    public Centroid registerCentroid(Centroid centroid) {
      return centroidMap.put(centroid.getId(), centroid);
    }

    /**
     * Create new centroid
     * 
     * @param zone zone on which the centroid is registered
     * @return registered new centroid
     */
    public Centroid registerNewCentroid(Zone zone) {
      Centroid newCentroid = new CentroidImpl(groupId, zone);
      registerCentroid(newCentroid);
      return newCentroid;
    }

    /**
     * Access to all centroids via iterator
     */
    @Override
    public Iterator<Centroid> iterator() {
      return centroidMap.values().iterator();
    }

    /**
     * Return number of registered centroids
     * 
     * @return number of registered centroids
     */
    public int getNumberOfCentroids() {
      return centroidMap.size();
    }

  }

  // Protected

  /**
   * Contiguous id generation within this group id token for all instances created with factory methods in this class
   */
  protected final IdGroupingToken groupId;

  // for now use tree map to ensure non-duplicate keys until we add functionality
  // to account for this (treemap is slower than hashmap)
  protected Map<Long, Connectoid> connectoidMap = new TreeMap<Long, Connectoid>();
  protected Map<Long, ConnectoidSegment> connectoidSegmentMap = new TreeMap<Long, ConnectoidSegment>();
  protected Map<Long, Centroid> centroidMap = new TreeMap<Long, Centroid>();

  // PUBLIC

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public VirtualNetwork(final IdGroupingToken groupId) {
    // a virtual network has no id, i.e., there is only a single copy so we can utilise the passed in group id
    this.groupId = groupId;
  }

  /**
   * internal class instance containing all connectoid specific functionality
   */
  public final Connectoids connectoids = new Connectoids();
  /**
   * internal class instance containing all connectoid segment specific functionality
   */
  public final ConnectoidSegments connectoidSegments = new ConnectoidSegments();
  /**
   * internal class instance containing all centroid specific functionality
   */
  public final Centroids centroids = new Centroids();

}
