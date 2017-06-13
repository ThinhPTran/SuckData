(ns winglue-well.components.tubingpipe
  (:require [reagent.core :as reagent]
            [winglue-well.utils.table :as table-utils]
            [cljsjs.d3]
            [clojure.set :refer [rename-keys]]
            [winglue-well.data.subs :as datasubs]
            [winglue-well.subs :as subs]
            [winglue-well.utils.common :as common-utils]))

(def default-options
  {:height 645
   :width 130
   :max-depth 10000
   :valves []
   :regimes []
   :animations-enabled true})

(defn- fixup-valve-map
  [valve-map]
  (-> valve-map
      (select-keys [:vert-depth-list :vpc-pct-open-list :status-list])
      (rename-keys {:vert-depth-list :depth, :vpc-pct-open-list :pct-open, :status-list :status})
      (update-in [:status] common-utils/decode-valve-status)))

(defn- get-valves-from-dp
  [depth-profile]
  (let [v-table (common-utils/parse-tao2-table (:valves-status-map depth-profile))]
    (vec (map fixup-valve-map v-table))))

;; The server currently gives us flow regimes that may have identical consecutive flow regimes
;; e.g. [20 bubble] [50 bubble] [60 bubble]
;; Use this to fix
(defn- consolidate-regimes
  [regimes]
  (loop [r regimes
         fr []]
    (let [curr-regime (second (first r))
          [identical-regimes rest-r] (split-with #(= curr-regime (second %)) r)
          fixed-regime (last identical-regimes)
          fixed-list (apply conj fr [fixed-regime])]
      (if (empty? rest-r)
        fixed-list
        (recur rest-r fixed-list)))))

(defn- get-flow-regimes-from-dp
  [depth-profile]
  (let [production-string-map (:production-string-depth-profile-map depth-profile)]
    (vec (for [idx (range (count (:vert-depth-list production-string-map)))
               :let [depth (nth (:vert-depth-list production-string-map) idx)
                     flow-regime (common-utils/decode-flow-regime (nth (:regime-list production-string-map) idx))]]
           [depth flow-regime]))))

(defn TubingPipe [data-source well input-options]
  (let [depth-profile (datasubs/get-depth-profile)
        window-focused? (subs/get-window-focus)
        options (clj->js (merge default-options
                                {:animations-enabled true}
                                {:valves (get-valves-from-dp depth-profile)}
                                {:regimes (consolidate-regimes (get-flow-regimes-from-dp depth-profile))}
                                input-options))]
    ;(.log js/console "TubingPipe!!!")
    [:div
     {:ref (fn [mydiv]
             (when (some? mydiv)
               (js/tubingpipe mydiv options)))}]))
               ;(.log js/console (str "nodiv"))))}]))



