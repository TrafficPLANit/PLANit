package org.goplanit.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CloneUtils;
import org.locationtech.jts.geom.LineString;

/**
 * Edge class connecting two vertices via some geometry. Each edge has one or two underlying edge segments in a particular direction which may carry additional information for each
 * particular direction of the edge.
 *
 * @author markr
 *
 */
public class EdgeImpl<V extends Vertex> extends GraphEntityImpl implements Edge {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(EdgeImpl.class.getCanonicalName());

  /** generated UID */
  private static final long serialVersionUID = -3061186642253968991L;

  /**
   * Vertex A
   */
  private V vertexA = null;

  /**
   * Vertex B
   */
  private V vertexB = null;

  /**
   * The line geometry of this link if set
   */
  protected LineString lineGeometry;

  /**
   * Generic input property storage
   */
  protected Map<String, Object> inputProperties = null;

  /**
   * Name of the edge
   */
  protected String name = "";

  /**
   * Length of edge
   */
  protected Double lengthInKm;

  /**
   * set vertex B
   * 
   * @param vertexB to set
   */
  protected void setVertexB(V vertexB) {
    this.vertexB = vertexB;
  }

  /**
   * set vertex A
   * 
   * @param vertexA to set
   */
  protected void setVertexA(V vertexA) {
    this.vertexA = vertexA;
  }

  /**
   * Constructor which injects link lengths directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first vertex in the link
   * @param vertexB  second vertex in the link
   */
  protected EdgeImpl(final IdGroupingToken groupId, final V vertexA, final V vertexB) {
    super(groupId, EDGE_ID_CLASS);
    this.vertexA = vertexA;
    this.vertexB = vertexB;
    this.lengthInKm = null;
    this.lineGeometry = null;
  }

  /**
   * Constructor which injects link lengths directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first vertex in the link
   * @param vertexB  second vertex in the link
   * @param lengthKm length of the link
   */
  protected EdgeImpl(final IdGroupingToken groupId, final V vertexA, final V vertexB, final double lengthKm) {
    this(groupId, vertexA, vertexB);
    this.lengthInKm = lengthKm;
  }

  /**
   * Copy constructor, input properties are copied using serialisation/deserialisation because shallow copy is considered dangerous
   * 
   * @param edgeImpl to copy
   */
  protected EdgeImpl(EdgeImpl<V> edgeImpl) {
    super(edgeImpl);
    if (edgeImpl.hasGeometry()) {
      setGeometry((LineString) edgeImpl.getGeometry().copy());
    }
    this.vertexA = edgeImpl.vertexA;
    this.vertexB = edgeImpl.vertexB;
    this.lengthInKm = edgeImpl.lengthInKm;
    this.name = edgeImpl.name;
    if (edgeImpl.inputProperties != null && !edgeImpl.inputProperties.isEmpty()) {
      for (var entry : edgeImpl.inputProperties.entrySet()) {
        addInputProperty(new String(entry.getKey()), CloneUtils.clone(entry.getValue()));
      }
    }
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public LineString getGeometry() {
    return lineGeometry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setGeometry(LineString lineString) {
    this.lineGeometry = lineString;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeVertex(Vertex vertex) {
    if (vertex != null) {
      if (getVertexA() != null && getVertexA().getId() == vertex.getId()) {
        return removeVertexA();
      } else if (getVertexB() != null && getVertexB().getId() == vertex.getId()) {
        return removeVertexB();
      }
    }
    return false;
  }

  /**
   * remove vertex B by setting it to null
   * 
   * @return true
   */
  public boolean removeVertexB() {
    setVertexB(null);
    return true;
  }

  /**
   * remove vertex A by setting it to null
   * 
   * @return true
   */
  public boolean removeVertexA() {
    setVertexA(null);
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addInputProperty(final String key, final Object value) {
    if (inputProperties == null) {
      inputProperties = new HashMap<String, Object>();
    }
    inputProperties.put(key, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getInputProperty(final String key) {
    if (inputProperties == null) {
      return null;
    }
    return inputProperties.get(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getLengthKm() {
    return lengthInKm;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setLengthKm(double lengthInKm) {
    this.lengthInKm = lengthInKm;
  }

  // Getters-Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public V getVertexA() {
    return vertexA;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V getVertexB() {
    return vertexB;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean replace(final Vertex vertexToReplace, final Vertex vertexToReplaceWith) {
    boolean vertexReplaced = false;

    /* replace vertices on edge */
    if (vertexToReplaceWith != null) {
      if (getVertexA() != null && vertexToReplace.getId() == getVertexA().getId()) {
        removeVertex(vertexToReplace);
        setVertexA((V) vertexToReplaceWith);
        vertexReplaced = true;
      } else if (getVertexB() != null && vertexToReplace.getId() == getVertexB().getId()) {
        removeVertex(vertexToReplace);
        setVertexB((V) vertexToReplaceWith);
        vertexReplaced = true;
      }
    }

    return vertexReplaced;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public EdgeImpl<V> clone() {
    return new EdgeImpl<V>(this);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public boolean validate() {

    if (getVertexA() == null) {
      LOGGER.warning(String.format("vertex A missing on edge (id:%d externalId:%s)", getId(), getExternalId()));
      return false;
    }

    if (getVertexB() == null) {
      LOGGER.warning(String.format("vertex B missing on edge segment (id:%d externalId:%s)", getId(), getExternalId()));
      return false;
    }

    if (getVertexA().getEdges(getVertexB()) == null || !(getVertexA().getEdges(getVertexB()).contains(this))) {
      LOGGER.warning(String.format("edge (id:%d externalId:%s) not registered on vertex A", getId(), getExternalId()));
      return false;
    }

    if (getVertexB().getEdges(getVertexA()) == null || !(getVertexB().getEdges(getVertexA()).contains(this))) {
      LOGGER.warning(String.format("edge (id:%d externalId:%s) not registered on vertex B", getId(), getExternalId()));
      return false;
    }

    if(getGeometry()!=null &&
        (!getGeometry().isWithinDistance(getVertexB().getPosition(), Precision.EPSILON_6) || !getGeometry().isWithinDistance(getVertexB().getPosition(), Precision.EPSILON_6))){
      LOGGER.warning(String.format("edge (id:%d externalId:%s) internal geometry does not cover its vertices", getId(), getExternalId()));
    }

    return true;
  }

}
