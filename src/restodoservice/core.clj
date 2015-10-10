(ns restodoservice.core
  (:require [ring.adapter.jetty :as jetty]
            [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [defroutes ANY]]
            [clojure.edn :as edn]
            [restodoservice.util :as util]
            [restodoservice.user :as user]
            [restodoservice.todo :as todo]
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
                                                                  (get-from-ctx ctx "verification-token"))}})))
  
  ;; Attempts login using email and password. returns authentication token.
  ;; If login fails, authentication token will be null. 200 is returned in either case.
  (ANY "/login" [] (resource :available-media-types ["application/json"]
                             :allowed-methods [:post]
                             :malformed? #(util/parse-json % ::data)
                             :handle-created #(json/write-str (% :success-query))
                             :post! (fn [ctx]
                                      {:success-query
                                       {:token 
                                        (user/login (get-from-ctx ctx "email") 
                                                    (get-from-ctx ctx "password"))}})))
  ;; Rest service for creating a new to-do. Must have authorization token in the header and it determines
  ;; a user for whom todo will be added. Body of the request must contain description of todo and a score,
  ;; which determines a priority by wich todo will be regarded.
  (ANY "/todos" [] (resource :available-media-types ["application/json"]
                             :allowed-methods [:post]
                             :malformed? #(util/parse-json % ::data)
                             :handle-created #(json/write-str (% ::data))
                             :authorized? (fn [ctx]                         
                                            (if-let [user (user/verify-token 
                                                            (get-in ctx [:request :headers "x-authorization"]))] 
                                              {::user user}))
                             :post! (fn [ctx] 
                                      (todo/add-todo 
                                        (ctx ::data) 
                                        (ctx ::user))))))

(def handler 
  (-> app 
      wrap-params))

(defn -main [& args]
  (jetty/run-jetty app {:port (util/config :app-port) :join? false}))
