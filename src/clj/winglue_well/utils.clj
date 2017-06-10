;; ========================================================================== ;;
;; Copyright (c) 2016 by AppSmiths Software LLC.  All Rights Reserved.        ;;
;; -------------------------------------------------------------------------- ;;
;; All material is proprietary to AppSmiths Software LLC and may be used only ;;
;; pursuant to license rights granted by AppSmiths Software LLC.  Other       ;;
;; reproduction, distribution, or use is strictly prohibited.                 ;;
;; ========================================================================== ;;

;; utility functions we write and want to reuse

(ns winglue-well.utils
  (:use flatland.protobuf.core)
  (:gen-class)
  (:require [clojure.string :as cs]
            [clojure.pprint :as pp]
            [environ.core :refer [env]]
            [clojure.java.io :as io])
  (:import java.io.StringWriter
           java.util.Properties))

;; http://stackoverflow.com/a/33070806
(defn get-version
  "gets the version of the requested dependency, can be used to get the project
   version. Only works in uberjars."
  [project]
  (let [path (str "META-INF/maven/" project "/pom.properties")
        props (io/resource path)]
    (when props
      (with-open [stream (io/input-stream props)]
        (let [props (doto (Properties.) (.load stream))]
          (.getProperty props "version"))))))

(defn tao2-version
  "retrieves the TAO2 version declared in project.clj"
  []
  ;; In debug, obtain with environ
  ;; In release (ubjerjar), obtain from project properties
  (some #(if (some? %) %)
        [(:winglue-well-version env)
         (get-version "com.AppSmiths/winglue-well")]))




