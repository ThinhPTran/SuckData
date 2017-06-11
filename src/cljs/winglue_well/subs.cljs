(ns winglue-well.subs
  (:require [winglue-well.db :as mydb]))

(defn get-window-focus []
  (get-in @mydb/well-state [:window :focus]))
