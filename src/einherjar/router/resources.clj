(ns einherjar.router.resources
  (:require
   [clojure.java.io :as io]
   [ring.util.http-response :as ring.response]
   [bidi.bidi :as router]
   [rum.core :as rum :refer [defc]]
   [taoensso.encore :as encore]
   [einherjar.middleware :as mdw]))

;; ---- index resource ----

(defn- asset-path
  [{:keys [einherjar.router.server/ring-router]} path]
  (let [routes (router/routes ring-router)]
    (str (router/path-for routes :einherjar.router.routes/asset) "/" path)))

(defc include-css
  [path]
  [:link {:type "text/css"
          :rel  "stylesheet"
          :href path}])

(defc include-js
  [path]
  [:script {:type "text/javascript"
            :src  path}])

(defc index-template
  [request]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name    "viewport"
            :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
    (include-css "https://fonts.googleapis.com/css?family=Roboto:300,400,500")
    (include-css "https://fonts.googleapis.com/icon?family=Material+Icons")
    [:title "einherjar"]]
   [:body
    [:div#app]
    (include-js (asset-path request "einherjar/app.js"))]])

(defn index-resource
  [{:keys [request-method] :as request}]
  (if (not= :get request-method)
    (ring.response/content-type
     (ring.response/method-not-allowed)
     "text/plain")
    (-> (rum/render-html (index-template request))
        (ring.response/ok)
        (ring.response/content-type "text/html"))))

;; ---- asset resource ----

(defn asset-resource
  [{:keys [request-method uri] :as request}]
  (encore/cond
    (not= :get request-method)
    (ring.response/content-type
     (ring.response/method-not-allowed)
     "text/plain")

    :let [file (io/file (subs uri 1))]

    (and (.exists file) (.isFile file))
    (ring.response/ok file)

    :else (ring.response/not-found)))
