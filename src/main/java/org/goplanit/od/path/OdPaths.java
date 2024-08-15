package org.goplanit.od.path;

import org.goplanit.utils.od.OdData;
import org.goplanit.utils.od.OdDataIterator;
import org.goplanit.utils.path.ManagedDirectedPath;

/**
 * A container class for Origin-Destination paths. OdPaths should be Idable. We leave it to the concrete implementations to determine what type of container is used, this is only a
 * placeholder to signify that it concerns a container for paths that is uniquely identifiable
 * 
 * @author markr
 *
 */
public interface OdPaths<T extends ManagedDirectedPath> extends OdData<T> {

  /**
   * The total number of paths currently registered.
   * (likely computed on the fly)
   *
   * @return total number of paths
   */
  long determineTotalPaths();

  /**
   * Returns an iterator which can iterate through all the origin-destinations and provide a path for each
   *
   * @return iterator through all the origin-destination cells
   */
  public OdPathIterator<T> iterator();

  /**
   * Shallow copy
   * @return shallow copy
   */
  public OdPaths<T> shallowClone();

  /**
   * deep copy
   * @return deep copy
   */
  public OdPaths<T> deepClone();


}
