package org.planit.network.physical;

import java.util.Iterator;
import org.planit.utils.graph.Vertices;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.Nodes;

/**
 * 
 * Nodes implementation wrapper that simply utilises passed in vertices of the desired generic type to delegate registration and creation of its nodes on
 * 
 * @author markr
 *
 * @param <N> concrete class of nodes that are being created
 */
public class NodesImpl<N extends Node> implements Nodes<N> {

  /**
   * The graph we use to create and register our nodes on
   */
  private final Vertices<N> vertices;

  /**
   * Constructor
   * 
   * @param graph the graph to use to create and register nodes on
   */
  public NodesImpl(final Vertices<N> vertices) {
    this.vertices = vertices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(final N node) {
    vertices.remove(node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(long nodeId) {
    vertices.remove(nodeId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public N createNew() {
    return vertices.createNew();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public N register(final N node) {
    return vertices.register(node);  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<N> iterator() {
    return vertices.iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public N registerNew() {
    return vertices.registerNew();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public N registerNew(Object externalId) {
    return vertices.registerNew(externalId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return vertices.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public N get(final long id) {
    return vertices.get(id);
  }

}
