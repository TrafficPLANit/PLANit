package org.planit.path;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.logging.Logger;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.path.DirectedPath;

/**
 * This object represents a path based on a number of consecutive LinkSegments
 *
 * The path creation makes use of the fact that the origin pair will have a null EdgeSegment, so there is no need to specify the origin.
 *
 * @author gman6028, markr
 *
 */
public class DirectedPathImpl extends ExternalIdAbleImpl implements DirectedPath {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DirectedPathImpl.class.getCanonicalName());

  /**
   * deque containing the edge segments in the path (we use a deque for easy insertion at both ends which is generally preferable when constructing paths based on shortest path
   * algorithms. Access is less of an issue as we only allow one to iterate
   */
  private final Deque<EdgeSegment> path;

  /**
   * Generate an id for this instance
   * 
   * @param groupId to use
   * @return created id
   */
  protected static long generateId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, DirectedPath.PATH_ID_CLASS);
  }

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected DirectedPathImpl(final IdGroupingToken groupId) {
    super(generateId(groupId));
    path = new ArrayDeque<EdgeSegment>();
  }

  /**
   * Constructor
   * 
   * @param groupId          contiguous id generation within this group for instances of this class
   * @param pathEdgeSegments the path to set (not copied)
   */
  @SuppressWarnings("unchecked")
  protected DirectedPathImpl(final IdGroupingToken groupId, final Deque<? extends EdgeSegment> pathEdgeSegments) {
    super(generateId(groupId));
    path = (Deque<EdgeSegment>) pathEdgeSegments;
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
    if (subPath == null || subPath.isEmpty()) {
      return false;
    }

    Iterator<? extends EdgeSegment> subPathIter = subPath.iterator();
    EdgeSegment subPathSegment = subPathIter.next();
    boolean started = false;
    for (EdgeSegment edgeSegment : path) {
      if (edgeSegment.idEquals(subPathSegment)) {
        started = true;
        if (!subPathIter.hasNext()) {
          break;
        }
        subPathSegment = subPathIter.next();
      } else if (started && subPathIter.hasNext()) {
        return false;
      }
    }

    return started;
  }

}
