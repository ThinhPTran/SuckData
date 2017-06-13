(ns winglue-well.pages.welloverview.welloverview
  (:require [reagent.core :as reagent]
            [clojure.set :refer [rename-keys]]
            [winglue-well.pages.welloverview.subs :as overviewsubs]
            [winglue-well.pages.welloverview.handlers :as overviewhandlers]
            [winglue-well.widgets.box :refer [BoxContainer]]
            [winglue-well.widgets.datatable :refer [DataTable]]
            [winglue-well.widgets.loadingoverlay :refer [LoadingOverlay]]
            [winglue-well.components.welltesttable :refer [WellTestInfo]]
            [winglue-well.components.tubingpipe :refer [TubingPipe]]
            [winglue-well.components.glvtable :refer [GLVTable]]
            [winglue-well.widgets.dropdownmenu :refer [DropdownMenuWithBlank]]
            [winglue-well.widgets.highchart :as highchart :refer [HighChart]]
            [winglue-well.utils.format :as rformat]
            [winglue-well.utils.merge :as merge-utils]
            [winglue-well.utils.table :as table-utils]
            [winglue-well.utils.interpolator :as i-util]
            [winglue-well.utils.common :as com-utils]
            [winglue-well.data.subs :as datasubs]
            [winglue-well.db :as mydb]))


(defn ContentMsg
  "Simple component to display a message to the user on the WellOverview page in place of the content"
  [msg]
  [:div {:style {:display "flex"
                 :align-items "center"
                 :justify-content "center"}}
   [:p {:style {:font-size "3rem"}} msg]])

(defn WellSelector [data-source on-select-fn]
  (let [well-list (overviewsubs/get-all-well)]
    [:div
     (if (some? well-list)
       [DataTable
        {:data well-list
         :columns [{:title "Field"
                    :data :field}
                   {:title "Lease"
                    :data :lease}
                   {:title "Well"
                    :data :well}
                   {:title "Completion"
                    :searchable false
                    :data :cmpl}]
         :deferRender true
         :select "single"}
        {:select (fn [e dt type index]
                   (on-select-fn (-> (.rows dt index)
                                     (.data)
                                     (aget 0)
                                     (js->clj)
                                     (rename-keys {"field" :field
                                                   "lease" :lease
                                                   "well" :well
                                                   "cmpl" :cmpl}))))}]
       [LoadingOverlay])]))

(defn DVSPChart [data-source well dvsp-config]
  (let [depth-profile (datasubs/get-depth-profile)
        equilibrium-profile (datasubs/get-equilibrium-profile)
        ppm-curve (:production-string-depth-profile-map depth-profile)
        ipm-curve (:injection-string-depth-profile-map depth-profile)
        valve-map (:valves-status-map depth-profile)
        mandrel-survey (datasubs/get-mandrel-survey)
        vert-depth-list (:vert-depth-list ppm-curve)
        max-depth (* (+ (int (/ (apply max vert-depth-list) 1000)) 1) 1000)
        plot-lines (vec (for [mandrel (:vert-depth-list valve-map)]
                          {:color "#000000"
                           :value mandrel
                           :marker {:enabled false}
                           :zIndex 1 ;; Show above gridlines
                           :width 2
                           :label {:text (str "Mandrel Line: " (rformat/format-dec mandrel 2) " ft")}
                           :tooltip {:headerFormat
                                                     (str "<span style=\"font-size: 10px\">Mandrel</span><br/>")
                                     :pointFormatter com-utils/mandrel-point-formatter}}))
        filtered-valve-map (com-utils/filter-dummy-valves-from-valve-map valve-map)
        chart-config (-> (merge-utils/deep-merge
                           (highchart/prep-chart-config
                             dvsp-config
                             {:ppm (table-utils/map-table-to-array
                                     ppm-curve :flow-press-list :vert-depth-list)}
                             {:ipm (table-utils/map-table-to-array
                                     ipm-curve :flow-press-list :vert-depth-list)}
                             {:eq (table-utils/map-table-to-array
                                    equilibrium-profile :flow-press-list :vert-depth-list)}
                             {:open-points (table-utils/map-table-to-array
                                             filtered-valve-map :open-press-list :vert-depth-list)}
                             {:close-points (table-utils/map-table-to-array
                                              filtered-valve-map :close-press-list :vert-depth-list)}
                             {:vpc-bfp (table-utils/map-table-to-array
                                         filtered-valve-map :vpc-begin-flow-list :vert-depth-list)})
                           {:chart {:height 645}}
                           {:yAxis {:plotLines plot-lines
                                    :max max-depth}})
                         ;; There's no way to turn plotlines off/on so we have a dummy series and remove/add the
                         ;; plot lines
                         (update-in [:series] #(into %1 %2)
                                    [{:marker {:enabled false} ;; Don't show the little marker on the legend to remain consistent with other curves
                                      :events {:show #(let [y-axis (aget (js* "this") "chart" "yAxis" 0)]
                                                        (.update y-axis (clj->js {:plotLines plot-lines})))
                                               :hide #(let [chart-plot-lines (aget % "target" "chart" "yAxis" 0 "plotLinesAndBands")]
                                                        (doall
                                                          (for [idx (range (- (aget chart-plot-lines "length") 1) -1 -1)]
                                                            (.destroy (aget chart-plot-lines idx)))))}
                                      :name "Mandrel Lines"}]))]
    (if (and
          (some? depth-profile)
          (some? max-depth))
      [:div.row
       [:div.col-xs-3.col-sm-2
        (if (and (some? max-depth)
                 (some? depth-profile))
          [TubingPipe data-source well {:max-depth max-depth}]
          [LoadingOverlay])]
       [:div.col-xs-9.col-sm-10
        (if (and  (some? depth-profile)
                  (some? max-depth)
                  (some? equilibrium-profile)
                  (some? ppm-curve)
                  (some? ipm-curve)
                  (some? valve-map)
                  (some? mandrel-survey))
          [HighChart chart-config]
          [LoadingOverlay])]]
      [LoadingOverlay])))

(defn PVSQChart [datasource well pvsq-config]
  (let [inflow-curve (datasubs/get-inflow-curve)
        outflow-curve (datasubs/get-outflow-curve)
        welltest-hist-map (datasubs/get-welltest-hist)
        welltest-list (vals welltest-hist-map)
        cal-wt (take 5 (filter #(com-utils/wtest-is-calibrated %) (sort-by :welltest-date > welltest-list)))
        Ncalwt (count cal-wt)
        uncal-wt (take (- 5 Ncalwt) (filter #(not (com-utils/wtest-is-calibrated %)) (sort-by :welltest-date > welltest-list)))
        chart-config (highchart/prep-chart-config
                       pvsq-config
                       {:meas_inflow_liquid_rate
                        (table-utils/map-table-to-array
                          inflow-curve :liquid-rate-list :fbhp-list)}
                       {:meas_outflow_liquid_rate
                        (sort-by first (table-utils/map-table-to-array
                                         outflow-curve :liquid-rate-list :fbhp-list))}
                       {:well-tests (vec
                                      (for [welltest cal-wt]
                                        (-> welltest
                                            (select-keys [:calib-liquid-rate :est-fbhp :welltest-date])
                                            (update-in [:welltest-date] rformat/format-iso-date)
                                            (rename-keys {:calib-liquid-rate :x, :est-fbhp :y
                                                          :welltest-date :name}))))}
                       {:uncal-well-tests (vec
                                            (for [welltest uncal-wt]
                                              (-> welltest
                                                  (select-keys [:calib-liquid-rate :est-fbhp :welltest-date])
                                                  (update-in [:welltest-date] rformat/format-iso-date)
                                                  (rename-keys {:calib-liquid-rate :x, :est-fbhp :y
                                                                :welltest-date :name}))))})]
    (if (and (some? inflow-curve)
             (some? outflow-curve)
             (some? (:est-fbhp (first welltest-list)))
             (or (some? (:est-fbhp (first cal-wt)))
                 (some? (:est-fbhp (first uncal-wt))))
             (some? chart-config))
      [:div
       ;[:div (str "welltest-list: " welltest-list)]
       ;[:div (str "cal-wt: " cal-wt)]
       ;[:div (str "uncal-wt: " uncal-wt)]
       [HighChart chart-config]]
      [LoadingOverlay])))

(defn QVSIChart [datasource well qvsi-config]
  (let [lg-response (datasubs/get-lgas-response)
        welltest-hist-map (datasubs/get-welltest-hist)
        welltest-list (vals welltest-hist-map)
        cal-wt (take 5 (filter #(com-utils/wtest-is-calibrated %) (sort-by :welltest-date welltest-list)))
        Ncalwt (count cal-wt)
        uncal-wt (take (- 5 Ncalwt) (filter #(not (com-utils/wtest-is-calibrated %)) (sort-by :welltest-date welltest-list)))
        chart-config (highchart/prep-chart-config
                       qvsi-config
                       {:lg-resp (table-utils/map-table-to-array
                                   lg-response
                                   :lift-gas-rate-list :calib-oil-rate-list)}
                       {:wellotests (vec
                                      (for [welltest cal-wt]
                                        (-> welltest
                                            (select-keys [:calib-lift-gas-rate :calib-oil-rate :welltest-date])
                                            (update-in [:welltest-date] rformat/format-iso-date)
                                            (rename-keys {:calib-oil-rate :y, :calib-lift-gas-rate :x
                                                          :welltest-date :name}))))}
                       {:uncal-wellotests (vec
                                            (for [welltest uncal-wt]
                                              (-> welltest
                                                  (select-keys [:calib-lift-gas-rate :calib-oil-rate :welltest-date])
                                                  (update-in [:welltest-date] rformat/format-iso-date)
                                                  (rename-keys {:calib-oil-rate :y, :calib-lift-gas-rate :x
                                                                :welltest-date :name}))))})]

    (if (and (some? lg-response)
             (some? (:est-fbhp (first welltest-list)))
             (or (some? (:est-fbhp (first cal-wt)))
                 (some? (:est-fbhp (first uncal-wt))))
             (some? chart-config))
      [HighChart chart-config]
      [LoadingOverlay])))

(defn DVSTChart []
  [:div "DVST chart"])

(defn WellTableDate [data-source well]
  (let [current-wt (datasubs/get-welltest)
        wt-history (datasubs/get-welltest-hist)]
    (when (and (some? current-wt) (some? wt-history) (some? (:welltest-date current-wt)))
      [:select {:value (:welltest-date current-wt)
                :class "form-control"}
       ;; Load first well test on list by default
       (for [date (sort > (keys wt-history))]
         ^{:key date} [:option {:value date} (rformat/format-iso-date date)])])))

(defn DataSourceDropdown []
  "Data Source selector for the well picker"
  (let [datasources (overviewsubs/get-datasources)
        selected-data-source (overviewsubs/get-selected-datasource)]
    ;(.log js/console (str "datasources: " datasources))
    ;(.log js/console (str "selected-dsn: " selected-data-source))
    ;; Don't show the data-source selector if only 1 data source available;;
    (if (some? datasources)
      (if (= 1 (count datasources))
        (do
          (overviewhandlers/set-selected-datasource (first (keys (first datasources))))
          [:div])
        [:div {:style {:margin-bottom "15px"}}
         [:label "Data Source"]
         [:div
          [DropdownMenuWithBlank (when (some? datasources) datasources)
           selected-data-source
           #(overviewhandlers/set-selected-datasource %)]]])
      [:div])))

(defn WellTable [data-source well]
  [:table.table {:style {:margin-bottom "0px"}}
   [:thead
    [:tr
     [:th "Field"]
     [:th "Lease"]
     [:th "Well"]
     [:th "Cmpl"]
     [:th "Date"]]]
   [:tbody
    [:tr
     [:td (:field well)]
     [:td (:lease well)]
     [:td (:well well)]
     [:td (:cmpl well)]
     [:td (when (some? well)
            [WellTableDate data-source well])]]]])

(defn Header []
  [:h1 {:style {:margin-top "5px"
                :margin-bottom "5px"}}
   "Well Overview"])

(defn WellPicker []
  (let [selected-well (overviewsubs/get-selected-well)
        selected-data-source (overviewsubs/get-selected-datasource)
        open-well-selector (overviewsubs/get-is-open-wellselector)]
    [:div
     [DataSourceDropdown]
     (if (nil? selected-data-source)
       [ContentMsg "Select a Data Source"]
       [:div
        ;; Well Selector
        (if open-well-selector
          [BoxContainer
           {:header
            {:title "Select Well"
             :with-border true
             :box-tools [:button.btn.butn-block.btn-primary
                         {:on-click #(overviewhandlers/set-open-well-selector false)}
                         [:i.fa.fa-close]]}}
           [WellSelector selected-data-source
            #(do
               (overviewhandlers/set-selected-well %)
               (overviewhandlers/set-open-well-selector false))]]
          [:div.row
           [:div.col-sm-2
            [:button.btn.butn-block.btn-primary
             {:on-click #(overviewhandlers/set-open-well-selector true)}
             "Select Well"
             " " ;; Spacing between well-name and change button
             [:i.fa.fa-exchange]]]
           (if (some? selected-well)
             [:div.col-sm-10
              [WellTable selected-data-source selected-well]]
             [ContentMsg "Select a Well"])])])]))

(defn Content
  "Well Overview Page meant to be used as a content page in the AdminLTE template"
  [datasource well]
  (let [well-doc (overviewsubs/get-well-doc)
        dvsp-config (overviewsubs/get-dvsp-config)
        pvsq-config (overviewsubs/get-pvsq-config)
        qvsi-config (overviewsubs/get-qvsi-config)]
    ;(.log js/console "Content!!!")
    [:div
     (when (some? well-doc)
       [:div
        [:div.row
         [:div.col-md-12
          [BoxContainer {:table-responsive true}
           [WellTestInfo]]]]
        [:div.row
         [:div.col-sm-12.col-md-8
          [BoxContainer
           [DVSPChart datasource well dvsp-config]]]
         [:div.col-sm-12.col-md-4
          [BoxContainer
           [PVSQChart datasource well pvsq-config]]
          [BoxContainer
           [QVSIChart datasource well qvsi-config]]]]
        [:div.row
         [:div.col-md-12
          [BoxContainer {:header {:title "Gas Lift Valves"}
                         :table-responsive true}
           [GLVTable datasource well]]]]])]))

(defn WellOverview
  "Well Overview Page meant to be used as a content page in the AdminLTE template"
  []
  (let [selected-data-source (overviewsubs/get-selected-datasource)
        selected-well (overviewsubs/get-selected-well)]
    [:div
     [BoxContainer {:solidBox true}
      [:div
       [Header]
       [WellPicker]]]
     [:section.content
      (if (every? some? [selected-data-source selected-well])
         [Content selected-data-source selected-well])]]))







