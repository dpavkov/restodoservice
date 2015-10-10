(ns restodoservice.todo
   (:require [taoensso.carmine :as car]
             [restodoservice.util :as util]
             [restodoservice.user :as user]))
   
;; saves todo in a sorted set
(defn add-todo [to-do user] 
  (let [todo-uuid (user/get-or-create-todo-uuid user)]
    (util/wcar* (car/zadd todo-uuid (to-do "score") (to-do "description")))))
