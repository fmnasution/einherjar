(ns einherjar.app
  (:require
   [mount.core :as mount]
   [taoensso.encore :as encore]))

(mount/in-cljc-mode)

(encore/if-cljs
 (enable-console-print!))

(encore/if-cljs
 (mount/start))
