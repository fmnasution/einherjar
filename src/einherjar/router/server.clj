(ns einherjar.router.server
  (:require
   [clojure.string :as string]
   [mount.core :refer [defstate]]
   [bidi.ring :as ring.router]
   [ring.util.http-response :as ring.response]
   [ring.middleware.defaults :as ring.defaults]
   [muuntaja.middleware :as ring.muuntaja]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.router.routes :as rtr.rts]
   [einherjar.websocket.server :as ws.srv]
   [einherjar.middleware :as mdw]))

;; ---- ring middleware ----

(defn wrap-trailing-slash
  [handler]
  (fn [{:keys [uri] :as request}]
    (handler (assoc request :uri (if (and (not= "/" uri)
                                          (string/ends-with? uri "/"))
                                   (subs uri 0 (dec (count uri)))
                                   uri)))))

(defn wrap-exception
  ([handler error-handler]
   (fn [{:keys [uri] :as request}]
     (let [error-handler (or error-handler
                             (-> (ring.response/internal-server-error)
                                 (ring.response/content-type "text/plain")
                                 (constantly)))]
       (encore/catching
        (handler request)
        error1
        (encore/catching
         (if (some? error-handler)
           (error-handler error1 request)
           (do (timbre/error error1 "`handler` error at:" uri)
               (error-handler request)))
         error2
         (do (timbre/error error2 "`error-handler` error at:" uri)
             (ring.response/internal-server-error)))))))
  ([handler]
   (wrap-exception handler nil)))

(defn wrap-default
  [handler]
  (ring.defaults/wrap-defaults handler ring.defaults/site-defaults))

(defn wrap-format
  [handler]
  (ring.muuntaja/wrap-format handler))

(defrecord RingMiddleware [entries])

(defstate ring-middleware
  :start (do (timbre/info "Creating ring middleware...")
             (->RingMiddleware
              [wrap-trailing-slash
               wrap-exception
               wrap-default
               wrap-format
               [ws.srv/wrap-websocket-server @ws.srv/websocket-server]])))

;; ---- ring router ----

(defrecord RingRouter [routes resources])

(defn- wrap-ring-router
  [handler ring-router]
  (fn [request]
    (handler (assoc request ::ring-router ring-router))))

(defn- resources
  []
  {::rtr.rts/index (constantly
                    {:status  200
                     :body    "Hello World!"
                     :headers {"content-type" "text/html"}})})

(defstate ring-router
  :start (do (timbre/info "Creating ring router...")
             (->RingRouter (rtr.rts/server-routes) (resources))))

;; ---- ring handler ----

(defn- create-ring-handler
  ([{:keys [routes resources] :as ring-router}
    {:keys [entries] :as ring-middleware}
    not-found-handler]
   (let [handler           (ring.router/make-handler routes resources)
         not-found-handler (or not-found-handler
                               (-> (ring.response/not-found)
                                   (ring.response/content-type "text/plain")
                                   (constantly)))
         middleware        (-> entries
                               (conj [wrap-ring-router ring-router])
                               (mdw/new-middleware))]
     (middleware (fn [request]
                   (or (handler request)
                       (not-found-handler request))))))
  ([ring-router ring-middleware]
   (create-ring-handler ring-router ring-middleware nil)))

(defstate ring-handler
  :start (do (timbre/info "Creating ring handler...")
             (create-ring-handler @ring-router @ring-middleware)))
