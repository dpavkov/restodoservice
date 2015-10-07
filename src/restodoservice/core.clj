(ns restodoservice.core
  (:require [ring.adapter.jetty :as jetty]
            [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [defroutes ANY]]
            [clojure.edn :as edn]
            [restodoservice.util :as util]
            [restodoservice.user :as user]
            [clojure.data.json :as json])
  (:gen-class))

(defn get-from-ctx [ctx param]
  (get-in ctx [::data param]))

(defroutes app
  ;; handles registering users. POST method. Expects the body of the request to be filled with 3
  ;; params: email, password and verification url. Creates an entry in db and sends a mail to the user for verification
  ;; verification token will be appended to the verification url in the mail. Client application is expected to 
  ;; redirect user verification to PATCH /users/:email with verification-token value in the patch body
  (ANY "/users" [] (resource :available-media-types ["application/json"]
                             :allowed-methods [:post]
                             :malformed? #(util/parse-json % ::data)
                             :handle-created #(json/write-str (% :entry))
                             :post! (fn [ctx] 
                                      (let [verification-token (user/register 
                                                                 (get-from-ctx ctx "email") 
                                                                 (get-from-ctx ctx "password") 
                                                                 (get-from-ctx ctx "verification-url"))]
                                        { :entry {:email (get-from-ctx ctx "email") 
                                                   :password (get-from-ctx ctx "password") 
                                                   :verification-url (str (get-from-ctx ctx "verification-url") verification-token)}}))))
  
  ;; Verifies user email, thus finishing registration process
  (ANY "/users/:email" [email] (resource :available-media-types ["application/json"]
                                                      :allowed-methods [:patch]
                                                      :malformed? #(util/parse-json % ::data)
                                                      :exists? (fn [ctx] 
                                                                 (let [entry (util/lookup-hash email)] 
                                                                 (if-not (empty? entry) 
                                                                   {::entry entry})))
                                                      :handle-ok #(json/write-str (% :success-query))
                                                      :respond-with-entity? true
                                                      :patch! (fn [ctx] 
                                                                {:success-query
                                                                 {:verification-success 
                                                                  (user/verify email 
                                                                               (get-from-ctx ctx "verification-token"))}}))))

(def handler 
  (-> app 
      wrap-params))

(defn -main [& args]
  (jetty/run-jetty app {:port (util/config :app-port) :join? false}))
