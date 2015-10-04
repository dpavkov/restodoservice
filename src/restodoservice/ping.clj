(ns restodoservice.ping
  (:require [taoensso.carmine :as car :refer (wcar)]
            [restodoservice.util :as util]
            [ postal.core :as postal]))

(defn send-ping-mail []
   (postal/send-message 
     {:host "mailtrap.io"
      :user "41996d833654ec47"
      :pass "5d3ebb8ff3cabd"}
     {:from "me@example.com"
      :to "you@example.com"
      :subject "Hi!"
      :body "Test."}))

(defn ping-redis []
  (last (util/wcar* (car/ping)
               (car/set "foo" "bar")
               (car/get "foo" ))))

(defn ping []
   (do 
     (send-ping-mail)
     (ping-redis)))