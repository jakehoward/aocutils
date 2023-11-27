(ns org.jakehoward.aocutils.trees
  (:require [nextjournal.clerk :as clerk]
            [clojure.walk]
            [clojure.string :as str]))

{:nextjournal.clerk/visibility {:code :show :result :hide}}
(def visit-order (atom 0))
(defn leaf? [n] (boolean (not (sequential? n))))

(def tree [:leaf
           [:leaf
            :leaf
            [:leaf :leaf]
            [:leaf]]
           :leaf])

(defn add-visit-order [n]
  (if (leaf? n)
    (-> n name (str "-" (swap! visit-order inc)) keyword)
    n))

{:nextjournal.clerk/visibility {:code :show :result :show}}
;; prewalk and postwalk both do a depth first search of the tree
(do (reset! visit-order 0)
    (clojure.walk/postwalk add-visit-order tree))
(do (reset! visit-order 0)
    (clojure.walk/prewalk add-visit-order tree))

;; the difference is that pre-walk can change the node before the walk
;; because it walks the result of calling f on the node
{:nextjournal.clerk/visibility {:code :show :result :hide}}
(defn add-a-leaf [ls] (conj ls :extra-leaf))

{:nextjournal.clerk/visibility {:code :show :result :show}}
(do (reset! visit-order 0)
    (clojure.walk/prewalk (fn [n] (if (leaf? n)
                                    (-> n name (str "-" (swap! visit-order inc)) keyword)
                                    (if (= [:leaf :leaf] n)
                                      (add-a-leaf n)
                                      n)))
                          tree))

(do (reset! visit-order 0)
    (clojure.walk/postwalk (fn [n] (if (leaf? n)
                                     (-> n name (str "-" (swap! visit-order inc)) keyword)
                                     (if (= [:leaf :leaf] n)
                                       (add-a-leaf n)
                                       n)))
                           tree))

;; ## Trimming a tree
(def tree [:A :B [:trim-> :this] :C [:D :E]])
(clojure.walk/postwalk (fn [n] (if (= n [:trim-> :this]) [:trim->] n)) tree)
;; Not really doing anything to the leaf nodes, so prewalk is fine too
(clojure.walk/prewalk (fn [n] (if (= n [:trim-> :this]) [:trim->] n)) tree)

;; ## Expanding a tree
(def tree [:A :B [:want :sibling->] :C [:D :E]])
(clojure.walk/postwalk (fn [n] (if (= n [:want :sibling->]) (conj n :here) n)) tree)
;; Not really doing anything to the leaf nodes, so prewalk is fine too
(clojure.walk/prewalk (fn [n] (if (= n [:want :sibling->]) (conj n :here) n)) tree)

(comment
  (def a 1)
  (def b 2)
  (def c 3)
  (def tree '(+ a (+ b c)))
  (->
   (clojure.walk/prewalk (fn [n] (if (leaf? n) (eval n) n)) tree)
   eval))

(def tree-2 {:name :A
           :children [{:name :B
                       :children [{:name :C}]}
                      {:name :D}]})

(clojure.walk/prewalk (fn [n] (when (map? n) (println (:name n))) n) tree-2)
;; :A
;; :B
;; :C
;; :D
