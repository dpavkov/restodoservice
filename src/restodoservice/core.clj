(ns restodoservice.core
  (:require [ring.adapter.jetty :as jetty]
            [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [defroutes ANY]])
  (:gen-class))

(def ^:const port 8080)

(defroutes app
  (ANY "/foo" [] (resource :available-media-types ["text/json"]
                           :handle-ok "{\"hello\" : \"world\"}")))

(def handler 
  (-> app 
      wrap-params))

(defn -main [& args]
  (jetty/run-jetty app {:port port :join? false}))
