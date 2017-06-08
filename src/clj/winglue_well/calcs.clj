;; ========================================================================== ;;
;; Copyright (c) 2016 by AppSmiths Software LLC.  All Rights Reserved.        ;;
;; -------------------------------------------------------------------------- ;;
;; All material is proprietary to AppSmiths Software LLC and may be used only ;;
;; pursuant to license rights granted by AppSmiths Software LLC.  Other       ;;
;; reproduction, distribution, or use is strictly prohibited.                 ;;
;; ========================================================================== ;;

(ns winglue-well.calcs
  (:gen-class)
  (:use flatland.protobuf.core)
  (:require [winglue-well.database.core :as wgdb]
            [winglue-well.database.util :as dbutil]
            [winglue-well.config.core :as config]
            [winglue-well.utils :as util]
            [winglue-well.log :as log]
            [zeromq.zmq :as zmq])
  (:import reqrep.Reqrep$Request
           reqrep.Reqrep$Response
           gasliftcalcs.Gasliftcalcs$MdtvdProperties
           gasliftcalcs.Gasliftcalcs$IprCurve
           gasliftcalcs.Gasliftcalcs$IprCurveControl
           gasliftcalcs.Gasliftcalcs$GlvalveData
           gasliftcalcs.Gasliftcalcs$GlValveStatus
           gasliftcalcs.Gasliftcalcs$GLValveStatusSet
           gasliftcalcs.Gasliftcalcs$LiftGasProperties
           gasliftcalcs.Gasliftcalcs$WellTest
           gasliftcalcs.Gasliftcalcs$ReservoirProperties
           gasliftcalcs.Gasliftcalcs$PvtProperties
           gasliftcalcs.Gasliftcalcs$PipeSegment
           gasliftcalcs.Gasliftcalcs$FlowLineSegment
           gasliftcalcs.Gasliftcalcs$TempData
           gasliftcalcs.Gasliftcalcs$CfpData
           gasliftcalcs.Gasliftcalcs$VfpData
           gasliftcalcs.Gasliftcalcs$DepthProfile
           gasliftcalcs.Gasliftcalcs$EquilibriumCurveData
           gasliftcalcs.Gasliftcalcs$OutflowCurveData
           gasliftcalcs.Gasliftcalcs$FlowMapData
           gasliftcalcs.Gasliftcalcs$FlowMapDataSet
           gasliftcalcs.Gasliftcalcs$LgrCurvesData
           gasliftcalcs.Gasliftcalcs$LGRCurveControl
           gasliftcalcs.Gasliftcalcs$ValvePerformanceSet
           gasliftcalcs.Gasliftcalcs$FlowingSurveyDetail
           gasliftcalcs.Gasliftcalcs$FlowingSurvey
           gasliftcalcs.Gasliftcalcs$WellMaster
           gasliftcalcs.Gasliftcalcs$ModelControl
           gasliftcalcs.Gasliftcalcs$DepthProfile
           gasliftcalcs.Gasliftcalcs$AboveBelowRange
           gasliftcalcs.Gasliftcalcs$MinMaxRange
           gasliftcalcs.Gasliftcalcs$wellbore_data_set

           gasliftcalcs.Gasliftcalcs$SingleFloatResponse
           gasliftcalcs.Gasliftcalcs$MultiPointResponse
           gasliftcalcs.Gasliftcalcs$ValvePerfCheckResponse
           gasliftcalcs.Gasliftcalcs$InjectionMatchResponse
           gasliftcalcs.Gasliftcalcs$CalibrationSet
           gasliftcalcs.Gasliftcalcs$CalibrationResults

           ;; protobuf enums
           gasliftcalcs.Gasliftcalcs$IprModel
           gasliftcalcs.Gasliftcalcs$ValveStatus
           gasliftcalcs.Gasliftcalcs$ValveType
           gasliftcalcs.Gasliftcalcs$CompositionModel
           gasliftcalcs.Gasliftcalcs$DensityModel
           gasliftcalcs.Gasliftcalcs$ViscosityModel
           gasliftcalcs.Gasliftcalcs$CriticalModel
           gasliftcalcs.Gasliftcalcs$PvtModel
           gasliftcalcs.Gasliftcalcs$InjectionModel
           gasliftcalcs.Gasliftcalcs$FlowLinePressModel
           gasliftcalcs.Gasliftcalcs$TemperatureModel
           gasliftcalcs.Gasliftcalcs$FlowRegime
           gasliftcalcs.Gasliftcalcs$ProductionPressureModel
           gasliftcalcs.Gasliftcalcs$LiftDepthModel
           gasliftcalcs.Gasliftcalcs$LgrCurveType
           gasliftcalcs.Gasliftcalcs$FGSTempType))


(defn pb->json [msg]
  (let [js-printer (com.google.protobuf.util.JsonFormat/printer)]
    (.print js-printer msg)))

;; shared 0mq context
(def ^:dynamic *zctx* (zmq/context))

(defn- api-transact-blocking
  "makes one blocking round-trip transaction to glcalcs API"
  [method payload]
  (with-open [sock (zmq/socket *zctx* :req)]
    (zmq/set-linger sock 0) ;; when closed, don't worry about runout
    (zmq/connect sock (get-in @config/tao2-cfg [:glcalcs :frontend])) ;; frontend of glcalcs-proxy

    ;; build the request part
    (let [rq-builder (Reqrep$Request/newBuilder)]
      (.setMethod rq-builder method)

      (log/trace method)
      (log/trace (pb->json payload))

      ;; send the request part
      (zmq/send sock
                (.toByteArray (.build rq-builder)) zmq/send-more)

      ;; send the payload part
      (zmq/send sock (.toByteArray payload))

      ;; << glcalcs does work here >>

      ;; recieve the reply part
      (let [rp-bytes (zmq/receive sock zmq/send-more)
            rp-builder (Reqrep$Response/newBuilder)]
        (.mergeFrom rp-builder rp-bytes)
        (let [reply (.build rp-builder)]
          ;; if the reply has an "outcome" then raise exception
          (if-let [has-outcome (> (count (.getOutcome reply)) 0)]
            (throw (Exception.
                    (str (.getOutcome reply)
                         (.getExceptionInfo reply))))))))

    ;; receive and yield the payload part
    (zmq/receive sock)))

(defn- api-transact
  "runs a blocking api transaction in a future, throws exception if times out"
  [method payload ret-proto]
  (let [ret-class (Class/forName ret-proto)
        timeout (:timeo (:glcalcs @config/tao2-cfg)) ;; from config
        fut     (future (api-transact-blocking method payload))
        outcome (util/protobuf-to-map ret-class
                  (util/str-invoke-static ret-proto "parseFrom"
                    (deref fut timeout :timed-out)))]
    (if (= :timed-out outcome)
      (throw (Exception. "glcalcs operation timed out"))
      outcome)))

(defn- ts-to-epoch
  "Convert a datetime string to epoch as uint64"
  [ts]
  (let [jdt (dbutil/str->jodatime ts)] (/ (.getMillis jdt) 1000)))

(defn pbuf-bytes
  "produce the serialized bytes from a protobuf"
  [pbuf]
  (.toByteArray pbuf))



(defn load-welltest
  "if :welltest-date in dbkeys, use that one, otherwise return the
   most recent welltest"
  [dbc pwi dbkeys]
  (let [wtd
        (if-let [d (:welltest-date dbkeys)]
          d
          (first (wgdb/get-welltest-history dbc pwi)))]
    (if-let [r (wgdb/get-welltest dbc pwi wtd)]
      (util/augment-welltest r)
      nil)))
      ;; return some kind of diagnostic on missing data???

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; next bank is functions that setup calc input protobufs with various data
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- MdtvdProperties
  "Generate a Dir Survey protobuf"
  [{:keys [dsvy-map]}]
  (if (not (or (nil? @dsvy-map) (< (count (keys @dsvy-map)) 2)))
   (let [b (Gasliftcalcs$MdtvdProperties/newBuilder)]
     (doto b
       (.addAllMeasDepthList (into [] (map double (:meas-depth-list  @dsvy-map))))
       (.addAllVertDepthList (into [] (map double (:vert-depth-list @dsvy-map)))))
     (.build b))
   (let [b (Gasliftcalcs$MdtvdProperties/newBuilder)]
     (doto b
       (.addAllMeasDepthList [0])
       (.addAllVertDepthList [0]))
     (.build b))))


(defn- WellTest
  "Generate a (mostly) full Well Test protobuf"
  [{:keys [welltest-map]}]
  {:pre [(not-any? nil? [@welltest-map])]}
  (let [b (Gasliftcalcs$WellTest/newBuilder)]
    (doto b
      (.setIgnoreAsBad             (:ignore-as-bad @welltest-map))
      (.setTestDuration            (util/getdefval (:test-duration-hours   @welltest-map) 0))
      (.setSeparatorPress          (util/getdefval (:separator-press        @welltest-map) 0))
      (.setSeparatorName           (util/getdefval (:separator-name        @welltest-map) ""))
      (.setDownstreamFlowlinePress (util/getdefval (:downstream-flowline-press        @welltest-map) 0))
      (.setCalibOilRate            (util/getdefval (:calib-oil-rate       @welltest-map) 0))
      (.setCalibWaterRate          (util/getdefval (:calib-water-rate     @welltest-map) 0))
      (.setCalibFormationGasRate   (util/getdefval (:calib-formation-gas-rate      @welltest-map) 0))
      (.setCalibLiftGasRate        (util/getdefval (:calib-lift-gas-rate      @welltest-map) 0))
      (.setCalibFlowingTubingPress (util/getdefval (:calib-flowing-tubing-press       @welltest-map) 0))
      (.setCalibCasingHeadPress    (util/getdefval (:calib-casing-head-press       @welltest-map) 0))
      (.setCalibWellheadChokeId    (util/getdefval (:calib-wellhead-choke-id     @welltest-map) 0))
      (.setMeasOilRate             (util/getdefval (:meas-oil-rate        @welltest-map) 0))
      (.setMeasWaterRate           (util/getdefval (:meas-water-rate      @welltest-map) 0))
      (.setMeasFormGasRate         (util/getdefval (:meas-form-gas-rate       @welltest-map) 0))
      (.setMeasLiftGasRate         (util/getdefval (:meas-lift-gas-rate       @welltest-map) 0))
      (.setMeasFlowingTubingPress  (util/getdefval (:meas-flowing-tubing-press        @welltest-map) 0))
      (.setMeasCasingHeadPress     (util/getdefval (:meas-casing-head-press        @welltest-map) 0))
      (.setMeasWellheadChokeId     (util/getdefval (:meas-wellhead-choke-id @welltest-map) 0))
      (.setEstFbhp                 (util/getdefval (:est-fbhp        @welltest-map) 0))
      (.setQmaxLiquid              (util/getdefval (:qmax-liquid     @welltest-map) 0))
      (.setProductivityIndex       (util/getdefval (:productivity-index @welltest-map) 0))
      (.setEstLiftDepth            (util/getdefval (:est-lift-meas-depth  @welltest-map) 0))
      (.setLiftGasMismatchTol      (util/getdefval (:lift-gas-mismatch-tol  @welltest-map) 0))
      (.setInjRate                 (util/getdefval (:inj-rate        @welltest-map) 0))
      (.setDate                    (ts-to-epoch (:welltest-date @welltest-map))))
    (.build b)))


(defn- ReservoirProperties
  "Generate a Reservoir data protobuf"
  [{:keys [rsvr-map reservoir-survey]}]
  (if (not (or (nil? rsvr-map) (nil? reservoir-survey)))
    (let [b (Gasliftcalcs$ReservoirProperties/newBuilder)]
     (doto b
       (.setWellApiGravity   (util/getdefval (:well-api-gravity   @rsvr-map) 0))
       (.setBubblePointPress (util/getdefval (:bubble-point-press @rsvr-map) 0))
       (.setMaxInflow        (util/getdefval (:max-inflow         @rsvr-map) 0))
       (.setOilFvf           (util/getdefval (:oil-fvf            @rsvr-map) 0))
       (.setRsvrFluidVisc    (util/getdefval (:rsvr-fluid-visc    @rsvr-map) 0))
       (.setRsvrGasSpg       (util/getdefval (:rsvr-gas-spg       @rsvr-map) 0))
       (.setRsvrWaterSpg     (util/getdefval (:rsvr-water-spg     @rsvr-map) 0))
       (.setPerfBht          (util/getdefval (:perf-bht           @rsvr-map) 0))
       (.setWlhdStaticTemp   (util/getdefval (:wlhd-static-temp   @rsvr-map) 0))
       (.setRsvrFluidPerm    (util/getdefval (:rsvr-fluid-perm    @rsvr-map) 0))
       (.setInflowSkin       (util/getdefval (:inflow-skin        @rsvr-map) 0))
       (.setNetTvt           (util/getdefval (:net-tvt            @rsvr-map) 0))
       (.setDrainageRadius   (util/getdefval (:drainage-radius    @rsvr-map) 0))
       (.setWellboreRadius   (util/getdefval (:wellbore-radius    @rsvr-map) 0))
       (.setPerfTopMeasDepth (util/getdefval (:perf-top-md        @rsvr-map) 0))
       (.setDarcysLaw        (:darcys-law @rsvr-map))
       (.setRsvrOilSpg       (util/getdefval (:rsvr-oil-spg       @rsvr-map) 0))
       (.setZone             (util/getdefval (:zone               @rsvr-map) ""))
       (.setHcRsvr           (util/getdefval (:hc-rsvr            @rsvr-map) 0))
       (.setRsvrPvtModl      (Gasliftcalcs$PvtModel/valueOf
                              (util/getdefval (:rsvr-pvt-modl     @rsvr-map) 0)))
       (.setSbhp             (util/getdefval (:sbhp @reservoir-survey) 0)))
     (.build b))))


(defn- FlowLineSegment
  "Generate a protobuf for a single flow line tubing segment"
  ([edst eht id ruff temp]
   (let [b (Gasliftcalcs$FlowLineSegment/newBuilder)]
     (doto b
       (.setEdstList (util/getdefval edst 0))
       (.setEhtList  (util/getdefval eht 0))
       (.setIdList   (util/getdefval id 0))
       (.setRuffList (util/getdefval ruff 0))
       (.setTempList (util/getdefval temp 0)))
     (.build b)))

  ([[edst eht id ruff temp]]
   (FlowLineSegment edst eht id ruff temp)))


(defn- FlowLineSegments
  "Generate pipe string data protobuf (either injection and production)"
  [vect]
  {:pre [(not-any? nil? [vect])]}
  (let [edst     (:edst-list     vect)
        eht     (:eht-list vect)
        id      (:id-list      vect)
        ruff    (:ruff-list    vect)
        temp (:temp-list vect)
        psegs (map FlowLineSegment (mapv vector edst eht id ruff temp))]
    psegs))



(defn- GlvalveData
  "Generate the valve data protobuf (for one valve)"
  [[glv-correlation glv-manf glv-series
    glv-desc mandrel-manf mandrel-series mandrel-series-num
    mandrel-name mandrel-desc mandrel-pocket-id mandrel-od
    mandrel-id mandrel-nominal-od meas-depth-list port-id prta tro
    choke tef bellowsa spring-pres glv-type glv-category
    dome fill ldvt travel slope temp cva cvb cvc cvd dxa
    dxb dxc dxd xta xtb xtc xtd maxdx maxxt maxcv xtr1 xtr2
    xtr3 xtr4 avt-backcheck-status avt-travel-status avt-ldrt-status
    avt-close-status avt-open-status avt-leak-status user-inj-rate
    tst-pvc installed pvcd pdov bchk-leak port-leak rfact
    opcl-dsp-cde prfm-dsp-cde]]
  (let [b (Gasliftcalcs$GlvalveData/newBuilder)]
    (doto b
      ;(.setMandrelSurveyDateList  (util/getdefval mandrel-survey-date  0))
      (.setGlvCorrelationList     (util/getdefval glv-correlation      ""))
      (.setGlvManfList            (util/getdefval glv-manf             ""))
      (.setGlvSeriesList          (util/getdefval glv-series           ""))
      (.setGlvDescList            (util/getdefval glv-desc             ""))
      (.setMandrelManfList        (util/getdefval mandrel-manf         ""))
      (.setMandrelSeriesList      (util/getdefval mandrel-series       ""))
      (.setMandrelSeriesNumList   (util/getdefval mandrel-series-num   ""))
      (.setMandrelNameList        (util/getdefval mandrel-name         ""))
      (.setMandrelDescList        (util/getdefval mandrel-desc         ""))
      (.setMandrelPocketIdList    (util/getdefval mandrel-pocket-id    0))
      (.setMandrelOdList          (util/getdefval mandrel-od           0))
      (.setMandrelIdList          (util/getdefval mandrel-id           0))
      (.setMandrelNominalOdList   (util/getdefval mandrel-nominal-od   0))
      (.setNstList                60.0)
      (.setMeasDepthList          (util/getdefval meas-depth-list      0))
      (.setPortIdList             (util/getdefval port-id              0))
      (.setPrtaList               (util/getdefval prta                 0))
      (.setTroList                (util/getdefval tro                  0))
      (.setChokeList              (util/getdefval choke                0))
      (.setTefList                (util/getdefval tef                  0))
      (.setBellowsaList           (util/getdefval bellowsa             0))
      (.setSpringPresList         (util/getdefval spring-pres          0))
      (.setGlvTypeList            (util/getdefval glv-type             ""))
      (.setGlvCategoryList        (util/getdefval glv-category         ""))
      (.setDomeList               (util/getdefval dome                 0))
      (.setFillList               (util/getdefval fill                 0))
      (.setLdvtList               (util/getdefval ldvt                 0))
      (.setTravelList             (util/getdefval travel               0))
      (.setSlopeList              (util/getdefval slope                0))
      (.setTempList               (util/getdefval temp                 0))
      (.setCvaList                (util/getdefval cva                  0))
      (.setCvbList                (util/getdefval cvb                  0))
      (.setCvcList                (util/getdefval cvc                  0))
      (.setCvdList                (util/getdefval cvd                  0))
      (.setDxaList                (util/getdefval dxa                  0))
      (.setDxbList                (util/getdefval dxb                  0))
      (.setDxcList                (util/getdefval dxc                  0))
      (.setDxdList                (util/getdefval dxd                  0))
      (.setXtaList                (util/getdefval xta                  0))
      (.setXtbList                (util/getdefval xtb                  0))
      (.setXtcList                (util/getdefval xtc                  0))
      (.setXtdList                (util/getdefval xtd                  0))
      (.setMaxdxList              (util/getdefval maxdx                0))
      (.setMaxxtList              (util/getdefval maxxt                0))
      (.setMaxcvList              (util/getdefval maxcv                0))
      (.setXtr1List               (util/getdefval xtr1                 0))
      (.setXtr2List               (util/getdefval xtr2                 0))
      (.setXtr3List               (util/getdefval xtr3                 0))
      (.setXtr4List               (util/getdefval xtr4                 0))
      (.setAvtBackcheckStatusList (util/getdefval avt-backcheck-status ""))
      (.setAvtTravelStatusList    (util/getdefval avt-travel-status    ""))
      (.setAvtLdrtStatusList      (util/getdefval avt-ldrt-status      ""))
      (.setAvtCloseStatusList     (util/getdefval avt-close-status     ""))
      (.setAvtOpenStatusList      (util/getdefval avt-open-status      ""))
      (.setAvtLeakStatusList      (util/getdefval avt-leak-status      ""))
      (.setUserInjRateList        (util/getdefval user-inj-rate        0))
      (.setTstPvcList             (util/getdefval tst-pvc              0))
      (.setInstalledList          (if (nil? installed) 0 (ts-to-epoch (util/getdefval installed 0))))
      (.setPvcdList               (util/getdefval pvcd                 0))
      (.setPdovList               (util/getdefval pdov                 0))
      (.setBchkLeakList           (util/getdefval bchk-leak            0))
      (.setPortLeakList           (util/getdefval port-leak            0))
      (.setRfactList              (util/getdefval rfact                0))
      (.setOpclDspCdeList         (util/getdefval opcl-dsp-cde         ""))
      (.setPrfmDspCdeList         (util/getdefval prfm-dsp-cde         "")))
    (.build b)))

(defn- BuildValveModel
  "add a wellmodel protobuf to the given builder, b
  where b is a Gasliftcalcs$CalcPpmCurveParams/newBuilder object"
  [b valves]
  (let [vects (mapv vector
                (:glv-correlation-list      valves)
                (:glv-manf-list             valves)
                (:glv-series-list           valves)
                (:glv-desc-list             valves)
                (:mandrel-manf-list         valves)
                (:mandrel-series-list       valves)
                (:mandrel-series-num-list   valves)
                (:mandrel-name-list         valves)
                (:mandrel-desc-list         valves)
                (:mandrel-pocket-id-list    valves)
                (:mandrel-od-list           valves)
                (:mandrel-id-list           valves)
                (:mandrel-nominal-od-list   valves)
                (:meas-depth-list           valves)
                (:port-id-list              valves)
                (:prta-list                 valves)
                (:tro-list                  valves)
                (:choke-list                valves)
                (:tef-list                  valves)
                (:bellowsa-list             valves)
                (:spring-pres-list          valves)
                (:glv-type-list             valves)
                (:glv-category-list         valves)
                (:dome-list                 valves)
                (:fill-list                 valves)
                (:ldvt-list                 valves)
                (:travel-list               valves)
                (:slope-list                valves)
                (:temp-list                 valves)
                (:cva-list                  valves)
                (:cvb-list                  valves)
                (:cvc-list                  valves)
                (:cvd-list                  valves)
                (:dxa-list                  valves)
                (:dxb-list                  valves)
                (:dxc-list                  valves)
                (:dxd-list                  valves)
                (:xta-list                  valves)
                (:xtb-list                  valves)
                (:xtc-list                  valves)
                (:xtd-list                  valves)
                (:maxdx-list                valves)
                (:maxxt-list                valves)
                (:maxcv-list                valves)
                (:xtr1-list                 valves)
                (:xtr2-list                 valves)
                (:xtr3-list                 valves)
                (:xtr4-list                 valves)
                (:avt-backcheck-status-list valves)
                (:avt-travel-status-list    valves)
                (:avt-ldrt-status-list      valves)
                (:avt-close-status-list     valves)
                (:avt-open-status-list      valves)
                (:avt-leak-status-list      valves)
                (:user-inj-rate-list        valves)
                (:tst-pvc-list              valves)
                (:installed-list            valves)
                (:pvcd-list                 valves)
                (:pdov-list                 valves)
                (:bchk-leak-list            valves)
                (:port-leak-list            valves)
                (:rfact-list                valves)
                (:opcl-dsp-cde-list         valves)
                (:prfm-dsp-cde-list         valves))
        vlist (mapv GlvalveData vects)]
   (.addAllMandrelSurveyMap b vlist)))



(defn- PipeSegment
  "Generate a protobuf for a single pipe segment"
  ([meas-depth-list id-list csng-od-list ruff-list]
   (let [b (Gasliftcalcs$PipeSegment/newBuilder)]
     (doto b
       (.setMeasDepthList(util/getdefval meas-depth-list 0))
       (.setIdList     (util/getdefval id-list 0))
       (.setCsngOdList (util/getdefval csng-od-list 0))
       (.setRuffList   (util/getdefval ruff-list 0)))
     (.build b)))

  ([[meas-depth-list id-list csng-od-list ruff-list]]
   (PipeSegment meas-depth-list id-list csng-od-list ruff-list)))

(defn- PipeString
  "Generate and add a pipe string protobuf (either injection and production)"
  [vect]
  {:pre [(not-any? nil? [vect])]}
  (let [meas-depth-list     (:meas-depth-list     vect)
        id-list      (:id-list      vect)
        csng-od-list (:csng-od-list vect)
        ruff-list    (:ruff-list    vect)]
    (map PipeSegment (mapv vector meas-depth-list id-list csng-od-list ruff-list))))

(defn- LGRCurveControl
  "Generates a protobuf for Lift Gas Response curve settings"
  [{:keys [lgas-perf-settings-map]}]
;  [{:keys [lgas-perf-settings-map welltest-map]}]
;  {:pre [(not-any? nil? [@welltest-map])]}
  (let [b (Gasliftcalcs$LGRCurveControl/newBuilder)
        lgsettings (if (nil? @lgas-perf-settings-map) {} @lgas-perf-settings-map)]

    (doto b
      (.setLiftDepthPt         (Gasliftcalcs$LiftDepthModel/valueOf
                                (util/getdefval (:lift-depth-pt lgsettings) 4))) ; fixed_depth (WG default)
;      (.setInjPress            (util/getdefval (:inj-press             lgsettings)
;                                 (util/getdefval (:calib-casing-head-press @welltest-map) 0)))
      (.setInjPress            (util/getdefval (:inj-press             lgsettings) 0))
      (.setMaxLgasRate         (util/getdefval (:max-lgas-rate         lgsettings) 0))
      (.setEnableMinRate       (:enable-min-rate             lgsettings))
      (.setMinLgasRate         (util/getdefval (:min-lgas-rate         lgsettings) 0))
;      (.setManifoldPress       (util/getdefval (:manifold-press        lgsettings)
;                                 (util/getdefval (:calib-flowing-tubing-press @welltest-map) 0)))
      (.setManifoldPress       (util/getdefval (:manifold-press        lgsettings) 0))
      (.setGoalVariation       (util/getdefval (:goal-variation        lgsettings) 0))
      (.setGoalExportSelCode   (:goal-export-sel-code        lgsettings))
      (.setBatchCalcCode       (:batch-calc-code             lgsettings))
      (.setOilSlopeCorrection  (util/getdefval (:oil-slope-correction  lgsettings) 0))
      (.setOilRateCorrection   (util/getdefval (:oil-rate-correction   lgsettings) 0))
      (.setGoalExportFname     (util/getdefval (:goal-export-fname     lgsettings) ""))
      #(.setGoalExportTimestamp (util/getdefval (:goal-export-timestamp lgsettings) 0))
      #(.setGoalGenTimestamp    (util/getdefval (:goal-gen-timestamp    lgsettings) 0))
      (.setNumCurves           (util/getdefval (:num-curves            lgsettings) 0))
      (.setCurveType           (Gasliftcalcs$LgrCurveType/valueOf
                                (util/getdefval (:curve-type lgsettings) 1)))) ; GLUE_LGAS_CURVE_TYPE
    (.build b)))

(defn- TempData
  "Generate a protobuf for a TempData"
  [{:keys [temp-modl-md temp-modl-temp]}]
  (let [b (gasliftcalcs.Gasliftcalcs$TempData/newBuilder)]
    (doto b
      (.addAllMeasDepthList (if-let [c (< 0 (count temp-modl-md))]
                              temp-modl-md
                              []))
      ;(.addAllVertDepthList (if-let [c (< 0 (count temp-modl-md))]
      ;                                         temp-modl-md []))
      (.addAllFlowTempList  (if-let [c (< 0 (count temp-modl-temp))]
                              temp-modl-temp
                              [])))
    (.build b)))

(defn- ModelControl
  "Generates a protobuf for a ModelControl"
  [{:keys [modl-ctrl-map alt-temps-map]}]
  (if (not (nil? modl-ctrl-map))
    (let [b (Gasliftcalcs$ModelControl/newBuilder)]
      (doto b
        (.setFlowLineModel            (:flowline-model @modl-ctrl-map))
        (.setChokeModel               (:choke-model @modl-ctrl-map))
        (.setWlhdChokeCoeff           (let [v (:wlhd-choke-coeff @modl-ctrl-map)]
                                       (if (or (nil? v) (< v 0.1)) 0.6 (util/getdefval (:wlhd-choke-coeff @modl-ctrl-map) 0.6))))
        (.setGasliftMaxMeasDepth      (util/getdefval (:gaslift-max-meas-depth @modl-ctrl-map) 0))
        (.setRuffMultiplier           (util/getdefval (:ruff-multiplier @modl-ctrl-map) 0))
        (.setIdShrink                 (util/getdefval (:id-shrink @modl-ctrl-map) 0))
        (.setWlhdChokeBodySize        (util/getdefval (:wlhd-choke-body-size @modl-ctrl-map) 2.0625))
        (.setCalibSurfTemp            (util/getdefval (:calib-surf-temp @modl-ctrl-map) 0))
        (.setInflowModel              Gasliftcalcs$IprModel/VOGEL_IPR)
        (.setFgsTempType              (Gasliftcalcs$FGSTempType/valueOf
                                        (util/getdefval (:fgs-temp-type @modl-ctrl-map) 0)))
        (.setPhysicalComp             (Gasliftcalcs$CompositionModel/valueOf
                                        (util/getdefval (:physical-comp @modl-ctrl-map) 0)))
        (.setPhysicalVisc             (Gasliftcalcs$ViscosityModel/valueOf
                                        (util/getdefval (:physical-Visc @modl-ctrl-map) 0)))
        (.setPhysicalCrit             (Gasliftcalcs$CriticalModel/valueOf
                                        (util/getdefval (:physical-Crit @modl-ctrl-map) 0)))
        (.setInjectionPressModel      (Gasliftcalcs$InjectionModel/valueOf
                                        (util/getdefval (:physical-Crit modl-ctrl-map) 0)))
        (.setProdPressModel           (Gasliftcalcs$ProductionPressureModel/valueOf
                                        (util/getdefval (:prod-press-model @modl-ctrl-map) 2))) ; PPM_MMSM
        (.setWellTempModel            (Gasliftcalcs$TemperatureModel/valueOf
                                        (util/getdefval (:well-temp-model @modl-ctrl-map) 1)))
        (.setFlowlinePressModel       (Gasliftcalcs$FlowLinePressModel/valueOf
                                        (util/getdefval (:flowline-press-model @modl-ctrl-map) 0)))
        (.setLiftDepthPt              (Gasliftcalcs$LiftDepthModel/valueOf
                                        (util/getdefval (:lift-depth-pt @modl-ctrl-map) 2))); deepest_mandrel_ldpt
        (.setFindLiquidRate           (util/getdefval (:find-liquid-rate @modl-ctrl-map) false))
        (.setTopToBot                 (util/getdefval (:top-to-bot @modl-ctrl-map) true))
        (.setFlwUseDownstreamDir      (util/getdefval (:flw-use-downstream-dir @modl-ctrl-map) true))
        (.setAltTempsMap              (TempData @alt-temps-map)))
      (.build b))))

(defn- WellMaster
  "Generates a protobuf for WellMaster data"
  [{:keys [well-mstr-map]}]
  {:pre [(not-any? nil? [@well-mstr-map])]}
  (let [b (Gasliftcalcs$WellMaster/newBuilder)]
    (doto b
      (.setName        (util/getdefval (:name        @well-mstr-map) ""))
      (.setLease       (util/getdefval (:lease       @well-mstr-map) ""))
      (.setDual        (util/getdefval (:dual        @well-mstr-map) ""))
      (.setApiNumber   (util/getdefval (:api-number  @well-mstr-map) ""))
      (.setType        (util/getdefval (:type        @well-mstr-map) ""))
      (.setKbElev      (util/getdefval (:kb-elev     @well-mstr-map) 0))
      (.setJacketName  (util/getdefval (:jacket-name @well-mstr-map) ""))
      (.setPwi         (util/getdefval (:pwi         @well-mstr-map) 0))
      (.setCmplSeq     (util/getdefval (:cmpl-seq    @well-mstr-map) ""))
      (.setCmpl        (util/getdefval (:cmpl        @well-mstr-map) 0))
      (.setWellheadMeasDepth  (util/getdefval (:wellhead-md @well-mstr-map) 0))
      (.setLastSavedDate (util/getdefval (:last-saved-date @well-mstr-map) "")))
    (.build b)))

(defn LiftGasProperties
  "Generates a protobuf for a Lift Gas Properties data"
  [{:keys [lgas-props-map]}]
  (if (not (nil? lgas-props-map))
    (let [b (Gasliftcalcs$LiftGasProperties/newBuilder)]
      (doto b
        (.setSpg            (util/getdefval (:spg           @lgas-props-map) 0))
        (.setWlhdInjTemp    (util/getdefval (:wlhd-inj-temp @lgas-props-map) 0))
        (.setPerfInjTemp    (util/getdefval (:perf-inj-temp @lgas-props-map) 0))
        (.setPctMethane     (util/getdefval (:pct-methane   @lgas-props-map) 0))
        (.setPctCo2         (util/getdefval (:pct-co2       @lgas-props-map) 0))
        (.setPctH2S         (util/getdefval (:pct-h2s       @lgas-props-map) 0)))
      (.build b))))

(defn- IprCurveControl
  "Generates a protobuf for the IPR Curve control"
  [{:keys [ipr-ctrl-map rsvr-map reservoir-survey]}]
  (let [b (Gasliftcalcs$IprCurveControl/newBuilder)
        iprctrl (if-let [ipr-ctrl-map nil?] {} @ipr-ctrl-map)]
    (doto b
      (.setModel              (Gasliftcalcs$IprModel/valueOf
                               (util/getdefval (:model iprctrl) 1))) ; Vogel
      (.setNumPoints        (util/getdefval (:num-points         iprctrl) 30))
      (.setBubblePointPress (util/getdefval (:bubble-point-press iprctrl)
                              (util/getdefval (:bubble-point-press @rsvr-map) 0)))
      (.setSbhp             (util/getdefval (:sbhp               iprctrl)
                              (util/getdefval (:sbhp @reservoir-survey) 0)))
      (.setQmax             (util/getdefval (:qmax               iprctrl)
                              (util/getdefval (:max-inflow         @rsvr-map) 0)))
      (.setMinLiquidRate    (util/getdefval (:min-liquid-rate    iprctrl) 0))
      (.setMaxLiquidRate    (util/getdefval (:max-liquid-rate    iprctrl)
                              (util/getdefval (:max-inflow         @rsvr-map) 0)))
      (.addAllAltLiquidRateList (if-let [c (< 0 (count (:alt-liquid-rate-list iprctrl)))]
                                  (:alt-liquid-rate-list iprctrl)
                                  []))
      (.addAllAltFbhpList       (if-let [c (< 0 (count (:alt-fbhp-list iprctrl)))]
                                  (:alt-fbhp-list iprctrl)
                                  [])))
   (.build b)))



(defn- FlowingSurveyDetail
  "Generates a protobuf for Flowing Gradient Survey row details"
  ([md tvd avg-pres avg-temp low-pres low-temp high-pres high-temp]
   (let [b (Gasliftcalcs$FlowingSurveyDetail/newBuilder)]
     (doto b
       (.setMeasDepthList (util/getdefval md        0))
       (.setVertDepthList (util/getdefval tvd       0))
       (.setAvgPresList   (util/getdefval avg-pres  0))
       (.setAvgTempList   (util/getdefval avg-temp  0))
       (.setLowPresList   (util/getdefval low-pres  0))
       (.setLowTempList   (util/getdefval low-temp  0))
       (.setHighPresList  (util/getdefval high-pres 0))
       (.setHighTempList  (util/getdefval high-temp 0)))
     (.build b)))
  ([[md tvd avg-pres avg-temp low-pres low-temp high-pres high-temp]]
   (FlowingSurveyDetail md tvd avg-pres avg-temp low-pres low-temp high-pres high-temp)))

(defn- FlowingSurvey
  "Generates a protobuf for Flowing Gradient Survey"
  [well]
  {:pre [(not-any? nil? [well])]}
  (let [b  (Gasliftcalcs$FlowingSurvey/newBuilder)
        fgs  @(:flowing-gradient-survey-map well)
        detail-map (:detail-map fgs)
        details (map FlowingSurveyDetail
                  (mapv vector
                    (:meas-depth-list detail-map)
                    (:vert-depth-list detail-map)
                    (:avg-pres-list  detail-map)
                    (:avg-temp-list  detail-map)
                    (:low-pres-list  detail-map)
                    (:low-temp-list  detail-map)
                    (:high-pres-list detail-map)
                    (:high-temp-list detail-map)))]
    (doto b
      (.setFbhpAtPerf   (util/getdefval (:fbhp-at-perf  fgs) 0))
      (.setPressureGrad (util/getdefval (:pressure-grad fgs) 0))
      (.setEstMeasDepth (util/getdefval (:est-md        fgs) 0))
      (.setEstTempGrad  (util/getdefval (:est-temp-grad fgs) 0))
      (.setTempAtPerf   (util/getdefval (:temp-at-perf  fgs) 0))
      (.setDate         (ts-to-epoch (:date fgs))))
    (.addAllDetailMap b details)
    (.build b)))

(defn- AboveBelowRange
  "Generates a protobuf for AboveBelowRange"
  [data]
  (let [b  (Gasliftcalcs$AboveBelowRange/newBuilder)]
    (doto b
      (.setAbove (util/getdefval (:above data) 0))
      (.setBelow (util/getdefval (:below data) 0)))
    (.build b)))


(defn- MinMaxRange
  "Generates a protobuf for MinMaxRange"
  [data]
  (let [b  (Gasliftcalcs$MinMaxRange/newBuilder)]
    (doto b
      (.setMinimum (util/getdefval (:minimum data) 0))
      (.setMaximum (util/getdefval (:maximum data) 1)))
    (.build b)))

(defn- CalibrationSet
  "Generates a protobuf for Calibration Control/Results map"
  [cb]
  {:pre [(not-any? nil? [cb])]}
  (let [b  (Gasliftcalcs$CalibrationSet/newBuilder)]
    (doto b
      (.setMaxIterations (AboveBelowRange (:max-iterations cb)))
      (.setGradPctLimitMap (AboveBelowRange (:grad-pct-limit-map cb)))
      (.setFormGasOilRatioLimitMap (MinMaxRange (:form-gas-oil-ratio-limit-map cb)))
      (.setLiftGasRateLimitMap (MinMaxRange (:lift-gas-rate-limit-map cb)))
      (.setEnableMultipoint (util/getdefval (:enable-multipoint cb) false))
      (.setTraverseTolerance (util/getdefval (:traverse-tolerance cb) 10.0)))
    (.build b)))

(defn- wellbore-data-set
  "Generates a protobuf for all reqiured fields of a wellbore_data_set
  Each command must add/update the optional fields as appropriate."
  [well]
  {:pre [(not-any? nil? [well])]}
  (let [b (Gasliftcalcs$wellbore_data_set/newBuilder)]
    (doto b
      (.setWellMstrMap      (WellMaster          well))
      (.setModlCtrlMap      (ModelControl        well))
      (.setLgasPropsMap     (LiftGasProperties   well))
      (.setRsvrMap          (ReservoirProperties well))
      (.setDsvyMap          (MdtvdProperties     well))
      (.setWelltestMap      (WellTest            well))
      (.setLgasPerfSettingsMap (LGRCurveControl     well))
      (.setIprCtrlMap          (IprCurveControl     well))
      (.addAllFlowLineMap (FlowLineSegments @(:flow-line-map well)))
      (.addAllInjMechMap  (PipeString @(:inj-mech-map  well)))
      (.addAllProdMechMap (PipeString @(:prod-mech-map well)))
      (.setCalibrationMap (CalibrationSet (:calibration-map well))))
    (if (not (nil? @(:flowing-gradient-survey-map well)))
      (.setFlowingGradientSurveyMap b (FlowingSurvey well)))
    (BuildValveModel b @(:mandrel-survey-map well))
    (let [retb (.build b)]
      (log/trace b)
      retb)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; next section is calculation bodies, they just dumbly do the
;; calcs, and return protobufs, for somebody else to decode, since
;; sometimes same calcs have different yields
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn calc-inflow-outflow
  "transacts with glcalcs to do an inflow calculation"
  [dbmap]
  {:ipr-curve-map (api-transact "calcIprCurve"
                                (wellbore-data-set dbmap)
                                "gasliftcalcs.Gasliftcalcs$IprCurve")
   :outflow-map (api-transact "calcOutflowCurves"
                              (wellbore-data-set dbmap)
                              "gasliftcalcs.Gasliftcalcs$OutflowCurveData")})

(defn calc-ipr-curve
  "transacts with glcalcs to do an inflow calculation"
  [dbmap]
  {:ipr-curve-map (api-transact "calcIprCurve"
                                (wellbore-data-set dbmap)
                                "gasliftcalcs.Gasliftcalcs$IprCurve")})

(defn calc-wellbore
  "transacts with glcalcs to run the PPM calcs, valve status and
   valve performance"
  [dbmap]
  (let [rslt (api-transact "calcWellBore"
                           (wellbore-data-set dbmap)
                           "gasliftcalcs.Gasliftcalcs$DepthProfile")
        vstatus-list  (dbutil/vector-slices
                        (:valve-status-list (:valves-status-map rslt)))]
      {:depth-profile-map (merge rslt {:valves-status-map vstatus-list})}))

(defn calc-valve-status
  "transacts with glcalcs to run the valve status calcs"
  [dbmap]
  (let [rslt (api-transact "calcValveStatus"
                           (wellbore-data-set dbmap)
                           "gasliftcalcs.Gasliftcalcs$GLValveStatusSet")
        vlist (util/protobuf-to-map
                gasliftcalcs.Gasliftcalcs$GLValveStatusSet rslt)]
      {:valves-status-map (dbutil/vector-slices (:valve-status-list vlist))}))

(defn calc-eq-curves
  "transacts with glcalcs to run the Equilibrium calc"
  [dbmap]
  {:equilibrium-map (api-transact "calcEqCurve"
                                  (wellbore-data-set dbmap)
                                  "gasliftcalcs.Gasliftcalcs$EquilibriumCurveData")})

(defn calc-outflow-curves
  "transacts with glcalcs to run the Outflow calc"
  [dbmap]
  {:outflow-map (api-transact "calcOutflowCurves"
                              (wellbore-data-set dbmap)
                              "gasliftcalcs.Gasliftcalcs$OutflowCurveData")})

;;(defn calc-flowmap
;;  "transacts with glcalcs to run the Flow Map calculations"
;;  [dbmap]
;;  (with-open [sock (api-socket)]
;;    (zmq/send sock (request-bytes "calcFlowMap") zmq/send-more)
;;    (zmq/send sock (.toByteArray (wellbore-data-set dbmap)))
;;    (grok-reply sock)
;;    {:flowmap-map
;;      (util/protobuf-to-map gasliftcalcs.Gasliftcalcs$FlowMapDataSet
;;        (Gasliftcalcs$FlowMapDataSet/parseFrom (zmq/receive sock)))}))

(defn calc-lgr-curves
  "transacts with glcalcs to calculate the Lift Gas Response curves"
  [dbmap]
  (let [lgr (api-transact "calcLGRCurves"
                          (wellbore-data-set dbmap)
                          "gasliftcalcs.Gasliftcalcs$LgrCurvesData")
        vstat-list (into []
                    (for [vst (:valves-models-list lgr)]
                       (dbutil/vector-slices (:valve-status-list vst))))]
      {:lgr-curves-map (merge lgr
                        {:valves-models-list (dbutil/reform-inverted-data vstat-list)})}))

(defn calc-valve-performance
  "transacts with glcalcs to run the Valve Performance calculations"
  [dbmap]
  {:valves-performance-map (api-transact "calcValvePerformance"
                            (wellbore-data-set dbmap)
                            "gasliftcalcs.Gasliftcalcs$ValvePerformanceSet")})

(defn get-fgs-lift-depth
  "transacts with glcalcs to calculate the lift depth based
   on the flowing gradient survey - used for calibration"
  [dbmap]
  {:modl-ctrl-map
    {:gaslift-max-meas-depth
      (:float-val
        (api-transact "getFGSLiftDepth"
                         (wellbore-data-set dbmap)
                         "gasliftcalcs.Gasliftcalcs$SingleFloatResponse"))}})

(defn get-fgs-wellhead-pressure
  "transacts with glcalcs to set the calibrated well head pressure"
  [dbmap]
  {:welltest-map
    {:calib-flowing-tubing-press
      (:float-val
        (api-transact "getFGSWellHeadPressure"
                      (wellbore-data-set dbmap)
                      "gasliftcalcs.Gasliftcalcs$SingleFloatResponse"))}})

(defn match-fgs-gradient-below-injection
  "transacts with glcalcs to set the calibrated FGOR"
  [dbmap]
  (let [gor (:float-val (api-transact "matchFGSGradientBelowInjection"
                                      (wellbore-data-set dbmap)
                                      "gasliftcalcs.Gasliftcalcs$SingleFloatResponse"))]
    {:welltest-map {:calib-formation-gas-rate
                    (* (:calib-oil-rate @(:welltest-map dbmap))
                       (/ gor 1000))}}))

(defn match-fgs-gradient-above-injection
  "transacts with glcalcs to set the calibrated lift gas rate"
  [dbmap]
  {:welltest-map
   {:calib-lift-gas-rate
    (:float-val
      (api-transact "matchFGSGradientAboveInjection"
                    (wellbore-data-set dbmap)
                    "gasliftcalcs.Gasliftcalcs$SingleFloatResponse"))}})

(defn calc-fgs-multi-point-injection
  "transacts with glcalcs to swith to mult-point model if applicable"
  [dbmap]
  (let [mresp (api-transact "calcFGSMultipointInjection"
                            (wellbore-data-set dbmap)
                            "gasliftcalcs.Gasliftcalcs$MultiPointResponse")]
    {:modl-ctrl-map {:gaslift-max-meas-depth (:max-lift-meas_depth mresp)
                     :calib-lift-gas-rate    (:lift-depth-pt       mresp)}}))

(defn calc-fgs-valve-performance
  "transacts with glcalcs to calculate and return the valve
   performance post calibration"
  [dbmap]
  {:fgs-valve-perf-check (api-transact "calcFGSValvePerformance"
                                       (wellbore-data-set dbmap)
                                       "gasliftcalcs.Gasliftcalcs$InjectionMatchResponse")})

(defn calc-qmax
  "transacts with glcalcs to calculate and return the new
  QMax post calibration"
  [dbmap]
  {:ipr-ctrl-map
    {:qmax
      (:float-val
        (api-transact "calcQMax"
                      (wellbore-data-set dbmap)
                      "gasliftcalcs.Gasliftcalcs$SingleFloatResponse"))}})

(defn calibrate-single
  "calibrate the given well - return updated welltest (calibrated) and
  step-by-step data from calibration process"
  [dbmap]
  (let [rslt (api-transact "calibrateSingle"
                           (wellbore-data-set dbmap)
                           "gasliftcalcs.Gasliftcalcs$CalibrationResults")
        vstatus-list (dbutil/vector-slices (-> rslt :depth-profile-map :valves-status-map :valve-status-list))]
    (merge rslt {:depth-profile-map (merge (:depth-profile-map rslt) {:valves-status-map vstatus-list})})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; here we start getting into the real answer getters
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- decode-valve-status
  "turns ValveStatus into keyword"
  [vstat]
  (get [:unknown :closed :open :transition :back-check :is-dummy :use-pct-open]
       (.getNumber vstat)))

(defn- decode-flow-regime
  "turns FlowRegime into keyword"
  [fr]
  (get [:liquid :bubble :slug :transiton :annular :stratified
        :gas :unld-liquid :unld-bubble :unld-slug :unld-transition
        :unld-annular :unld-stratified :unld-gas
        :1ph-liq :1ph-gas]
       (.getNumber fr)))

(defn last-n-welltests
  "return table of upto last n non-ignore-as-bad welltests for the given well"
  [dbc pwi n]
  (if-let [rawrows (not-empty (take 3 (wgdb/get-welltests dbc pwi)))]
    (let [usable (filter #(= (:ignore-as-bad %) 0) rawrows)
          augmented (map util/augment-welltest usable)
          slices (dbutil/vector-slices augmented)]
      (dbutil/reform-as-table (into [] (keys slices)) slices))))
