package org.goplanit.network.virtual.graph;

import org.goplanit.graph.directed.DirectedEdgeImpl;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;

import java.util.logging.Logger;

/**
 * Edge implementation that represent edges that exist between centroids and connectoids
 * (their node reference), so not physical entities but rather virtual links
 * 
 * @author markr
 *
 */
public class ConnectoidDirectedEdgeImpl
    extends DirectedEdgeImpl<DirectedVertex, ConnectoidSegment> implements ConnectoidDirectedEdge {

  private static final Logger LOGGER = Logger.getLogger(ConnectoidDirectedEdgeImpl.class.getCanonicalName());

  /**
   * Constructor
   *
   * @param groupId   contiguous id generation within this group for instances of this class
   * @param centroidA the centroidVertex at one end of the connectoid
   * @param vertexB   the vertex at the other end of the connectoid
   * @param length    length of the current connectoid
   */
  protected ConnectoidDirectedEdgeImpl(
      final IdGroupingToken groupId, final CentroidVertex centroidA, final DirectedVertex vertexB, final double length) {
    super(groupId, centroidA, vertexB, length);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConnectoidDirectedEdgeImpl(ConnectoidDirectedEdgeImpl other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * Utilising the A and B vertex construct a direct line between the two points as the geometry. In case the centroid
   * vertex has no geometry, we try to construct the closes projected point ont the parent zone's geometry instead.
   *
   * @param overwrite when true, overwrite existing geometry, otherwise ignore
   * @return true when successful, false otherwise
   */
  public boolean populateGeometry(boolean overwrite) {
    boolean success = super.populateBasicGeometry(overwrite);
    if(success){
      return success;
    }

    /* no success, likely because connected zone has no centroid location, but instead geometry is polygon or linestring
     * covered by parent zone geometry instead. In those cases, construct geometry based on closest projected point on
     * parent zone geometry */
    var centroid = getCentroidVertex().getParent();
    if(centroid == null){
      return false;
    }

    if(centroid.hasPosition()){
      LOGGER.severe("Centroid has position, yet populating basic geometry via Edge failed, this shouldn't happen");
      return false;
    }

    if(centroid.getParentZone()==null || !centroid.getParentZone().hasGeometry()){
      return false;
    }

    return false;
  }

  /**
   * Recreate internal ids: id and connectoid edge id
   * 
   * @return recreated id
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidDirectedEdgeImpl shallowClone() {
    return new ConnectoidDirectedEdgeImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidDirectedEdgeImpl deepClone() {
    return new ConnectoidDirectedEdgeImpl(this, true);
  }

}
