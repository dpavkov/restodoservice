(ns restodoservice.core
  (:require [ring.adapter.jetty :as jetty]
            [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [defroutes ANY]]
            [clojure.edn :as edn]
            [restodoservice.ping :refer [ping]]
            [restodoservice.util :as util])
  (:gen-class))

(defroutes app
  (ANY "/foo" [] (resource :available-media-types ["text/json"]
                           :handle-ok (str "{ \"foo\" : \""  (ping)  "\" }"))))

(def handler 
  (-> app 
      wrap-params))

(defn -main [& args]
  (jetty/run-jetty app {:port (util/config :app-port) :join? false}))
