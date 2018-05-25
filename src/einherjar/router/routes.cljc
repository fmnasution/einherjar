(ns einherjar.router.routes)

(defn server-routes
  []
  ["" {"/login"            ::login
       "/logout"           ::logout
       "/chsk"             ::websocket
       "/resources/public" {true ::asset}
       "/"                 ::index}])

(defn client-routes
  []
  ["" {"/" ::index}])
