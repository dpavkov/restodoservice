(ns restodoservice.core
  (:require [ring.adapter.jetty :as jetty]
            [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [defroutes ANY POST GET DELETE]]
            [clojure.edn :as edn]
            [restodoservice.util :as util]
            [restodoservice.user :as user]
            [restodoservice.todo :as todo]
            [clojure.data.json :as json])
  (:gen-class))

;; retrieves the request param
(defn- get-from-ctx [ctx field]
  (get-in ctx [::data field]))

;; retrieves x-authorization header and calls verify-token method to perform authorization
(defn- authorize [ctx]
  (if-let [user (user/verify-token 
                  (get-in ctx [:request :headers "x-authorization"]))] 
    {::user user}))

;; retrieves user from ctx, reads the first todo and formats it. refactored for reuse
(defn- read-first-todo [ctx]  
  (json/write-str
     (todo/read-first-todo (ctx ::user))))

(defroutes app
  "Defines all handlers"
  ;; handles registering users. Expects the body of the request to be filled with 3
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
  ;; which determines a priority by wich todo will be regarded. Lower score, lower priority
  (POST "/todos" [] (resource :available-media-types ["application/json"]
                             :allowed-methods [:post]
                             :malformed? #(util/parse-json % ::data)
                             :handle-created #(json/write-str (% ::data))
                             :authorized? #(authorize %)
                             :post! (fn [ctx] 
                                      (todo/add-todo 
                                        (ctx ::data) 
                                        (ctx ::user)))))
  ;; retrieves all todos for a user starting with score 0 and ending with score provided as query param.
  ;; returns json in a form description : score. e.g. 
  ;; {"do something" : 5, "do something less important" : 10}
  (GET "/todos" [] (resource :available-media-types ["application/json"]
                             :allowed-methods [:get]
                             :authorized? #(authorize %)
                             :handle-ok (fn [ctx] 
                                          (json/write-str
                                            (todo/read-todos (ctx ::user) 
                                                             ((util/parse-query-string (get-in ctx [:request :query-string])) "max-score"))))))
  ;; retrieves todo with highest priority
  (ANY "/todos/first" [] (resource :available-media-types ["application/json"]
                                   :allowed-methods [:get]
                                   :authorized? #(authorize %)
                                   :handle-ok #(read-first-todo %)))
  ;; Deletes todo with the highest priority and returns todo with the next highest priority
  (DELETE "/todos" [] (resource :available-media-types ["application/json"]
                               :allowed-methods [:delete]
                               :authorized? #(authorize %)
                               :handle-ok #(read-first-todo %)
                               :respond-with-entity? true
                               :delete! (fn [ctx] (todo/delete (ctx ::user))))))

(def handler 
  (-> app 
      wrap-params))

(defn -main [& args]
  (jetty/run-jetty app {:port (util/config :app-port) :join? false}))
