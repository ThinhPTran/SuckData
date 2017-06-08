(ns winglue-well.test.ringmaster
  (:use clojure.test)
  (:require [winglue-well.ringmaster :as ringmaster]
            [ring.mock.request :as mock]
            [winglue-well.config.core :as cfg]
            [winglue-well.glcalcs-proxy :as glcalcs-proxy]
            [winglue-well.database.core :as wgdb]))

(defn setup-ringmaster!
  "Setup the configs dbs glcalcs etc. for testing the api end points.
   Good for REPL use too!"
  []
  (cfg/set-config! (#'cfg/via-file "test/resources/tao2-config.clj"))
  (cfg/init-logging @cfg/tao2-cfg)
  (cfg/set-db-connections (wgdb/get-data-source-revs))
  (glcalcs-proxy/init))

(def wrapped-app
  (-> ringmaster/app
      (ringmaster/wrap-exceptions)))

(defn =keys
  "Returns true if testmap's keyvals are equal to test's respective keyvals"
  [test testmap]
  (= (select-keys test (keys testmap)) testmap))

(deftest apitest
  (setup-ringmaster!)
  (is (=keys (wrapped-app (-> (mock/request :post "/api/Hello")
                              (mock/body "Hello")))
             {:status 200}))

  (is (=keys (wrapped-app (-> (mock/request :post "/api/Insult")
                              (mock/body "Fuck off")))
             {:status 500})))
