(ns winglue-well.utils.table)

(defn table-to-array
  "converts data in a table to a vector of sub-vectors where each sub-vector
   contain the requested column entries of a row. cols are the keywords of the
   columns.
   e.g. [{:col1 00 :col2 01}
         {:col1 10 :col2 11}]
        ->
        [[00 01] [10 11]]"
  [table & cols]
  (vec (filter #(not-any? nil? %)
               (for [row table]
                 (vec (for [col cols]
                        (col row)))))))

(defn map-table-to-array
  [table & cols]
  (vec (for [idx (range (count (first (vals table))))]
         (vec (for [col cols]
                (nth (col table) idx))))))



