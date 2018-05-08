(ns einherjar.config.client
  (:require
   [mount.core :refer [defstate]]
   [bidi.bidi :as router]
   [taoensso.timbre :as timbre]
   [einherjar.routes.routes :as rtr.rts]))

(defn- websocket-server-uri
  []
  (router/path-for (rtr.rts/server-routes) ::rtr.rts/websocket))

(defstate config
  :start (do (timbre/info "Reading config...")
             {:websocket-client {:uri (websocket-server-uri)}}))
