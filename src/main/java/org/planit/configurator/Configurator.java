package org.planit.configurator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;

/**
 * 
 * This is the base class for all configuration oriented proxy classes. Whenever we configure a traffic assignment component, or any other component with functionality that should
 * not be exposed to the user, while at the same time this user must be able to configure this class by setting one or more configuration options, we must use this class to provide
 * a convenient mapping mechanism that work for any such situation without having to manually implement all the configuration options that are already present on the class that
 * contains the functionality.
 * 
 * The aim of this configurator is to store all the function calls that should be delayed and deferred to the class that contains the actual functionality in a concise and general
 * way.
 * 
 * @author markr
 *
 */
public abstract class Configurator<T> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(Configurator.class.getCanonicalName());

  /** the methods to invoke on the to be configured object instance and their parameters */
  protected final Map<String, Object[]> delayedMethodCalls;

  /**
   * collect the parameter types of the passed in object in their original order
   * 
   * @param parameters the parameters
   * @return parameterTypes array
   * @throws PlanItException thrown if error
   */
  protected Class<?>[] collectParameterTypes(Object... parameters) throws PlanItException {
    PlanItException.throwIf(parameters == null, "The parameters to collect signature for are null");
    Class<?>[] parameterTypes = new Class<?>[parameters.length];
    for (int index = 0; index < parameters.length; ++index) {
      parameterTypes[index] = parameters.getClass();
    }
    return parameterTypes;
  }

  /**
   * Call a void method on the toConfigure class instance
   * 
   * @param instance   to call method on
   * @param methodName to call
   * @param parameters to add to call
   * @throws IllegalAccessException    thrown if error
   * @throws IllegalArgumentException  thrown if error
   * @throws InvocationTargetException thrown if error
   * @throws PlanItException           thrown if instance or its class are unknown
   * @throws NoSuchMethodException     thrown if error
   * @throws SecurityException         thrown if error
   */
  protected void callVoidMethod(T instance, String methodName, Object... parameters)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, PlanItException, NoSuchMethodException, SecurityException {
    PlanItException.throwIf(instance == null, "The instance to configure by calling " + methodName + " is not available");
    Class<?>[] parameterTypes = collectParameterTypes(parameters);
    Method method = instance.getClass().getDeclaredMethod(methodName, parameterTypes);
    method.invoke(instance, parameters);
  }

  /**
   * Register a method call to a setter that should be invoked on the to be configured object instance once it is available
   * 
   * @param methodName the method name
   * @param parameters the parameters of the method
   */
  protected void registerDelayedSetter(String methodName, Object... parameters) {
    delayedMethodCalls.put(methodName, parameters);
  }

  /**
   * Constructor
   */
  protected Configurator() {
    this.delayedMethodCalls = new HashMap<String, Object[]>();
  }

  /**
   * Configure the passed in instance with the registered method calls
   * 
   * @param toConfigureInstance the instance to configure
   * @throws PlanItException thrown if error
   */
  public void configure(T toConfigureInstance) throws PlanItException {
    for (Map.Entry<String, Object[]> methodCall : delayedMethodCalls.entrySet()) {
      try {
        callVoidMethod(toConfigureInstance, methodCall.getKey(), methodCall.getValue());
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
        LOGGER.severe(e.getMessage());
        throw new PlanItException("could not call configurator delayed method call to " + methodCall.getKey() + " on class " + toConfigureInstance.getClass().getCanonicalName());
      }
    }
  }

}
