(ns einherjar.async.handler
  (:require
   [taoensso.timbre :as timbre]
   [einherjar.async.effect :as asnc.efc]))

;; ---- event handler ----

(asnc.efc/reg-event
 :default
 (fn [_ [id]]
   (timbre/warn "Unknown event:" id)))

;; ---- effect handler ----

(asnc.efc/reg-effect
 :default
 (fn [_ [id]]
   (timbre/warn "Unknown effect:" id)))
