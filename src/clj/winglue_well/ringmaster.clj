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
  (:use     [ring.util.response :only [redirect response resource-response]]
            [ring.middleware.transit :only
             [wrap-transit-response wrap-transit-body]])
  (:require [jdbc.core :as jdbc]
            [clojure.pprint :as pp]
            [winglue-well.config.core :as config]
            [winglue-well.utils :as utils]
            [winglue-well.systemevents :as sys]
            [winglue-well.db :as mydb]
            [compojure.core :refer [defroutes POST GET]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [winglue-well.oauth :as oauth]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.reload :refer [wrap-reload]]
            [org.httpkit.server :refer [run-server]]
            [environ.core :refer [env]]))

;; routes for the application
(defroutes app
  (GET "/" []
    (redirect (:uri oauth/auth-request)))
  (GET "/google-oauth" []
    (redirect (:uri oauth/auth-request)))
  (GET "/oauth2callback" []
    oauth/verifed-appsmiths)

  ;; this here to serve web content, if any
  ;(GET  "/" [] (resource-response "public/index.html"))
  (GET  "/chsk" req (sys/ring-ws-handoff req))
  (POST "/chsk" req (sys/ring-ws-post req))
  (POST "/login" req (sys/login-handler req))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

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
      (wrap-exceptions)
      (handler/site)
      (wrap-request-logging)))



(defn -main [& args]
  (println (format "===================================\nTao2 Version : %s\n===================================\n" (utils/tao2-version)))
  (config/load-config!)
  (sys/ws-message-router)
  (let [port (or (:server-port @config/tao2-cfg) 3000)]
    (println (format "Starting server on port: %d\n" port))
    (println "Selected Config");
    (pp/pprint @config/tao2-cfg)
    (run-server wrapped-app {:port port :join? false})))
