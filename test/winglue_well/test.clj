(ns winglue-well.test
  (:use clojure.test)
  (:require [winglue-well.test config database ringmaster]))

;; Tips and Tricks
;; * Use #' reader macro to access private functions for tests

(run-tests
  'winglue-well.test.config
  'winglue-well.test.database
  'winglue-well.test.ringmaster)

;; (run-all-tests)
