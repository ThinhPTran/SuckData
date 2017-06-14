(ns winglue-well.components.welltesttable
  (:require [reagent.core :as reagent]
            [winglue-well.widgets.loadingoverlay :refer [LoadingOverlay]]
            [winglue-well.data.subs :as datasubs]
            [goog.string :as gstring]
            [goog.string.format]))

(defn- format
  "Safely tries to format the given value.
   Avoids the problem of exception throwing when value is nil"
  [format value]
  (if (nil? value)
    [:span {:style {:background-color "pink"}} [:em "no data"]]
    (gstring/format format value)))

(def key-mapping
  {:calib-oil-rate {:label "Oil"
                    :units "bbl/day"}
   :calib-water-rate {:label "Water"
                      :units "bbl/day"}
   :calib-liquid-rate {:label "Total Liquid"
                       :units "bbl/day"}
   :calib-wc {:label "Water Cut"
              :units "pct"}
   :calib-total-gas {:label "Total Gas"
                     :units "MCF/day"}
   :calib-flowing-tubing-press {:label "Tubing Press."
                                :full-label "Tubing Pressure"
                                :units "psig"}
   :calib-casing-head-press {:label "Casing Press."
                             :full-label "Cassing Pressure"
                             :units "psig"}
   :est-fbhp {:label "Stored FBHP"
              :full-label "Flowing Bottom Hole Pressure"
              :units "psig"}
   :calib-lift-gas-rate {:label "Gas LG"
                         :full-label "Gas Lift Gas"
                         :units "MCF/day"}
   :calib-total-glr {:label "GLR"
                     :full-label "Gas Liquid Ratio"
                     :units "Ratio"}
   :calib-formation-gas-rate {:label "F Gas"
                              :full-label "Formation Gas"
                              :units "MCF/day"}})

(def cols [:calib-oil-rate :calib-water-rate :calib-liquid-rate :calib-wc :calib-total-gas :calib-flowing-tubing-press
           :calib-casing-head-press :est-fbhp :calib-lift-gas-rate :calib-total-glr :calib-formation-gas-rate])

(defn WellTestTable
  "Table showing the well test data"
  [well-test]
  ;(.log js/console "WellTestTable")
  [:table.table
   [:thead
    ^{:key "headers"}
    [:tr
     (doall
       (for [col cols
             :let [options (col key-mapping)]]
         (if (contains? options :full-label)
           ^{:key col} [:th {:data-toggle "tooltip" :title (:full-label options)}
                        (:label options)]
           ^{:key col} [:th (:label options)])))]
    ^{:key "units"}
    [:tr
     (for [col cols
           :let [options (col key-mapping)]]
       ^{:key col}
       [:th (:units options)])]]
   [:tbody
    [:tr
     ^{:key "oil"}[:td (format "%.2f" (:calib-oil-rate well-test))]
     ^{:key "water"}[:td (format "%.2f" (:calib-water-rate well-test))]
     ^{:key "total-fluid"}[:td (format "%.2f" (:calib-liquid-rate well-test))]
     ^{:key "watercut"}[:td (format "%.1f%%" (:calib-wc well-test))]
     ^{:key "total-gas"}[:td (format "%.2f" (:calib-total-gas well-test))]
     ^{:key "ftp"}[:td (format "%.2f" (:calib-flowing-tubing-press well-test))]
     ^{:key "chp"}[:td (format "%.2f" (:calib-casing-head-press well-test))]
     ^{:key "fbhp"}[:td (format "%.2f" (:est-fbhp well-test))]
     ^{:key "lgas"}[:td (format "%.2f" (:calib-lift-gas-rate well-test))]
     ^{:key "glr"}[:td (format "%.2f" (:calib-total-glr well-test))]
     ^{:key "fgas"}[:td (format "%.2f" (:calib-formation-gas-rate well-test))]]]])


(defn WellTestInfo []
  "Component that displays well test information"
  (let [well-test (datasubs/get-welltest)
        welltest-date (:welltest-date well-test)]
    (if (and (some? well-test) (some? welltest-date))
      [:div
       [WellTestTable well-test]
       [:h4 "Comments"]
       [:div
        (if-let [comments (:welltest-comments well-test)]
          comments
          [:i {:style {:color "gray"}} "No Comments"])]]
      [LoadingOverlay])))











