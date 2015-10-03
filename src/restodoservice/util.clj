(ns restodoservice.util
  (:require [clojure.edn :as edn]
            [taoensso.carmine :as car :refer (wcar)]))

(def config (edn/read-string (slurp "config.edn")))

(def server-conn {:pool {} :spec {:host (config :redis-host) :port (config :redis-port) } }) ; See `wcar` docstring for opts
(defmacro wcar* [& body] `(car/wcar server-conn ~@body))