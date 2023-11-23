{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns org.jakehoward.aocutils.a-star
  (:require [clojure.string :as str]
            [nextjournal.clerk :as clerk]))

;; # A-Star

{:nextjournal.clerk/visibility {:code :show :result :hide}}
(defn min-by [f coll]
  (when (seq coll)
    (reduce (fn [min other]
              (if (> (f min) (f other))
                other
                min))
            coll)))

(defn neighbours [size yx]
  (let [deltas [[-1 0] [1 0] [0 -1] [0 1]]]
    (->> deltas
         (map #(vec (map + yx %)))
         (filter (fn [new-yx] (every? #(< -1 % size) new-yx))))))

(defn joy-heuristic [step-cost-est size y x]
  (* step-cost-est
     (- (+ size size) y x 2)))

;; estimate-cost => h(...) in the literature (presumably "heuristic")
(def estimate-cost joy-heuristic)

;; path-cost => g(...) in the literature (presumably "god, it's annoying when ppl use symbols")
(defn path-cost [node-cost cheapest-nbr cost-to-move]
  (+ node-cost cost-to-move (* cost-to-move (count (:xys cheapest-nbr [])))
     (or (:cost cheapest-nbr) 0)))

;; total-cost => f(...) in the literature
(defn total-cost [newcost step-cost-est size [y x]]
  (+ newcost
     (estimate-cost step-cost-est size y x)))

(defn a-star [start-yx step-est cell-costs cost-to-move]
  (let [size (count cell-costs)]
    (loop [steps 0
           routes (vec (repeat size (vec (repeat size nil))))
           work-todo (sorted-set [0 start-yx])]
      (if (empty? work-todo)
        [(peek (peek routes)) :steps steps]
        (let [[_ yx :as work-item] (first work-todo)
              rest-work-todo (disj work-todo work-item)
              nbr-yxs (neighbours size yx)
              cheapest-nbr (min-by :cost (keep #(get-in routes %) nbr-yxs))
              newcost (path-cost (get-in cell-costs yx) cheapest-nbr cost-to-move)
              oldcost (:cost (get-in routes yx))]
          (if (and oldcost (>= newcost oldcost))
            (recur (inc steps) routes rest-work-todo)
            (recur (inc steps)
                   (assoc-in routes yx {:cost newcost :yxs (conj (:yxs cheapest-nbr []) yx)})
                   (into rest-work-todo
                         (map (fn [w] [(total-cost newcost step-est size w) w]) nbr-yxs)))))))))

{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(defn lpad [n char s]
  (str (str/join (repeat (- n (count (str s))) char)) s))

(defn rpad [n char s]
  (str s (str/join (repeat (- n (count (str s))) char))))

(defn visualise-route [world route]
  (let [route-set (set route)
        max-len (apply max (mapcat (fn [row] (map (comp count str) row)) world))]
    (->> (for [y (range (count world)) x (range (count world))]
           (let [str-cell (get-in world [y x])]
             (if (route-set [y x])
               (lpad (inc max-len) " " (str "*" str-cell))
               (lpad (inc max-len) " " (str "" str-cell)))))
         (partition (count world))
         (map #(str/join " " %))
         (str/join "\n"))))

(defn print-viz-a-star [start-yx step-est world cost-to-move]
  (let [[result _ steps] (a-star start-yx step-est world cost-to-move)]
    (println "Steps:" steps)
    (println "Cost:" (:cost result))
    (println
     (visualise-route world (:yxs result)))))

(defn viz-a-star [start-yx step-est world cost-to-move]
  (let [[result _ steps] (a-star start-yx step-est world cost-to-move)]
    (println "Steps:" steps)
    (println "Cost:" (:cost result))
    (visualise-route world (:yxs result))))

(comment
  (let [world [[1 1 1 1 1]
               [999 999 999 999 1]
               [1 1 1 1 1]
               [1 999 999 999 999]
               [1 1 1 1 1]]]
    (print-viz-a-star [0 0] 1 world 123))
  (+ 999 124)
  (+ 9 (* 9 124))

  (let [world [[1 1 1 1 1]
               [999 999 1 999 999]
               [1 1 1 1 1]
               [999 999 1 999 999]
               [1 1 1 1 1]]]
    (print-viz-a-star [0 0] 9999999 world 9999999)))

;; ## Example usage

;; With this cost to move, the obvious route is taken (you have to expand \* signifies route)

{:nextjournal.clerk/visibility {:code :show :result :show}}
(let [step-est    1
              cost-to-move 1
              world        [[1,,,1,,,1,,,1,,,1]
                            [999 999 999 999 1]
                            [1,,,1,,,1,,,1,,,1]
                            [1,,,999 999 999 999]
                            [1,,,1,,,1,,,1,,,1]]]
           (viz-a-star [0 0] step-est world cost-to-move))

;; Tipping point, just before cost is high enough to change route
(let [step-est     1
      cost-to-move 123
      world        [[1,,,1,,,1,,,1,,,1]
                    [999 999 999 999 1]
                    [1,,,1,,,1,,,1,,,1]
                    [1,,,999 999 999 999]
                    [1,,,1,,,1,,,1,,,1]]]
  (viz-a-star [0 0] step-est world cost-to-move))

;; Tip the cost of 9 moves, with cell cost 1 and move cost 124 over the
;; cost of cell 999  and the algorithm chooses a different path
(let [step-est     1
      cost-to-move 124
      world        [[1,,,1,,,1,,,1,,,1]
                    [999 999 999 999 1]
                    [1,,,1,,,1,,,1,,,1]
                    [1,,,999 999 999 999]
                    [1,,,1,,,1,,,1,,,1]]]
  (viz-a-star [0 0] step-est world cost-to-move))
