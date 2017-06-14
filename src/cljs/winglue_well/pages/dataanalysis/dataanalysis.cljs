(ns winglue-well.pages.dataanalysis.dataanalysis
  (:require [winglue-well.db :as mydb]
            [clojure.set :refer [rename-keys]]
            [winglue-well.pages.welloverview.subs :as overviewsubs]
            [winglue-well.pages.dataanalysis.subs :as dataanalsubs]
            [winglue-well.pages.welloverview.handlers :as overviewhandlers]
            [winglue-well.pages.dataanalysis.handlers :as dataanalhandlers]
            [winglue-well.data.subs :as datasubs]
            [winglue-well.widgets.datatable :refer [DataTable]]
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

(defn WellSelector [data-source on-select-fn]
  (let [well-list (dataanalsubs/get-all-well-status)]
    [:div
     (if (some? well-list)
       [DataTable
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

(defn WellPicker []
  (let [selected-data-source (overviewsubs/get-selected-datasource)]
    [:div
     [DataSourceDropdown]
     (if (nil? selected-data-source)
       [ContentMsg "Select a Data Source"]
       [:div
        ;; Well Selector
        [BoxContainer
         {:header
          {:title "Select Well"
           :with-border true}}
         [WellSelector selected-data-source
          #(do
             (dataanalhandlers/set-selected-well %))]]])]))

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
        indata (map (fn [in] {:welltest-date (rformat/format-iso-date (:welltest-date in))
                              :calib-oil-rate (rformat/format-dec (:calib-oil-rate in) 2)
                              :calib-water-rate (rformat/format-dec (:calib-water-rate in) 2)
                              :calib-liquid-rate (rformat/format-dec (:calib-liquid-rate in) 2)
                              :calib-wc (rformat/format-dec (:calib-wc in) 2)
                              :calib-total-gas (rformat/format-dec (:calib-total-gas in) 2)
                              :calib-flowing-tubing-press (rformat/format-dec (:calib-flowing-tubing-press in) 2)
                              :calib-casing-head-press (rformat/format-dec (:calib-casing-head-press in) 2)
                              :est-fbhp (rformat/format-dec (:est-fbhp in) 2)
                              :calib-lift-gas-rate (rformat/format-dec (:calib-lift-gas-rate in) 2)
                              :calib-total-glr (rformat/format-dec (:calib-total-glr in) 2)
                              :calib-formation-gas-rate (:calib-formation-gas-rate in) })(sort-by :welltest-date > welltest-list))]
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
                    {:title "Oil (bbl/day)"
                     :data :calib-oil-rate}
                    {:title "Water (bbl/day)"
                     :data :calib-water-rate}
                    {:title "Total liquid (bbl/day)"
                     :data :calib-liquid-rate}
                    {:title "Watercut (%)"
                     :data :calib-wc}
                    {:title "Total Gas (MCF/data)"
                     :data :calib-total-gas}
                    {:title "Tubing Press. (psig)"
                     :data :calib-flowing-tubing-press}
                    {:title "Casing Press. (psig)"
                     :data :calib-casing-head-press}
                    {:title "Stored FBHP (psig)"
                     :data :est-fbhp}
                    {:title "Gas LG (MCF/data)"
                     :data :calib-lift-gas-rate}
                    {:title "Total GL Rate (MCF/day)"
                     :data :calib-total-glr}]
          :deferRender true
          :select "single"}
         {:select (fn [e dt type index])}]
        [LoadingOverlay])]]))

(defn DataAnalysis
  "Data Analysis Page"
  []
  (let []
    (.log js/console "Data Analysis Page!!!")
    [:div
     [BoxContainer {:solidBox true}
      [:div
       [Header]
       [:div.row
        [:div.col-md-6
         [WellPicker]]
        [:div.col-sm-6.col-md-6
         [BoxContainer
          {:header
           {:title "Selected Well"
            :with-border true}}
          [SelectedWellInf]]
         [BoxContainer
          [DVSPChart]]]]
       [:div.row
        [:div.col-sm-12.col-md-12
         [WellTestInfos]]]
       ;[:div.row
       ; [:div.col-md-12
       ;  [BoxContainer {:table-responsive true}
       ;   [WellTestInfo]]]]
       [:div.row
        [:div.col-md-12
         [BoxContainer {:header {:title "Gas Lift Valves"}
                        :table-responsive true}
          [GLVTable]]]]]]]))
