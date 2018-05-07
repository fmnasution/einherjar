(defproject einherjar "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [;; ---- clj ----
                 [org.clojure/clojure "1.10.0-alpha4"]
                 [com.google.guava/guava "24.1-jre"]
                 [aero "1.1.3"]
                 [http-kit "2.3.0"]
                 [metosin/ring-http-response "0.9.0"]
                 [ring/ring-defaults "0.3.1"]
                 [metosin/muuntaja "0.5.0"]
                 ;; ---- cljc ----
                 [mount "0.1.12"]
                 [bidi "2.1.3"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.taoensso/encore "2.96.0"]
                 ;; ---- cljs ----
                 [org.clojure/clojurescript "1.10.238"]]
  :source-paths ["src/"]
  :resource-paths ["resources/"]
  :profiles {:dev {:source-paths ["env/dev/"]
                   :dependencies [[org.clojure/tools.nrepl "0.2.13"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.15"]
                                  [org.clojure/tools.namespace "0.3.0-alpha4"]]
                   :plugins [[refactor-nrepl "2.4.0-SNAPSHOT"]]
                   :repl-options {:nrepl-middleware
                                  [cemerick.piggieback/wrap-cljs-repl]

                                  :init-ns einherjar.dev}}}
  :aliases {"dev" ["with-profile" "+dev" "do" "clean," "repl" ":headless"]})
