(ns winglue-well.browsereventlisteners
  (:require [winglue-well.handlers :as handler]))

(defn hook-event-listeners []
  (.log js/console "hook-event-listeners")
  (set! (.-onresize js/window) #(handler/window-resize-handler
                                  (.-innerWidth js/window)
                                  (.-innerHeight js/window)))
  (set! (.-onfocus js/window) #(handler/set-window-focused? true))
  (set! (.-onblur js/window) #(handler/set-window-focused? false)))
