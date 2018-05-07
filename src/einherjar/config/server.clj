(ns einherjar.config.server
  (:require
   [clojure.spec.alpha :as spec]
   [mount.core :as mount :refer [defstate]]
   [aero.core :as aero]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]))

;; ---- config ----

(defn- read-config!
  [config-input]
  (let [{:keys [source option]} (spec/assert ::config-input config-input)]
    (assoc (aero/read-config source option) ::option option)))

(defstate config
  :start (do (timbre/info "Reading config...")
             (read-config! (mount/args))))

;; ---- spec ----

(spec/def ::source
  encore/nblank-str?)

(spec/def ::profile
  #{:dev})

(spec/def ::option
  (spec/keys :req-un [::profile]))

(spec/def ::config-input
  (spec/keys :req-un [::source ::option]))
