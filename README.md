# AspectJ intra-advice precedence rule simulator

I created this little tool in order to reason about AspectJ intra-advice precedence rules, both existing and
hypothetical ones. It helps me visualise my thought experiments.

Features:
  * Simulate 3 different types of intra-aspect advice precedence rules
  * Identify cycles in the precedence graph which would lead to compiler/weaver errors
  * Perform transitive reduction on the precedence graph, if no cycles exist
  * Graphically visualise the resulting precedence graphs
  * Simulate aspect execution according to chosen precedence mode and print console log of how the aspect would behave 

See also:
  * [AspectJ issue #25 concerning precedence rule simplification](https://github.com/eclipse/org.aspectj/issues/25) 
  * [AspectJ manual section explaining precedence rules](https://www.eclipse.org/aspectj/doc/next/progguide/semantics-advice.html#d0e6257)
  * [StackOverflow answer explaining precedence rules](https://stackoverflow.com/a/40071281/1082681)
