(ns winglue-well.utils.interpolator)

(defn interpolator
  "Takes a coll of 2D points (vectors) and returns
   their linear interpolation function."
  [points]
  (let [m (into (sorted-map) points)]
    (fn [x]
      (let [[[x1 y1]] (rsubseq m <= x)
            [[x2 y2]] (subseq m > x)]
        (if x2
          (+ y1 (* (- x x1) (/ (- y2 y1) (- x2 x1))))
          y1)))))


