(ns einherjar.async.event
  (:require
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   #?@(:clj  [[clojure.core.async :as async]]
       :cljs [[cljs.core.async :as async]])))

;; ---- event dispatcher ----

(defrecord EventDispatcher [event-chan])

(defstate event-dispatcher
  :start (do (timbre/info "Starting event dispatcher...")
             (->EventDispatcher (async/chan 100)))
  :stop  (do (timbre/info "Stopping event dispatcher...")
             (async/close! (:event-chan @event-dispatcher))))

(defn dispatch!
  [{:keys [event-chan] :as event-dispatcher} event]
  (async/put! event-chan event))
