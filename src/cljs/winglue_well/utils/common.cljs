(ns winglue-well.utils.common
  (:import [goog.date.DateTime])
  (:require [winglue-well.utils.format :as rformat]
            [goog.date :as gdate]
            [goog.string :as gstring]
            [goog.string.format]))

(defn getUTCtime [StringDate]
  (let [date (gdate/fromIsoString StringDate)]
    (.getTime date)))

(defn decode-valve-status
  [v]
  (get [:unknown :closed :open :transition
        :back-check :is-dummy :use-pct-open] v))

(defn parse-tao2-table [table]
  "parses raw table data obtained from server into an array of row maps.
   e.g. [{:col0 entry00 :col1 entry01}
         {:col1 entry10 :col2 entry11}]"
  (let [cols (keys table)]
    (vec
      (for [idx (range (apply max (map count (vals table))))]
        (apply assoc {} (interleave cols (for [col cols] (nth (col table) idx))))))))

(defn decode-flow-regime
  [fr]
  (get [:liquid :bubble :slug :transiton :annular :stratified
        :gas :unld-liquid :unld-bubble :unld-slug :unld-transition
        :unld-annular :unld-stratified :unld-gas
        :1ph-liq :1ph-gas] fr))

(defn filter-func
  "Filter function for filter-dummy-valves-from-valve-map function"
  [[idx val] status-list]
  (if (= :is-dummy (nth status-list idx))
    false
    val))

(defn- mandrel-point-formatter
  []
  (let [js-point (js* "this")]
    (str "<span style=\"color:{point.color}\">●</span>Depth: <b>"
         (rformat/format-dec (.-y js-point) 2)
         "</b><br/>")))

(defn filter-dummy-valves-from-valve-map
  "Filters all dummy valves from the valve-map"
  [valve-map]
  (let [status-list (map decode-valve-status (:status-list valve-map))]
    (apply hash-map
           (apply concat
                  (for [col (keys valve-map)
                        :let [vals (col valve-map)]]
                    [col (vec (map (fn [[_ itm]] itm)
                                   (filter #(filter-func % status-list)
                                           (map-indexed (fn [idx itm] [idx itm]) vals))))])))))

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


(defn decode-surf-close
  [cat-id press]
  (if (or (= cat-id "UIPS") (= cat-id "UIPO")) press "N/A"))

(defn decode-surf-open
  [cat-id press]
  (if (or (= cat-id "UIPS") (= cat-id "UIPO")) press
                                               (if (= cat-id "ORIF") "N/A for ORIFICE"
                                                                     (if (= cat-id "DUM") "N/A for DUMMY"
                                                                                          (if (or (= cat-id "UPPS") (= cat-id "UPPO")) "N/A for PPO"
                                                                                                                                       "N/A")))))

(defn decode-tro
  [cat-id press]
  (if (= cat-id "ORIF") "N/A for ORIFICE"
                        (if (= cat-id "DUM") "N/A for DUMMY"
                                             press)))