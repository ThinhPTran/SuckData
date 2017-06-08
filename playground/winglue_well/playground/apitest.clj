;; ========================================================================== ;;
;; Copyright (c) 2016 by AppSmiths Software LLC.  All Rights Reserved.        ;;
;; -------------------------------------------------------------------------- ;;
;; All material is proprietary to AppSmiths Software LLC and may be used only ;;
;; pursuant to license rights granted by AppSmiths Software LLC.  Other       ;;
;; reproduction, distribution, or use is strictly prohibited.                 ;;
;; ========================================================================== ;;

;; some testers for the API

(ns winglue-well.playground.apitest
  (:gen-class)
  (:require [ajax.core :refer [GET POST]]
            [cognitect.transit :as transit]
            [clojure.pprint :as pp]
            [jdbc.core :as jdbc]
            [honeysql.core :as sql]
            [honeysql.helpers :as sqlh]
            [winglue-well.database :as wgdb]
            [winglue-well.calcs :as calcs]
            [winglue-well.config :as config]
            [winglue-well.glcalcs-proxy :as glcproxy]))

(defn- success-handler
  "this gets invoked when handler succeeds, delivers promise with success"
  [prom response]
  (deliver prom {:status :success :response response}))

(defn- failure-handler
  "this gets invoked when handler fails, delivers promise with fail status"
  [prom response]
  (deliver prom {:status :fail :error response}))

(defn dothing-async
  "makes async request yielding promise for complete"
  [method args]
  (let [prom (promise)]
    (POST (format "http://localhost:3000/api/%s" method)
          ;    (POST (format "http://192.168.1.94:3000/api/%s" method)
          ;    (POST (format "http://localhost:3003/api/%s" method)
          ;    (POST (format "http://192.168.1.95:3000/api/%s" method)
          ;    (println (format "http://localhost:3333/api/%s" method))
          ;    (POST (format "http://localhost:3333/api/%s" method)
          ;    (POST (format "http://localhost:3334/api/%s" method)
          {:params args
           :handler (partial success-handler prom)
           :error-handler (partial failure-handler prom)
           :timeout 50000})
    prom))

(defn syncreq
  "makes a asynchronous request, by waiting on async request to
  complete you get an exception if it fails"
  [method args]
  @(dothing-async method args))

(defn dothing-sync
  "makes a asynchronous request, by waiting on async request to
  complete you get an exception if it fails"
  [method args]
  @(dothing-async method args))

(defn- dflt-well []
  {:dsn     :glue
   :field   "EXAMPLE"
   :lease   "A"
   :well    "A1"
   :cmpl    "L"})

; a well that seems to pop up often
(defn- test-well [] {:field "EAST WOLFBERRY" :lease "MERCHAN" :well "1408" :cmpl "BL"})

(comment
  (dothing "Hello"
           {:select-set #{:field :lease :well :cmpl}
            :where-map {:field "EXAMPLE"}}))

(defn- run-calc
  [method well]
  (let [rslt (dothing-sync method  well)
        status (:status rslt)
        ret (:response rslt)
        ret-well (if (= status :success) (merge well ret) well)]

    (if (not (= status :success)) (println method " failed with error: " (:error rslt)))
    ret-well))

(defn- run-all
  ([dsn field lease well cmpl]
   (println "running " field lease well cmpl)
   (let [wid {:dsn dsn :field field :lease lease :well well :cmpl cmpl}
         tw (:response (dothing-sync "query-well" wid))
         ;tw1 (run-calc "cmd-profile-depth" tw)
         ;tw2 (run-calc "cmd-profile-equilibrium-depth" tw)
         ;tw3 (run-calc "cmd-profile-fbhp" tw)
         ;tw4 (run-calc "cmd-flow-regime-map" tw)
         ;tw5 (run-calc "cmd-profile-injection-rate" tw)
         tw6 (run-calc "cmd-calibrate-string" tw)]
     ;tw7 (run-calc "query-stored-lift-gas-response" tw)

     tw))

  ([[dsn field lease well cmpl]]
   (run-all dsn field lease well cmpl)))


(defn- make_wt_list_well_id [s]
  {:field (nth s 0) :lease (nth s 1) :well (nth s 2) :cmpl (nth s 3) :dsn :glue :count 10})

(defn- tao-deref [k w] (:response (dothing-sync "Deref" (k w))))

(comment
  (load-file "src/winglue_well/util.clj")
  (use 'flatland.protobuf.core)
  (import 'gasliftcalcs.Gasliftcalcs$wellbore_data_set)
  (def WGWell (protodef gasliftcalcs.Gasliftcalcs$wellbore_data_set))
  (protobuf-schema WGWell)

  (println well)

  (load-file "src/winglue_well/calcs.clj")
  (def wells
    (:response (dothing-sync "query-matching-wells"
                             {:dsn :glue :select-set #{:field :lease :well :cmpl}})))
  (def wlist (into [] (for [w wells]
                        {:dsn :glue
                         :field (nth w 0)
                         :lease (nth w 1)
                         :well (nth w 2)
                         :cmpl (nth w 3)})))

  (load-file "src/winglue_well/apitest.clj")
  (ns winglue-well.apitest)
  (def twells
    (:response (dothing-sync "query-matching-wells"
                             {:dsn :tao2 :select-set #{:field :lease :well :cmpl}})))
  (def wlist (into [] (for [w twells]
                        {:dsn :tao2
                         :field (nth w 0)
                         :lease (nth w 1)
                         :well (nth w 2)
                         :cmpl (nth w 3)})))
  (def w4ls (nth wlist 16))
  (def well (:response (dothing-sync "query-well" w4ls)))
  (def w1 (run-calc "cmd-profile-depth" well))
  (def wlist2 [ {:dsn :tao2, :field "DULANG", :lease "B",  :well "14", :cmpl "L"}
               {:dsn :tao2, :field "DULANG", :lease "B",  :well "14", :cmpl "S"}
               {:dsn :tao2, :field "WT",     :lease "EX", :well "3",  :cmpl "1"}
               {:dsn :tao2, :field "WT",     :lease "EX", :well "4",  :cmpl "LS"}
               {:dsn :tao2, :field "WT",     :lease "EX", :well "4",  :cmpl "SS"}
               {:dsn :tao2, :field "WT",     :lease "EX", :well "5",  :cmpl "1"}])
  (def wl
    [
     {:dsn :tao2 :field "ANGSI" :lease "DP-B" :well "B005" :cmpl "LS"}
     {:dsn :tao2 :field "ANGSI" :lease "DP-B" :well "B005" :cmpl "SS"}
     {:dsn :tao2 :field "BARAM" :lease "DP-A" :well "A054" :cmpl "LS"}
     {:dsn :tao2 :field "BARAM" :lease "DP-A" :well "A054" :cmpl "SS"}
     {:dsn :tao2 :field "CENDOR" :lease "3" :well "CDC-10" :cmpl "SS"}
     {:dsn :tao2 :field "EXAMPLE" :lease "A" :well "A1" :cmpl "L"}
     {:dsn :tao2 :field "EXAMPLE" :lease "A" :well "A18" :cmpl "T"}
     {:dsn :tao2 :field "EXAMPLE" :lease "A" :well "A2" :cmpl "U"}
     {:dsn :tao2 :field "EXAMPLE" :lease "A" :well "A3" :cmpl "1"}
     {:dsn :tao2 :field "EXAMPLE" :lease "D" :well "A4" :cmpl "L"}
     {:dsn :tao2 :field "EXAMPLE" :lease "D" :well "A4" :cmpl "S"}
     {:dsn :tao2 :field "WT" :lease "EX" :well "1" :cmpl "1"}
     {:dsn :tao2 :field "WT" :lease "EX" :well "2" :cmpl "1"}
     {:dsn :tao2 :field "WT" :lease "EX" :well "3" :cmpl "1"}
     {:dsn :tao2 :field "WT" :lease "EX" :well "4" :cmpl "LS"}
     {:dsn :tao2 :field "WT" :lease "EX" :well "4" :cmpl "SS"}
     {:dsn :tao2 :field "WT" :lease "EX" :well "5" :cmpl "1"}])


  (def results (into [] (for [w wl] (run-all (:dsn w) (:field w) (:lease w) (:well w) (:cmpl w)))))

  (def wid2 (nth wlist 14))
  (def rstl (into [] (for [w wl] (do (println w) (run-calc "cmd-profile-depth" w)))))
  (def results (into [] (for [w wl] (run-all (cons :glue w)))))

  (def wid {:field "DULANG" :lease "DP-A" :well "A002" :cmpl "LS" :dsn :glue})

  (load-file "src/winglue_well/calcs.clj")
  (load-file "src/winglue_well/apitest.clj")
  (ns winglue-well.apitest)
  (dothing-sync "query-data-sources" nil)
  (def wid {:field "EXAMPLE" :lease "A" :well "A1" :cmpl "L" :dsn :glue})
  (def well (:response (dothing-sync "query-well" wid)))
  (def well1 (run-calc "cmd-profile-depth" well))
  (def well2 (run-calc "cmd-profile-equilibrium-depth" well))
  (def well3 (run-calc "cmd-profile-fbhp" well))
  (def well5 (run-calc "cmd-flow-regime-map" well))
  (def well6 (run-calc "cmd-profile-injection-rate" well))
  (def well7 (run-calc "cmd-calibrate-string" well))
  (def well8 (run-calc "query-stored-lift-gas-response" wid2))
  (def well9 (run-calc "cmd-outflow" well))

  (def wid2 {:dsn :glue :field "EXAMPLE" :lease "A" :well "A1" :cmpl "L"})
  (def wella1 (:response (dothing-sync "query-well" wid2)))

  (def wella (run-calc "cmd-outflow" well))

  (def wt (:response (dothing-sync "Deref" (:welltest well))))
  (println "Original QMax: " (:qmax well))
  (def w2 (run-calc "CalcFGSQMax" well))
  (def wt (:response (syncreq "Deref" (:welltest w2))))
  (println "New QMax: " (:qmax wt))

  (def w2 (run-calc "cmd-calibrate-string" well))

  (def tubing-head-press (:tubing-head-press calib))
  (def meas-lift-depth   (:meas-lift-depth   calib))
  (def lift-gas-rate     (:lift-gas-rate     calib))
  (def fgor              (:fgor              calib))
  (def form-gas-rate     (:form-gas-rate     calib))
  (def qmax              (:qmax              calib))

  (println "tubing-head-press " tubing-head-press)
  (println "meas-lift-depth   " meas-lift-depth)
  (println "lift-gas-rate     " lift-gas-rate)
  (println "fgor              " fgor)
  (println "form-gas-rate     " form-gas-rate)
  (println "qmax              " qmax)


  ;  (def w2 (run-calc "cmd-profile-depth" well))
  ;  (def vfp (:depth-profile-map w2))

  (def lwt (merge {:count 6} wid))
  (def wells
    (:response (dothing-sync "MatchingWells"
                             {:dsn :glue :select-set #{:field :lease :well :cmpl}})))
  (for [w wells] (println w (run-calc "LatestWellTests" (make_wt_list_well_id w))))
  (run-calc "LatestWellTests" lwt)

  (def well (:response (dothing-sync "query-well-map" wid)))
  (def well-mstr-map (:response (dothing-sync "Deref" (:well-mstr-map well))))
  (def well (:response (dothing-sync "GetFGSLiftDepth" wid)))
  (def well (run-calc "cmd-valve-status" well))
  (def well (run-calc "cmd-profile-depth" well))
  (def well (run-calc "cmd-generate-outflow-map" well))
  (def fm (run-calc "FlowMap" well))
  (def well (run-calc "cmd-generate-inflow-map" well))
  (def well (run-calc "CalcLGResponse" well)))



(comment
  (dothing "MatchingWells" {:select-set {:field :lease :well :cmpl}
                            :where-map {}}))

(defn get-all-wells [whmap]
  (let [r (dothing-sync "MatchingWells"
                        {:select-set #{:field :lease :well :cmpl}
                         :where-map whmap})]
    (if-let [wells (:response r)]
      (map #(zipmap [:field :lease :well :cmpl] %) wells))))

(comment
  ;; old code, functs no longer exist
  (defn load-well-data [dbc wk]
    (if-let [wk2 (wgdb/get-well-db-keys dbc wk)]
      (calcs/load-winglue-well-database dbc wk2))))


;; load well pile
(comment (load-file "src/winglue_well/oxy_wells.dat"))

(defn run-one-well
  [w]
  (let [runs (into {} (mapv (fn [x] [(keyword x) (dothing-sync x w)])
                            ["WellData"
                             "cmd-profile-depth"
                             "IPMCurve"
                             "cmd-profile-equilibrium-depth"
                             "cmd-generate-inflow-map"
                             "cmd-generate-outflow-map"
                             "MandrelLines"
                             "cmd-valve-status"
                             "TubingFlowRegimes"
                             "query-stored-lift-gas-response"
                             "FlowMap"
                             "LGResponse"
                             "WellTestHistory"]))
        fails (filter (fn [x] (not= (:status (deref (second x))))) runs)]
    fails))

;(defn read-all-wells
;  [dbc]
;  (let [wells (wgdb/get-matching-wells dbc
;                 {:select-set #{:field :lease :well :cmpl}})]
;    (doseq [w wells]
;      (let [wk {:field (nth w 0)
;                :lease (nth w 1)
;                :well (nth w 2)
;                :cmpl (nth w 3)}
;            wdat (load-well-data dbc wk)
;            ]
;      wdat))))

(defn run-first
  [dbspec]
  (let [all-wells (get-all-wells {})]
    (let [w (first all-wells)]
      (run-one-well w))))

(defn run-frickin-everyting
  [dbspec]
  (let [all-wells (get-all-wells {})]
    (doseq [w all-wells]
      (println w)
      (run-one-well w))))

(defn dump-table
  "dump a usual return table form in csv so it's easier to debug things"
  [tb]
  ;; tables have :cols and :vals
  (let [cols (:cols tb)
        vals (:vals tb)]
    (with-out-str
      (println (apply str (interpose "," (map #(:name %) cols))))
      (loop [vals (:vals tb)]
        (when (seq vals)
          (println (apply str (interpose "," (first vals))))
          (recur (rest vals)))))))
;;
;; this is here to help setup shop for local debugging
;; these atoms are useful for calling things
(def well-keys (atom nil))
(def well-data (atom nil))
(def dbc (atom nil))
(def well-id (atom nil))
;;
;; if you want to change wells
;(defn- setwell! [wellid]
;  (reset! well-keys (wgdb/get-well-db-keys @dbc @well-id))
;  (reset! well-data (calcs/load-winglue-well-database @dbc @well-keys)))

;; repl setup to test things
;(defn- ipl! []
;  (config/init)
;  (glcproxy/init)
;  (reset! dbc (jdbc/connection (wgdb/dbspec)))
;  (reset! well-id {:field "EAST WOLFBERRY"
;                 :lease "BARNETT"
;                 :well "2128"
;                   :cmpl "A"})
;  (setwell! @well-id)
;  @well-id)


(defn testing123
  []
  (let [mydsn :pioneer
        doit  (fn [verb arg]
                (println (format "running %s : %s" verb arg))
                (pp/pprint (dothing-sync verb arg)))]

    (doit "Hello" [{:you "are not"} "funny"])
    (doit "Insult" {:ur :ugly})

    #_(doit "GetDataSources" {})
    #_(doit "MatchingWells"
            {:dsn mydsn
             :select-set #{:fields}})
    #_(doit "BiteMe" {})))

(defn prnvs [vs]
  (println "MDpth\tPPrs\tIPrs\tTEMP\tClPrs\tOpPrs\tVBF\tVPCT\tQ\tST")
  (for [x (range (count (:status-list vs)))]
    (println (format "%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%d\t%.2f\t%d"
                     (nth (:meas-depth-list vs) x)
                     (nth (:prod-press-list vs) x)
                     (nth (:inj-press-list vs) x)
                     (nth (:temperature-list vs) x)
                     (nth (:close-press-list vs) x)
                     (nth (:open-press-list vs) x)
                     (nth (:vpc-begin-flow-list vs) x)
                     (nth (:vpc-pct-open-list vs) x)
                     (nth (:gas-flow-rate-list vs) x)
                     (nth (:status-list vs) x)))))

(defn get-all-wells "" [dsn]
  (let [wells (:response (dothing-sync "query-matching-wells"
                                       {:dsn dsn :select-set #{:field :lease :well :cmpl}}))
        wlist (into [] (for [w wells] {:dsn dsn
                                       :field (nth w 0)
                                       :lease (nth w 1)
                                       :well  (nth w 2)
                                       :cmpl  (nth w 3)}))]
    wlist))

(defn load-well [wid] (:response (dothing-sync "query-well" wid)))

(defn run-well "" [wid]
  (let [well (load-well wid)]
    (if (not (nil? (:welltest-map well))) (do (println wid "running")
                                              (let [dp (run-calc "cmd-profile-depth" wid)
                                                    calib (run-calc "cmd-calibrate-string" wid)
                                                    ip (run-calc "cmd-profile-injection-rate" wid)]
                                                (count dp)))
                                          (println wid "NO WellTest, NOT running"))))

(defn check-wt [wid]
  (let [well (load-well wid)]
    (println wid)
    (println "welltest: " (:welltest-map well) "\n")))

(defn dref [item] (dothing-sync "query-ref-from-db" item))
