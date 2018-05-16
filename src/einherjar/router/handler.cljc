(ns einherjar.router.handler
  (:require
   [einherjar.async.effect :as asnc.efc]))

;; ---- event handler ----

#?(:cljs
   (asnc.efc/reg-event
    :html-router/error
    (fn [_ [_ data]]
      [[:logger/error data]])))
