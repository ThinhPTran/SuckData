(ns winglue-well.utils.format
  (:import [goog.date.DateTime])
  (:require [goog.date :as gdate]
            [goog.string :as gstring]
            [goog.string.format]))

(def months
  ;; 0 indexed
  ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul"
   "Aug" "Sep" "Oct" "Nov" "Dec"])

(defn format-iso-date
  "transforms iso date string to human readable date string"
  [date]
  (let [date (gdate/fromIsoString date)]
    (str (.getFullYear date) " "
         (nth months (.getMonth date)) " "
         (gstring/format "%02d" (.getDate date)) " "
         (.toUsTimeString date))))

(defn format-date
  "transforms iso date string to human readable date string"
  [date]
  (let [date (gdate/fromIsoString date)]
    (str (.getFullYear date) "-"
         (gstring/format "%02d" (.getMonth date) "-")
         (gstring/format "%02d" (.getDate date)))))

(defn- format-dec
  "Formats a number to have the specified number of decimal places"
  [num dec-places]
  (if (nil? num)
    "No data"
    (gstring/format (str "%." dec-places "f") num)))