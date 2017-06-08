;; ========================================================================== ;;
;; Copyright (c) 2016 by AppSmiths Software LLC.  All Rights Reserved.        ;;
;; -------------------------------------------------------------------------- ;;
;; All material is proprietary to AppSmiths Software LLC and may be used only ;;
;; pursuant to license rights granted by AppSmiths Software LLC.  Other       ;;
;; reproduction, distribution, or use is strictly prohibited.                 ;;
;; ========================================================================== ;;

(ns winglue-well.config.core
  (:require  [clojure.java.io :as io]
             [winglue-well.log :as log]
             [clojure.spec :as s])
  (:import   [java.io PushbackReader])
  (:import   [java.io File])
  (:import   [java.io FileNotFoundException]))

(def cfg-base "tao2-config.clj")

;; this structure (less the def) can be overridden
;; the precedence is
;; - TAO2_CONFIG environment variable pointing at a file
;; - /etc/tao2-config.clj
;; - ./tao2-config.clj
;; - tao2-config.clj in the active JAR file
;; - the defaults given below in default-config

(s/def :data-source/description string?)
(s/def :data-source/classname string?)
(s/def :data-source/subprotocol string?)
(s/def :data-source/subname string?)
(s/def :data-source/user string?)
(s/def :data-source/password string?)
(s/def :data-source/dsn keyword?)
(s/def ::data-source (s/keys :req-un [:data-source/description
                                      :data-source/classname
                                      :data-source/subprotocol
                                      :data-source/subname
                                      :data-source/user
                                      :data-source/password]))

(s/def ::data-sources (s/map-of :data-source/dsn ::data-source))

(s/def :glcalcs/glcalcs-override boolean?)
(s/def :glcalcs/path string?)
(s/def :glcalcs/timeo (s/and int? pos?))
(s/def :glcalcs/worker-count (s/and int? pos?))
(s/def :glcalcs/frontend string?)
(s/def :glcalcs/backend string?)
(s/def ::glcalcs (s/keys :req-un [:glcalcs/glcalcs-override
                                  :glcalcs/path
                                  :glcalcs/timeo
                                  :glcalcs/worker-count
                                  :glcalcs/frontend
                                  :glcalcs/backend]))

(s/def ::config (s/keys :req-un [::data-sources ::glcalcs]))

(def default-config
  {:data-sources
   {:glue-oracle
    {:classname   "oracle.jdbc.OracleDriver"
     :subprotocol "oracle"
     :subname     "thin:@127.0.0.1:1521/orcl.localdomain"
     :user        "glueuser"
     :password    "glue"
     :db-rev      ""}}

   :server-port  3000
   :glcalcs
   {:path "glclacs"
    :glcalcs-override false
    :timeo 1000
    :worker-count 5
    :frontend "tcp://localhost:2112"
    :backend "tcp://localhost:2113"}})

;; More config examples...

;; Firebird/jaybird
;;
;; {:dbspec
;;  {:classname   "org.firebirdsql.jdbc.FBDriver"
;;   :subprotocol "firebirdsql"
;;   :subname     "//localhost:3050//opt/glue.fdb"
;;   :user        "glueuser"
;;   :password    "glue"}
;;  :glcalcs
;;  {:path "/home/jel/asi/src/build-tao2/glcalcs/glcalcs"
;;   :glcalcs-override 1 ; Use unpacked glcalcs from path above.
;;   :timeo 30000000
;;   :frontend "tcp://localhost:2112"
;;   :backend "tcp://localhost:2113"
;;   :worker-count 1}}

;; Oracle
;;
;; {:dbspec
;;  {:classname   "oracle.jdbc.OracleDriver"
;;   :subprotocol "oracle"
;;   :subname     "thin:@oravmjel:1521/orcl.localdomain"
;;   :user        "oxy"
;;   :password    "oxy"}
;;  :glcalcs
;;  {:path "/home/jel/asi/src/build-tao2/glcalcs/glcalcs"
;;   :glcalcs-override 1 ; Use unpacked glcalcs from path above.
;;   :timeo 30000000
;;   :frontend "tcp://localhost:2112"
;;   :backend "tcp://localhost:2113"
;;   :worker-count 1}}


;; the config atom for external consumption
(def tao2-cfg (atom default-config))

;; util functions
(defn- root-dir
  "return the name of the root directory"
  []
  (.getAbsolutePath (first (File/listRoots))))

(defn- path-join
  "join pathname parts"
  [arg & args]
  (.getAbsolutePath (reduce (fn [f g] (File. f g)) arg args)))

(defn- via-file
  "tries to get config from a file"
  [path]
  (try
    (with-open [r (io/reader path)]
      (read (PushbackReader. r)))
    (catch FileNotFoundException _ nil)))

(defn- via-env
  "sees if file can be found via TAO2_CONFIG"
  []
  (if-let [pn (System/getenv "TAO2_CONFIG")]
    (via-file pn)))

(defn- via-etc
  "sees if we have file in /etc"
  []
  (via-file (path-join (root-dir) "etc" cfg-base)))

(defn- via-cwd
  "sees if we have file in local directory"
  []
  (via-file (path-join (System/getProperty "user.dir") cfg-base)))

(defn- via-jar
  "trys for config from JAR resources"
  []
  (io/resource cfg-base))

(defn init-logging
  "Use settings from tao2-config.clj to configure the logger."
  [cfg]
  (if-let [log-file (:log-file cfg)] (log/add-log-file log-file))
  (if-let [log-level (:log-level cfg)] (log/set-log-level! log-level))
  (if-let [log-console (:log-console cfg)] (log/add-log-console)))

(defn set-db-connections
  [data-sources]
  (let [cfg (merge @tao2-cfg {:data-sources data-sources})]
    (swap! tao2-cfg (fn [_] cfg)))) ;; Can be changed to (reset! tao2-cfg cfg)

(defn set-config!
  "Validates and sets the tao2-cfg atom with the provided config"
  [config]
  (let [cfg (s/conform ::config config)]
    (if (= cfg ::s/invalid)
      (throw (ex-info "Attemping to set innvalid TAO2 configuration" (s/explain-data ::config config)))
      (reset! tao2-cfg cfg))))

(defn load-config!
  "Attemps to load the TAO2 configuration from various places.
   Loads default configuration if no configuration file is found"
  []
  (binding [*read-eval* false] ;; nothing executable please
    (if-let [config (or (via-env) (via-etc) (via-cwd) (via-jar))]
      (set-config! config)
      (do
        (println "No configuration file found. Using default configuration")
        (set-config! default-config)))))

(defn get-data-sources
  "returns a map containing the data sources"
  [config]
  (let [dsmap (:data-sources config)
        dskeys (keys dsmap)]
    (zipmap dskeys (map #(:description (% dsmap)) dskeys))))
