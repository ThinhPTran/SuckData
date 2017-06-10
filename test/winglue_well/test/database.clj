(ns winglue-well.test.database
  (:use clojure.test)
  (:require [clojure.string :as clj-string]
            [winglue-well.config.core :as cfg]
            [jdbc.core :as jdbc]
            [clojure.spec :as s]))

