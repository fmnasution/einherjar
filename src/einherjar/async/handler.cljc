(ns einherjar.async.handler
  (:require
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.async.effect :as asnc.efc]
   [einherjar.async.event :as asnc.evt]))

;; ---- event handler ----

(asnc.efc/reg-event
 :default
 (fn [_ event]
   [[:event-consumer/unknown {:event event} {:error? true}]]))

(asnc.efc/reg-event
 :event-consumer/unknown
 (fn [_ [_ {:keys [event]}]]
   (let [[id] event]
     [[:logger/warn {:error (ex-info "Unknown event" {:id id})}]])))

(asnc.efc/reg-event
 :effect-executor/unknown
 (fn [_ [_ {:keys [effect]}]]
   (let [[id] effect]
     [[:logger/warn {:error (ex-info "Unknown effect" {:id id})}]])))

(asnc.efc/reg-event
 :event-consumer/error
 (fn [_ [_ data]]
   [[:logger/error data]]))

(asnc.efc/reg-event
 :effect-executor/error
 (fn [_ [_ data]]
   [[:logger/error data]]))

;; ---- effect handler ----

(asnc.efc/reg-effect
 :default
 (fn [_ effect]
   [:effect-executor/unknown {:effect effect} {:error? true}]))

(asnc.efc/reg-effect
 :event-dispatcher/dispatch
 (fn [{:keys [event-dispatcher]} [_ {:keys [event]}]]
   (asnc.evt/dispatch! event-dispatcher event)))

(asnc.efc/reg-effect
 :logger/info
 (fn [_ [_ {:keys [error data]}]]
   (encore/cond
     (and (some? error) (some? data))
     (timbre/info error "\n"
                  "Data:" data)

     (some? error)
     (timbre/info error))))

(asnc.efc/reg-effect
 :logger/warn
 (fn [_ [_ {:keys [error data]}]]
   (encore/cond
     (and (some? error) (some? data))
     (timbre/warn error "\n"
                  "Data:" data)

     (some? error)
     (timbre/warn error))))

(asnc.efc/reg-effect
 :logger/error
 (fn [_ [_ {:keys [error data]}]]
   (encore/cond
     (and (some? error) (some? data))
     (timbre/error error "\n"
                   "Data:" data)

     (some? error)
     (timbre/error error))))
