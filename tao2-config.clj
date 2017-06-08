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
 ;; http://firebirdsql.org/file/Jaybird_2_1_JDBC_driver_manual.pdf firebird
 :data-sources
 { :pioneer
  {:description "Tao2 Firebird Database"
   :classname   "org.firebirdsql.jdbc.FBDriver"
   :subprotocol "firebirdsql"
   :subname     "//localhost:3050//home/setup/databases/Petronas.FDB"
   :user "glueuser"
   :password "glue"}}



 ;; port where the web server serves
 :server-port 3000

 ;; parameters for glcalcs
 :glcalcs
 {
  ;; if true, the path to the executable is specified below
  :glcalcs-override true

  ;; where the executable lives, else expected in jar
;;  :path "v:/gdrive/a/share-outside-appsmiths/share-beta-testers/for-pune/2016-08-16-tue-release-jel/glcalcs.exe"
  :path "/home/debtao/Programming/Clojure/LearnWinglueWell/glcalcs/glcalcs/glcalcs"
  ;;:path "v:/gdrive/a/share-outside-appsmiths/share-beta-testers/for-pune/glcalcs/glcalcs.exe"

  ;; timeout to deem a server hung
  :timeo 20000

  ;; number of worker processes
  :worker-count 4

  ;; ports used on front and backend of 0MQ router/dealer pair
  ;;:frontend "tcp://localhost:4108"
  ;;:backend "tcp://localhost:4109"
  :frontend "tcp://localhost:2112"
  :backend "tcp://localhost:2113"}}


