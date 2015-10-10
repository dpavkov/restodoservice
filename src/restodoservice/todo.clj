(ns restodoservice.todo
  (:require [taoensso.carmine :as car]
            [restodoservice.util :as util]
            [restodoservice.user :as user]))

;; saves todo in a sorted set
(defn add-todo [to-do user] 
  (let [todo-uuid (user/get-or-create-todo-uuid user)]
    (do (println todo-uuid) 
      (util/wcar* (car/zadd todo-uuid (to-do "score") (to-do "description"))))))

(defn zip-map-to-array [array]
  (let [keys (take-nth 2 array)
        vals (take-nth 2 (rest array))]
    (zipmap keys vals)))

;; retrieves all todos for a user starting with score 0 and ending with score provided.
;; returns a map in a form description : score. e.g. 
;; {"do something" 5 "do something less important" 10}
(defn read-todos [user max]
  (let [todo-uuid (user/get-or-create-todo-uuid user)]
        (zip-map-to-array  (util/wcar* (car/zrangebyscore todo-uuid 0 max :withscores)))))

(defn read-first-todo [user]
  (let [todo-uuid (user/get-or-create-todo-uuid user)
        result (util/wcar* (car/zrange todo-uuid "0" "1" :withscores))]
    {(first result) (second result)}))
