(ns winglue-well.db
  (:require [winglue-well.config.core :as config]
            [winglue-well.utils :as utils]
            [clojure.pprint :as pp]
            [clojure.data :as da :refer [diff]]
            [durable-atom.core :refer [durable-atom]]))

(def well-state (atom {}))
(def field-state (atom {}))

(def persist-atom (durable-atom "/home/debtao/Datastore/glue.dat"))

(defn initinfo []
  (let [dsnset (->> @persist-atom
                    (:wells)
                    (map #(:dsn %))
                    (distinct)
                    (vec))
        firstdsn (first dsnset)
        wellset (->> @persist-atom
                     (:wells)
                     (map #(:well %))
                     (vec))
        fielddata (->> @persist-atom
                       (:wells))]
    (println (str "initinfo: "))
    (println (str "dsnset: " (pr-str dsnset)))
    (println (str "there is: " (count wellset) " wells."))
    ;(println (str "app-state: " @app-state))
    (swap! well-state assoc :all-dsn dsnset)
    (swap! well-state assoc :current-dsn firstdsn)))

(defn pick-dsn [dsn]
  (println (str "pick-dsn: " dsn))
  (swap! well-state assoc :current-dsn dsn)
  (let [wellset (->> @persist-atom
                     (:wells)
                     (filter #(= (first (keys (:dsn %))) dsn))
                     (map #(:well %))
                     (vec))
        fielddata (->> @persist-atom
                       (:wells))]
    ;(println (str "wellset: " (pr-str wellset)))
    (swap! well-state assoc :all-well wellset)
    (if (nil? (:wells @field-state))
      (swap! field-state assoc :wells fielddata))))

(defn pick-well [well]
  (let [dsn (:current-dsn @well-state)
        welldata (->> @persist-atom
                      (:wells)
                      (filter #(and (= dsn (first (keys (:dsn %))))
                                    (= well (:well %)))))

        welldoc (:welldoc (first welldata))]
    (println (str "pick-well: " well))
    ;(println (str "welldata: " (pr-str welldata)))
    (swap! well-state assoc :current-well well)
    (swap! well-state assoc :welldoc welldoc)))








