(ns winglue-well.pages.welloverview.handlers
  (:require [winglue-well.serverevents :as se]
            [winglue-well.db :as mydb]))

(defn set-selected-datasource [dsn]
  (.log js/console (str "set-selected-datasource: " dsn))
  ;(se/changeState [:current-dsn] dsn))
  (se/sendAction :pickdsn dsn))

(defn set-selected-well [well]
  (.log js/console (str "well: " well))
  ;(swap! mydb/app-state :welldoc {})
  ;(let [localwells (:wells @mydb/field-state)]
  ;  (if (> (count localwells) 0)
  ;    (let [localwell (first (filter #(= well (:well %)) localwells))]
  ;      (if (some? localwell)
  ;        (do
  ;          (.log js/console "Got a local well!!!")
  ;          (swap! mydb/well-state assoc :welldoc (:welldoc localwell))
  ;          (.log js/console "Change successfully"))))))
  (swap! mydb/well-state assoc :current-well well)
  (se/sendAction :pickwell well))

(defn set-open-well-selector [in]
  (swap! mydb/local-app-state assoc :open-well-selector in))

