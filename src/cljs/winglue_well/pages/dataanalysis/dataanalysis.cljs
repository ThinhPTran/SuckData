(ns winglue-well.pages.dataanalysis.dataanalysis
  (:require [winglue-well.db :as mydb]
            [reagent.core :as reagent]))


(defn DataAnalysis
  "Data Analysis Page"
  []
  (let [data @mydb/app-state]
    [:div (str data)]))
