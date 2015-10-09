(ns restodoservice.user
  (:require [taoensso.carmine :as car]
            [restodoservice.util :as util]
            [postal.core :refer (send-message)]))

;; Given address to which email should be sent, sends the mail to the user which the information on 
;; token and verification url. Token is generated in restodo application (register funct), while
;; verification url is provided by client
(defn send-verification-mail [to token verification-url]
   (send-message 
     {:host (util/config :mail-host)
      :user (util/config :mail-username)
      :pass (util/config :mail-password)}
     {:from "me@example.com"
      :to to
      :subject "Email verification"
      :body (str "You have applied for todo application. Verify your email <a href=\"" verification-url token "\"> here!</a>")}))

;; Takes three params, email, password and verification mail. Creates user, sends verification mail, 
;;and then returns craeted verification token.
(defn register [email password verification-url]
  (let [verification-token (str (java.util.UUID/randomUUID))]
    (do 
      (util/wcar* (car/hmset* email { :password (util/pbkdf2 password) :verification-token verification-token } ))
      (send-verification-mail email verification-token verification-url)
      verification-token)))

;; Returns true if and only if refs are equal and not nil
(defn match-and-not-null [ref ref-2]
  (and (= ref ref-2) (not (nil? ref))))

;; Takes email and token, checks if token is the same one as the one in db, and deletes it 
;; if it maches, thus allowing user to login. Returns true if verification was successful, false otherwise
(defn verify [email token]
  (let [should-verify (match-and-not-null 
                        token 
                        ((util/lookup-hash email) "verification-token"))]
      (do 
        (if should-verify (util/wcar* (car/hdel email :verification-token)))
        should-verify)))

;; hashes one of the refs and compares it with another
(defn compare-with-hashed [ref hashed-ref]
  (match-and-not-null (util/pbkdf2 ref) hashed-ref))

;; performs login
(defn login [email password]
    (if (compare-with-hashed password ((util/lookup-hash email) "password"))
      (let [token (str (java.util.UUID/randomUUID))]
        (do 
          (util/wcar* (car/hmset* token {:email email :created (java.util.Date.)}))
          token))))

(defn get-or-create-todo-uuid [user]
  (if-let [todo-uuid (user "todo-uuid")] todo-uuid
    (let [todo-uuid (str (java.util.UUID/randomUUID))]
      (do
        (util/wcar* (car/hmset* (user "email") { :todo-uuid todo-uuid }))
        todo-uuid)))) 


