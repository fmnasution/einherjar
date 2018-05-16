(ns einherjar.web.ajax.client
  (:require
   [mount.core :refer [defstate]]
   [ajax.core :as ajax]
   [bidi.bidi :as router]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.web.impl.ajax :as wb.ipl.jx]
   [einherjar.router.routes :as rtr.rts]
   [einherjar.websocket.client :as ws.clt]
   [einherjar.async.event :as asnc.evt]))

;; ---- server ajax caller ----

(defrecord ServerAjaxCaller [routes requester]
  router/RouteProvider
  (routes [server-ajax-caller]
    (:routes server-ajax-caller)))

(defn- csrf-token-interceptor
  [websocket-client]
  (ajax/to-interceptor
   {:name    "CSRF Token"
    :request (fn [request]
               (if-let [csrf-token (ws.clt/csrf-token websocket-client)]
                 (assoc-in request [:headers :x-csrf-token] csrf-token)
                 request))}))

(defn- event->handler
  [event-dispatcher [id data]]
  (fn [response]
    (let [event [id (assoc data :ajax/response response)]]
      (asnc.evt/dispatch! event-dispatcher event))))

(defn- bootstrap-handler
  [{:keys [event error-event] :as option} event-dispatcher]
  (let [handler       (event->handler event-dispatcher event)
        error-handler (event->handler event-dispatcher error-event)]
    (-> option
        (dissoc :event :error-event)
        (assoc :handler handler :error-handler error-handler))))

(defn- start-server-ajax-caller!
  [event-dispatcher websocket-client routes]
  (let [interceptors [(csrf-token-interceptor websocket-client)]
        requester    (fn [handler route-params request-method option]
                       (let [option (-> option
                                        (encore/update-in
                                         [:interceptors]
                                         []
                                         #(into % interceptors))
                                        (bootstrap-handler event-dispatcher)
                                        (assoc :format          :transit
                                               :response-format :transit))]
                         (wb.ipl.jx/request! routes
                                             handler
                                             route-params
                                             request-method
                                             option)))]
    (->ServerAjaxCaller routes requester)))

(defstate server-ajax-caller
  :start (do (timbre/info "Starting server ajax caller...")
             (start-server-ajax-caller!
              @asnc.evt/event-dispatcher
              @ws.clt/websocket-client
              (rtr.rts/server-routes))))

(defn request!
  ([{:keys [requester] :as ajax-caller}
    handler route-params request-method option]
   (requester handler route-params request-method option))
  ([ajax-caller handler request-method option]
   (request! ajax-caller handler {} request-method option)))
