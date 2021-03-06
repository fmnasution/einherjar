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
                 [com.datomic/datomic-free "0.9.5697"]
                 [datomic-schema "1.3.0"]
                 [io.rkn/conformity "0.5.1"]
                 [buddy/buddy-hashers "1.3.0"]
                 [com.cognitect/transit-clj "0.8.309"]
                 ;; ---- cljc ----
                 [org.clojure/core.async "0.4.474"]
                 [com.rpl/specter "1.1.1"]
                 [mount "0.1.12"]
                 [bidi "2.1.3"]
                 [rum "0.11.2"]
                 [cljs-ajax "0.7.3"]
                 [datascript "0.16.5"]
                 [datascript-schema "0.2.1"]
                 [com.taoensso/sente "1.12.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.taoensso/encore "2.96.0"]
                 ;; ---- cljs ----
                 [org.clojure/clojurescript "1.10.238"]
                 [cljs-react-material-ui "0.2.50"]
                 [com.cognitect/transit-cljs "0.8.256"]]
  :source-paths ["src/"]
  :resource-paths ["resources/"]
  :profiles {:dev {:clean-targets ^{:protect false}
                   [:target-path
                    "resources/public/einherjar/app.js"
                    "resources/public/einherjar/out"]
                   :source-paths ["env/dev/"]
                   :dependencies [[cider/nrepl "0.3.0"]
                                  [cider/piggieback "0.3.2"]
                                  [figwheel-sidecar "0.5.16"]
                                  [org.clojure/tools.namespace "0.3.0-alpha4"]]
                   :plugins [[refactor-nrepl "2.4.0-SNAPSHOT"]]
                   :repl-options {:nrepl-middleware
                                  [cider.piggieback/wrap-cljs-repl]

                                  :init-ns einherjar.dev}}}
  :aliases {"dev" ["with-profile" "+dev" "do" "clean," "repl" ":headless"]})
