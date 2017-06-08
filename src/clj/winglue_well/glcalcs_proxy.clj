;; Simple message queuing broker
;; Same as request-reply broker but using QUEUE device
;; See: http://zguide.zeromq.org/page:all#toc39

(ns winglue-well.glcalcs-proxy
  (:require [winglue-well.config.core :as config])
  (:require [zeromq
             [device :as device]
             [zmq :as zmq]])
  (:require [me.raynes.conch.low-level :as sh])
  (:use     [clojure.java.io])
  (:import  [java.util.zip ZipInputStream])
  (:import  [java.io IOException])
  (:import  [org.apache.commons.io FileUtils])
  (:gen-class))

(defn- glcalcs-cfg [] (:glcalcs @config/tao2-cfg))
(defn- poll-timeo [] (:timeo (glcalcs-cfg))) ; ms
(defn- worker-path [] (:path (glcalcs-cfg)))
(defn- worker-count [] (:worker-count (glcalcs-cfg)))
(defn- glcalcs-frontend [] (:frontend (glcalcs-cfg)))
(defn- glcalcs-backend [] (:backend (glcalcs-cfg)))

(defn- proxy-socket
  "Poll from receiver socket and proxy to sender socket"
  [rcv snd poller poll-id]
  (when (zmq/check-poller poller poll-id :pollin)
    (loop [part (zmq/receive rcv)]
      (let [more? (zmq/receive-more? rcv)]
        (zmq/send snd part (if more? zmq/send-more 0))
        (when more?
          (recur (zmq/receive rcv)))))))

; list of currently running worker processes:
(def current-workers (atom []))

; name of subdirectory in temp to extract glcalcs to:
(def glcalcs-dest "glcalcs")

(defn- aplatform
  "placehoder to compute aplatform - will be needed on target system so
   environment is not sufficient, unless we insist on it"
  []
  ;; environment wins if set
  (or (System/getenv "aplatform")
      (let [osname (System/getProperty "os.name")] 
        (cond
          (.startsWith osname "Windows") "vs14-64"
          (.startsWith osname "Linux")   "x86_64-pc-linux-gnu"
          :else (throw (Exception.
                        "unable to determine usable 'aplatform'"))))))


(defn- native-resources-dir []
  "java.lang.file object for the proper resource dir"
  (let [res (file (System/getProperty "user.dir") "resources" "glcalcs")
        plat (aplatform)
        target (file res plat)]
    (.mkdirs target)
    target))

(defn native-files-list
  "make iseq of native java.io.file with relative path only"
  []
  (let [ap (aplatform)]
    (cond (.startsWith ap "vs")
          (list (file "bin" "glcalcs.exe")
                (file "bin" "asvglcalcs.dll")
                (file "bin" "asvglmodels.dll")
                (file "bin" "asvglsuplib.dll")
                (file "bin" "asvtwophase.dll")
                (file "bin" "asvtools.dll")
                (file "bin" "nlopt.dll")
                (file "bin" "libzmq-mt-4_2_0.dll")
                (file "lib" "boost_program_options-vc140-mt-1_61.dll")
                (file "bin" "libf77.dll")
                (file "bin" "libi77.dll"))
          
          (.contains ap "-linux-")
          (list (file "bin" "glcalcs")
                (file "lib64" "libasvglmodels.so")
                (file "lib64" "libasvglsuplib.so")
                (file "lib64" "libasvtwophase.so")
                (file "lib64" "libasvtools.so")
                (file "lib64" "libnlopt.so.0")
                (file "lib"   "libzmq.so.4.2.0")
                (file "lib"   "libboost_system-gcc62-mt-1_61.so.1.61.0")
                (file "lib"   "libboost_serialization-gcc62-mt-1_61.so.1.61.0")
                (file "lib"   "libboost_regex-gcc62-mt-1_61.so.1.61.0")
                (file "lib"   "libboost_filesystem-gcc62-mt-1_61.so.1.61.0")
                (file "lib"   "libboost_chrono-gcc62-mt-1_61.so.1.61.0")
                (file "lib"   "libboost_thread-gcc62-mt-1_61.so.1.61.0")
                (file "lib"   "libboost_iostreams-gcc62-mt-1_61.so.1.61.0")
                (file "lib"   "libboost_program_options-gcc62-mt-1_61.so.1.61.0")
                (file "lib"   "libboost_date_time-gcc62-mt-1_61.so.1.61.0"))

          :else
          (throw (Exception.
                  (format "can't determine native files for aplatform: %s"
                          ap))))))

(defn install-native-resources
  "a process that gets all of the required native resources out of the
   filesystem, and stuffs these into the resources directory for inclusion
   in the uberjar.  This is a build-time activity, not runtime, so either
   run it by hand, or build a small script to call it from outside"
  []
  (let [ports-var "ASV_PLAT_PORTS"
        ports-env (or (System/getenv ports-var)
                      (throw (Exception.
                              (format "env: %s not set" ports-var))))
        ports-dir (file ports-env)
        resnat    (native-resources-dir)]
    (doseq [nf (native-files-list)]
      (let [basename (.getName nf)
            src (file ports-dir nf)
            trg (file resnat basename)]
        (println (format "copying: %s" basename))
        (FileUtils/copyFile src trg true)))))

(defn- extract-res [filename dstdir]
  (let [res (resource filename)
        tmppath (file filename) ; strip path
        outfile (file dstdir (.getName tmppath))
        dstname (.getPath outfile)]
    (when res
      (println "Extracting " filename " to " dstname)
      (try
        (FileUtils/copyURLToFile res outfile)
        (doto outfile
          (.setReadable true)
          (.setExecutable true))
        (catch IOException e (println "Failed to extract: "
                                      (.getMessage e)))))))

(defn- extract-worker []
  (let [tmpdir    (System/getProperty "java.io.tmpdir")
        dstdir    (file tmpdir glcalcs-dest)
        basenames (map #(.getName %) (native-files-list))
        glc-exe   (first (filter #(.startsWith % "glcalcs") basenames))
        resbase   (str "glcalcs" "/" (aplatform))]

    (.mkdirs dstdir)
    (doseq [fn basenames]
      (extract-res (str resbase "/" fn) dstdir))
      
    ; change settings to point to extracted path:
    (let [glc-path (.getPath (file dstdir glc-exe))]
      (swap! config/tao2-cfg assoc-in [:glcalcs :path] glc-path))))

(defn- glcalcs-cfg [] (:glcalcs @config/tao2-cfg))

(defn- environ
  "get environment as a clojure map"
  []
  (into {} (System/getenv)))

(defn- is-linux
  "test if running under linux"
  []
  (.startsWith (System/getProperty "os.name") "Linux"))

(defn- prepend-path
  "add the given entry to the front of a path string"
  [path-str entry]
  (if (and path-str (not (empty? path-str)))
    (str entry ":" path-str)
    entry))

(defn- update-with-ldlp
  "returns environment with LD_LIBRARY_PATH updated to prepend directory 
   of the given path"
  [path]
  (let [parent   (.getParent (file path))
        prev-env (environ)
        ldlp-key "LD_LIBRARY_PATH"]
    (assoc prev-env ldlp-key (prepend-path (get prev-env ldlp-key) parent))))

(defn- spawn-worker [path & args]
  (let [rargs (cond-> (concat [path] args)
                (is-linux) (concat [:env (update-with-ldlp path)]))]
    (apply sh/proc rargs)))

(defn- spawn-workers []
  (let [path (worker-path)]
    (loop [ix (worker-count) workers []]
      (if (not (pos? ix))
        workers
        (do
          (println "spawning worker:" path)
          (Thread/sleep 500)
          (recur (dec ix) (conj workers
                                (spawn-worker path (glcalcs-backend)))))))))

(defn- worker-exit-val [worker]
  "Return exit code if worker has terminated, else worker still running."
  (if-let [process (:process worker)]
    (try
      (.exitValue (:process worker))
      (catch Exception e :worker-still-running))
    :bad-worker-info))

(defn- check-worker [worker]
  "Checks to see if worker process is still alive and spawn if not"
  (if (number? (worker-exit-val worker))
    (do
      (println "respawned failed worker")
      (sh/proc (worker-path) (glcalcs-backend)))
    worker))

(defn- check-workers [workers]
  "Check all workers for termination."
  (if (empty? workers)
    (spawn-workers)
    (map check-worker workers)))

(defn- kill-worker [worker]
  (println "Killing worker")
  (.destroy (:process worker)))

(defn- kill-workers [workers]
  "Kills all child worker processes in 'workers'"
  (doall (map kill-worker workers))
  [])

(defn init []
  "Called on ringmaster startup.  Spawns a thread to manage glcalcs
   processes and their communication with the ring-server"
   (println "================\nStarting worker proxy...\n================\n")
  (if (not (:glcalcs-override (glcalcs-cfg)))
    (extract-worker))
  (when (not (.exists (as-file (worker-path))))
    (println "ERROR: Extracted '" (worker-path) "' not found")
    (System/exit 1))
  (future
    (let [context (zmq/zcontext)
          poller (zmq/poller context 2)]
      (with-open [frontend (doto (zmq/socket context :router)
                             (zmq/bind (glcalcs-frontend)))
                  backend (doto (zmq/socket context :dealer)
                            (zmq/bind (glcalcs-backend)))]
        (zmq/register poller frontend :pollin)
        (zmq/register poller backend :pollin)
        (swap! current-workers check-workers)
        (while (not (.. Thread currentThread isInterrupted))
          (when (not (= -1 (zmq/poll poller (poll-timeo))))
            (proxy-socket frontend backend poller 0)
            (proxy-socket backend frontend poller 1))
          (swap! current-workers check-workers))

        (swap! current-workers kill-workers)))))

(defn destroy []
  "Called on ringmaster shutdown"
  (swap! current-workers kill-workers))
