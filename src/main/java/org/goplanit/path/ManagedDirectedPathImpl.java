package org.goplanit.path;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.SimpleDirectedPath;
import org.w3.xlink.Simple;

/**
 * This object represents a path based on a number of consecutive LinkSegments
 *
 * The path creation makes use of the fact that the origin pair will have a null EdgeSegment, so there is no need to specify the origin.
 *
 * @author gman6028, markr
 *
 */
public class ManagedDirectedPathImpl extends ExternalIdAbleImpl implements ManagedDirectedPath {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ManagedDirectedPathImpl.class.getCanonicalName());

  /** representation of path through simple path */
  private final SimpleDirectedPath path;

  /**
   * Generate an id for this instance
   * 
   * @param groupId to use
   * @return created id
   */
  protected static long generateId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, ManagedDirectedPath.PATH_ID_CLASS);
  }

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected ManagedDirectedPathImpl(final IdGroupingToken groupId) {
    super(generateId(groupId));
    path = new SimpleDirectedPathImpl();
  }

  /**
   * Constructor
   * 
   * @param groupId          contiguous id generation within this group for instances of this class
   * @param pathEdgeSegments the path to set (not copied)
   */
  @SuppressWarnings("unchecked")
  protected ManagedDirectedPathImpl(final IdGroupingToken groupId, final Deque<? extends EdgeSegment> pathEdgeSegments) {
    super(generateId(groupId));
    path = new SimpleDirectedPathImpl(pathEdgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<EdgeSegment> iterator() {
    return path.iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long size() {
    return path.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsSubPath(Collection<? extends EdgeSegment> subPath) {
    return path.containsSubPath(subPath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsSubPath(Iterator<? extends EdgeSegment> subPath) {
    return path.containsSubPath(subPath);
  }

}
