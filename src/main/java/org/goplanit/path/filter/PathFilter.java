package org.goplanit.path.filter;

import org.goplanit.component.PlanitComponent;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.PathUtils;
import org.goplanit.utils.path.SimpleDirectedPath;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiPredicate;

/**
 * The path filter component allows for the configuration of path filtering during on-the-fly (or after the fact) filtering
 * of (newly) created paths. For now only a single implementation exists, so this is not an abstract class. this may change
 * in the future
 *
 *
 * @author markr
 *
 */
public class PathFilter extends PlanitComponent<PathFilter> implements Serializable,
        Iterable<BiPredicate<ManagedDirectedPath, Collection<? extends ManagedDirectedPath>>> {

  /** generate UID */


  /** List of filters in form of predicates to apply when checking if a newly created path is eligible for inclusion in the set */
  private List<BiPredicate<ManagedDirectedPath, Collection<? extends ManagedDirectedPath>>> pathFilters = new ArrayList<>();

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param clazz the component class id
   */
  public PathFilter(IdGroupingToken groupId, Class<? extends PathFilter> clazz) {
    super(groupId, clazz);
  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public PathFilter(IdGroupingToken groupId) {
    this(groupId, PathFilter.class);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public PathFilter(final PathFilter other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PathFilter shallowClone(){
    return new PathFilter(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PathFilter deepClone(){
    return new PathFilter(this, true);
  }

  @Override
  public void reset() {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    var keyValueMap = new HashMap<String,String>();
    keyValueMap.put("numPathFilters", String.valueOf(pathFilters.size()));
    return keyValueMap;
  }

  /**
   * Activate maximum overlap factor (between 0 and 1). Setting this to 1 means any new path
   * is accepted as it may overlap 100% with an existing path, whereas setting it to 0 means any
   * new path must be fully disjoint from any existing path, otherwise it is discarded.
   *
   * @param maxOverlapFactor factor between 0 (0%) and 1 (100%)
   */
  public void activateMaxOverlapFilter(double maxOverlapFactor){
    pathFilters.add(
            (p , ps) -> ps.stream().noneMatch(
                    pOther -> Precision.greaterEqual(PathUtils.getOverlapFactor(p, pOther), maxOverlapFactor)));
  }

  /**
   * Activate a custom created filter on paths
   *
   * @param pathFilterPredicate to consider
   */
  public void addCustomFilter(BiPredicate<ManagedDirectedPath, Collection<? extends ManagedDirectedPath>> pathFilterPredicate){
    pathFilters.add(pathFilterPredicate);
  }

  /**
   * Verify if filters have been registered
   *
   * @return true when one or more are present, false otherwise
   */
  public boolean hasFilters(){
    return !pathFilters.isEmpty();
  }

  /**
   * Iterable access to filters currently registered
   *
   * @return iterator
   */
  @Override
  public Iterator<BiPredicate<ManagedDirectedPath, Collection<? extends ManagedDirectedPath>>> iterator() {
    return pathFilters.iterator();
  }
}
