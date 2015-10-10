(ns restodoservice.test_user
  (:use clojure.test)
  (:require [restodoservice.user :as user]))

(deftest test-match-and-not-null
  (is (user/match-and-not-null "asd" "asd"))
  (is (not (user/match-and-not-null "asd" "asdf")))
  (is (not (user/match-and-not-null "asd" nil)))
  (is (not (user/match-and-not-null nil "asdf")))
  (is (not (user/match-and-not-null nil nil))))

(deftest test-compare-with-hashed
  (is (user/compare-with-hashed "1234" "9f607ec178c77601fdb25afb0785374c395a6103a8d93b5"))
  (is (not (user/compare-with-hashed "9f607ec178c77601fdb25afb0785374c395a6103a8d93b5" "9f607ec178c77601fdb25afb0785374c395a6103a8d93b5"))))

(deftest test-expired-token?
  (is (user/expired-token?  (.plusMinutes (java.time.LocalDateTime/now) -21)))
  (is (not (user/expired-token?  (.plusMinutes (java.time.LocalDateTime/now) -19)))))