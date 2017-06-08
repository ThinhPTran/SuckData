(ns winglue-well.widgets.box
  (:require [clojure.string :as str]))

;options
;{:type "default" ;;default, primary, info, warning, success, danger ;;standard bootstrap
; :special ":collapsable/:removable/:expandable" ;; collapsable and expandable differ in their initial states
; :table-responsive "bool" ;;set to true to allow horizontal scrollable area for large tables
; :solidBox "bool"
; :loading "bool"
; :header {:title "string"
;          :with-border "bool"
;          :box-tools "other reagent components"}
; :footer {:content }}

(def default-options
  {:type "default"
   :solidBox false
   :loading false
   :header {:with-border false}})

(defn BoxContainer
  "Box container from the adminLTE html template"
  ([content] (BoxContainer default-options content))
  ([options content]
   (let [options (merge default-options options) ;;any unspecified options fall back to the default options
         box-class (str/join " " ["box"
                                  (str "box-" (:type options))
                                  (if (:solidBox options) "box-solid")])
         header-class (str/join " " ["box-header"
                                     (if (:with-border options) "with-border")])
         box-body-class (str/join " " ["box-body"
                                       (if (:table-responsive options)
                                         "table-responsive")])]
     [:div
      [:div {:class box-class}
       ;; Header
       (if-not (nil? (get-in options [:header :title]))
         [:div {:class header-class}
          [:div.box-title
           (get-in options [:header :title])]
          ;; box-tools
          [:div.box-tools.pull-right
           ;; user supplied
           (if-not (nil? (get-in options [:header :box-tools]))
             (get-in options [:header :box-tools]))
           ;; if box is expandable/collapsable/removable
           (condp = (:special options)
             :collapsable [:button.btn.btn-box-tool {:data-widget "collapse"}
                           [:i.fa.fa-minus]]
             :expandable [:button.btn.btn-box-tool {:data-widget "collapse"}
                          [:i.fa.fa-plus]]
             :removable [:button.btn.btn-box-tool {:data-widget "remove"}
                         [:i.fa.fa-times]]
             ())]])

       ;; Body
       [:div {:class box-body-class}
        content]

       ;; Footer
       (if-not (nil? (get-in options [:footer :content]))
         [:div.box-footer
          (get-in options [:footer :content])])

       ;; Loading animation if needed
       (if (:loading options)
         [:div.overlay
          [:i.fa.fa-refresh.fa-spin]])]])))

