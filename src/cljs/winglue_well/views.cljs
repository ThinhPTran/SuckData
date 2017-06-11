(ns winglue-well.views
  (:require [reagent.core :as reagent]
            [winglue-well.pages.mainpage.mainpage :refer [MainPage]]
            [winglue-well.pages.page404 :refer [Page404]]
            [winglue-well.pages.loadingpage :refer [LoadingPage]]
            [winglue-well.db :as mydb]))

(defn main-panel []
  "The main reagent component"
  (let [current-page (:current-page @mydb/well-state)]
    (cond
      (= :home current-page) [MainPage]
      (= :page404 current-page) [Page404]
      :else [LoadingPage])))
