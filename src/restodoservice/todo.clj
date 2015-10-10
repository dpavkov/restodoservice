(ns restodoservice.todo
  (:require [taoensso.carmine :as car]
            [restodoservice.util :as util]
            [restodoservice.user :as user]))

;; saves todo in a sorted set
(defn add-todo [to-do user] 
  (let [todo-uuid (user/get-or-create-todo-uuid user)]
    (do (println todo-uuid) 
      (util/wcar* (car/zadd todo-uuid (to-do "score") (to-do "description"))))))

;; transforms array to the map, where odd members are keys and even members are values
;; e.g. ['123' 123 '456' 345] to {'123' 123, '456' 345}
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

;; given user from the map, reads to-do with the highest priority and returns it.
(defn read-first-todo [user]
  (let [todo-uuid (user/get-or-create-todo-uuid user)]
    (zip-map-to-array (util/wcar* (car/zrange todo-uuid "0" "0" :withscores)))))

;; deletes todo with the highest priority
(defn delete [user]
    (let [todo-uuid (user/get-or-create-todo-uuid user)]
          (util/wcar* (car/zremrangebyrank todo-uuid 0 0))))
