(ns winglue-well.systemevents
  (:require [taoensso.sente :as sente]
            [winglue-well.db :as mydb]
            [clojure.tools.logging :as log]
            [org.httpkit.server :as http-kit]
            [clojure.data :as da :refer [diff]]
            [clojure.pprint :as pp]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]))

(let [;; Serializtion format, must use same val for client + server:
      packer :edn ; Default packer, a good choice in most cases
      ;; (sente-transit/get-transit-packer) ; Needs Transit dep

      chsk-server
      (sente/make-channel-socket-server!
        (get-sch-adapter) {:packer packer})

      {:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      chsk-server]
  (def ring-ws-post ajax-post-fn)
  (def ring-ws-handoff ajax-get-or-ws-handshake-fn)
  (def receive-channel ch-recv)
  (def channel-send! send-fn)
  (def connected-uids connected-uids))

;; We can watch this atom for changes if we like

(defn connected-uids-change-handler [_ _ old new]
  (when (not= old new)
    (let [oldsk (:any old)
          newsk (:any new)
          newlogin (nth (diff oldsk newsk) 1)]
      (println "Connected uids change: %s" new)
      (println "oldsk: " oldsk)
      (println "newsk: " newsk)
      (println "newlogin: " newlogin))))

(add-watch connected-uids :connected-uids connected-uids-change-handler)

;; Messages handler

;; App-state change handler
(defn well-state-change-handler [key atom old-state new-state]
  (println "-- Well-state Changed --")
  (doseq [uid (:any @connected-uids)]
    (doseq [k (keys @mydb/well-state)]
      (channel-send! uid [:db/changeWellState {k (k @mydb/well-state)}]))))

(defn field-state-change-handler [key atom old-state new-state]
  (println "-- Field-state Changed --")
  (doseq [uid (:any @connected-uids)]
    (doseq [k (keys @mydb/field-state)]
      (channel-send! uid [:db/changeFieldState {k (k @mydb/field-state)}]))))

(add-watch mydb/well-state :well-watcher well-state-change-handler)
(add-watch mydb/field-state :field-watcher field-state-change-handler)

;; After login, please init the session app-state
(defn login-handler
  "Here's where you'll add your server-side login/auth procedure (Friend, etc.).
  In our simplified example we'll just always successfully authenticate the user
  with whatever user-id they provided in the auth request."
  [ring-req]
  (let [{:keys [session params]} ring-req
        {:keys [user-id]} params]
    (log/info "Login request: %s" params)
    (log/info "Session: %s" (str session))
    {:status 200 :session (assoc session :uid user-id)}))

;; Handle messages sent by session other than login-handler

(defn action-processing [{:keys [action value]}]
  (cond
    (= :pickdsn action) (mydb/pick-dsn value)
    (= :pickwell action) (mydb/pick-well value)))

;; Handle init message
(defn init-handler [{:keys [wsid]}]
  (mydb/initinfo)
  (doseq [k (keys @mydb/well-state)]
    (channel-send! wsid [:db/changeWellState {k (k @mydb/well-state)}]))
  (doseq [k (keys @mydb/field-state)]
    (channel-send! wsid [:db/changeFieldState {k (k @mydb/field-state)}])))

(defn changeWellState [{:keys [path data]}]
  (swap! mydb/well-state assoc-in path data))

(defn- ws-msg-handler []
  (fn [{:keys [event] :as msg} _]
    (let [[id data :as ev] event]
      (case id
        :db/init (init-handler data)
        :db/action (action-processing data)
        :db/changeWellState (changeWellState data)
        (log/info "Unmatched event: " id " data: " data)))))

(defn ws-message-router []
  (sente/start-chsk-router-loop! (ws-msg-handler) receive-channel))