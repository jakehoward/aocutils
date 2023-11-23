{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns org.jakehoward.aocutils.core
  (:require [nextjournal.clerk :as clerk]
            [criterium.core :refer [quick-bench]]))


(defonce x (clerk/serve! {:watch-paths ["src"] :browse true}))
