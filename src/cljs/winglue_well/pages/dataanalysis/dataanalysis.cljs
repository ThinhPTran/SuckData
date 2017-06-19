(ns winglue-well.pages.dataanalysis.dataanalysis
  (:require [winglue-well.db :as mydb]
            [clojure.set :refer [rename-keys]]
            [winglue-well.pages.welloverview.subs :as overviewsubs]
            [winglue-well.pages.dataanalysis.subs :as dataanalsubs]
            [winglue-well.pages.welloverview.handlers :as overviewhandlers]
            [winglue-well.pages.dataanalysis.handlers :as dataanalhandlers]
            [winglue-well.data.subs :as datasubs]
            [winglue-well.widgets.datatable :refer [DataTable NewDataTable]]
            [winglue-well.widgets.loadingoverlay :refer [LoadingOverlay]]
            [winglue-well.components.glvtable :refer [GLVTable]]
            [winglue-well.widgets.highchart :as highchart :refer [HighChart]]
            [winglue-well.widgets.dropdownmenu :refer [DropdownMenuWithBlank]]
            [winglue-well.widgets.box :refer [BoxContainer]]
            [winglue-well.components.welltesttable :refer [WellTestInfo]]
            [winglue-well.components.tubingpipe :refer [TubingPipe]]
            [winglue-well.utils.format :as rformat]
            [winglue-well.utils.merge :as merge-utils]
            [winglue-well.utils.table :as table-utils]
            [winglue-well.utils.interpolator :as i-util]
            [winglue-well.utils.common :as com-utils]
            [winglue-well.data.subs :as datasubs]
            [winglue-well.utils.format :as rformat]
            [reagent.core :as reagent]))

(defn ContentMsg
  "Simple component to display a message to the user on the WellOverview page in place of the content"
  [msg]
  [:div {:style {:display "flex"
                 :align-items "center"
                 :justify-content "center"}}
   [:p {:style {:font-size "3rem"}} msg]])

(defn Header []
  [:h1 {:style {:margin-top "5px"
                :margin-bottom "5px"}}
   "Data Analysis"])

(defn DataSourceDropdown []
  "Data Source selector for the well picker"
  (let [datasources (overviewsubs/get-datasources)
        selected-data-source (overviewsubs/get-selected-datasource)]
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
      [LoadingOverlay])))

(defn WellSelector [data-source currentwell on-select-fn]
  (let [well-list (dataanalsubs/get-all-well-status)]
    [:div
     (if (some? well-list)
       [NewDataTable
        {:data well-list
         :columns [{:title "Well"
                    :data :well}
                   {:title "Completion"
                    :searchable false
                    :data :cmpl}
                   {:title "GL status"
                    :searchable false
                    :data :glstatus
                    :render dataanalsubs/render-GL-status}]
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
                                                   "cmpl" :cmpl
                                                   "glstatus" :glstatus}))))}]
       [LoadingOverlay])]))

(defn SelectedWellInf []
  (let [selected-well (overviewsubs/get-selected-well)
        field (:field selected-well)
        lease (:lease selected-well)
        well (:well selected-well)
        cmpl (:cmpl selected-well)]
    [:div
     [:p (str " Field: " field " Lease: " lease " Well: " well " Cmpl: " cmpl)]]))

(defn WellTable [data-source well]
  (let [well-list (dataanalsubs/get-all-well-status)
        currentwell (->> well-list
                         (filter #(and (= (:field well) (:field %))
                                       (= (:lease well) (:lease %))
                                       (= (:well well) (:well %))
                                       (= (:cmpl well) (:cmpl %))))
                         (first))]
    ;(.log js/console (str "currentwell: " currentwell))
    [:table.table {:style {:margin-bottom "0px"}}
     [:thead
      [:tr
       [:th "Field"]
       [:th "Lease"]
       [:th "Well"]
       [:th "Cmpl"]
       ;[:th "Oil rate (bbq/day)"]
       [:th "GL status"]]]
     [:tbody
      [:tr
       [:td (:field currentwell)]
       [:td (:lease currentwell)]
       [:td (:well currentwell)]
       [:td (:cmpl currentwell)]
       ;[:td (:calib-oil-rate currentwell)]
       [:td (dataanalsubs/glstatustoimg (:glstatus currentwell))]]]]))

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
           [WellSelector selected-data-source selected-well
            #(do
               (dataanalhandlers/set-selected-well %)
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

(defn DVSPChart []
  (let [data-source (overviewsubs/get-selected-datasource)
        well (overviewsubs/get-selected-well)
        dvsp-config (overviewsubs/get-dvsp-config)
        depth-profile (datasubs/get-depth-profile)
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
                           {:chart {:height 500}}
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
          [TubingPipe data-source well {:max-depth max-depth :height 480}]
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

(defn WellTestInfos []
  (let [welltest-hist-map (datasubs/get-welltest-hist)
        welltest-list (vals welltest-hist-map)
        indata (map (fn [in] {:welltest-date (rformat/format-date (:welltest-date in))
                              :calib-oil-rate (rformat/format-dec (:calib-oil-rate in) 2)
                              :meas-oil-rate (rformat/format-dec (:meas-oil-rate in) 2)
                              :calib-water-rate (rformat/format-dec (:calib-water-rate in) 2)
                              :meas-water-rate (rformat/format-dec (:meas-water-rate in) 2)
                              :calib-liquid-rate (rformat/format-dec (:calib-liquid-rate in) 2)
                              :meas-liquid-rate (rformat/format-dec (:meas-liquid-rate in) 2)
                              :calib-flowing-tubing-press (rformat/format-dec (:calib-flowing-tubing-press in) 2)
                              :meas-flowing-tubing-press (rformat/format-dec (:meas-flowing-tubing-press in) 2)
                              :calib-casing-head-press (rformat/format-dec (:calib-casing-head-press in) 2)
                              :meas-casing-head-press (rformat/format-dec (:meas-casing-head-press in) 2)
                              :calib-lift-gas-rate (rformat/format-dec (:calib-lift-gas-rate in) 2)
                              :meas-lift-gas-rate (rformat/format-dec (:meas-lift-gas-rate in) 2)
                              :calib-formation-gas-rate (rformat/format-dec (:calib-formation-gas-rate in) 2)
                              :meas-form-gas-rate (rformat/format-dec (:meas-form-gas-rate in) 2)
                              :calib-wc (rformat/format-dec (:calib-wc in) 2)
                              :calib-total-gas (rformat/format-dec (:calib-total-gas in) 2)
                              :est-fbhp (rformat/format-dec (:est-fbhp in) 2)
                              :calib-total-glr (rformat/format-dec (:calib-total-glr in) 2)}) (sort-by :welltest-date > welltest-list))]
    [:div
     [BoxContainer
      {:header
       {:title "Well Test Infos"
        :with-border true}}
      (if (> (count indata) 1)
        [DataTable
         {:data indata
          :columns [{:title "Date"
                     :data :welltest-date}
                    {:title "Calib. Oil (bbl/day)"
                     :data :calib-oil-rate}
                    {:title "Meas. Oil (bbl/day)"
                     :data :meas-oil-rate}
                    {:title "Calib. Water (bbl/day)"
                     :data :calib-water-rate}
                    {:title "Meas. Water (bbl/day)"
                     :data :meas-water-rate}
                    {:title "Calib. Casing Press. (psig)"
                     :data :calib-casing-head-press}
                    {:title "Meas. Casing Press. (psig)"
                     :data :meas-casing-head-press}
                    {:title "Calib. Tubing Press. (psig)"
                     :data :calib-flowing-tubing-press}
                    {:title "Meas. Tubing Press. (psig)"
                     :data :meas-flowing-tubing-press}
                    {:title "Calib. Gas LG (MCF/day)"
                     :data :calib-lift-gas-rate}
                    {:title "Meas. Gas LG (MCF/day)"
                     :data :meas-lift-gas-rate}
                    {:title "Calib. Form. GR"
                     :data :calib-formation-gas-rate}
                    {:title "Meas. Form. GR"
                     :data :meas-form-gas-rate}
                    {:title "Calib. Watercut (%)"
                     :data :calib-wc}
                    {:title "Calib. Total liquid (bbl/day)"
                     :data :calib-liquid-rate}
                    {:title "Calib. Total Gas (MCF/day)"
                     :data :calib-total-gas}
                    {:title "Calib. Stored FBHP (psig)"
                     :data :est-fbhp}
                    {:title "Calib. Total GL Rate (MCF/day)"
                     :data :calib-total-glr}]
          :order [[0, "desc"]]
          :deferRender true
          :scrollX true
          :select "single"}
         {:select (fn [e dt type index])}]
        [LoadingOverlay])]]))

(defn OilrateInf []
  (let [welltest-hist-map (datasubs/get-welltest-hist)
        welltest-list (vals welltest-hist-map)
        indata1 (vec (map (fn [in] [(com-utils/getUTCtime (:welltest-date in)) (js/parseFloat (rformat/format-dec (:calib-oil-rate in) 2))])
                          (sort-by :welltest-date > welltest-list)))
        indata2 (vec (map (fn [in] [(com-utils/getUTCtime (:welltest-date in)) (js/parseFloat (rformat/format-dec (:calib-water-rate in) 2))])
                          (sort-by :welltest-date > welltest-list)))
        indata3 (vec (map (fn [in] [(com-utils/getUTCtime (:welltest-date in)) (js/parseFloat (rformat/format-dec (:calib-liquid-rate in) 2))])
                          (sort-by :welltest-date > welltest-list)))
        chart-config {:chart {:type "spline"}
                      :title {:text "Liquid rate vs. time"}
                      :xAxis {:type "datetime"
                              :labels {:format "{value:%Y-%m-%d}"}
                              :title {:text "Date"}}
                      :yAxis {:title {:text "Oil rate (bbq/day)"}}
                      :series [{:name "Oil rate"
                                :data indata1}
                               {:name "Water rate"
                                :data indata2}
                               {:name "Liquid rate"
                                :data indata3}]}]
    [:div 
     [BoxContainer
      {:header
       {:title "Oil rate vs. time"
        :with-border true}}
      [HighChart chart-config]]]))

(defn PressureInf []
  (let [welltest-hist-map (datasubs/get-welltest-hist)
        welltest-list (vals welltest-hist-map)
        indata1 (vec (map (fn [in] [(com-utils/getUTCtime (:welltest-date in)) (js/parseFloat (rformat/format-dec (:calib-casing-head-press in) 2))])
                          (sort-by :welltest-date > welltest-list)))
        indata2 (vec (map (fn [in] [(com-utils/getUTCtime (:welltest-date in)) (js/parseFloat (rformat/format-dec (:calib-flowing-tubing-press in) 2))])
                          (sort-by :welltest-date > welltest-list)))
        chart-config {:chart {:type "spline"}
                      :title {:text "Press. vs. time"}
                      :xAxis {:type "datetime"
                              :labels {:format "{value:%Y-%m-%d}"}
                              :title {:text "Date"}}
                      :yAxis {:title {:text "Pressure (psig)"}}
                      :series [{:name "Casing Head Pressure"
                                :data indata1}
                               {:name "Flowing Tubing Pressure"
                                :data indata2}]}]
    [:div
     [BoxContainer
      {:header
       {:title "Pressure vs. time"
        :with-border true}}
      [HighChart chart-config]]]))

(defn GasInf []
  (let [welltest-hist-map (datasubs/get-welltest-hist)
        welltest-list (vals welltest-hist-map)
        indata1 (vec (map (fn [in] [(com-utils/getUTCtime (:welltest-date in)) (js/parseFloat (rformat/format-dec (:calib-lift-gas-rate in) 2))])
                          (sort-by :welltest-date > welltest-list)))
        indata2 (vec (map (fn [in] [(com-utils/getUTCtime (:welltest-date in)) (js/parseFloat (rformat/format-dec (:calib-formation-gas-rate in) 2))])
                          (sort-by :welltest-date > welltest-list)))
        indata3 (vec (map (fn [in] [(com-utils/getUTCtime (:welltest-date in)) (js/parseFloat (rformat/format-dec (:calib-total-gas in) 2))])
                          (sort-by :welltest-date > welltest-list)))
        chart-config {:chart {:type "spline"}
                      :title {:text "Gas vs. time"}
                      :xAxis {:type "datetime"
                              :labels {:format "{value:%Y-%m-%d}"}
                              :title {:text "Date"}}
                      :yAxis {:title {:text "Gas rate (MCF/day)"}}
                      :series [{:name "GL rate"
                                :data indata1}
                               {:name "Formation gas rate"
                                :data indata2}
                               {:name "Total gas rate"
                                :data indata3}]}]
    [:div
     [BoxContainer
      {:header
       {:title "Gas vs. time"
        :with-border true}}
      [HighChart chart-config]]]))

(defn WaterCutInf []
  (let [welltest-hist-map (datasubs/get-welltest-hist)
        welltest-list (vals welltest-hist-map)
        indata1 (vec (map (fn [in] [(com-utils/getUTCtime (:welltest-date in)) (js/parseFloat (rformat/format-dec (:calib-wc in) 2))])
                          (sort-by :welltest-date > welltest-list)))
        chart-config {:chart {:type "spline"}
                      :title {:text "Water Cut vs. time"}
                      :xAxis {:type "datetime"
                              :labels {:format "{value:%Y-%m-%d}"}
                              :title {:text "Date"}}
                      :yAxis {:title {:text "Percentage (%)"}}
                      :series [{:name "Water Cut"
                                :data indata1}]}]
    [:div
     [BoxContainer
      {:header
       {:title "Gas vs. time"
        :with-border true}}
      [HighChart chart-config]]]))

(defn Content []
  [:div
   [:div.row
    [:div.col-sm-12.col-md-12
     [BoxContainer
      [DVSPChart]]]]
    ;[:div.col-sm-12.col-md-4
    ; [BoxContainer
    ;  [PVSQChart datasource well pvsq-config]]
    ; [BoxContainer
    ;  [QVSIChart datasource well qvsi-config]]]]
   [:div.row
    [:div.col-sm-12.col-md-12
     [OilrateInf]]]
   [:div.row
    [:div.col-sm-12.col-md-12
     [WaterCutInf]]]
   [:div.row
    [:div.col-sm-12.col-md-12
     [PressureInf]]]
   [:div.row
    [:div.col-sm-12.col-md-12
     [GasInf]]]
   [:div.row
    [:div.col-sm-12.col-md-12
     [WellTestInfos]]]
   [:div.row
    [:div.col-md-12
     [BoxContainer {:header {:title "Gas Lift Valves"}
                    :table-responsive true}
      [GLVTable]]]]])

(defn DataAnalysis
  "Data Analysis Page"
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
