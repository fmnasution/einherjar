(ns einherjar.figwheel
  (:require
   [clojure.spec.alpha :as spec]
   [mount.core :refer [defstate]]
   [figwheel-sidecar.repl-api :as figwheel]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.config.server :as cfg.srv]))

;; ---- figwheel server ----

(defn- figwheel-server-config
  [config]
  (spec/assert ::figwheel-server-config (:figwheel-server config)))

(defstate ^{:on-reload :noop} figwheel-server
  :start (do (timbre/info "Starting figwheel server...")
             (figwheel/start-figwheel!
              (figwheel-server-config @cfg.srv/config)))
  :stop (do (timbre/info "Stopping figwheel server...")
            (figwheel/stop-figwheel!)))

;; ---- spec ----

(spec/def ::id
  encore/nblank-str?)

(spec/def ::source-paths
  (spec/coll-of encore/nblank-str?))

(spec/def ::compiler
  map?)

(spec/def ::websocket-host
  (spec/or :exact     encore/nblank-str?
           :non-exact #{:js-client-host :server-ip :server-hostname}))

(spec/def ::on-jsload
  encore/nblank-str?)

(spec/def ::autoload
  boolean?)

(spec/def ::heads-up-display
  boolean?)

(spec/def ::load-warninged-code
  boolean?)

(spec/def ::figwheel-client-option
  (spec/keys :opt-un [::websocket-host
                      ::on-jsload
                      ::autoload
                      ::heads-up-display
                      ::load-warninged-code]))

(spec/def ::figwheel
  (spec/or :boolean boolean?
           :config  ::figwheel-client-option))

(spec/def ::build
  (spec/keys :req-un [::id ::source-paths ::compiler ::figwheel]))

(spec/def ::all-builds
  (spec/coll-of ::build))

(spec/def ::http-server-port
  encore/pos-int?)

(spec/def ::server-port
  encore/pos-int?)

(spec/def ::server-ip
  encore/nblank-str?)

(spec/def ::css-dirs
  (spec/coll-of encore/nblank-str?))

(spec/def ::ring-handler
  encore/qualified-symbol?)

(spec/def ::clj
  boolean?)

(spec/def ::cljc
  boolean?)

(spec/def ::reload-clj-files-config
  (spec/keys :opt-un [::clj ::cljc]))

(spec/def ::reload-clj-files
  (spec/or :boolean boolean?
           :config  ::reload-clj-files-config))

(spec/def ::open-file-command
  encore/nblank-str?)

(spec/def ::repl
  boolean?)

(spec/def ::server-logfile
  encore/nblank-str?)

(spec/def ::nrepl-port
  encore/pos-int?)

(spec/def ::nrepl-middleware
  (spec/coll-of encore/nblank-str?))

(spec/def ::watcher
  #{:polling})

(spec/def ::hawk-options
  (spec/keys :req-un [::watcher]))

(spec/def ::load-all-builds
  boolean?)

(spec/def ::figwheel-options
  (spec/keys :opt-un [::http-server-port
                      ::server-port
                      ::server-ip
                      ::css-dirs
                      ::ring-handler
                      ::reload-clj-files
                      ::open-file-command
                      ::repl
                      ::server-logfile
                      ::nrepl-port
                      ::nrepl-middleware
                      ::hawk-options
                      ::load-all-builds]))

(spec/def ::build-ids
  (spec/coll-of encore/nblank-str?))

(spec/def ::figwheel-server-config
  (spec/keys :req-un [::all-builds ::figwheel-options ::build-ids]))
