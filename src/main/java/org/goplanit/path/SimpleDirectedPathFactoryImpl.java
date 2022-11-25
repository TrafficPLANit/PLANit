package org.goplanit.path;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.path.DirectedPathFactory;
import org.goplanit.utils.path.SimpleDirectedPath;

import java.util.Deque;

/**
 * Factory for creating simple directed paths
 * 
 * @author markr
 */
public class SimpleDirectedPathFactoryImpl implements DirectedPathFactory<SimpleDirectedPath> {

  /**
   * {@inheritDoc}
   */
  @Override
  public SimpleDirectedPath createNew() {
    return new SimpleDirectedPathImpl();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SimpleDirectedPath createNew(Deque<? extends EdgeSegment> edgeSegments) {
    return new SimpleDirectedPathImpl(edgeSegments);
  }

}
