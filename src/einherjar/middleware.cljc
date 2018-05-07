(ns einherjar.middleware)

(defn- as-middleware
  [entry]
  (if (vector? entry)
    #(apply (first entry) % (rest entry))
    entry))

(defn new-middleware
  [middlewares]
  (apply comp (into [] (map as-middleware) middlewares)))
