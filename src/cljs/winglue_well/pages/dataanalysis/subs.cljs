(ns winglue-well.pages.dataanalysis.subs
  (:require [winglue-well.db :as mydb]
            [winglue-well.utils.common :as com-utils]
            [winglue-well.data.subs :as datasubs])
  (:require-macros [hiccups.core :as hiccups :refer [html]]))

(defn cal-glstatus [inwell]
  (let [valve-map (datasubs/get-valve-map-of-well inwell)
        status-list (mapv #(com-utils/decode-valve-status %) (:status-list valve-map))
        islastopen (= :open (last status-list))
        listopen (filterv #(= :open %) status-list)
        Nopen (count listopen)]
    ;(.log js/console (str "status-list: " status-list))
    ;(.log js/console (str "islastopen: " islastopen))
    ;(.log js/console (str "listopen: " listopen))
    (cond
      (and (= true islastopen)
           (= 1 Nopen)) (assoc inwell :glstatus 2)
      (and (= true islastopen)
           (> Nopen 1)) (assoc inwell :glstatus 1)
      :else (assoc inwell :glstatus 0))))

(defn get-all-well-status
  []
  (let [in-welllist (get-in @mydb/well-state [:all-well])
        out-welllist (map #(cal-glstatus %) in-welllist)]
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

