# Planner

Development notes:

1. This planner has been tested with 3 different sets of operators but is still in "alpha" release. It has not been
packaged or name-spaced.
2. The documentation below is thin. If you are using the planner and want more documentation please open an issue.
Better still submit a PR with some improved documentation.

Planner provides a simple approach to planning based on means-end analysis/STRIPS/GPS. It uses a stack based algorithm
which is considerably more efficient than algorithms based on search. The computational cost of running the planner is
(roughly) linearly proportional to the length of the plan it produces. Before reading this guide it is (probably) a good
idea to check out [opsearch](https://github.com/cognesence/opsearch).

Planner takes the following arguments:

+ `state` - a start state
+ `goal` - a minimally specified goal state (see below)
+ `operators` - a collection of planner operators

A map is returned showing:

+ the goal state it reached
+ any commands to send to another subsystem
+ a textual description of the path

**Note:** it is possible to use planning operators without the pattern matcher but its use is then highly restricted.
For this guide we assume the pattern matcher will be used.

Goals are minimally specified so any state which is a superset of the goal is deemed a goal state.

## Operators

Planner operators are specified in a map which should associate the operator name with its definition. Planner operators
definitions describe preconditions and effects (effects in terms of deletions and additions). They also define other
attributes (see below).

Note that the operators used in the examples below are based on a tuples but you can use other representations.

Planning operators can have all of the slots in the following example which is expanded below:

```clojure
{ :name put-on
  :achieves (on ?x ?y)
  :when   ( (at ?x ?sx) (at ?y ?sy) (:guard (not= (? sx) (? sy))) )
  :post   ( (protected ?sx) (protected ?sy)
            (cleartop ?x) (cleartop ?y) (hand empty) )
  :pre ()
  :del ( (at ?x ?sx) (cleartop ?y)
         (protected ?sx) (protected ?sy) )
  :add ( (at ?x ?sy) (on ?x ?y) )
  :cmd ( (pick-from ?sx) (drop-at ?sy) )
  :txt (put ?x on ?y)
  }
```

### Explanation

```clojure
{ :name     put-on         ;; operator name
  :achieves (on ?x ?y)     ;; the goal this op can solve
  :when     ;; the conditions for considering this operator
            ;; this exmple is suitably applied when ?X and ?y
            ;; are on different stacks (?sx ?yy) – note :guard
            ( (at ?x ?sx) (at ?y ?sy)
              (:guard (not= (? sx) (? sy))) )
  :post     ;; sub-goals to be achieved before this operator is
            ;; applied. See explanation of protected later
            ( (protected ?sx) (protected ?sy)
              (cleartop ?x) (cleartop ?y) (hand empty) )
  ;; the remaining slots are the same as those used with ops-search operators
  ;; - see ops-search documentation
  :pre ()
  :del ( (at ?x ?sx) (cleartop ?y)
         (protected ?sx) (protected ?sy) )
  :add ( (at ?x ?sy) (on ?x ?y) )
  :cmd ( (pick-from ?sx) (drop-at ?sy) )
  :txt (put ?x on ?y)
  }
```

Planner operaors  are processed as follows:

```
goal <- (pop goal-stack)
match (:achieves op) goal
  match (:when op) BD
    push( expand op , goal-stack )
    push-all( expand (:post op), goal-stack )
```

## Protected Goals

To understand why it is necessary to protect goals/preconditions please read around GPS/STRIPS. To understand why/how we 
choose to implement protections as sub-goals ask SL (or attend a relevant lecture).

The implementation of protections as sub-goals requires a separate operator as follows:

```clojure
  :protect-x
  { :name protect-x
    :achieves (protected ?x ?c)
    :add  ((protected ?x ?c)  )
    }
```

## License

Copyright © 2017 Simon Lynch

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
