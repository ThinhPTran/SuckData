(ns winglue-well.core
  (:require [goog.dom :as gdom]
            [winglue-well.db :as mydb]
            [winglue-well.serverevents :as se]
            [winglue-well.views :as views]
            [winglue-well.browsereventlisteners :as bels]
            [winglue-well.handlers :as handlers]
            [winglue-well.pages.mainpage.handlers :as mainpagehandlers]
            [winglue-well.router.router :as router]
            [reagent.core :as reagent]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page

;(defn page []
;      (let [input-text (:input-text @mydb/local-app-state)
;            user (:user @mydb/local-app-state)
;            appstate @mydb/app-state]
;           [:div ""
;            [:p
;             [:button#btn1 {:type "button"} "chsk-send! (w/o reply)"]
;             [:button#btn2 {:type "button"} "chsk-send! (with reply)"]]
;            [:p
;             [:button#btn5 {:type "button"} "Disconnect!"]
;             [:button#btn6 {:type "button"} "Reconnect"]]
;            [:hr]
;            [:p
;             [:input#input-login {:type :text
;                                  :placeholder "User-id"
;                                  :value input-text
;                                  :onChange se/usernameChange}]
;             [:button#btn-login {:type "button"
;                                 :onClick se/loginHandler} "Secure login!"]]
;            [:div (str "input-text: " input-text)]
;            [:div (str "User: " user)]
;            [:div (str "app-state: ")]
;            [:div (str appstate)]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize App

(defn dev-setup []
  (when ^boolean js/goog.DEBUG
    (enable-console-print!)
    (println "dev mode")))

(defn reload []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export main []
  (router/init-routes)
  (dev-setup)
  (handlers/init-states)
  (bels/hook-event-listeners)
  (reload))
