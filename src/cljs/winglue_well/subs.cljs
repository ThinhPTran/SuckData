(ns winglue-well.subs
  (:require [winglue-well.db :as mydb]))

(defn get-window-focus []
  (get-in @mydb/app-state [:window :focus]))
