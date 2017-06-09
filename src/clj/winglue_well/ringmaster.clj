;; ========================================================================== ;;
;; Copyright (c) 2016 by AppSmiths Software LLC.  All Rights Reserved.        ;;
;; -------------------------------------------------------------------------- ;;
;; All material is proprietary to AppSmiths Software LLC and may be used only ;;
;; pursuant to license rights granted by AppSmiths Software LLC.  Other       ;;
;; reproduction, distribution, or use is strictly prohibited.                 ;;
;; ========================================================================== ;;

;; ring server for the winglue-well api

(ns winglue-well.ringmaster
  (:gen-class)
  (:use     [ring.util.response :only [response resource-response]]
            [ring.middleware.transit :only
             [wrap-transit-response wrap-transit-body]])
  (:require [jdbc.core :as jdbc]
            [clojure.pprint :as pp]
            [winglue-well.calcs :as calcs]
            [winglue-well.database.core :as wgdb]
            [winglue-well.config.core :as config]
            [winglue-well.glcalcs-proxy :as glcalcs-proxy]
            [winglue-well.mimerefs :as mrefs]
            [winglue-well.utils :as utils]
            [winglue-well.log :as log]
            [winglue-well.systemevents :as sys]
            [winglue-well.db :as mydb]
            [compojure.core :refer [defroutes POST GET]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.reload :refer [wrap-reload]]
            [org.httpkit.server :refer [run-server]]
            [environ.core :refer [env]]
            [winglue-well.database.util :as dbutil])
  (:import  gasliftcalcs.Gasliftcalcs$wellbore_data_set))

(defn- getval [v]
    (if (mrefs/refstr? v) (mrefs/realize-refstr v) v))

;; Thank you, stack overflow:
;; http://stackoverflow.com/questions/17327733/merge-two-complex-data-structures
(defn deep-merge
  "Deep merges two maps together, updates any keys with the values from the right map"
  [a b]
  (merge-with
    (fn [x y]
      (let [rx (getval x) ry (getval y)]
        (cond (map? ry) (deep-merge rx ry)
              (vector? ry) (concat rx ry)
              :else ry)))
    a b))

(defn- with-db-connection
  "create a database connection and run a function with it"
  [dsn fnc]
  (with-open [dbc (jdbc/connection (dbutil/dbspec-of dsn))]
    (fnc dbc)))

(defn- wellid?
  "sees if this thing is a wellid"
  [x]
  (and (map? x)
       (= (set (keys x)) #{:dsn :field :lease :well :cmpl})))

(defn get-well-db
  "given a well keyset, load the well document with all the fun, or nil"
  [arg]
  (let [welldoc
        (cond

          (wellid? arg)
          (wgdb/get-well arg)

          (mrefs/refstr? arg)
          (mrefs/realize-refstr arg)

          :else arg)]
    (wgdb/delay-loadify-well welldoc)))

(defn get-well-refs
  "given a well keyset, load the well document with all the fun, or nil
  but do not use delay-loaded data members
  "
  [arg]
  (let [welldoc
        (cond

          (wellid? arg)
          (wgdb/get-well arg)

          (mrefs/refstr? arg)
          (mrefs/realize-refstr arg)

          :else arg)]
    welldoc))

(defn- yield-error
  "this produces a response that contains an error"
   [msg]
  {:status 500
   :headers {"Content-Type" "text/html; charset=utf8"}
   :body msg})

(defn- no-such-well
  "produces a response indicating a well doesn't exist"
  [req]
  (yield-error (format "Well does not exist for: %s" req)))

(defn- invoke-well-method
  "standard dance for well-based methods.  get the well data,
  then run the function, dealing with errors"
  [request runme]
  (let [req (:body request)]
    (log/trace (format "Invoking well method:\n%s" (utils/to-string request)))
    (if-let [wdb (get-well-db req)]
      (response (runme wdb))
      (no-such-well req))))

(defn- invoke-merged-calc-method
  "standard dance for well-based merged calc methods.  get the well data,
  then run the function, dealing with errors"
  [request runme]
  (let [req (:body request)]
    (if-let [wdb (get-well-db req)]
      (do
        (log/trace (format "Well doc\n%s" (utils/to-string wdb)))
        (let [rsp (deep-merge (get-well-refs req) (runme wdb))]
          (log/trace (format "Returned Well doc\n%s" (utils/to-string rsp)))
          (response rsp)))
     (no-such-well req))))

(defn- log-request [req]
  (log/trace (format "Client request:\n%s" (utils/to-string req))))

;; routes for the application
(defroutes app

  ;; ----------------------------------------------------------------------
  ;; Structure: WellID
  ;; Purpose:   uniquely identifies a well
  ;;
  ;; format:  {:dsn <dsn> :field <field> :lease <lease>
  ;;           :well <well> :cmpl <cmpl>}
  ;;
  ;; ----------------------------------------------------------------------
  ;; Structure: ObjRef
  ;; Purpose:   a server produced reference to refer to an entity.  Clients
  ;;            shall treat these as opaque (not produce them, not modify them)
  ;;
  ;; format:  an opaque string of form '#^ref:...'
  ;;
  ;; ----------------------------------------------------------------------
  ;; Structure: ResultSet
  ;; Purpose:   rectangular structure produced by server for various results,
  ;;            (typically curves and such) formatted for efficiency on wire.
  ;;
  ;; format: { :cols [ {:name <name> ... } ]
  ;;           :vals [ [value ... ] ] }
  ;;
  ;; :name is required for each column.  Other metadata can be returned as
  ;; additional keyworded entities, but are not required.
  ;;
  ;; :vals is a vector of vector of value, type depends on particular method
  ;; the order of the inner vectors is always the same order as :cols
  ;;
  ;; ----------------------------------------------------------------------
  ;; Structure: WellDoc
  ;; Purpose:   Represents a well, which allows perusal or mutable reuse
  ;;            through various client reconfigurations.  A WellDoc can be
  ;;            used in lieu of a WellID for any well-based API calls.
  ;;
  ;; format: { :well-mstr-map               <ObjRef>
  ;;           :modl-ctrl               <ObjRef>
  ;;           :lgas-props              <ObjRef>
  ;;           :rsvr                    <ObjRef>
  ;;           :dsvy                    <ObjRef>
  ;;           :flow-line               <ObjRef>
  ;;           :inj-mech                <ObjRef>
  ;;           :prod-mech-list               <ObjRef>
  ;;           :welltest                <ObjRef>
  ;;           :flowing-gradient-survey-map          <ObjRef>
  ;;           :static-survey           <ObjRef>
  ;;           :buildup-survey          <ObjRef>
  ;;           :reservoir-survey        <ObjRef>
  ;;           :pvt-sample              <ObjRef>
  ;;           :scada-survey            <ObjRef>
  ;;           :mandrel-survey          <ObjRef>
  ;;           :welltracer-survey       <ObjRef>
  ;;           :welltest-hist           { <timestamp> : <ObjRef> ... }
  ;;           :flowing-gradient-survey-hist     { <timestamp> : <ObjRef> ... }
  ;;           :static-survey-hist      { <timestamp> : <ObjRef> ... }
  ;;           :buildup-survey-hist     { <timestamp> : <ObjRef> ... }
  ;;           :reservoir-survey-hist   { <timestamp> : <ObjRef> ... }
  ;;           :pvt-sample-hist         { <timestamp> : <ObjRef> ... }
  ;;           :scada-survey-hist       { <timestamp> : <ObjRef> ... }
  ;;           :mandrel-survey-hist     { <timestamp> : <ObjRef> ... }
  ;;           :welltracer-survey-hist  { <timestamp> : <ObjRef> ... }
  ;;
  ;; Notes:
  ;;
  ;; On the client side, any ObjRef can be retrieved from the
  ;; server for inspection/use (See query-ref-from-db API).
  ;;
  ;; The ObjRef's can also be replaced by the client with structure of
  ;; the same shape to facilitate client mutation in subsequent service
  ;; calls.
  ;;
  ;; The -hist items are ignored when sending back a well, they can be
  ;; dropped client side, although this is not necessary.
  ;;
  ;; Some use cases:
  ;;
  ;; All the FBHP surveys for a well can be ascertained by examining the
  ;; timestamps of :flowing-gradient-survey-hist.
  ;;
  ;; A particular survey can be retrieved by doing query-ref-from-db on the ObjRef
  ;; corresponding to the desired timestamp.
  ;;
  ;; A particular set of well test values can be used in calculations
  ;; by replacing :welltest with structure containing desired values,
  ;; and a prototype of said structure can be retrieved by query-ref-from-db prior to
  ;; modification.
  ;;
  ;; ----------------------------------------------------------------------
  ;;
  ;; Union: WellDef := WellID | WellDoc | Well-ObjRef
  ;;
  ;; notes: this bascially means that whenever a WellDef is called for,
  ;; the client can supply either the ID, the (possibly modified) well
  ;; document, or an ObjRef that refers to a well.
  ;;
  ;;=======================================================================
  ;; Method:  Hello
  ;; Purpose: useful for testing connectivity and structure conversion
  ;; in: any
  ;; out: ["If you could stop saying" <in> "That would be great"]
  (POST "/api/Hello" request
    (response ["Version: "
               (utils/tao2-version)
               "\nHello "
               (:body request)
               " from Tao2!"]))

  ;;=======================================================================
  ;; Method:  Insult
  ;; Purpose: This method is useful for testing how exceptions are handled
  ;; in: any
  ;; out: (nothing - throws exception)
  (POST "/api/Insult" request
    (throw (Exception. (format "You Made Me do this! %s" (:body request)))))

  ;;=======================================================================
  ;; Method:  query-data-sources-list
  ;; Purpose: returns the list of data sources
  ;; in: nil
  ;; out: { :<dsn> <description> ... }
  (POST "/api/query-data-sources" request
    (response (config/get-data-sources @config/tao2-cfg)))

  ;;=======================================================================
  ;; Method:  query-matching-wells-list
  ;; Purpose: find wells in a data source matching an API number
  ;; in: {:dsn <ds to look for well in>
  ;;      :api-number <apinum> }
  (POST "/api/query-well-by-api" request
    (let [body (:body request)
          api-number (:api-number body)
          dsn (:dsn body)]
      (response (wgdb/resolve-well-by-api dsn api-number))))

  ;;=======================================================================
  ;; Method:  query-matching-wells-list
  ;; Purpose: find wells in a data source matching criteria
  ;; in: { :dsn <dsn>
  ;;       [:select-set #{[:field] [:lease] [:well] [:cmpl]}]
  ;;       [:where-map {
  ;;         [ :field <field> ]
  ;;         [ :lease <lease> ]
  ;;         [ :well  <well>  ]
  ;;         [ :cmpl  <cmpl>  ]] }
  ;; out: ( ([f1] [l1] [w1] [c1]) ... )
  ;;
  ;; This solves the well selector problem.  You can specify items of interest
  ;; through :select-set, and constrain via :where-map.  The result is a
  ;; list of list, with each inner list element containing only the fields
  ;; in :select-set - always in (f, l, w, c) order, without duplicates
  ;;
  ;; use case examples:
  ;;
  ;;   give all the fields in the database
  ;;   :select-set #{:field}
  ;;
  ;;   give all the leases in field 'foo'
  ;;   :select-set #(:lease) :where-map {:field 'foo'}
  ;;
  ;;   give all the strings for well 'w1' in any lease/field
  ;;   :select-set #{:field :lease :well :cmpl}
  ;;   :where-map {:well 'w1'}
  ;;
  (POST "/api/query-matching-wells" request
    (let [body (:body request)
          dsn (:dsn body)]
      (if (nil? dsn)
        (yield-error "missing :dsn")
        (response (wgdb/get-matching-wells dsn body)))))

  ;;=======================================================================
  ;; Method:  query-well-map
  ;; Purpose: retreive the WellDoc for the specified well
  ;; in: WellId
  ;; out: { :ref   <ObjRef>
  ;;        :dsn   <dsn>
  ;;        :field <field>
  ;;        :lease <lease>
  ;;        :well  <well>
  ;;        :cmpl  <cmpl>
  ;;        :doc   <WellDoc> }
  (POST "/api/query-well" request
    (response (wgdb/get-well (:body request))))


  ;;=======================================================================
  ;; Method:  create-new-welltest-map
  ;; Purpose: Create a new well test for the specified well
  ;; in: {:well WellId :date date}
  ;; out: welltest ref
  ;; Notes:
  ;; date must be a transit date type
  (POST "/api/cmd-create-new-welltest" request
    (response {:objref (wgdb/new-welltest (:body request))}))

  ;;=======================================================================
  ;; Method:  cmd-profile-depth
  ;; Purpose: calculate a production pressure model for a well
  ;; in: WellDef
  ;; out: ResultSet (:md :tvd :flow-pres)
  (POST "/api/cmd-profile-depth" request
    (let [new-request (assoc request :body
                       (dissoc (:body request) :depth-profile-map))]
      (invoke-merged-calc-method new-request calcs/calc-wellbore)))

  ;;=======================================================================
  ;; Method:  cmd-profile-equilibrium-depth
  ;; Purpose: calculate an Equilibrium curve and associated data for a well
  ;; in: WellDef
  ;; out: ResultSet (:md :tvd :flow-pres)
  (POST "/api/cmd-profile-equilibrium-depth" request
    (invoke-merged-calc-method request calcs/calc-eq-curves))

  ;;=======================================================================
  ;; Method:  cmd-profile-fbhp
  ;; Purpose: calculate an Inflow and Outflow curves for a well
  ;; in: WellDef
  ;; out: ResultSet (:liquid-rate :fbhp)
  (POST "/api/cmd-profile-fbhp" request
    (invoke-merged-calc-method request calcs/calc-inflow-outflow))

  ;;=======================================================================
  ;; Method:  cmd-flow-regime-map
  ;; Purpose: return the stored lift-gas response curve
  ;; in: WellDef
  ;; out: ResultSet (:stratified_smooth
  ;;                 :stratified-wavy
  ;;                 :stratified-dispersed
  ;;                 :annular
  ;;                 :annular-dispersed
  ;;                 :slug
  ;;                 :churn
  ;;                 :bubble
  ;;                 :dispersed-bubble
  ;;                 :single-phase)
  ;; Notes:
  ;;
  ;; Each key/value pair in the result set:
  ;;    * exists only when generated by the calculation results
  ;;    * has a value that is a map with these keys:
  ;;                  :gas_velocity
  ;;                  :liquid_velocity
  ;;        * the values associated with these keys are vectors
  ;;        * the vectors are the same length (for each ResultSet key)
  ;;          i.e., this always returns true (if :slug exists is result_set):
  ;;                (=
  ;;                    (count (:gas_velocity (:slug result_set)))
  ;;                    (count (:liquid_velocity (:slug result_set))))
  ;;        * each vector pair (gas and liquid velocity) at a given index
  ;;          is the coordinates of a polygon outline for that regime
  ;;
;  (POST "/api/cmd-flow-regime-map" request
;    (invoke-merged-calc-method request calcs/calc-flowmap))

  ;;=======================================================================
  ;; Method:  cmd-profile-injection-rate
  ;; Purpose: calculates the lift-gas response curve for a well
  ;; in: WellDef
  ;; out: ResultSet (:lift_gas_rate :uncal_oil_rate :uncal_water_rate
  ;;                 :lift_meas_depth :inj_press :ftp :fbhp
  ;;                 :op_mandrel_ndx :op_inj_press
  ;;                 :op_prod_press :op_upst_press
  ;;                 :op_upst_surf_press :op_flow_rate
  ;;                 :stability_num)
  (POST "/api/cmd-profile-injection-rate" request
    (let [new-request (assoc request :body
                         (dissoc (:body request) :lgr-curves-map))]
        (invoke-merged-calc-method new-request calcs/calc-lgr-curves)))

  ;;=======================================================================
  ;; Method:  query-latest-welltests
  ;; Purpose: get Last n welltest which are not INFO-ONLY
  ;; in: WellId
  ;; out: ResultSet(
  ;;        :info-only :calib-wellhead-choke-id :description :separator-press
  ;;        :productivity-index :date :mismatchbg-tol :calib-formation-gas-rate
  ;;        :downstream-flowline-press :qmax-liquid :comments :separator-name
  ;;        :meas-water-rate :meas-form-gas-rate :meas-lift-gas-rate :meas-casing-head-press
  ;;        :calib-flowing-tubing-press :inj-rate :meas-oil-rate :est-fbhp
  ;;        :calib-casing-head-press :calib-lift-gas-rate :calib-oil-rate
  ;;        :meas-choke-size :meas-flowing-tubing-press :est-lift-depth
  ;;        :calib-water-rate :test-duration)
  ;;
  ;; Note: converts the well test list into a table to enable UI manipulation
  ;;
  (POST "/api/query-latest-welltests" request
    (let [req (:body request)
          num (get req :count 5)]
      (with-db-connection (:dsn req)
        (fn [dbc]
          (if-let [wdbkys (wgdb/get-well-db-keys dbc req)]
            (response (calcs/last-n-welltests dbc (:pwi wdbkys) num))
            (no-such-well req))))))

  ;;=======================================================================
  ;; Method:  query-schema-map
  ;; Purpose: return the wellbore_data_set data schema
  ;; in: nil
  ;; out: ResultSet (:liquid-rate :fbhp)
  (POST "/api/query-schema" request
    (response (utils/get-schema gasliftcalcs.Gasliftcalcs$wellbore_data_set)))

  ;;=======================================================================

  ;; Method:  cmd-calibrate-string
  ;; Purpose: calibrateds the given well based on the current FGS
  ;; in: WellDef
  ;; out: ResultSet WellDef + (:welltest :calibration-results)
  (POST "/api/cmd-calibrate-string" request
    (let [body (dissoc (:body request) :depth-profile-map)
          calib-map (dissoc (:calibration-map body) :message-list :ppm-replay-list)
          new-request (assoc request :body
                         (assoc body :calibration-map calib-map))]
        (invoke-merged-calc-method new-request calcs/calibrate-single)))

  ;;=======================================================================
  ;; Method:  query-ref-from-db
  ;; Purpose: dereference objects
  ;; in: ObjRef | [ObjRef ...]
  ;; out: Realized Object | [Realized Object ...] ; structure depends on type
  (POST "/api/query-ref-from-db" request
    (let [arg (:body request)
          r  (cond (seq? arg)          (mrefs/realize-refstrs arg)
                   (mrefs/refstr? arg) (mrefs/realize-refstr arg))]
      (response r)))

  ;;=======================================================================
  ;; Method:  cmd-update-to-db
  ;; Purpose: apply updates to objects and save to underlying database
  ;; in: { :objref <refstr> :updates <map> }
  ;; out: result
  (POST "/api/cmd-update-to-db" request (do)
        (let [{:keys [objref updates]} (:body request)]
          (if (nil? objref) (throw (Exception. ":objref required")))
          (if (nil? updates) (throw (Exception. ":updates required")))
          (response {:result (mrefs/update-refstr objref updates)})))

  ;; this here to serve web content, if any
  (GET  "/" [] (resource-response "public/index.html"))
  (GET  "/chsk" req (sys/ring-ws-handoff req))
  (POST "/chsk" req (sys/ring-ws-post req))
  (POST "/login" req (sys/login-handler req))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

  ;; defroute

(defn- format-exception
  "creates a string describing exception"
  [ex]
  (let [sw (java.io.StringWriter.)]
    (binding [*out* sw]
      (println "Exception Occured: " (.getMessage ex))
      (println "Traceback follows:")
      (doseq [st (.getStackTrace ex)]
        (println (.toString st))))
    (.toString sw)))

(defn wrap-exceptions [handler]
  "Turns exceptions into HTTP error responses"
  (fn [request]
    (let [foo
          (try (handler request)
               (catch Exception e
                 {:status 500
                  :headers {"Content-Type" "text/plain"}
                  :body (format-exception e)}))]
      foo)))

(defn wrap-log
  "Ring middleware for logging incoming TAO2 requests"
  [handler]
  (fn [request]
    (do
      (log-request request)
      (handler request))))

(defn- wrap-request-logging [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [resp (handler req)]
      (println (name request-method) (:status resp)
               (if-let [qs (:query-string req)]
                 (str uri "?" qs) uri))
      resp)))

(def wrapped-app
  (-> app
      (wrap-reload)
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :put :post :delete])
      (wrap-transit-response {:encoding :json, :opts {}})
      (wrap-transit-body)
      (wrap-log)
      (wrap-exceptions)
      (handler/site)
      (wrap-request-logging)))



(defn -main [& args]
  (println (format "===================================\nTao2 Version : %s\n===================================\n" (utils/tao2-version)))
  (config/load-config!)
  (config/init-logging @config/tao2-cfg)
  (config/set-db-connections (wgdb/get-data-source-revs))
  (sys/ws-message-router)
  (glcalcs-proxy/init)
  ;(let [port (or (:server-port @config/tao2-cfg) 3000)]
  ;  (println (format "Starting server on port: %d\n" port))
  ;  (println "Selected Config");
  ;  (pp/pprint @config/tao2-cfg)
  ;  (run-server wrapped-app {:ip "192.168.1.3" :port port :join? false}))
  (mydb/suckdata)
  (let [wells (:wells @mydb/persist-atom)
        dsn (map #(:dsn %) wells)
        wellinfo (map #(:well %) wells)]
    (println (str "dsn: " (pr-str dsn)))
    (println (str "well: " (pr-str wellinfo))))
  (glcalcs-proxy/destroy))
