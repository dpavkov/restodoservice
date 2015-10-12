(ns restodoservice.user
  (:require [taoensso.carmine :as car]
            [restodoservice.util :as util]
            [postal.core :refer (send-message)]))


(defn send-verification-mail 
  "Given address to which email should be sent, sends the mail to the user which the information on 
   token and verification url. Token is generated in restodo application (register funct), while
   verification url is provided by client"
  [to token verification-url]
   (send-message 
     {:host (util/config :mail-host)
      :user (util/config :mail-username)
      :pass (util/config :mail-password)}
     {:from "me@example.com"
      :to to
      :subject "Email verification"
      :body (str "You have applied for todo application. Verify your email <a href=\"" verification-url token "\"> here!</a>")}))


(defn register 
  "Takes three params, email, password and verification mail. Creates user, sends verification mail, 
   and then returns craeted verification token."
  [email password verification-url]
  (let [verification-token (str (java.util.UUID/randomUUID))]
    (do 
      (util/wcar* (car/hmset* email { :password (util/pbkdf2 password) :verification-token verification-token } ))
      (send-verification-mail email verification-token verification-url)
      verification-token)))

(defn match-and-not-null 
  "Returns true if and only if refs are equal and not nil"
  [ref ref-2]
  (and (= ref ref-2) (not (nil? ref))))

(defn verify 
  "Takes email and token, checks if token is the same one as the one in db, and deletes it 
   if it maches, thus allowing user to login. Returns true if verification was successful, false otherwise"
  [email token]
  (let [should-verify (match-and-not-null 
                        token 
                        ((util/lookup-hash email) "verification-token"))]
      (do 
        (if should-verify (util/wcar* (car/hdel email :verification-token)))
        should-verify)))

(defn compare-with-hashed [ref hashed-ref]
  "hashes one of the refs and compares it with another"
  (match-and-not-null (util/pbkdf2 ref) hashed-ref))

(defn login 
  "performs login"
  [email password]
    (if (compare-with-hashed password ((util/lookup-hash email) "password"))
      (let [token (str (java.util.UUID/randomUUID))]
        (do 
          (util/wcar* (car/hmset* token {:email email :created (java.time.LocalDateTime/now)}))
          token))))

;; a variable that determines how many minutes an authorization token will last.
(def minutes-to-expire 20)

(defn expired-token? 
  "Determines if token was created minutes-to-expire minutes before now()"
  [token-created]
  (.isBefore 
    (.plusMinutes token-created minutes-to-expire)
    (java.time.LocalDateTime/now)))

(defn verify-token 
  "Receives token as a string. Verifies that token is valid and not expired. Returns a map containing user data:
  { \"email\" email \"password\" password \"todo-uuid\" todo-uuid }"
  [token]
  (if-let [token-data (util/lookup-hash token)]
    (if-not (expired-token? (token-data "created"))
      (let [email (token-data "email")] 
        (assoc (util/lookup-hash email) "email" email)))))


(defn get-or-create-todo-uuid 
  "For a given user, determines if he has todo-uuid. If he does, returns it, if not,
   creates a new one, stores it and returns it."
  [user]
  (if-let [todo-uuid (user "todo-uuid")] todo-uuid
    (let [todo-uuid (str (java.util.UUID/randomUUID))]
      (do
        (util/wcar* (car/hmset* (user "email") { "todo-uuid" todo-uuid }))
        todo-uuid))))


