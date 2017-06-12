(ns winglue-well.db
  (:require [reagent.core :as reagent]
            [winglue-well.utils.format :as rformat]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Vars
(defn series-tooltip-formatter
  [x-text y-text x-units y-units]
  (fn []
    (let [js-point (js* "this")
          y (rformat/format-dec (aget js-point "y") 2)
          x (rformat/format-dec (aget js-point "x") 2)
          color (aget js-point "series" "color")]
      (str "<span style=\"color:" color "\">\u25CF</span>" x-text ": <b>" x "</b> " x-units "<br/>"
           "<span style=\"color:" color "\">\u25CF</span>" y-text ": <b>" y "</b> " y-units))))

(defn line-tooltip
  [x-text y-text x-units y-units]
  {:headerFormat
   ;; Makes the date appear in the tooltip
                   "<span style=\"font-size: 10px\">{series.name}</span><br/>"
   :pointFormatter (series-tooltip-formatter x-text y-text x-units y-units)})

(defn scatter-tooltip
  [x-text y-text x-units y-units]
  {:headerFormat
   ;; Makes the date appear in the tooltip
                   "<span style=\"font-size: 10px\">
                   {series.name}<br/>
                   {point.key}<br/>
                   </span>"
   :pointFormatter (series-tooltip-formatter x-text y-text x-units y-units)})

(def dvsp-tooltip
  (line-tooltip "Pressure" "Depth" "psig" "ft"))

(def dvsp-scatter-tooltip
  ;; Don't want point.key in these
  {:headerFormat
   ;; Makes the date appear in the tooltip
                   "<span style=\"font-size: 10px\">
                   {series.name}<br/>
                   </span>"
   :pointFormatter (series-tooltip-formatter "Presure" "Depth" "psig" "ft")})

(def pvsq-tooltip
  (line-tooltip "Liquid Rate" "Pressure" "bbl/day" "psig"))

(def pvsq-scatter-tooltip
  (scatter-tooltip "Liquid Rate" "Pressure" "bbl/day" "psig"))

(def qvsi-tooltip
  (line-tooltip "Injection" "Production" "MCF/day" "bbl/day"))

(def qvsi-scatter-tooltip
  (scatter-tooltip "Injection" "Production" "MCF/day" "bbl/day"))

(defonce local-app-state
         (reagent/atom
           {:user nil
            :group nil
            :input-text ""
            :open-well-selector false
            :network {:connected true}
            :chart/by-id {:dvsp {:id     "dvsp"
                                 :title  {:text "Depth vs. Pressure"}
                                 :xAxis  {:title {:text "Pressure (psig)"}
                                          :opposite true ;; Draw on top of graph
                                          :reversed false}
                                 :yAxis  {:title {:text "Depth (ft)"}
                                          :reversed  true}
                                 :series {:ppm
                                          {:name  "Production Pres."
                                           :color "#008B8B"
                                           :tooltip dvsp-tooltip
                                           :marker {:enabled false}}
                                          :ipm
                                          {:name "Injection Pres."
                                           :color "#EE0000"
                                           :tooltip dvsp-tooltip
                                           :marker {:enabled false}}
                                          :eq
                                          {:name "Equilibrium"
                                           :tooltip dvsp-tooltip
                                           :marker {:enabled false}}
                                          :open-points
                                          {:name "Opening Points"
                                           :color "#00FF00"
                                           :type "scatter"
                                           :tooltip dvsp-scatter-tooltip}
                                          :close-points
                                          {:name "Closing Points"
                                           :color "#FF0000"
                                           :type "scatter"
                                           :tooltip dvsp-scatter-tooltip}
                                          :vpc-bfp
                                          {:name "VPC BFP Points"
                                           :color "#0000FF"
                                           :type "scatter"
                                           :tooltip dvsp-scatter-tooltip}}}
                          :pvsq {:id "pvsq"
                                 :title  {:text "Pressure vs Production"}
                                 :xAxis  {:title {:text "Liquid Rate (bbl/day)"}
                                          :reversed false}
                                 :yAxis  {:title {:text "Pressure (psig)"}
                                          :reversed false}
                                 :series {:meas_outflow_liquid_rate
                                          {:name "Measured Outflow - Gross Liquid"
                                           :color  "#00008B"
                                           :tooltip pvsq-tooltip
                                           :marker {:enabled false}}
                                          :meas_inflow_liquid_rate
                                          {:name  "Measured IPR - Gross Liquid"
                                           :color "#FF00FF"
                                           :tooltip pvsq-tooltip
                                           :marker {:enabled false}}
                                          :well-tests
                                          {:name "Well Test Points"
                                           :type "scatter"
                                           :color  "#00ff00"
                                           :xAxis 0
                                           :yAxis 0
                                           :tooltip pvsq-scatter-tooltip}}}
                                          ;:uncal-well-tests
                                          ;{:name "Well Test Points (Uncalibrated)"
                                          ; :type "scatter"
                                          ; :color  "#ff0000"
                                          ; :xAxis 0
                                          ; :yAxis 0
                                          ; :tooltip pvsq-scatter-tooltip}}}

                          :qvsi {:id "qvsi"
                                 :title  {:text "Production vs Injection"}
                                 :xAxis  {:title {:text "Injection (MCF/day)"}
                                          :reversed false}
                                 :yAxis  {:title {:text "Production (bbl/day)"}
                                          :reversed false}
                                 :series {:lg-resp
                                          {:name "Oil Rate Response"
                                           :lineWidth 3
                                           :tooltip qvsi-tooltip
                                           :marker {:enabled false}}
                                          ;:uncal-wellotests
                                          ;{:name "Well Test Oil Points (Uncalibrated)"
                                          ; :type "scatter"
                                          ; :color  "#ff0000"
                                          ; :tooltip qvsi-scatter-tooltip}
                                          :wellotests
                                          {:name "Well Test Oil Points (Calibrated)"
                                           :type "scatter"
                                           :color  "#00ff00"
                                           :tooltip qvsi-scatter-tooltip}}}}}))

(defonce well-state (reagent/atom {}))
(defonce field-state (reagent/atom {}))