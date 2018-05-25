(ns einherjar.element.dom.mixin
  (:require
   [cljs.spec.alpha :as spec]
   [com.rpl.specter :as specter :include-macros true]
   [rum.core :as rum]
   [taoensso.encore :as encore]))

;; ---- form ----

(def ^:private LEAF-PATH
  (specter/recursive-path []
                          p
                          [(specter/cond-path
                            map?
                            [specter/MAP-VALS p]

                            vector?
                            [specter/ALL p]

                            any?
                            [specter/STAY])]))

(defn- generate-touched
  [data check-touched?]
  (specter/setval LEAF-PATH check-touched? data))

(defn- generate-error
  [data]
  (specter/setval LEAF-PATH "" data))

(defn- validation->validator
  [entries]
  (fn [v]
    (let [result (->> entries
                      (map (fn [[x y]]
                             (let [validate-fn (encore/cond!
                                                (keyword? x)
                                                #(spec/valid? x %)

                                                (fn? x)
                                                x)
                                   ok? (validate-fn v)]
                               (when-not ok?
                                 (let [message-fn (encore/cond!
                                                   (string? y)
                                                   (constantly y)

                                                   (fn? y)
                                                   y)]
                                   (message-fn v))))))
                      (filter some?)
                      (first))]
      (if (some? result)
        result
        ""))))

(defn- generate-validator
  [data]
  (specter/transform [(specter/recursive-path []
                                              p
                                              [(specter/cond-path
                                                map?
                                                [specter/MAP-VALS p]

                                                any?
                                                specter/STAY)])]
                     validation->validator
                     data))

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
    {:will-mount   (fn [{component :rum/react-component :as state}]
                     (let [watch-fn       (fn [_ _ old-state next-state]
                                            (when (not= old-state next-state)
                                              (rum/request-render component)))
                           new-updater-fn (fn [state_]
                                            (fn [& args]
                                              (apply swap! state_ args)))]
                       (add-watch data_ ::form-data watch-fn)
                       (add-watch error_ ::form-error watch-fn)
                       (add-watch touched_ ::form-touched watch-fn)
                       (assoc state
                              ::form {:update-data    (new-updater-fn data_)
                                      :update-touched (new-updater-fn touched_)
                                      :update-error   (new-updater-fn error_)})))
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
                       (let [data  {:fields    fields
                                    :validator validator
                                    :on-submit #(encore/do-nil
                                                 (on-submit manager @data_))
                                    :data      @data_
                                    :touched   @touched_
                                    :error     @error_}
                             state (update state ::form encore/merge data)]
                         (render-fn state))))}))
