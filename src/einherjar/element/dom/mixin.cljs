(ns einherjar.element.dom.mixin
  (:require
   [cljs.spec.alpha :as spec]
   [com.rpl.specter :as specter :include-macros true]
   [rum.core :as rum]
   [taoensso.encore :as encore]))

;; ---- form ----

(def ^:private LEAF
  (specter/recursive-path []
                          p
                          [(specter/cond-path
                            map?
                            [specter/MAP-VALS p]

                            coll?
                            [specter/ALL p]

                            any?
                            [specter/STAY])]))

(defn- generate-touched
  [data check-touched?]
  (specter/setval [LEAF] check-touched? data))

(defn- generate-error
  [data]
  (specter/setval [LEAF] "" data))

(defn- validation->validator
  [validation]
  (let [pred-fn (fn [pred]
                  (encore/cond
                    (keyword? pred)
                    #(spec/valid? pred %)

                    (fn? pred)
                    pred))
        message-fn (fn [message]
                     (encore/cond
                       (string? message)
                       (constantly message)

                       (fn? message)
                       message))
        validation (->> validation
                        (spec/assert ::validation)
                        (specter/multi-transform
                         [specter/ALL
                          (specter/multi-path
                           [specter/FIRST
                            (specter/terminal pred-fn)]
                           [specter/LAST
                            (specter/terminal message-fn)])]))]
    (fn [v]
      (if-let [result (->> validation
                           (keep (fn [[pred message]]
                                   (when-not (pred v)
                                     (message v))))
                           (first))]
        result
        ""))))

(def ^:private MAP-LEAF
  (specter/recursive-path []
                          p
                          [(specter/cond-path
                            map?
                            [specter/MAP-VALS p]

                            any?
                            [specter/STAY])]))

(defn- generate-validator
  [data]
  (specter/transform [MAP-LEAF] validation->validator data))

(defn- disable-button?
  [touched error]
  (or (true? (::first? touched))
      (->> touched
           (specter/setval [::first?] specter/NONE)
           (specter/select-first [LEAF false?])
           (some?))
      (->> error
           (specter/select-first [LEAF encore/nblank-str?])
           (some?))))

(defn form
  [{:keys [fields validator initial check-touched? on-submit]
    :or   {initial        {}
           check-touched? false}}]
  (let [data_        (atom initial)
        touched-init (assoc (generate-touched initial check-touched?)
                            ::first? (not check-touched?))
        touched_     (atom touched-init)
        error-init   (generate-error initial)
        error_       (atom error-init)
        validator    (generate-validator validator)]
    {:will-mount (fn [{component :rum/react-component :as state}]
                   (let [watch-fn       (fn [_ _ old-state next-state]
                                          (when (not= old-state next-state)
                                            (rum/request-render component)))
                         new-updater-fn (fn [state_]
                                          (fn [& args]
                                            (apply swap! state_ args)))]
                     (add-watch data_ ::form-data watch-fn)
                     (add-watch error_ ::form-error watch-fn)
                     (add-watch touched_ ::form-touched watch-fn)
                     (let [form-option {:fields fields

                                        :update-data
                                        (new-updater-fn data_)

                                        :update-touched
                                        (new-updater-fn touched_)

                                        :update-error
                                        (new-updater-fn error_)}]
                       (assoc state ::form form-option))))
     :will-unmount (fn [state]
                     (remove-watch data_ ::form-data)
                     (remove-watch error_ ::form-error)
                     (remove-watch touched_ ::form-touched)
                     (reset! data_ initial)
                     (reset! touched_ touched-init)
                     (reset! error_ error-init)
                     (assoc state ::form {}))
     :wrap-render  (fn [render-fn]
                     (fn [{[manager] :rum/args :as state}]
                       (let [data  {:validator      validator
                                    :on-submit      #(encore/do-nil
                                                      (on-submit manager @data_))
                                    :disable-button #(disable-button? @touched_
                                                                      @error_)
                                    :data           @data_
                                    :touched        @touched_
                                    :error          @error_}
                             state (update state ::form encore/merge data)]
                         (render-fn state))))}))

;; ---- spec ----

(spec/def ::validation
  (spec/cat :validation
            (spec/+ (spec/tuple (spec/or :spec encore/qualified-keyword?
                                         :pred fn?)
                                (spec/or :message     encore/nblank-str?
                                         :constructor fn?)))))

