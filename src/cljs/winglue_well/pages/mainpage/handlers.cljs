(ns winglue-well.pages.mainpage.handlers
  (:require [winglue-well.db :as mydb]))

(def default-options
  {:nav-bar true
   :left-sidebar true
   :right-sidebar true
   :footer true
   :left-sidebar-on? true})

(defn set-main-page-option []
  (swap! mydb/local-app-state assoc-in [:pages :mainpage :options] default-options))

(defn set-main-page-content [content]
  (swap! mydb/local-app-state assoc-in [:pages :mainpage :content] content))

(defn toggle-left-sidebar []
  (.log js/console "Toggling")
  (let [on? (get-in @mydb/local-app-state [:pages :mainpage :options :left-sidebar-on?])]
     (if (= on? true)
       (swap! mydb/local-app-state assoc-in [:pages :mainpage :options :left-sidebar-on?] false)
       (swap! mydb/local-app-state assoc-in [:pages :mainpage :options :left-sidebar-on?] true))))
