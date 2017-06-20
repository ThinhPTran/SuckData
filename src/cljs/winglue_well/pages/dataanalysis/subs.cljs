(ns winglue-well.pages.dataanalysis.subs
  (:require [winglue-well.db :as mydb]
            [winglue-well.utils.common :as com-utils]
            [winglue-well.utils.format :as rformat]
            [winglue-well.data.subs :as datasubs])
  (:require-macros [hiccups.core :as hiccups :refer [html]]))

(defn cal-morewelldata [inwell]
  (let [welldoc (datasubs/get-welldoc-of-well inwell)
        valve-map (get-in welldoc [:depth-profile-map :valves-status-map])
        calib-oil-rate (rformat/format-dec (get-in welldoc [:welltest-map :calib-oil-rate]) 2)
        status-list (mapv #(com-utils/decode-valve-status %) (:status-list valve-map))
        islastopen (= :open (last status-list))
        listopen (filterv #(= :open %) status-list)
        Nopen (count listopen)
        outwell (assoc inwell :calib-oil-rate calib-oil-rate)]
    (cond
      (and (= true islastopen)
           (= 1 Nopen)) (assoc outwell :glstatus 2)
      (and (= true islastopen)
           (> Nopen 1)) (assoc outwell :glstatus 1)
      :else (assoc outwell :glstatus 0))))

(defn get-oil-rate [inwell]
  (let [welltest (datasubs/get-welltest-of-well inwell)]
    (assoc inwell :calib-oil-rate (:calib-oil-rate welltest))))

(defn get-all-well-status
  []
  (let [in-welllist (get-in @mydb/well-state [:all-well])
        out-welllist (map #(cal-morewelldata %) in-welllist)]
    out-welllist))

(defn glstatustoimg [data]
  (cond
    (= 0 data) [:div {:style {:height "20px" :width "20px"}}
                [:img {:src "images/DataTables/redcircle20x20.png"
                       :style {:height "100%" :width "100%"}}]]
    (= 1 data) [:div {:style {:height "20px" :width "20px"}}
                [:img {:src "images/DataTables/yellowcircle20x20.png"
                       :style {:height "100%" :width "100%"}}]]
    (= 2 data) [:div {:style {:height "20px" :width "20px"}}
                [:img {:src "images/DataTables/greencircle20x20.png"
                       :style {:height "100%" :width "100%"}}]]
    :else [:div {:style {:height "20px" :width "20px"}}
           "No data"]))

(defn render-GL-status [data type row]
  (cond
    (= 0 data) (html
                 [:div {:style {:height "32px" :width "32px"}}
                  [:img {:src "images/DataTables/redcircle20x20.png"
                         :style {:height "100%" :width "100%"}}]])
    (= 1 data) (html
                 [:div {:style {:height "32px" :width "32px"}}
                  [:img {:src "images/DataTables/yellowcircle20x20.png"
                         :style {:height "100%" :width "100%"}}]])
    (= 2 data) (html
                 [:div {:style {:height "32px" :width "32px"}}
                  [:img {:src "images/DataTables/greencircle20x20.png"
                         :style {:height "100%" :width "100%"}}]])))


