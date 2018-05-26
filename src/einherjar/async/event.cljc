(ns einherjar.async.event
  (:require
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   #?@(:clj  [[clojure.spec.alpha :as spec]
              [clojure.core.async :as async]]
       :cljs [[cljs.spec.alpha :as spec]
              [cljs.core.async :as async]])))

;; ---- event dispatcher ----

(defrecord EventDispatcher [event-chan])

(defstate event-dispatcher
  :start (do (timbre/info "Starting event dispatcher...")
             (->EventDispatcher (async/chan 100)))
  :stop  (do (timbre/info "Stopping event dispatcher...")
             (async/close! (:event-chan @event-dispatcher))))

(defn dispatch!
  [{:keys [event-chan] :as event-dispatcher} event]
  (->> event
       (spec/assert ::command)
       (async/put! event-chan)))

;; ---- spec ----

(spec/def ::command
  (spec/nilable (spec/cat :command-id   encore/qualified-keyword?
                          :command-data (spec/? map?)
                          :command-meta (spec/? map?))))
