(ns einherjar.web.ajax.client
  (:require
   [mount.core :refer [defstate]]
   [ajax.core :as ajax]
   [bidi.bidi :as router]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.web.impl.ajax :as wb.ipl.jx]
   [einherjar.router.routes :as rtr.rts]
   [einherjar.websocket.client :as ws.clt]))

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

(defn- start-server-ajax-caller!
  [websocket-client routes]
  (let [interceptors [(csrf-token-interceptor websocket-client)]
        requester    (fn [handler route-params request-method option]
                       (let [option (encore/update-in
                                     option
                                     [:interceptors]
                                     []
                                     #(into % interceptors))]
                         (wb.ipl.jx/request! routes
                                             handler
                                             route-params
                                             request-method
                                             option)))]
    (->ServerAjaxCaller routes requester)))

(defstate server-ajax-caller
  :start (do (timbre/info "Starting server ajax caller...")
             (start-server-ajax-caller! (rtr.rts/server-routes))))

(defn request!
  ([{:keys [requester] :as ajax-caller} handler route-params request-method option]
   (requester handler route-params request-method option))
  ([ajax-caller handler request-method option]
   (request! ajax-caller handler {} request-method option)))
