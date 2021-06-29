package org.planit.network.virtual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.planit.network.Network;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.virtual.ConnectoidEdge;
import org.planit.utils.network.virtual.ConnectoidSegment;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.DirectedConnectoid;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.utils.zoning.Zone;

/**
 * Model free virtual network which is part of the zoning and holds all the virtual infrastructure connecting the zones to the physical road network.
 * 
 * @author markr
 */
public class VirtualNetwork extends Network {

  // INNER CLASSES

  /** generated id */
  private static final long serialVersionUID = -4088201905917614130L;

  /**
   * Internal class for all Connectoid specific code
   *
   */
  public class ConnectoidEdges implements Iterable<ConnectoidEdge> {

    /**
     * Add connectoid edge to the internal container
     * 
     * If new connectoid edge overrides an existing connectoid edge , the removed connectoid edge is returned
     * 
     * @param connectoidEdge the connectoid edge to be registered
     * @return connectoidEdge added
     */
    protected ConnectoidEdge register(ConnectoidEdge connectoidEdge) {
      return connectoidEdgeMap.put(connectoidEdge.getId(), connectoidEdge);
    }

    /**
     * Create new connectoid edge to from a specified centroid to a specified node
     * 
     * @param connectoid extract information from connectoid to create virtual connectoid edge
     * @return newly created connectoid edges (reference nodes not yet aware of connection)
     * @throws PlanItException thrown if there is an error
     */
    public Collection<ConnectoidEdge> registerNew(Connectoid connectoid) throws PlanItException {

      /* constructed from connectoid information */
      ArrayList<ConnectoidEdge> connectoidEdges = new ArrayList<ConnectoidEdge>();
      for (Zone accessZone : connectoid) {

        /* Access node */
        /* for now we only utilise a single access node, either the given one, or the downstream node of a directed connectoid */
        /* TODO: when we implement PT assignments this likely will change */
        DirectedVertex accessVertex = null;
        if (connectoid instanceof UndirectedConnectoid) {
          accessVertex = UndirectedConnectoid.class.cast(connectoid).getAccessVertex();
        } else if (connectoid instanceof DirectedConnectoid) {
          EdgeSegment accessEdgeSegment = DirectedConnectoid.class.cast(connectoid).getAccessLinkSegment();
          accessVertex = (Node) (accessEdgeSegment != null ? accessEdgeSegment.getDownstreamVertex() : null);
        } else {
          throw new PlanItException(String.format("connectoid %s is of unrecognised type and access node could not be retrieved", connectoid.getXmlId()));
        }

        /* create and register connectoid edge */
        Optional<Double> connectoidLength = connectoid.getLengthKm(accessZone);
        connectoidLength.orElseThrow(() -> new PlanItException("unable to retrieve lenght for connectoid %s (id:%d)", connectoid.getXmlId(), connectoid.getId()));
        ConnectoidEdge newConnectoidEdge = new ConnectoidEdgeImpl(getIdGroupingToken(), accessZone.getCentroid(), accessVertex, connectoidLength.get());
        register(newConnectoidEdge);
        connectoidEdges.add(newConnectoidEdge);
      }
      return connectoidEdges;
    }

    /**
     * Get connectoid edge by id
     * 
     * @param id the id of this connectoid edge
     * @return the retrieved connectoid edge
     */
    public ConnectoidEdge get(long id) {
      return connectoidEdgeMap.get(id);
    }

    /**
     * Return number of connectoid edges on the network
     * 
     * @return the number of connectoid edges
     */
    public int size() {
      return connectoidEdgeMap.size();
    }

    /**
     * Collect the connectoid edges available through an iterator
     */
    @Override
    public Iterator<ConnectoidEdge> iterator() {
      return connectoidEdgeMap.values().iterator();
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
     * @param parent      the connectoid which will contain this connectoid segment
     * @param directionAB direction of travel
     * @return created connectoid segment
     * @throws PlanItException thrown if there is an error
     */
    public ConnectoidSegment createAndRegisterConnectoidSegment(ConnectoidEdge parent, boolean directionAB) throws PlanItException {
      ConnectoidSegment connectoidSegment = new ConnectoidSegmentImpl(getIdGroupingToken(), parent, directionAB);
      parent.registerConnectoidSegment(connectoidSegment, directionAB);
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

  // Protected

  // for now use tree map to ensure non-duplicate keys until we add functionality
  // to account for this (treemap is slower than hashmap)
  protected Map<Long, ConnectoidEdge> connectoidEdgeMap = new TreeMap<Long, ConnectoidEdge>();
  protected Map<Long, ConnectoidSegment> connectoidSegmentMap = new TreeMap<Long, ConnectoidSegment>();

  // PUBLIC

  /**
   * Constructor
   * 
   * @param tokenId contiguous id generation for instances of this class
   */
  public VirtualNetwork(final IdGroupingToken tokenId) {
    super(tokenId);
  }

  /**
   * internal class instance containing all connectoid specific functionality
   */
  public final ConnectoidEdges connectoidEdges = new ConnectoidEdges();
  /**
   * internal class instance containing all connectoid segment specific functionality
   */
  public final ConnectoidSegments connectoidSegments = new ConnectoidSegments();

  /**
   * free up memory by clearing contents for garbage collection
   */
  public void clear() {
    connectoidEdgeMap.clear();
    connectoidSegmentMap.clear();
  }

}
