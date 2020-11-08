# AspectJ intra-advice precedence rule simulator

I created this little tool in order to reason about AspectJ intra-advice precedence rules, both existing and
hypothetical ones. It helps me visualise my thought experiments.

Features:
  * Simulate 3 different types of intra-aspect advice precedence rules
  * Identify cycles in the precedence graph which would lead to compiler/weaver errors
  * Perform transitive reduction on the precedence graph
  * Graphically visualise the resulting precedence graphs
  * Simulate aspect execution according to chosen precedence mode and print console log of how the aspect would behave
  * Optionally export graphs to CSV files (TODO: import from CSV) 

See also:
  * [AspectJ issue #25 concerning precedence rule simplification](https://github.com/eclipse/org.aspectj/issues/25) 
  * [AspectJ manual section explaining precedence rules](https://www.eclipse.org/aspectj/doc/next/progguide/semantics-advice.html#d0e6257)
  * [StackOverflow answer explaining precedence rules](https://stackoverflow.com/a/40071281/1082681)

The program output can look like this with the 3 sample aspects I have put into
[`AspectJAdvicePrecedenceSimulator.main`](https://github.com/kriegaex/AJ_AdvicePrecedenceGraph/blob/master/src/main/java/de/scrum_master/aspectj/graph/AspectJAdvicePrecedenceSimulator.java)
for you to play with, each of them simulated in 3 alternative precedence rule modes.
```
Advice precedence mode = ASPECTJ_CLASSIC
Original graph = ([after-1, around-2, around-3, before-4, before-5, after-6], [=(around-2,after-1), =(around-3,after-1), =(before-4,after-1), =(before-5,after-1), =(after-6,after-1), =(around-2,around-3), =(around-2,before-4), =(around-2,before-5), =(after-6,around-2), =(around-3,before-4), =(around-3,before-5), =(after-6,around-3), =(before-4,before-5), =(after-6,before-4), =(after-6,before-5)])
Transitively reduced graph = ([after-1, around-2, around-3, before-4, before-5, after-6], [=(before-5,after-1), =(around-2,around-3), =(after-6,around-2), =(around-3,before-4), =(before-4,before-5)])

after-6 → 
· around-2 → pre-action (can change arguments)
· · around-3 → pre-action (can change arguments)
· · · before-4 → pre-action
· · · · before-5 → pre-action
· · · · · after-1 → 
· · · · · · JOINPOINT
· · · · · after-1 → post-action
· · · · before-5 → 
· · · before-4 → 
· · around-3 → post-action (can change return value)
· around-2 → post-action (can change return value)
after-6 → post-action
--------------------------------------------------------------------------------
Advice precedence mode = ASPECTJ_CLASSIC
Original graph = ([around-1, after-2, around-3, before-4, before-5, after-6], [=(after-2,around-1), =(around-1,around-3), =(around-1,before-4), =(around-1,before-5), =(after-6,around-1), =(around-3,after-2), =(before-4,after-2), =(before-5,after-2), =(after-6,after-2), =(around-3,before-4), =(around-3,before-5), =(after-6,around-3), =(before-4,before-5), =(after-6,before-4), =(after-6,before-5)])
Transitively reduced graph = ([around-1, after-2, around-3, before-4, before-5, after-6], [=(after-6,after-2), =(after-2,around-1), =(around-1,around-3), =(around-3,before-4), =(before-4,before-5), =(before-5,after-2)])
Precedence graph contains cycles, cannot simulate advice execution
--------------------------------------------------------------------------------
Advice precedence mode = ASPECTJ_CLASSIC
Original graph = ([before-1, after-2, before-3], [=(after-2,before-1), =(before-1,before-3), =(before-3,after-2)])
Transitively reduced graph = ([before-1, after-2, before-3], [=(after-2,before-1), =(before-1,before-3), =(before-3,after-2)])
Precedence graph contains cycles, cannot simulate advice execution
--------------------------------------------------------------------------------
Advice precedence mode = ASPECTJ_WITH_BEFORE_ALWAYS_PRECEDING_AFTER
Original graph = ([after-1, around-2, around-3, before-4, before-5, after-6], [=(around-2,after-1), =(around-3,after-1), =(before-4,after-1), =(before-5,after-1), =(after-6,after-1), =(around-2,around-3), =(around-2,before-4), =(around-2,before-5), =(after-6,around-2), =(around-3,before-4), =(around-3,before-5), =(after-6,around-3), =(before-4,before-5), =(before-4,after-6), =(before-5,after-6)])
Transitively reduced graph = ([after-1, around-2, around-3, before-4, before-5, after-6], [=(after-6,after-1), =(after-6,around-2), =(around-2,around-3), =(around-3,before-4), =(before-4,before-5), =(before-5,after-6)])
Precedence graph contains cycles, cannot simulate advice execution
--------------------------------------------------------------------------------
Advice precedence mode = ASPECTJ_WITH_BEFORE_ALWAYS_PRECEDING_AFTER
Original graph = ([around-1, after-2, around-3, before-4, before-5, after-6], [=(after-2,around-1), =(around-1,around-3), =(around-1,before-4), =(around-1,before-5), =(after-6,around-1), =(around-3,after-2), =(before-4,after-2), =(before-5,after-2), =(after-6,after-2), =(around-3,before-4), =(around-3,before-5), =(after-6,around-3), =(before-4,before-5), =(before-4,after-6), =(before-5,after-6)])
Transitively reduced graph = ([around-1, after-2, around-3, before-4, before-5, after-6], [=(after-2,around-1), =(around-1,around-3), =(around-3,before-4), =(before-4,before-5), =(before-5,after-6), =(after-6,after-2)])
Precedence graph contains cycles, cannot simulate advice execution
--------------------------------------------------------------------------------
Advice precedence mode = ASPECTJ_WITH_BEFORE_ALWAYS_PRECEDING_AFTER
Original graph = ([before-1, after-2, before-3], [=(before-1,after-2), =(before-1,before-3), =(before-3,after-2)])
Transitively reduced graph = ([before-1, after-2, before-3], [=(before-1,before-3), =(before-3,after-2)])

before-1 → pre-action
· before-3 → pre-action
· · after-2 → 
· · · JOINPOINT
· · after-2 → post-action
· before-3 → 
before-1 → 
--------------------------------------------------------------------------------
Advice precedence mode = CHRONOLOGICAL
Original graph = ([after-1, around-2, around-3, before-4, before-5, after-6], [=(after-1,around-2), =(after-1,around-3), =(after-1,before-4), =(after-1,before-5), =(after-1,after-6), =(around-2,around-3), =(around-2,before-4), =(around-2,before-5), =(around-2,after-6), =(around-3,before-4), =(around-3,before-5), =(around-3,after-6), =(before-4,before-5), =(before-4,after-6), =(before-5,after-6)])
Transitively reduced graph = ([after-1, around-2, around-3, before-4, before-5, after-6], [=(after-1,around-2), =(around-2,around-3), =(around-3,before-4), =(before-4,before-5), =(before-5,after-6)])

after-1 → 
· around-2 → pre-action (can change arguments)
· · around-3 → pre-action (can change arguments)
· · · before-4 → pre-action
· · · · before-5 → pre-action
· · · · · after-6 → 
· · · · · · JOINPOINT
· · · · · after-6 → post-action
· · · · before-5 → 
· · · before-4 → 
· · around-3 → post-action (can change return value)
· around-2 → post-action (can change return value)
after-1 → post-action
--------------------------------------------------------------------------------
Advice precedence mode = CHRONOLOGICAL
Original graph = ([around-1, after-2, around-3, before-4, before-5, after-6], [=(around-1,after-2), =(around-1,around-3), =(around-1,before-4), =(around-1,before-5), =(around-1,after-6), =(after-2,around-3), =(after-2,before-4), =(after-2,before-5), =(after-2,after-6), =(around-3,before-4), =(around-3,before-5), =(around-3,after-6), =(before-4,before-5), =(before-4,after-6), =(before-5,after-6)])
Transitively reduced graph = ([around-1, after-2, around-3, before-4, before-5, after-6], [=(around-1,after-2), =(after-2,around-3), =(around-3,before-4), =(before-4,before-5), =(before-5,after-6)])

around-1 → pre-action (can change arguments)
· after-2 → 
· · around-3 → pre-action (can change arguments)
· · · before-4 → pre-action
· · · · before-5 → pre-action
· · · · · after-6 → 
· · · · · · JOINPOINT
· · · · · after-6 → post-action
· · · · before-5 → 
· · · before-4 → 
· · around-3 → post-action (can change return value)
· after-2 → post-action
around-1 → post-action (can change return value)
--------------------------------------------------------------------------------
Advice precedence mode = CHRONOLOGICAL
Original graph = ([before-1, after-2, before-3], [=(before-1,after-2), =(before-1,before-3), =(after-2,before-3)])
Transitively reduced graph = ([before-1, after-2, before-3], [=(before-1,after-2), =(after-2,before-3)])

before-1 → pre-action
· after-2 → 
· · before-3 → pre-action
· · · JOINPOINT
· · before-3 → 
· after-2 → post-action
before-1 → 
--------------------------------------------------------------------------------
```
