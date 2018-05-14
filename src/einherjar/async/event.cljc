(ns einherjar.async.event
  (:require
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [einherjar.async.protocols :as asnc.prt]
   #?@(:clj [[clojure.core.async :as async]]
       :cljs [[cljs.core.async :as async]])))

;; ---- event dispatcher ----

(defrecord EventDispatcher [event-chan]
  asnc.prt/ISource
  (source-chan [event-dispatcher]
    (:event-chan event-dispatcher))

  asnc.prt/ISink
  (sink-chan [event-dispatcher]
    (:event-chan event-dispatcher)))

(defstate event-dispatcher
  :start (do (timbre/info "Starting event dispatcher...")
             (->EventDispatcher (async/chan 100)))
  :stop  (do (timbre/info "Stopping event dispatcher...")
             (async/close! (:event-chan @event-dispatcher))))
