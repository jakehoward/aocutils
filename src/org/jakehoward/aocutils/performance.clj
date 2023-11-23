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
  (let [my-seq (map identity (range 10000))]
    (quick-bench
     (doall (filter even? my-seq)))))
;; => 178 µs to 225 µs

;; ### 2. Realised lazy-seq
(comment
  (let [my-seq (doall (map identity (range 10000)))]
    (quick-bench
     (doall (filter even? my-seq)))))
;; - => 178 µs to 219 µs
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

;; ### 5. A vector, normal map
(comment
  (let [my-seq (vec (range 10000))]
    (quick-bench
     (doall (map #(* % %) my-seq)))))
;; - => 262 µs to 564 µs

;; ### 6. A vector, mapv
(comment
  (let [my-seq (vec (range 10000))]
    (quick-bench
     (mapv #(* % %) my-seq))))
;; - =>  182 µs to 190 µs

;; ### 6.1. A Java array
(comment
  (let [my-seq (int-array (range 10000))]
    (quick-bench
     (doall (map inc my-seq)))))
;; - =>  822 µs to 1,116 µs

{:nextjournal.clerk/visibility {:code :show :result :show}}
;; ### 6.2. A Java array (bash in place)
(let [xs (int-array (range 5))]
  (dotimes [idx (alength xs)]
    (aset xs idx (inc (aget xs idx))))
  (seq xs))

(comment
  (let [my-seq ^"[I" (int-array (range 10000))]
    (quick-bench
     (dotimes [idx (alength my-seq)]
       (aset-int my-seq idx (inc (aget my-seq idx)))))))
;; - =>  314 µs to 459 µs
;; - => am I doing something wrong? Expected this to be fast... => yes! use aset not aset-int

(comment
  (do
    (set! *warn-on-reflection* true)
    (let [my-seq (long-array (range 10000))
          len    (alength my-seq)]
      (quick-bench
       (loop [idx 0]
         (when (< idx len)
           (do
             (aset my-seq idx (unchecked-add-int 1 (aget my-seq idx)))
             (recur (unchecked-add-int idx 1)))))))
    (set! *warn-on-reflection* false)))
;; - => 7 µs to 7.4 µs
;; - => Finally! So, unless you're measuring and being careful, not faster
;; - => If you are being careful sooooo much faster!

{:nextjournal.clerk/visibility {:code :show :result :hide}}
;; ### 7. A vector, mapv and filterv (to compare to transduce)
(comment
  (let [my-seq (vec (range 10000))]
    (quick-bench
     (->> my-seq
          (mapv #(* % %))
          (filterv even?)))))
;; - =>  335 µs to 353 µs

;; ## Transducers
;; ### 8. A vector, map and filter (transducer)
(comment
  (let [my-seq (vec (range 10000))
        xf     (comp
                (map #(* % %))
                (filter even?))]
    (quick-bench
     (into [] xf my-seq))))
;; - =>  282 µs to 306 µs

;; ### 9. A vector, map and filter (swap order) (transducer)
(comment
  (let [my-seq (vec (range 10000))
        xf     (comp
                (filter even?)
                (map #(* % %)))]
    (quick-bench
     (into [] xf my-seq))))
;; - =>  239 µs to 259 µs

;; ## Reduced
;; ### 10. short circuiting a reduce
(comment
  (let [my-seq (vec (range 10000))
        f      (fn [acc item] (if (< item 1000) (+ acc item) (reduced :big)))]
    (quick-bench
     (reduce f my-seq)))) ;; (returns :big)
;; - =>  15 µs to 21 µs

;; ## TODO
;; - Investigate `(set! *warn-on-reflection* true)`
