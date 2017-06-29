(ns ^{:doc "A stack-based planning algorithm in Clojure for applying STRIPS-style operators."
     :author "Simon Lynch"}
 org.clojars.cognesence.planner.core)


;===================================================
; based on: strips-search-1a.clj from SHRDLU model
; naming changes only
;===================================================


;these operators can have all of these slots...
;{ :name put-on
;  :achieves (on ?x ?y)
;  :when   ( (at ?x ?sx) (at ?y ?sy) (:guard (not= (? sx) (? sy))) )
;  :post   ( (protected ?sx) (protected ?sy)
;            (cleartop ?x)
;            (cleartop ?y)
;            (hand empty) )
;  :pre ()
;  :del ( (at ?x ?sx)
;         (cleartop ?y)
;         (protected ?sx)
;         (protected ?sy) )
;  :add ( (at ?x ?sy)
;         (on ?x ?y) )
;  :cmd ( (pick-from ?sx)
;         (drop-at ?sy) )
;  :txt (put ?x on ?y)
;  }
;
;NB: in this example the ops have unique :achieves + :when
;
;They are processed as follows...
;
;goal <- (pop goal-stack)
;match (:achieves op) goal
;  match (:when op) BD
;    push( expand op , goal-stack )
;    push-all( expand (:post op), goal-stack )




(defn print-goals [q]
  (if (not (empty? q))
    (do
      (ui-out :dbg "GOALS:")
      (doseq [x q]
        (ui-out :dbg "      " (if (map? x) [(:name x) :=> (:achieves x)] x))
        )
      ;(ui-out :dbg '------)
      )
    ))


(def goalq (atom (java.util.concurrent.LinkedBlockingDeque.)))


(declare strips-loop update-path
  goal-mop-apply apply-goal-op)

(defn planner [state goal goal-ops]
  (.clear @goalq)
  (.push @goalq goal)
  (strips-loop {:state state, :cmds nil, :txt nil} goal-ops 60))


(defn strips-loop
  [path goal-ops limit]
  (if (zero? limit)
    (throw (new RuntimeException "limit exceeded in run-goal-ops")))

  ;(println path)
  (print-goals @goalq)

  (if-let [goal (.poll @goalq)]
    (cond
      (map? goal) ;; it is a partially matched op
      (do
        (ui-out :dbg '** 'APPLYING (:name goal) '=> (:achieves goal))
       ; (ui-out :dbg '** (:add goal))
        (recur
          (update-path path (goal-mop-apply (:state path) goal))
          goal-ops (dec limit))
        )

      ;; else it is a fact
      (not (contains? (:state path) goal))
      (do (ui-out :dbg 'solving goal)
        (some (partial apply-goal-op (:state path) goal)
              (vals goal-ops))
        (recur path goal-ops (dec limit))
        )
      ;; else it is an existing fact
      :else
      (recur path goal-ops (dec limit))
      )
    path
    )
  )


(defn goal-mop-apply [bd mop]
  (mfind* [(:pre mop) bd]
    (ui-out :dbg '** (mout (:add mop)))
   ; (ui-out :dbg '=> (mout mop))
    {:state (union (mout (:add mop))
              (difference bd (mout (:del mop))))
     :cmd   (mout (:cmd mop))
     :txt   (mout (:txt mop))
     }
    ))


(defn apply-goal-op [bd goal op]
  ;(println (list 'trying (:name op)))
  (mlet [(:achieves op) goal]

    (mfind* [(:when op) bd]
      (ui-out :dbg 'using=> (:name op))
      (let [mop (mout op)]
        ;(println (list 'new-mop mop))
        (.push @goalq mop)
        (ui-out :dbg 'new-goals (or (:post mop) '-none))
        (doseq [p (reverse (:post mop))]
          (.push @goalq p))

        ;(println (list 'succeeded (:name op)))
        true
        ))
    ))


(defn update-path
  [current newp]
  { :state (:state newp),
    :cmds  (concat (:cmds current) (:cmd newp)),
    :txt   (concat (:txt current) (:txt newp))
    })




