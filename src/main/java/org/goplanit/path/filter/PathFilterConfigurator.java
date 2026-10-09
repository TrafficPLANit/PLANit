package org.goplanit.path.filter;

import org.goplanit.utils.builder.Configurator;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.SimpleDirectedPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Class for path filter configurator
 * 
 * @author markr
 */
public class PathFilterConfigurator extends Configurator<PathFilter> {

  /** method name for setting step size on actual instance */
  public static final String ACTIVATE_MAX_OVERLAP = "activateMaxOverlapFilter";

  /** List of custom filters in form of predicates to apply */
  private List<BiPredicate<ManagedDirectedPath, Collection<? extends ManagedDirectedPath>>> customFilters = new ArrayList<>();


  /**
   * Constructor
   *
   */
  public PathFilterConfigurator() {
    super(PathFilter.class);
  }

  /**
   * Return the current max overlap filter (assuming it is set)
   *
   * @return max overlap filter, 1 when not set
   */
  public double getMaxOverlapFilter() {
    return (double) (hasRegisteredDelayedMethodCall(ACTIVATE_MAX_OVERLAP) ? getFirstParameterOfDelayedMethodCall(ACTIVATE_MAX_OVERLAP) : 1);
  }

  /**
   * Activate maximum overlap factor (between 0 and 1). Setting this to 1 means any new path
   * is accepted as it may overlap 100% with an existing path, whereas setting it to 0 means any
   * new path must be fully disjoint from any existing path, otherwise it is discarded.
   *
   * @param maxOverlapFactor factor between 0 (0%) and 1 (100%)
   */
  public void activateMaxOverlapFilter(double maxOverlapFactor) {
    registerDelayedMethodCall(ACTIVATE_MAX_OVERLAP, maxOverlapFactor);
  }

  /**
   * Activate a custom created filter on paths. The predicate should return true for the path to be kept, i.e., not be filtered out
   * of the set of eligible paths.
   *
   * @param pathFilterPredicate to consider
   */
  public void addCustomFilter(BiPredicate<ManagedDirectedPath, Collection<? extends ManagedDirectedPath>> pathFilterPredicate){
    customFilters.add(pathFilterPredicate);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends path filter
   * 
   * @param pathFilter the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(PathFilter pathFilter){
    super.configure(pathFilter);

    /* requires custom configuration due to signature */
    customFilters.forEach(f -> pathFilter.addCustomFilter(f));
  }
}
