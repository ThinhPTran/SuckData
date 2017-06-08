(ns winglue-well.components.glvtable
  (:require [winglue-well.utils.common :as com-utils]
            [goog.string :as gstring]
            [goog.string.format]
            [winglue-well.data.subs :as datasubs]
            [winglue-well.widgets.loadingoverlay :refer [LoadingOverlay]]))

(def key-mapping
  {:meas-depth-list {:label "Meas. Depth"
                     :full-label "Measured Depth"
                     :units "ft"}
   :vert-depth-list {:label "Vert. Depth"
                     :full-label "Vertical Depth"
                     :units "ft"}
   :glv-desc-list {:label "Description"}
   :tro-list {:label "TRO Press"
              :full-label "Test Rack Opening Pressure"
              :units "psig"}
   :choke-list {:label "Choke"
                :units "1/64\""}
   :inj-press-list {:label "Inj. Press"
                    :full-label "Injection Pressure"
                    :units "psig"}
   :prod-press-list {:label "Prod. Press"
                     :full-label "Production Pressure"
                     :units "psig"}
   :temperature-list {:label "Prod. Temp"
                      :full-label "Production Temperature"
                      :units "\u00B0F"}
   :open-press-list {:label "Open Press"
                     :full-label "Opening Pressure"
                     :units "psig"}
   :close-press-list {:label "Close Press"
                      :full-label "Closing Pressure"
                      :units "psig"}
   :vpc-begin-flow-list {:label "VPC BFP"
                         :full-label "VPC Begin Flow Pressure"
                         :units "psig"}
   :surface-open-press-list {:label "Surf. Open Press"
                             :full-label "Surface Opening Pressure"
                             :units "psig"}
   :surface-close-press-list {:label "Surf. Close Press"
                              :full-label "Surface Closing Pressure"
                              :units "psig"}
   :gas-flow-rate-list {:label "Est. Rate"
                        :full-label "Estimated Gas Flow Rate"
                        :units "MCF/day"}
   :status-list {:label "Status"
                 :units ""}})

(def cols [:meas-depth-list :vert-depth-list :glv-desc-list :tro-list :choke-list :inj-press-list
           :prod-press-list :temperature-list :open-press-list :close-press-list
           :vpc-begin-flow-list :surface-open-press-list :surface-close-press-list
           :gas-flow-rate-list :status-list])

(defn- format-status [status vpc-pct-open]
  (condp = status
    :use-pct-open (str vpc-pct-open "% Open")
    :is-dummy   "Dummied"
    :back-check "Back Checked"
    :unknown    "Unknown"
    :closed     "Closed"
    :open       "Open"
    :transition "Transition"
    (name status)))

(defn- format-dec
  [num dec-places]
  (gstring/format (str "%." dec-places "f") num))

(defn- get-bottom-open-valve [valves-map]
  (reduce max (for [idx (range (count (first (vals valves-map))))
                    :let [get-cell (fn [row col] (nth (col valves-map) row))
                          status (com-utils/decode-valve-status (get-cell idx :status-list))]
                    :when (or (= status :open) (= status :transition))] idx)))

(defn GLVTable
  "Gas Lift Valves Table"
  [data-source well]
  (let [depth-profile (datasubs/get-depth-profile)
        mandrel-survey (datasubs/get-mandrel-survey)
        valves-map (:valves-status-map depth-profile)]
    (.log js/console "GLVTable!!!!!")
    (if (and (some? depth-profile)
             (some? mandrel-survey)
             (some? valves-map))
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
        (doall
          (for [idx (range (count (first (vals valves-map))))
                :let [get-cell (fn [row col] (nth (col valves-map) row))
                      ;; We assume the mandrel survey row matches the rows in the valve-map
                      get-cell-mand (fn [row col] (nth (col mandrel-survey) row))
                      dec-cell (fn [col-key dec-places] (format-dec (get-cell idx col-key) dec-places))
                      bot-idx (get-bottom-open-valve valves-map)
                      bg-color (if (= idx bot-idx) "LightGreen" "White")]]
            ^{:key idx}
            [:tr {:style {:text-align "right" :background-color bg-color}}
             ^{:key :meas-depth-list} [:td (dec-cell :meas-depth-list 1)]
             ^{:key :vert-depth-list} [:td (dec-cell :vert-depth-list 1)]
             ^{:key :glv-desc-list} [:td (get-cell-mand idx :glv-desc-list)]
             ^{:key :tro-list} [:td (let [tro-val (com-utils/decode-tro
                                                    (get-cell-mand idx :glv-category-list)
                                                    (get-cell-mand idx :tro-list))]
                                      (if (string? tro-val) tro-val (format-dec tro-val 0)))]
             ^{:key :choke-list} [:td (get-cell-mand idx :choke-list)]
             (doall (map (fn [col] ^{:key col} [:td (dec-cell col 2)])
                         [:inj-press-list :prod-press-list :temperature-list :open-press-list
                          :close-press-list :vpc-begin-flow-list]))

             ^{:key :surface-open-press-list} [:td (let [press (com-utils/decode-surf-open
                                                                 (get-cell-mand idx :glv-category-list)
                                                                 (get-cell idx :surface-open-press-list))]
                                                     (if (string? press) press (format-dec press 2)))]
             ^{:key :surface-close-press-list} [:td (let [press (com-utils/decode-surf-close
                                                                  (get-cell-mand idx :glv-category-list)
                                                                  (get-cell idx :surface-close-press-list))]
                                                      (if (string? press) press (format-dec press 2)))]
             ^{:key :gas-flow-rate-list} [:td (dec-cell :gas-flow-rate-list 3)]
             ^{:key :status} [:td (format-status
                                    (com-utils/decode-valve-status (get-cell idx :status-list))
                                    (get-cell idx :vpc-pct-open-list))]]))]]
      [LoadingOverlay])))