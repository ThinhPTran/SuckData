(ns winglue-well.pages.mainpage.subs
  (:require [reagent.core :as reagent]
            [winglue-well.db :as mydb]))

(defn get-main-page-option []
  (get-in @mydb/local-app-state [:pages :mainpage :options]))

(defn get-main-page-content []
  (get-in @mydb/local-app-state [:pages :mainpage :content]))