(ns winglue-well.widgets.highchart
  (:require [reagent.core :as reagent]
            [cljsjs.highcharts]
            [winglue-well.utils.merge :as merge-utils]))

(def default-chart-config
  "The base default options we apply to HighChart in our application.
   We apply this to normalize the style of highcharts throughout our application.
   Any of the options can be overriden by the chart-config parameter passed to the
   HighChart component."
  {:chart {:height 300}
   :credits {:enabled false}
   :legend  {:layout        "horizontal"
             :align         "center"
             :verticalAlign "bottom"
             :borderWidth   0}
   :yAxis {:labels {:format "{value}"}}
   :xAxis {:gridLineWidth 1
           :labels {:format "{value}"}}})

(defn prep-chart-config
  "This is a utility function to merge individual chart configs from the appdb and
   the provided curves, and structure in the format HighCharts expects. Each curve is a
   {:curve-key data} map"
  [config & curves]
  ;; First define some helper fns
  (let [get-curve-series (fn [config curve-key]
                           (get-in config [:series curve-key] {}))]
    ;; Our recursion loop to process the curves and config
    (loop [working-config (assoc config :series [])
           curves curves]
      (let [curve (first curves)
            key (first (keys curve))]
        ;; Done if no more curves left to process
        (if (some? curve)
          ;; Update the working-config with the new curve and repeat process with rest
          ;; of curves
          (recur (update-in working-config [:series] #(conj %1 %2)
                            (merge (get-curve-series config key) {:data (get curve key)}))
                 (rest curves))
          working-config)))))

(defn HighChart [chart-config]
  (let [chart (atom {:chart nil})]
    [:div
     {:style {:height "100%" :width "100%" :position "relativ"}
      :ref (fn [mydiv]
             (if (some? mydiv)
               (swap! chart assoc :chart (js/Highcharts.Chart. mydiv (clj->js (merge-utils/deep-merge default-chart-config chart-config))))
               (let [mychart (:chart @chart)]
                 (if (some? mychart)
                   (.destroy mychart)
                   (swap! chart assoc :chart nil)))))}]))


