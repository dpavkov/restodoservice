(ns restodoservice.todo
   (:require [taoensso.carmine :as car]
             [restodoservice.util :as util]
             [restodoservice.user :as user]))
   
(defn add-todo [to-do user] 
  (let [todo-uuid (user/get-or-create-todo-uuid user)]
    (util/wcar* (car/zadd todo-uuid (to-do "due-date") (to-do "description")))))
