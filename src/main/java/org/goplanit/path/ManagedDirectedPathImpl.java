package org.goplanit.path;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.SimpleDirectedPathImpl;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * This object represents a path based on a number of consecutive LinkSegments
 *
 * @author gman6028, markr
 *
 */
public class ManagedDirectedPathImpl extends ExternalIdAbleImpl implements ManagedDirectedPath {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ManagedDirectedPathImpl.class.getCanonicalName());

  /** representation of path through simple path */
  private final SimpleDirectedPathImpl path;

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
   */
  private ManagedDirectedPathImpl() {
    super(-1);
    path = new SimpleDirectedPathImpl();
  }

  /**
   * Allow creation of specific empty path for dummy use, assigned unused id of -1.
   *
   * @return dummy empty path
   */
  public static ManagedDirectedPathImpl createEmptyDummyPath(){
    return new ManagedDirectedPathImpl();
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

  /** Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ManagedDirectedPathImpl(ManagedDirectedPathImpl other, boolean deepCopy /* no impact yet */) {
    super(other);
    /* composite, so simple path is managed path and therefore required copy */
    this.path = new SimpleDirectedPathImpl(other.path);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getFirstSegment() {
    return this.path.getFirstSegment();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getLastSegment() {
    return this.path.getLastSegment();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ManagedDirectedPathImpl shallowClone() {
    return new ManagedDirectedPathImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ManagedDirectedPathImpl deepClone() {
    return new ManagedDirectedPathImpl(this, true);
  }
}
