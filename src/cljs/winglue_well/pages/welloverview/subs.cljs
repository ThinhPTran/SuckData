(ns winglue-well.pages.welloverview.subs
  (:require [winglue-well.db :as mydb]))

(defn get-datasources
  []
  (get-in @mydb/well-state [:all-dsn]))

(defn get-selected-datasource
  []
  (get-in @mydb/well-state [:current-dsn]))

(defn get-all-well
  []
  (get-in @mydb/well-state [:all-well]))

(defn get-selected-well
  []
  (get-in @mydb/well-state [:current-well]))

(defn get-is-open-wellselector
  []
  (get-in @mydb/local-app-state [:open-well-selector]))

(defn get-well-doc
  []
  (get-in @mydb/well-state [:welldoc]))

(defn get-dvsp-config
  []
  (get-in @mydb/local-app-state [:chart/by-id :dvsp]))

(defn get-pvsq-config
  []
  (get-in @mydb/local-app-state [:chart/by-id :pvsq]))

(defn get-qvsi-config
  []
  (get-in @mydb/local-app-state [:chart/by-id :qvsi]))


