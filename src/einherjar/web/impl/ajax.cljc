(ns einherjar.web.impl.ajax
  (:require
   [bidi.bidi :as router]
   [ajax.core :as ajax]
   [ajax.protocols]
   [taoensso.encore :as encore]
   #?@(:clj [[clojure.spec.alpha :as spec]]
       :cljs [[cljs.spec.alpha :as spec]])))

;; ---- server ajax caller ----

(defn request!
  ([routes handler route-params request-method option]
   (let [route-seq  (flatten (seq route-params))
         uri        (apply router/path-for routes handler route-seq)
         request-fn (case request-method
                      :get    ajax/GET
                      :post   ajax/POST
                      :put    ajax/PUT
                      :delete ajax/DELETE)]
     (request-fn uri option)))
  ([routes handler request-method option]
   (request! routes handler {} request-method option)))

;; ---- spec ----

(spec/def ::routes
  vector?)

(spec/def ::handler
  encore/qualified-keyword?)

(spec/def ::route-params
  (spec/nilable map?))

(spec/def ::request-method
  #{:get :post :put :delete})

(spec/def ::handler
  fn?)

#?(:cljs
   (spec/def ::progress-handler
     fn?))

(spec/def ::error-handler
  fn?)

(spec/def ::finally
  fn?)

(spec/def ::format
  #{:transit :json :text :raw :url})

(spec/def ::response-format
  #{:transit :json :text :ring :raw :detect})

(spec/def ::params
  map?)

(spec/def ::timeout-ms
  encore/pos-int?)

(spec/def ::headers
  (spec/map-of keyword? some?))

(spec/def ::with-credentials
  boolean?)

(spec/def ::body
  some?)

(spec/def ::interceptor
  #(satisfies? ajax.protocols/Interceptor %))

(spec/def ::interceptors
  (spec/coll-of ::interceptor))

(spec/def ::option
  (spec/keys :req-un [::handler
                      ::error-handler]
             :opt-un [#?(:cljs ::progress-handler)
                      ::finally
                      ::format
                      ::response-format
                      ::params
                      ::timeout-ms
                      ::headers
                      ::with-credentials
                      ::body
                      ::interceptors]))
