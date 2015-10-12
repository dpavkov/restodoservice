(ns restodoservice.util
  (:require [clojure.edn :as edn]
            [taoensso.carmine :as car :refer (wcar)]
            [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:import
    java.security.SecureRandom
    javax.crypto.SecretKeyFactory
    javax.crypto.spec.PBEKeySpec
    org.eclipse.jetty.util.UrlEncoded
    org.eclipse.jetty.util.MultiMap))

(defn parse-query-string 
  "Parses query string"
  [query]
  (let [params (MultiMap.)]
    (UrlEncoded/decodeTo query params "UTF-8")
    (into {} params)))


;; Reads file properties. Props are stored as maps, so they can be accessed as (config :key)
(def config (edn/read-string (slurp "config.edn")))

;; Defines redis connection params.
(def server-conn {:pool {} :spec {:host (config :redis-host) :port (config :redis-port) } }) ; See `wcar` docstring for opts

(defmacro wcar* 
  "Wraps redis command with the server connection params. Can chain multiple calls to redis. If that is the case, return type
   will be an array consisting of all results, otherwise returns a single result."
  [& body] `(car/wcar server-conn ~@body))
;; Simple util function, for each hash field retrieves all fields. Nil if there's no hash
(defn lookup-hash [identifier]
  (if identifier
    (let [entity (wcar* (car/hgetall* identifier))]
      (if (empty? entity) nil entity))))


;; 
(defn pbkdf2
  "Get a hash for the given string
   Hashing algorithm, courtesy of http://adambard.com/blog/3-wrong-ways-to-store-a-password/"
  ([x]
    (let [k (PBEKeySpec. (.toCharArray x) (.getBytes (config :salt)) 1000 192)
          f (SecretKeyFactory/getInstance "PBKDF2WithHmacSHA1")]
      (format "%x"
              (java.math.BigInteger. (.getEncoded (.generateSecret f k)))))))


(defn body-as-string 
  "Courtesy of http://clojure-liberator.github.io/liberator/tutorial/all-together.html
   convert the body to a reader. Useful for testing in the repl
   where setting the body to a string is much simpler."
  [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (condp instance? body
      java.lang.String body
      (slurp (io/reader body)))))


(defn parse-json 
  "For PUT and POST parse the body as json and store in the context
   under the given key."
  [ctx key]
  (when (#{:put :post :patch} (get-in ctx [:request :request-method]))
    (try
      (if-let [body (body-as-string ctx)]
        (let [data (json/read-str body)]
          [false {key data}])
        {:message "No body"})
      (catch Exception e
        (.printStackTrace e)
        {:message (format "IOException: %s" (.getMessage e))}))))
