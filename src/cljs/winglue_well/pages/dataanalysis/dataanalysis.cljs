(ns winglue-well.pages.dataanalysis.dataanalysis
  (:require [winglue-well.db :as mydb]
            [reagent.core :as reagent]))


(defn DataAnalysis
  "Data Analysis Page"
  []
  (let [data @mydb/well-state
        wells (:wells @mydb/field-state)]
    [:div (str "Number of wells: " (count wells))]))
