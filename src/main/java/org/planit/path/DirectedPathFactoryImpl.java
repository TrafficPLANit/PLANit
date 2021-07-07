package org.planit.path;

import java.util.Deque;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.path.DirectedPathFactory;

/**
 * Factory for creating directed paths on container
 * 
 * @author markr
 */
public class DirectedPathFactoryImpl extends ManagedIdEntityFactoryImpl<DirectedPath> implements DirectedPathFactory {

  /**
   * Constructor
   * 
   * @param groupIdToken  to use for creating element ids
   */
  public DirectedPathFactoryImpl(final IdGroupingToken groupIdToken) {
    super(groupIdToken);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPath createNew() {
    return new DirectedPathImpl(groupIdToken);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPath createNew(Deque<? extends EdgeSegment> edgeSegments) {
    return new DirectedPathImpl(getIdGroupingToken(), edgeSegments);    
  }

}
