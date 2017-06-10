(ns winglue-well.db
  (:require [winglue-well.config.core :as config]
            [winglue-well.utils :as utils]
            [clojure.pprint :as pp]
            [clojure.data :as da :refer [diff]]
            [durable-atom.core :refer [durable-atom]]))

(def app-state (atom {}))
(def persist-atom (durable-atom "/home/debtao/Datastore/glue.dat"))

(defn initinfo []
  (let [dsnset (->> @persist-atom
                    (:wells)
                    (map #(:dsn %))
                    (distinct)
                    (vec))]
    (println (str "initinfo: "))
    (println (str "dsnset: " (pr-str dsnset)))
    (println (str "app-state: " @app-state))))

(defn pick-dsn [dsn]
  (println (str "pick-dsn: " dsn)))

(defn pick-well [well]
  (println (str "pick-well: " well)))








