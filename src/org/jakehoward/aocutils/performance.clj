{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns org.jakehoward.aocutils.performance
  (:require [nextjournal.clerk :as clerk]
            [criterium.core :refer [quick-bench]]))

;; # Performance
;;
;; ⚠️ Performance testing is hard. Clojure has many traps when it comes
;;    to misinterpreting performance results. This is simplistic but
;;    it does at least use criterium and I attempt to remember to (doall ...)
;;    my lazy sequences. Take the results with a pinch of salt.
;;
;; Advent of code problems sometimes require a solution that's more
;; sophisticated than the brute force approach. In these cases, even
;; running over a subset of the data, a solution can run quite slowly.
;; In an ideal world, your solution would be so elegant that performance
;; concerns at the language level wouldn't make a difference. In practice,
;; a feedback loop can go from a few hundred milliseconds to multiple seconds
;; when there's a slow operation in a tight loop. That can really mess up
;; your flow!

;; Let's see what the peformance impact is with some (mostly) equivalent
;; ways of doing things in Clojure.

;; ## Updating a map
;; ### 1. The "normal" way using assoc
{:nextjournal.clerk/visibility {:code :show :result :hide}}
(comment
  (quick-bench
      (loop [n 0
             m {}]
        (if (< n 10000)
          (recur (inc n) (assoc m :n n))
          m))))

;; - => 233 µs to 400 µs
;; - => verdict: baseline!

;; ### 2. Using a transient map
{:nextjournal.clerk/visibility {:code :show :result :hide}}
(comment
  (quick-bench
      (loop [n 0
             m (transient {})]
        (if (< n 10000)
          (recur (inc n) (assoc! m :n n))
          (persistent! m)))))

;; - => 80 µs to 122 µs
;; - => verdict: good speedup

;; ### 3. Using a Java HashMap
(comment
  (quick-bench
      (loop [n 0
             m (java.util.HashMap.)]
        (if (< n 10000)
          (recur (inc n) (do (.put m :n n) m))
          m))))

;; - => 61 µs to 105 µs
;; - => verdict: annoying enough that put doesn't return it's not worth it over transient


;; ## Iterating over sequences
;; Some problems are well modelled by sequences of numbers. Let's
;; look at a few ways of doing it

;; ### 1. Unrealised lazy-seq
(comment
  (let [my-seq (range 10000)]
    (quick-bench
        (doall (filter even? my-seq)))))
;; => 214 µs to 257 µs

;; ### 2. Realised lazy-seq
(comment
  (let [my-seq (doall (range 10000))]
    (quick-bench
        (doall (filter even? my-seq)))))
;; - => 219 µs to 296 µs
;; - => did I do the test right?

;; ### 3. A vector, normal filter
(comment
  (let [my-seq (vec (range 10000))]
    (quick-bench
        (doall (filter even? my-seq)))))
;; - => 217 µs to 223 µs


;; ### 4. A vector, filterv
(comment
  (let [my-seq (vec (range 10000))]
    (quick-bench
        (filterv even? my-seq))))
;; - =>  157 µs to 160 µs


;; - =>  µs to  µs

