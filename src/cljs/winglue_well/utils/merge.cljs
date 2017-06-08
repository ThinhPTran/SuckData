(ns winglue-well.utils.merge)

(defn deep-merge-with
  "recursive merge-with"
  [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps)))
    maps))

(defn deep-merge
  "recursive merge for maps"
  [& maps]
  (apply deep-merge-with (fn [& vals] (last vals)) maps))

