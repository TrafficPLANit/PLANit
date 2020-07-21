package org.planit.configurator;

import java.util.Optional;

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
public class Configurator {

  // oneline to obtain the current method name --> use to call method that should be called as delayed method call
  Optional<String> methodName = StackWalker.getInstance().walk(frames -> frames.findFirst().map(StackWalker.StackFrame::getMethodName));

}
