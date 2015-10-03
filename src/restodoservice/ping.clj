(ns restodoservice.ping
  (:require [taoensso.carmine :as car :refer (wcar)]
            [restodoservice.util :as util]))

(defn ping-redis []
  (last (util/wcar* (car/ping)
               (car/set "foo" "bar")
               (car/get "foo" ))))