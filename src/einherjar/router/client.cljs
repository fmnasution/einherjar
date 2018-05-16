(ns einherjar.router.client
  (:require
   [cljs.core.async :as async]
   [mount.core :refer [defstate]]
   [bidi.router :as router.html]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.router.routes :as rtr.rts]
   [einherjar.async.event :as asnc.evt]))

;; ---- html router ----

(defrecord HtmlRouter [routes router location-chan])

(defn- start-html-router!
  ([routes default-location location-chan]
   (let [router (router.html/start-router!
                 routes
                 {:on-navigate      #(async/put! location-chan %)
                  :default-location default-location})]
     (->HtmlRouter routes router location-chan)))
  ([routes default-location]
   (start-html-router! routes default-location (async/chan 100))))

(defstate html-router
  :start (do (timbre/info "Starting html router...")
             (start-html-router! (rtr.rts/client-routes)
                                 {:handler ::default})))

;; ---- html router pipeliner ----

(defn- bootstrap-location
  [{:keys [handler route-params]}]
  (encore/assoc-when {:location/handler handler}
                     :location/route-params (not-empty route-params)))

(defn- location->event
  [location]
  [:html-router/location location])

(defstate html-router-pipeliner
  :start (do (timbre/info "Pipelining location"
                          "from html router"
                          "to event dispatcher...")
             (async/pipeline 1
                             (:event-chan @asnc.evt/event-dispatcher)
                             (comp
                              (map bootstrap-location)
                              (map location->event))
                             (:location-chan @html-router)
                             false
                             (fn [error]
                               [:html-router/error
                                {:error error}
                                {:error? true}]))))
