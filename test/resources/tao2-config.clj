;; ========================================================================== ;;
;; Copyright (c) 2016 by AppSmiths Software LLC.  All Rights Reserved.        ;;
;; -------------------------------------------------------------------------- ;;
;; All material is proprietary to AppSmiths Software LLC and may be used only ;;
;; pursuant to license rights granted by AppSmiths Software LLC.  Other       ;;
;; reproduction, distribution, or use is strictly prohibited.                 ;;
;; ========================================================================== ;;

;; configuration file for tao2 server
;; this is all one clojure map
;;
;; search order for finding this file:
;; - TAO2_CONFIG environment variable
;; - /etc/tao2-config.clj
;; - ./tao2-config.clj
;; - resources/tao2-config.clj in jar file
;; - there is a hard-wired fallback, which you likely will not enjoy
;;   hard coded in config.clj
{
 ;; define the data sources
 :data-sources
 {:wgdb
  {:description "WinGLUE Firebird Database Rev. 3.16"
   :classname   "org.firebirdsql.jdbc.FBDriver"
   :subprotocol "firebirdsql"
   :subname     "//localhost:3050//db/glue_3_16.fdb"
   :user "glueuser"
   :password "glue"}}
 ;:oxy-oracle
 ;{:description "Oxy Database Housed in Oracle"
 ; :classname   "oracle.jdbc.OracleDriver"
 ; :subprotocol "oracle"
 ; :subname     "thin:@oravmch:1521/orcl.localdomain"
 ; :user        "oxy"
 ; :password    "oxy"}



 ;; port where the web server serves
 :server-port 3000

 ;; logger settings
 :log-file "./tao-2.log"         ; nil to turn off
 :log-console false              ; true/false
 :log-database nil               ; future..., e.g. :tao2
 :log-level :trace               ; :trace, :debug, :info, :warn, :error, :off

 ;; parameters for glcalcs
 :glcalcs
 {
  ;; if true, the path to the executable is specified below
  :glcalcs-override true

  ;; where the executable lives, else expected in jar
  :path "/home/max/projects/tao2-build/glcalcs/glcalcs"

  ;; timeout to deem a server hung
  :timeo 1000

  ;; number of worker processes
  :worker-count 5

  ;; ports used on front and backend of 0MQ router/dealer pair
  :frontend "tcp://localhost:4108"
  :backend "tcp://localhost:4109"}}


