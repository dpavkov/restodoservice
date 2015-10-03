(ns restodoservice.core
  (:require [ring.adapter.jetty :as jetty]
            [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [defroutes ANY]]
            [taoensso.carmine :as car :refer (wcar)]
            [clojure.edn :as edn])
  (:gen-class))

(def config (edn/read-string (slurp "config.edn")))

(def ^:const port (config :app-port))

(def server-conn {:pool {} :spec {:host (config :redis-host) :port (config :redis-port) } }) ; See `wcar` docstring for opts
(defmacro wcar* [& body] `(car/wcar server-conn ~@body))

(defroutes app
  (ANY "/foo" [] (resource :available-media-types ["text/json"]
                           :handle-ok (str "{ \"foo\" : \""  (last (wcar* (car/ping)
                                             (car/set "foo" "bar")
                                             (car/get "foo" )))  "\" }"))))

(def handler 
  (-> app 
      wrap-params))

(defn -main [& args]
  (jetty/run-jetty app {:port port :join? false}))
