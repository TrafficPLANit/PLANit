package org.goplanit.assignment.traditionalstatic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.od.path.OdMultiPaths;
import org.goplanit.od.path.OdPath2MultiPathWrapper;
import org.goplanit.od.path.OdPathMatrix;
import org.goplanit.output.adapter.PathOutputTypeAdapterImpl;
import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.reflection.ReflectionUtils;

/**
 * Adapter providing access to the data of the TraditionalStaticAssignment class relevant for OD path outputs without exposing the internals of the traffic assignment class itself
 * 
 * @author gman6028
 *
 */
public class TraditionalStaticPathOutputTypeAdapter extends PathOutputTypeAdapterImpl {

  private <U extends Collection<?>, V, W extends Collection<V>> Class<W> wrap(Class<U> class1, Class<V> class2){
    return (Class<W>) class1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected TraditionalStaticAssignment getAssignment() {
    return (TraditionalStaticAssignment) super.getAssignment();
  }

  /**
   * Constructor
   * 
   * @param outputType        the output type for the current persistence
   * @param trafficAssignment the traffic assignment used to provide the data
   */
  public TraditionalStaticPathOutputTypeAdapter(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * Retrieve OD paths for a specified mode
   *
   * @param mode the specified mode
   * @return the OD path object
   */
  @Override
  public Optional<OdMultiPaths<?,?>> getOdMultiPaths(Mode mode) {
    var odSinglePaths = getAssignment().getIterationData().getOdPaths(mode);
    return Optional.of(
            new OdPath2MultiPathWrapper(
                    getAssignment().getIdGroupingToken(),
                    odSinglePaths,
                    ReflectionUtils.injectClassIntoContainerGenerics(ArrayList.class,odSinglePaths.getDataClass())));
  }
}
