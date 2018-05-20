(ns einherjar.websocket.resources
  (:require
   [ring.util.http-response :as ring.response]
   [taoensso.encore :as encore]
   [einherjar.websocket.server :as ws.srv]))

(defn websocket-server-resource
  [{:keys [request-method] :as request}]
  (encore/cond
    :let [websocket-server (::ws.srv/websocket-server request)]

    (nil? websocket-server)
    (ring.response/service-unavailable)

    :let [resource (ws.srv/ring-resource websocket-server request-method)]

    (nil? resource)
    (ring.response/method-not-allowed)

    :else (resource request)))

