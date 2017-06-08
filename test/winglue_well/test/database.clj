(ns winglue-well.test.database
  (:use clojure.test)
  (:require [clojure.string :as clj-string]
            [winglue-well.config.core :as cfg]
            [winglue-well.database.core :as wgdb]
            [winglue-well.database.util :as dbutil]
            [jdbc.core :as jdbc]
            [clojure.spec :as s]))

;; Some tests for the DB
;; I don't know if we will change the db soon so I've just done a couple of tests
;; as reference.

(deftest mykw
  (is (= :com.appsmiths.wgdb.kw (#'wgdb/mykw "kw"))))

(defn setup-db!
  []
  (cfg/set-config! (#'cfg/via-file "test/resources/tao2-config.clj"))
  (cfg/init-logging @cfg/tao2-cfg)
  (cfg/set-db-connections (wgdb/get-data-source-revs)))

(deftest simple-db-tests
  (setup-db!)
  (with-open [dbc (jdbc/connection (dbutil/dbspec-of :wgdb))]
    (is (= 316 (wgdb/get-db-rev dbc)) "WGDB should be rev 3.16")))

(deftest db-query-tests
  (setup-db!)
  (with-open [dbc (jdbc/connection (dbutil/dbspec-of :wgdb))]
    (is (= 4 (count (wgdb/get-matching-wells :wgdb {:select-set {:field :lease :well :cmpl}
                                                    :where-map {:field "EXAMPLE"}})))
        "There should be 4 wells in EXAMPLE field in wgdb")
    (let [dbkeys (wgdb/get-well-db-keys dbc {:field "EXAMPLE" :lease "A" :well "A1" :cmpl "L"})
          cmpl-num (:cmpl dbkeys)
          pwi (:pwi dbkeys)]
      (is (= 1 pwi) "PWI number of A1 should be 1")
      (is (= 1 cmpl-num) "CMPL number of A1 should now be 1")
      (is (s/valid? ::wgdb/well-master (wgdb/get-well-master dbc pwi :wgdb)))
      (is (= 1 (count (wgdb/get-welltest-history dbc cmpl-num))) "1 Well Test in A1")
      ;; The format of the well test queries seems all over the place right now so skip these for now
      (is (s/valid? ::wgdb/dsvy (wgdb/get-dsvy dbc cmpl-num))))))
