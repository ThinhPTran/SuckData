(ns winglue-well.pages.dataanalysis.subs
  (:require [winglue-well.db :as mydb])
  (:require-macros [hiccups.core :as hiccups :refer [html]]))



(defn get-all-well-status
  []
  (let [in-welllist (get-in @mydb/well-state [:all-well])
        out-welllist (map #(assoc % :glstatus (rand-int 3))  in-welllist)]
    out-welllist))

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

