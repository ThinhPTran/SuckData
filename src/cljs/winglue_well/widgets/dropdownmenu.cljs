(ns winglue-well.widgets.dropdownmenu)

(defn DropdownMenuWithBlank
  "Dropdown menu with a blank first entry"
  [item-map selected-item on-change-fn]
  [:select.form-control
   {:value selected-item
    :on-change (fn [e]
                 ;; pass back nil if nothing was selected
                 (let [value (.-value (.-target e))
                       x (if (= value "") nil (keyword value))]
                   (on-change-fn x)))}

   ^{:key :select-none} [:option {:value nil} ""] ;; display nothing by default
   (for [item item-map]
     ^{:key item} [:option {:value (key item)} (val item)])])