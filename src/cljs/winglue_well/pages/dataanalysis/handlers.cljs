(ns winglue-well.pages.dataanalysis.handlers
  (:require [winglue-well.serverevents :as se]
            [winglue-well.db :as mydb]))

(defn set-selected-well [well]
  (let [rawwell well
        cleanwell (dissoc rawwell :glstatus)]
    (.log js/console (str "well: " cleanwell))
    (swap! mydb/well-state assoc :current-well cleanwell)
    (se/sendAction :pickwell cleanwell)))