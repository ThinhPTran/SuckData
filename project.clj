(defproject com.AppSmiths/winglue-well "0.2.1"
  :description "Library for loading bits of WinGLUE well from database"
  :main winglue-well.ringmaster
  :url "http://appsmiths.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.229"]
                 [compojure "1.6.0"]
                 [funcool/clojure.jdbc "0.9.0"]
                 ;;[oracle.jdbc/oracledriver "12.1.0.2.0"]
                 [org.firebirdsql.jdbc/jaybird-jdk18 "2.2.10"]
                 [com.google.protobuf/protobuf-java "3.1.0"]
                 [honeysql "0.8.0"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [org.zeromq/jeromq "0.3.5"]
                 [org.zeromq/cljzmq "0.1.4" :exclusions [org.zeromq/jzmq]]
                 [ring "1.5.0"]
                 [ring/ring-defaults "0.2.1"]
                 [javax.servlet/servlet-api "2.5"]
                 [ring-transit "0.1.6" :exclusions [commons-codec]]
                 [com.cognitect/transit-clj "0.8.285" :exclusions [commons-codec]]
                 [ring-cors "0.1.8"]
                 [prismatic/schema "1.1.3"]
                 [me.raynes/conch "0.8.0"]
                 [http-kit "2.2.0"]
                 [clj-time "0.12.0"]
                 [org.flatland/protobuf "0.8.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [com.google.protobuf/protobuf-java-util "3.0.2" :exclusions [com.google.guava/guava]]
                 [environ "1.1.0"]
                 [reagent "0.6.1"]
                 [cheshire "5.7.1"]
                 [cljsjs/d3 "3.5.16-0"]
                 [secretary "1.2.3"]
                 [binaryage/devtools "0.8.3"]
                 [cljsjs/bootstrap "3.3.6-1"]
                 [cljsjs/bootstrap-slider "7.0.1-0"]
                 [cljsjs/highcharts "5.0.4-0"]
                 [com.taoensso/sente "1.11.0"]
                 [com.taoensso/timbre "4.7.4"]]

  :min-lein-version "2.5.3"

  :plugins [[lein-protobuf "0.5.0"]
            [lein-ring "0.9.7"]
            [lein-ancient "0.6.10"]
            [cider/cider-nrepl "0.13.0"]
            [lein-environ "1.1.0"]
            [lein-cljsbuild "1.1.4"]
            [deraen/lein-sass4clj "0.3.1"]
            [lein-asset-minifier "0.3.2" :exclusions [org.clojure/clojure]]
            [lein-pdo "0.1.1"]]

  :proto-path "../protobufs"
  :protoc "/usr/local/bin/protoc"

  ;;:protoc ~(let [osname (System/getProperty "os.name")
  ;;               suffix (if (.contains osname "Windows") ".exe" "")
  ;;               base (str "protoc" suffix)
  ;;               asv-plat-ports (java.io.File. (System/getenv "ASV_PLAT_PORTS"))
  ;;               asv-plat-bin   (java.io.File. asv-plat-ports "bin")]
  ;;           (.getCanonicalPath (java.io.File. asv-plat-bin base)))

  ;; :ring  - do not add, run with 'lein run' instead
  ;; loading protobuf.jar into local repo now, see README.md
  ;;:resource-paths ["resources"
  ;;                 "/usr/share/java/protobuf.jar"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :profiles
  {:dev
   {:source-paths ["src/clj" "playground"]
    :dependencies [[javax.servlet/servlet-api "2.5"]
                   [ring-mock "0.1.5"]]
    :uberjar {:aot :all}}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "winglue-well.core/reload"}
     :compiler     {:main                 winglue-well.core
                    :optimizations        :none
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/dev"
                    :asset-path           "js/compiled/dev"
                    :source-map-timestamp true}}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            winglue-well.core
                    :optimizations   :advanced
                    :output-to       "resources/public/js/compiled/app.js"
                    :externs ["src/cljs/js/externs/datatables.ext.js"
                              "src/cljs/js/externs/adminlte.ext.js"
                              "src/cljs/js/externs/tubingpipe.ext.js"]
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    true
                    :pseudo-names true}}]}

  :sass {:source-paths ["src/cljs/css"]
         :target-path "target/css"
         :output-style :compressed}

  :minify-assets {:assets {"resources/public/css/site.min.css" "target/css/"
                           "resources/public/js/compiled/site.min.js"
                           ["src/cljs/js/external/adminlte.js"
                            "src/cljs/js/external/datatables.min.js"
                            "src/cljs/js/external/dataTables.select.min.js"
                            "src/cljs/js/tubingpipe.js"]}}

  :aliases {"build-dev" ["do" ["sass4clj" "once"] ["minify-assets"] ["cljsbuild" "once" "dev"]]
            "build-min" ["do" ["sass4clj" "once"] ["minify-assets"] ["cljsbuild" "once" "min"]]})
