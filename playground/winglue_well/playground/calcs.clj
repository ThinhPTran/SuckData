(ns winglue-well.playground.calcs
  (:use [winglue-well.calcs]))

;; ============================================================================
;; helpful stuff and examples
; Test Wel ID
(def testid  {:field "EXAMPLE" :lease "A" :well "A1" :cmpl "S"})

(comment
  (load-file "src/winglue_well/calcs.clj")
  (ns winglue-well.calcs)
  (config/init)
  (def testid  {:field "EXAMPLE" :lease "A" :well "A1" :cmpl "L" :dsn :glue})
  (def well (wgdb/delay-loadify-well (wgdb/get-well testid)))
  (def ppm (ppm-curve well))

  (def pwell (wellbore-data-set well))
  (def modctrl (ModelControl well))
  (println modctrl)

  (def wgwell ( well))

  (def welltest-map (WellTest well))
  (println welltest-map)

  (require '[clj-time.coerce :as time-coerce])
  (require '[clj-time.core :as time-core])
  (require '[jdbc.core :as jdbc])
  (require '[honeysql.core :as sql])
  (require '[honeysql.helpers :refer :all])
  (config/init)
  (def datasrcs (config/get-data-sources @config/tao2-cfg))
  (def dsn :pioneer)
  (def dsn :glue)

  (config/init)
  (def testid  {:field "EXAMPLE" :lease "A" :well "A1" :cmpl "L" :dsn :pioneer})
  (def well (wgdb/get-well testid))

  (load-file "src/winglue_well/calcs.clj")
  (ns winglue-well.calcs)
  (config/init)
  (def testid {:dsn :tao2 :field "WT" :lease "EX" :well "4" :cmpl "LS"})
  (def well (wgdb/get-well testid))
  (def w (wgdb/delay-loadify-well well))
  (def wb (calc-wellbore w))

  (load-file "src/winglue_well/calcs.clj")
  (ns winglue-well.calcs)
  (config/init)
  (def testid {:dsn :glue :field "EXAMPLE" :lease "A" :well "A1" :cmpl "L"})
  (def well (wgdb/get-well testid))
  (def w (wgdb/delay-loadify-well well))
  (def flowmap (calc-flowmap w))

  ; Test Wel ID for Oxy
  ; (def testid {:field "COLLIE" :lease "MHPR" :well "001" :cmpl "H"})
  ; database connection
  (def dbc (jdbc/connection (wgdb/dbspec)))
  (def wkeys        (wgdb/get-well-db-keys dbc testid))
  (def pwi          (:pwi  wkeys))
  (def cmpl         (:cmpl wkeys))

  (def modl-ctrl-map    (:modl-ctrl-map  well))
  (def well-mstr-map    (:well-mstr-map  well))
  (def rsvr-map         (:rsvr-map       well))
  (def rsvr_hist    (:rsvr_hist  well))
  (def welltest     (:welltest-map   well))
  (def dsvy-map         (:dsvy-map       well))
  (def lgas-props-map   (:lgas-props-map well))
  (def flow-line-map    (:flow-line-map  well))
  (def valves       (:mandrel-survey-map     well))
  (def inj-mech-map     (:inj-mech-map   well))
  (def prod-mech-map    (:prod-mech-map  well))
  (def pvt-sample-map   (:pvt-sample-map well))
  (def lgas-perf-settings-map    (:lgas-perf-settings-map  well))

  (def ppm          (calc-ppm-curves     well))
  ;(def ipm          (calc-ipm-curves     well))
  (def equilib      (calc-eq-curves      well))
  (def outflow      (calc-outflow-curves well))
  (def valve-status (calc-valve-status   well))
  (def mandrels     (calc-mandrels       well))
  (def ipr          (calc-ipr-curve      well))
  (def flowmap      (calc-flowmap        well))
  (def well2 (merge well {:lgas-perf-settings-map
                          (merge (:lgas-perf-settings-map well)
                                 {:oil-slope-correction 0.85
                                  :oil-rate-correction 40.0})})))

; MdtvdProperties
; WellTest
; ReservoirProperties
; FlowLineSegment
; FlowLineSegments
; GlvalveData
; BuildValveModel
; PipeSegment
; PipeString
; LGRCurveControl
; ModelControl
; WellMaster
; LiftGasProperties
; wellbore-data-set

; calc-ipr-curve
; calc-ppm-curves
; calc-eq-curves
; calc-outflow-curves
; calc-ipr
; calc-flowmap
; calc-lgr-curves
; ppm-curve
; eq-curve
; inflow-curve
; outflow-curve
; tubing-flow-regimes
; lift-gas-response
; calc-lift-gas-response
; parse-fm-pset
; flowmap
; last-n-welltests

(comment
  (defn coldstart-repl
    []
    (ns winglue-well.calcs)
    (config/init)
    (def dsn :pioneer)
    (def dbc (jdbc/connection (wgdb/dbspec-of dsn)))
    (def wellident
      {:field "EXAMPLE" :lease "A" :well "A1" :cmpl "L" :dsn dsn})
    (def wellident
      {:field "HUTT C" :lease "HUTT_C" :well "3H" :cmpl "1"
       :dsn dsn})
    (def wellkeys (wgdb/get-well-db-keys dbc wellident))))

;(println "calcs loaded")