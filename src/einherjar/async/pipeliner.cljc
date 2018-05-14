(ns einherjar.async.pipeliner
  (:require
   [taoensso.encore :as encore]
   [einherjar.async.protocols :as asnc.prt]
   #?@(:clj [[clojure.spec.alpha :as spec]
             [clojure.core.async :as async]]
       :cljs [[cljs.core.async :as async]
              [cljs.spec.alpha :as spec]])))

;; ---- pipeliner creator ----

(defn create-pipeliner
  [{:keys [pipeline-fn parallelism flow-to xform-fn flow-from error-handler]
    :as   pipeliner-input}
   & args]
  (spec/assert ::pipeliner-input pipeliner-input)
  (let [pipeline-fn (or pipeline-fn async/pipeline)
        parallelism (or parallelism 1)
        sink-chan   (asnc.prt/sink-chan flow-to)
        xform       (apply xform-fn args)
        source-chan (asnc.prt/source-chan flow-from)]
    (pipeline-fn parallelism sink-chan xform source-chan false error-handler)))

;; ---- spec ----

(spec/def ::flow-to
  #(satisfies? asnc.prt/ISink %))

(spec/def ::xform-fn
  fn?)

(spec/def ::flow-from
  #(satisfies? asnc.prt/ISource %))

(spec/def ::pipeline-fn
  #{async/pipeline
    async/pipeline-async
    (encore/if-clj async/pipeline-blocking)})

(spec/def ::parallelism
  encore/pos-int?)

(spec/def ::error-handler
  fn?)

(spec/def ::pipeliner-input
  (spec/keys :req-un [::flow-to ::xform-fn ::flow-from ::error-handler]
             :opt-un [::pipeline-fn ::parallelism]))
