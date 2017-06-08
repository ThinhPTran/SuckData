(ns winglue-well.test.config
  (:use clojure.test)
  (:require [winglue-well.config.core :as cfg])
  (:import (clojure.lang ExceptionInfo)))

(deftest via-file
  (is (nil? (#'cfg/via-file "asdf")))
  (is (some? (#'cfg/via-file "test/resources/tao2-config.clj")))
  (let [config (#'cfg/via-file "test/resources/tao2-config.clj")]
    (is (= config (cfg/set-config! config)) "set-config! should return the config if it was valid"))
  (is (thrown? ExceptionInfo (cfg/set-config! (#'cfg/via-file "test/resources/tao2-inavlid-config.clj")))))
