(ns winglue-well.log
  (:require [clojure.pprint :as pprint])
  (:import [ch.qos.logback.classic Level Logger]
           [ch.qos.logback.core]
           [ch.qos.logback.classic.encoder]
           [java.io StringWriter]
           [org.slf4j LoggerFactory MDC]))

(def logger ^ch.qos.logback.classic.Logger (LoggerFactory/getLogger "Tao2"))

(defn set-log-level!
  "Pass keyword :error :info :debug"
  [level]
  (case level
    :off   (.setLevel logger Level/OFF)
    :trace (.setLevel logger Level/TRACE)
    :debug (.setLevel logger Level/DEBUG)
    :info  (.setLevel logger Level/INFO)
    :warn  (.setLevel logger Level/WARN)
    :error (.setLevel logger Level/ERROR)))

(defn add-log-file
  "Add a log file appender to the current logger using the given file name."
  [file-name]
  (let [file-appender (new ch.qos.logback.core.FileAppender)
        context (.getLoggerContext logger)
        encoder (new ch.qos.logback.classic.encoder.PatternLayoutEncoder)]

       (.setContext file-appender context)
       (.setName file-appender "FILE")
       (.setFile file-appender file-name)
       (.setContext encoder context)
       (.setPattern encoder "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %-10contextName %logger{36} - %msg%n")
       (.start encoder)
       (.setEncoder file-appender encoder)
       (.start file-appender)
       (.addAppender logger file-appender)))

(defn add-log-console
  "Add a console appender to the current logger."
  []
  (let [console-appender (new ch.qos.logback.core.ConsoleAppender)
        context (.getLoggerContext logger)
        encoder (new ch.qos.logback.classic.encoder.PatternLayoutEncoder)]

       (.setContext console-appender context)
       (.setName console-appender "CONSOLE")
       (.setContext encoder context)
       (.setPattern encoder "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %-10contextName %logger{36} - %msg%n")
       (.start encoder)
       (.setEncoder console-appender encoder)
       (.start console-appender)
       (.addAppender logger console-appender)))

(defmacro with-logging-context [context & body]
  "Use this to add a map to any logging wrapped in the macro. Macro can be nested.
  (with-logging-context {:key \"value\"} (log/info \"yay\"))
  "
  `(let [wrapped-context# ~context
         ctx# (MDC/getCopyOfContextMap)]
     (try
       (if (map? wrapped-context#)
         (doall (map (fn [[k# v#]] (MDC/put (name k#) (str v#))) wrapped-context#)))
       ~@body
       (finally
         (if ctx#
           (MDC/setContextMap ctx#)
           (MDC/clear))))))

(defmacro debug [& msg]
  `(.debug logger (print-str ~@msg)))

(defmacro info [& msg]
  `(.info logger (print-str ~@msg)))

(defmacro warn [& msg]
  `(.warn logger (print-str ~@msg)))

(defmacro trace [& msg]
  `(.trace logger (print-str ~@msg)))

(defmacro error [throwable & msg]
  `(if (instance? Throwable ~throwable)
    (.error logger (print-str ~@msg) ~throwable)
    (.error logger (print-str ~throwable ~@msg))))

(defmacro spy
  [expr]
  `(let [a# ~expr
         w# (StringWriter.)]
     (pprint/pprint '~expr w#)
     (.append w# " => ")
     (pprint/pprint a# w#)
     (error (.toString w#))
     a#))
