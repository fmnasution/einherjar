(ns einherjar.dev
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.tools.namespace.repl :refer [refresh]]
   [mount.core :as mount]
   [einherjar.app]))

(defn toggle-assertion!
  []
  (if (spec/check-asserts?)
    (spec/check-asserts false)
    (spec/check-asserts true)))

(defn start!
  []
  (mount/start-with-args {:source "resources/private/einherjar/config.edn"
                          :option {:profile :dev}}))

(defn stop!
  []
  (mount/stop))

(defn restart!
  []
  (stop!)
  (refresh :after 'einherjar.dev/start!))