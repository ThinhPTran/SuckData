(ns winglue-well.router.router
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [winglue-well.db :as mydb]
            [winglue-well.pages.mainpage.handlers :as mainpagehandler]))

(defn- hook-browser-navigation! []
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn init-routes []
  (defroute "/" []
    (swap! mydb/app-state assoc :current-page :home))

  (defroute "*" []
    (swap! mydb/app-state assoc :current-page :page404))

  (hook-browser-navigation!))
