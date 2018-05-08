(ns einherjar.web.server
  (:require
   [clojure.spec.alpha :as spec]
   [mount.core :refer [defstate]]
   [org.httpkit.server :as httpkit]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.config.server :as cfg.srv]
   [einherjar.router.server :as rtr.srv]))

;; ---- web server ----

(defrecord WebServer [server config])

(defn- start-web-server!
  [handler config]
  (let [server (httpkit/run-server
                handler
                (spec/assert ::web-server-config config))]
    (->WebServer server config)))

(defn- stop-web-server!
  [{:keys [server config] :as web-server}]
  (server :timeout (:timeout config)))

(defstate web-server
  :start (do (timbre/info "Starting web server...")
             (start-web-server! @rtr.srv/ring-handler
                                (:web-server @cfg.srv/config)))
  :stop  (do (timbre/info "Stopping web server...")
             (stop-web-server! @web-server)))

;; ---- spec ----

(spec/def ::port
  encore/pos-int?)

(spec/def ::timeout
  encore/pos-int?)

(spec/def ::web-server-config
  (spec/keys :req-un [::port ::timeout]))
