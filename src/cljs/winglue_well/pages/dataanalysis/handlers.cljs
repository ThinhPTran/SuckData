(ns winglue-well.pages.dataanalysis.handlers
  (:require [winglue-well.serverevents :as se]
            [winglue-well.db :as mydb]))

(defn set-selected-well [well]
  (let [rawwell well
        cleanwell (dissoc rawwell :glstatus)
        welldata (->> @mydb/field-state
                      (:wells)
                      (filter #(= cleanwell (:well %))))
        welldoc (:welldoc (first welldata))]
    (.log js/console (str "well: " cleanwell))
    (swap! mydb/well-state assoc :current-well cleanwell)
    (if (and (some? welldoc)
             (some? (:welltest-hist-map welldoc)))
      (swap! mydb/well-state assoc :welldoc welldoc)
      (se/sendAction :pickwell cleanwell))))