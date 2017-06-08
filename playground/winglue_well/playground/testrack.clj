;; ========================================================================== ;;
;; Copyright (c) 2016 by AppSmiths Software LLC.  All Rights Reserved.        ;;
;; -------------------------------------------------------------------------- ;;
;; All material is proprietary to AppSmiths Software LLC and may be used only ;;
;; pursuant to license rights granted by AppSmiths Software LLC.  Other       ;;
;; reproduction, distribution, or use is strictly prohibited.                 ;;
;; ========================================================================== ;;

;; a handy place to test things

(comment
 (ns winglue-well.playground.testrack
   (:gen-class)
   (:require [winglue-well.calcs :as calcs]
             [winglue-well.database :as wgdb]
             [winglue-well.config :as config]
             [winglue-well.glcalcs-proxy :as glcalcs-proxy]
             [jdbc.core :as jdbc]
             [clojure.pprint :as pp]))

 (def dbc (future (jdbc/connection (wgdb/dbspec))))

 (defn- setup []
   (config/init)
   (glcalcs-proxy/init))

 (defn- t1
   []
   (let [wellid       {:field "EAST WOLFBERRY"
                       :lease "BARNETT"
                       :well  "2128"
                       :cmpl  "A"
                       :welltest-date "2016-May-07"}
         wellkeys     (wgdb/get-well-db-keys @dbc wellid)
         welldoc      (calcs/load-winglue-well-database @dbc wellkeys)]
     welldoc))

 (defn- wontwork [] (calcs/ppm-curve welldoc)))


