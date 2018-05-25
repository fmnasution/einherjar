(ns einherjar.web.handler
  (:require
   [einherjar.async.effect :as asnc.efc]
   [einherjar.router.routes :as rtr.rts]
   #?@(:cljs [[einherjar.web.ajax.client :as wb.jx.clt]])))

;; ---- event handler ----

#?(:cljs
   (asnc.efc/reg-event
    :server-ajax-caller/request-authentication
    (fn [_ [_ {:keys [credential]}]]
      (let [event       [:server-ajax-caller/authentication-success]
            error-event [:server-ajax-caller/authentication-fail]
            option      {:event       event
                         :error-event error-event
                         :params      credential}]
        [[:server-ajax-caller/request {:handler        ::rtr.rts/login
                                       :request-method :post
                                       :option         option}]]))))

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
