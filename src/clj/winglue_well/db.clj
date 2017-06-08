(ns winglue-well.db
  (:require [winglue-well.config.core :as config]
            [winglue-well.utils :as utils]
            [winglue-well.database.core :as dbcore]
            [winglue-well.mimerefs :as mref :refer [str->ref refstr? realize-refstr]]
            [winglue-well.calcs :as calcs]
            [clojure.pprint :as pp]
            [clojure.data :as da :refer [diff]]
            [winglue-well.database.core :as wgdb]))

(def app-state (atom {}))

(defn initinfo []
  (let [alldsn (@app-state :all-dsn)]
    (when (nil? alldsn)
      (println "alldsn: " alldsn)
      (swap! app-state assoc :all-dsn (config/get-data-sources @config/tao2-cfg)))))
      ;(swap! app-state assoc :current-dsn (first (keys (:all-dsn @app-state))))
      ;(swap! app-state assoc :all-well (->> (dbcore/get-matching-wells (:current-dsn @app-state) {:select-set #{:field :lease :well :cmpl}
      ;                                                                                            :where-map {}})
      ;                                      (vec)
      ;                                      (map vec)
      ;                                      (map #(zipmap [:field :lease :well :cmpl] %)))))))

(defn get-well-mstr-map []
  (println "Get :well-mstr-map")
  (when (refstr? (:well-mstr-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :well-mstr-map] (realize-refstr (:well-mstr-map (:welldoc @app-state))))))

(defn get-modl-ctrl-map []
  (println "Get :modl-ctrl-map")
  (when (refstr? (:modl-ctrl-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :modl-ctrl-map] (realize-refstr (:modl-ctrl-map (:welldoc @app-state))))))

(defn get-lgas-props-map []
  (println "Get :lgas-props-map")
  (when (refstr? (:lgas-props-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :lgas-props-map] (realize-refstr (:lgas-props-map (:welldoc @app-state))))))

(defn get-rsvr-map []
  (println "Get :rsvr-map")
  (when (refstr? (:rsvr-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :rsvr-map] (realize-refstr (:rsvr-map (:welldoc @app-state))))))

(defn get-dsvy-map []
  (println "Get :dsvy-map")
  (when (refstr? (:dsvy-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :dsvy-map] (realize-refstr (:dsvy-map (:welldoc @app-state))))))

(defn get-flow-line-map []
  (println "Get :flow-line-map")
  (when (refstr? (:flow-line-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :flow-line-map] (realize-refstr (:flow-line-map (:welldoc @app-state))))))

(defn get-inj-mech-map []
  (println "Get :inj-mech-map")
  (when (refstr? (:inj-mech-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :inj-mech-map] (realize-refstr (:inj-mech-map (:welldoc @app-state))))))

(defn get-prod-mech-map []
  (println "Get :prod-mech-map")
  (when (refstr? (:prod-mech-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :prod-mech-map] (realize-refstr (:prod-mech-map (:welldoc @app-state))))))

(defn get-lgas-perf-settings-map []
  (println "Get :lgas-perf-settings-map ")
  (when (refstr? (:lgas-perf-settings-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :lgas-perf-settings-map ] (realize-refstr (:lgas-perf-settings-map  (:welldoc @app-state))))))

(defn get-alt-temps-map []
  (println "Get :alt-temps-map  ")
  (when (refstr? (:alt-temps-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :alt-temps-map  ] (realize-refstr (:alt-temps-map   (:welldoc @app-state))))))

(defn get-stored-lgas-response-map []
  (println "Get :stored-lgas-response-map   ")
  (println (str (:stored-lgas-response-map (:welldoc @app-state))))
  (println (str "realize: " (realize-refstr (:stored-lgas-response-map (:welldoc @app-state)))))
  (when (refstr? (:stored-lgas-response-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :stored-lgas-response-map] (realize-refstr (:stored-lgas-response-map (:welldoc @app-state))))))

(defn get-welltest-map []
  (println "Get :welltest-map")
  (when (refstr? (:welltest-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :welltest-map] (realize-refstr (:welltest-map (:welldoc @app-state))))))

(defn get-flowing-gradient-survey-map []
  (println "Get :flowing-gradient-survey-map")
  (when (refstr? (:flowing-gradient-survey-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :flowing-gradient-survey-map] (realize-refstr (:flowing-gradient-survey-map (:welldoc @app-state))))))

(defn get-reservoir-survey []
  (println "Get :reservoir-survey")
  (when (refstr? (:reservoir-survey (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :reservoir-survey] (realize-refstr (:reservoir-survey (:welldoc @app-state))))))

(defn get-scada-survey []
  (println "Get :scada-survey")
  (when (refstr? (:scada-survey (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :scada-survey] (realize-refstr (:scada-survey (:welldoc @app-state))))))

(defn get-mandrel-survey-map []
  (println "Get :mandrel-survey-map")
  (when (refstr? (:mandrel-survey-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :mandrel-survey-map] (realize-refstr (:mandrel-survey-map (:welldoc @app-state))))))

;;---------
; Equality test for welltest values - they just need to be "close"
(defn is-welltest-close [x1 x2] (< (Math/abs (- x1 x2)) 0.001))

(def wt-key-pairs
  [[:calib-oil-rate             :meas-oil-rate]
   [:calib-water-rate           :meas-water-rate]
   [:calib-formation-gas-rate   :meas-form-gas-rate]
   [:calib-flowing-tubing-press :meas-flowing-tubing-press]
   [:calib-lift-gas-rate        :meas-lift-gas-rate]
   [:calib-casing-head-press    :meas-casing-head-press]
   [:calib-wellhead-choke-id    :meas-wellhead-choke-id]])


(defn wtest-is-calibrated [wt]
  (not (every? identity
               (for [kp wt-key-pairs]
                 (is-welltest-close ((first kp) wt) ((last kp) wt))))))

(defn get-welltest-hist-map []
  (println "Get :welltest-hist-map ")
  (let [ks (keys (:welltest-hist-map (:welldoc @app-state)))]
    (when (some? ks)
      (do
        (doseq [k ks]
          (let [aref (get (:welltest-hist-map (:welldoc @app-state)) k)]
            (when (refstr? aref)
              (swap! app-state assoc-in [:welldoc :welltest-hist-map k] (realize-refstr aref)))))
        (let [welltest-list (get-in @app-state [:welldoc :welltest-hist-map])
              cal-wt (filter #(wtest-is-calibrated %) (sort-by :welltest-date > (vals welltest-list)))
              uncal-wt (filter #(not (wtest-is-calibrated %)) (sort-by :welltest-date > (vals welltest-list)))]
          ;(println (str "welltest-list: " welltest-list))
          ;(println (pr-str "cal-wt: " cal-wt))
          ;(println (pr-str "uncal-wt: " uncal-wt))
          (swap! app-state assoc-in [:welldoc :cal-wt] cal-wt)
          (swap! app-state assoc-in [:welldoc :uncal-wt] uncal-wt))))))

(defn get-flowing-gradient-survey-hist-map []
  (println "Get :flowing-gradient-survey-hist-map ")
  (when (refstr? (:flowing-gradient-survey-hist-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :flowing-gradient-survey-hist-map (first (keys (:flowing-gradient-survey-hist-map  (:welldoc @app-state))))] (realize-refstr (first (vals (:flowing-gradient-survey-hist-map  (:welldoc @app-state))))))))

(defn get-reservoir-survey-hist-map []
  (println "Get :reservoir-survey-hist-map  ")
  (when (refstr? (:reservoir-survey-hist-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :reservoir-survey-hist-map  (first (keys (:reservoir-survey-hist-map   (:welldoc @app-state))))] (realize-refstr (first (vals (:reservoir-survey-hist-map   (:welldoc @app-state))))))))

(defn get-scada-survey-hist-map []
  (println "Get :scada-survey-hist-map ")
  (when (refstr? (:scada-survey-hist-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :scada-survey-hist-map  (first (keys (:scada-survey-hist-map   (:welldoc @app-state))))] (realize-refstr (first (vals (:scada-survey-hist-map   (:welldoc @app-state))))))))

(defn get-mandrel-survey-hist-map []
  (println "Get :mandrel-survey-hist-map ")
  (when (refstr? (:mandrel-survey-hist-map (:welldoc @app-state)))
    (swap! app-state assoc-in [:welldoc :mandrel-survey-hist-map  (first (keys (:mandrel-survey-hist-map   (:welldoc @app-state))))] (realize-refstr (first (vals (:mandrel-survey-hist-map   (:welldoc @app-state))))))))

(defn get-profile-depth []
  (println "Get :profile-depth ")
  (let [wellid {:dsn (:current-dsn @app-state)
                :field (:field (:current-well @app-state))
                :lease (:lease (:current-well @app-state))
                :well (:well (:current-well @app-state))
                :cmpl (:cmpl (:current-well @app-state))}
        welldoc (wgdb/get-well wellid)
        welldb (wgdb/delay-loadify-well welldoc)
        out (calcs/calc-wellbore welldb)]
    ;(println "wellid: " wellid)
    ;(println "out: " (keys out))
    (swap! app-state assoc-in [:welldoc :depth-profile-map] (first (vals out)))))

(defn get-equilibrium-depth-profile []
  (println "Get equilibrium depth profile ")
  (let [wellid {:dsn (:current-dsn @app-state)
                :field (:field (:current-well @app-state))
                :lease (:lease (:current-well @app-state))
                :well (:well (:current-well @app-state))
                :cmpl (:cmpl (:current-well @app-state))}
        welldoc (wgdb/get-well wellid)
        welldb (wgdb/delay-loadify-well welldoc)
        out (calcs/calc-eq-curves welldb)]
    ;(println "out: " (keys out))
    (swap! app-state assoc-in [:welldoc :equilibrium-map] (:equilibrium-map out))))

(defn get-fbhp-profile []
  (println "Get equilibrium depth profile ")
  (let [wellid {:dsn (:current-dsn @app-state)
                :field (:field (:current-well @app-state))
                :lease (:lease (:current-well @app-state))
                :well (:well (:current-well @app-state))
                :cmpl (:cmpl (:current-well @app-state))}
        welldoc (wgdb/get-well wellid)
        welldb (wgdb/delay-loadify-well welldoc)
        out (calcs/calc-inflow-outflow welldb)]
    ;(println "out: " (keys out))
    (swap! app-state assoc-in [:welldoc :ipr-curve-map] (:ipr-curve-map out))
    (swap! app-state assoc-in [:welldoc :outflow-map] (:outflow-map out))))

(defn get-calced-injection-rate-profile []
  (println "Get calced injection rate profile ")
  (let [wellid {:dsn (:current-dsn @app-state)
                :field (:field (:current-well @app-state))
                :lease (:lease (:current-well @app-state))
                :well (:well (:current-well @app-state))
                :cmpl (:cmpl (:current-well @app-state))}
        welldoc (wgdb/get-well wellid)
        welldb (wgdb/delay-loadify-well welldoc)
        out (calcs/calc-lgr-curves welldb)]
    (if (some? (:lift-gas-rate-list (:lgr-curves-map out)))
      (do (swap! app-state assoc-in [:welldoc :calced-lgr-curves-map] (:lgr-curves-map out))
          (swap! app-state assoc-in [:welldoc :lgr-curves-map] (:lgr-curves-map out))))))

(defn get-injection-rate-profile []
  (println "Get injection rate profile ")
  (let [stored-lgr (get-in @app-state [:welldoc :stored-lgas-response-map])
        calced-lgr (get-in @app-state [:welldoc :calced-lgr-curves-map])]
    (if (some? (:lift-gas-rate-list stored-lgr))
      (swap! app-state assoc-in [:welldoc :lgr-curves-map] stored-lgr))))


(defn pick-dsn [dsn]
  (swap! app-state assoc :current-dsn dsn)
  (swap! app-state assoc :all-well (->> (dbcore/get-matching-wells (:current-dsn @app-state) {:select-set #{:field :lease :well :cmpl}
                                                                                              :where-map {}})
                                        (vec)
                                        (map vec)
                                        (map #(zipmap [:field :lease :well :cmpl] %)))))

(defn pick-well [well]
  (swap! app-state assoc :current-well well)
  (swap! app-state assoc :welldoc (dbcore/get-well {:dsn (:current-dsn @app-state)
                                                    :field (:field (:current-well @app-state))
                                                    :lease (:lease (:current-well @app-state))
                                                    :well (:well (:current-well @app-state))
                                                    :cmpl (:cmpl (:current-well @app-state))}))
  (get-well-mstr-map)
  (get-modl-ctrl-map)
  (get-lgas-props-map)
  (get-rsvr-map)
  (get-dsvy-map)
  (get-flow-line-map)
  (get-inj-mech-map)
  (get-prod-mech-map)
  (get-lgas-perf-settings-map)
  (get-alt-temps-map)
  (get-stored-lgas-response-map)
  (get-welltest-map)
  (get-flowing-gradient-survey-map)
  (get-reservoir-survey)
  (get-scada-survey)
  (get-mandrel-survey-map)
  (get-welltest-hist-map)
  (get-flowing-gradient-survey-hist-map)
  (get-reservoir-survey-hist-map)
  (get-scada-survey-hist-map)
  (get-mandrel-survey-hist-map)
  (get-profile-depth)
  (get-equilibrium-depth-profile)
  (get-fbhp-profile)
  (get-injection-rate-profile)
  (get-calced-injection-rate-profile))

(defn pick-first-well []
  (swap! app-state assoc :current-well (first (:all-well @app-state)))
  (pp/pprint (:all-well @app-state))
  (println "Pick a well: ")
  (pp/pprint (:current-well @app-state))
  (swap! app-state assoc :welldoc (dbcore/get-well {:dsn :pioneer
                                                    :field (:field (:current-well @app-state))
                                                    :lease (:lease (:current-well @app-state))
                                                    :well (:well (:current-well @app-state))
                                                    :cmpl (:cmpl (:current-well @app-state))}))
  (get-well-mstr-map)
  (get-modl-ctrl-map)
  (get-lgas-props-map)
  (get-rsvr-map)
  (get-dsvy-map)
  (get-flow-line-map)
  (get-inj-mech-map)
  (get-prod-mech-map)
  (get-lgas-perf-settings-map)
  (get-alt-temps-map)
  (get-stored-lgas-response-map)
  (get-welltest-map)
  (get-flowing-gradient-survey-map)
  (get-reservoir-survey)
  (get-scada-survey)
  (get-mandrel-survey-map)
  (get-welltest-hist-map)
  (get-flowing-gradient-survey-hist-map)
  (get-reservoir-survey-hist-map)
  (get-scada-survey-hist-map)
  (get-mandrel-survey-hist-map))





