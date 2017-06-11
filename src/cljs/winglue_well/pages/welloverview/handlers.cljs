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
  (se/sendAction :pickwell well))

(defn set-open-well-selector [in]
  (swap! mydb/local-app-state assoc :open-well-selector in))

