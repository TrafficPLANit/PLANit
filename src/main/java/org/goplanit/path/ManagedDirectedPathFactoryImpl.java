package org.goplanit.path;

import java.util.Deque;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.ManagedDirectedPathFactory;

/**
 * Factory for creating directed paths on container
 * 
 * @author markr
 */
public class ManagedDirectedPathFactoryImpl extends ManagedIdEntityFactoryImpl<ManagedDirectedPath> implements ManagedDirectedPathFactory<ManagedDirectedPath> {

  /**
   * Constructor
   * 
   * @param groupIdToken  to use for creating element ids
   */
  public ManagedDirectedPathFactoryImpl(final IdGroupingToken groupIdToken) {
    super(groupIdToken);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ManagedDirectedPath createNew() {
    return new ManagedDirectedPathImpl(groupIdToken);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ManagedDirectedPath createNew(Deque<? extends EdgeSegment> edgeSegments) {
    return new ManagedDirectedPathImpl(getIdGroupingToken(), edgeSegments);
  }

}
