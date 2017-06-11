(ns winglue-well.serverevents
  (:require
    [goog.dom :as gdom]
    [reagent.core :as r]
    [winglue-well.db :as mydb]
    [clojure.string :as str]
    [taoensso.encore :as encore :refer-macros (have have?)]
    [taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]
    [taoensso.sente :as sente :refer (cb-success?)]))

; sente js setup
(let [chsk-type :auto
      ;; Serializtion format, must use same val for client + server:
      packer :edn ; Default packer, a good choice in most cases
      ;; (sente-transit/get-transit-packer) ; Needs Transit dep

      {:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client!
        "/chsk" ; Must match server Ring routing URL
        {:type   chsk-type
         :packer packer})]
     (def receive-channel ch-recv)
     (def send-channel! send-fn)
     (def chsk chsk)
     (def chsk-state state))

;; Messages will be used to send to server

;; Can be used but not the main action. Client only submit his actions to server
(defn changeState
  [path data]
  (send-channel! [:db/changeAppState {:path path
                                      :data data}]))

(defn sendAction
  [action value]
  (send-channel! [:db/action {:action action
                              :value value}]))

;; Event handlers

(defn usernameChange [_]
      (let [v (.-value (gdom/getElement "input-login"))]
           ;(.log js/console "change something!!!: " v)
           (swap! mydb/local-app-state assoc :input-text v)))

; Login handler
(defn loginHandler [ev]
      (let [user-id (:input-text @mydb/local-app-state)]
           (if (str/blank? user-id)
             (js/alert "Please enter a user-id first")
             (do
               (.log js/console "Logging in with user-id %s" user-id)

               ;;; Use any login procedure you'd like. Here we'll trigger an Ajax
               ;;; POST request that resets our server-side session. Then we ask
               ;;; our channel socket to reconnect, thereby picking up the new
               ;;; session.

               (sente/ajax-lite "/login"
                                {:method :post
                                 :headers {:X-CSRF-Token (:csrf-token @chsk-state)}
                                 :params  {:user-id (str user-id)}}

                                (fn [ajax-resp]
                                    (.log js/console "Ajax login response: %s" ajax-resp)
                                    (let [login-successful? true] ; Your logic here

                                         (if-not login-successful?
                                                 (.log js/console "Login failed")
                                                 (do
                                                   (.log js/console "Login successful")
                                                   (sente/chsk-reconnect! chsk))))))))))

; handle application-specific events
(defn- app-message-received [[msgType data]]
   (case msgType
         :db/changeWellState (do
                              ;(.log js/console (str "data: " data))
                              (let [k (first (keys data))
                                    val (first (vals data))
                                    oldval (k @mydb/well-state)]
                                (when (not= val oldval)
                                  (swap! mydb/well-state assoc (first (keys data)) (first (vals data))))))
         :db/changeFieldState (do
                                ;(.log js/console (str "data: " data))
                                (let [k (first (keys data))
                                      val (first (vals data))
                                      oldval (k @mydb/field-state)]
                                  (when (not= val oldval)
                                    (swap! mydb/field-state assoc (first (keys data)) (first (vals data))))))
         (do
           (.log js/console "Unmatched application event")
           (.log js/console "Received message: \n")
           (.log js/console "msgType: " (str msgType) "\n")
           (.log js/console "data: " (str data) "\n"))))

; handle websocket-connection-specific events
(defn- channel-state-message-received [state]
       (if (:first-open? state)
         (.log js/console "First open!!!\n")))

; handle websocket handshake events
(defn- handshake-message-received [[wsid csrf-token hsdata isfirst]]
       (.log js/console "Handshake message:")
       (.log js/console "wsid: " (str wsid))
       (.log js/console "csrf-token: " (str csrf-token))
       (.log js/console "hsdata: " (str hsdata))
       (.log js/console "isFirst: " (str isfirst))
       (swap! mydb/local-app-state assoc :user wsid)
       (send-channel! [:db/init {:wsid wsid}]))

; main router for websocket events
(defn- event-handler [[id data] _]
       (case id
             :chsk/state (channel-state-message-received data)
             :chsk/recv (app-message-received data)
             :chsk/handshake (handshake-message-received data)
             (.log js/console "Unmatched connection event with " (str id) " and data " (str data))))

(sente/start-chsk-router-loop! event-handler receive-channel)



