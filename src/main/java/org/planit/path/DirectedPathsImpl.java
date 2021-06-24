package org.planit.path;

import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.path.DirectedPaths;

/**
 * Implementation of DirectedPaths interface
 * 
 * @author markr
 * 
 * @param <p> type of directed path
 */
public class DirectedPathsImpl<P extends DirectedPath> implements DirectedPaths<P> {

  /**
   * The builder to create paths
   */
  private final DirectedPathBuilder<P> pathBuilder;

  /**
   * Map to store paths by their Id
   */
  private final Map<Long, P> pathMap;


  /**
   * Constructor
   * 
   * @param pathBuilder the builder for path instances
   */
  public DirectedPathsImpl(DirectedPathBuilder<P> pathBuilder) {
    this.pathBuilder = pathBuilder;
    this.pathMap = new TreeMap<Long, P>();
  }

  /**
   * Add path to the internal container. Do not use this unless you know what you are doing because it can mess up the contiguous internal id structure of the paths. 
   * Preferred method is to only use registerNew.
   *
   * @param path path to be registered based on its internal id
   * @return path, in case it overrides an existing path, the removed path is returned
   */
  public P register(final P path) {
    return pathMap.put(path.getId(), path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(P path) {
    pathMap.remove(path.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(long pathId) {
    pathMap.remove(pathId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<P> iterator() {
    return pathMap.values().iterator();
  }
  
  /**
   * {@inheritDoc}
   */  
  @Override
  public P registerNew(){
    final P newPath = pathBuilder.createPath();
    register(newPath);   
    return newPath;
  }  
  
  /**
   * {@inheritDoc}
   */    
  @Override
  public P registerNew(Deque<EdgeSegment> edgeSegments) {
    final P newPath = pathBuilder.createPath(edgeSegments);
    register(newPath);   
    return newPath;
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public P get(final long id) {
    return pathMap.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return pathMap.size();
  }



}
