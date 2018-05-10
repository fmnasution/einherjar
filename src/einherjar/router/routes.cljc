(ns einherjar.router.routes)

(defn server-routes
  []
  ["" {"/chsk"             ::websocket
       "/resources/public" {true ::asset}
       "/"                 ::index}])

(defn client-routes
  []
  ["" {"/" ::index}])
