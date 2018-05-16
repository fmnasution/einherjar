(ns einherjar.web.handler
  (:require
   [einherjar.async.effect :as asnc.efc]
   #?@(:cljs [[einherjar.web.ajax.client :as wb.jx.clt]])))

;; ---- effect handler ----

#?(:cljs
   (asnc.efc/reg-effect
    :server-ajax-caller/request
    (fn [{:keys [server-ajax-caller]}
        [_ {:keys [handler route-params request-method option]
            :or   {route-params   {}
                   request-method :get}}]]
      (wb.jx.clt/request! server-ajax-caller
                          handler
                          route-params
                          request-method
                          option))))
