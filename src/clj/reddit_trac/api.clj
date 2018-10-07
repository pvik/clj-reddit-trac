(ns reddit-trac.api
  (:require [digest :refer [sha-1]]
            [taoensso.timbre     :as log]
            [reddit-trac.data.db :as db])
  (:gen-class))

(defonce ^:private ^:const secret
  (:secret (clojure.edn/read-string (slurp "resources/config.edn"))))

(defn gen-token
  "Generate SHA-1 of data appended with secret
  and get the last 10 characters"
  [data]
  (subs (sha-1 (str data secret)) 30))

(defn wrap-response [body & [status error?]]
  {:status (or status 200)
   :headers {"Content-Type" "text/json"}
   :body (if error? {:error body} body)})

(defn create-watch
  "Create watch in DB (active=false) and
  TODO send email to validate watch"
  [data]
  (log/debug "create watch" data)
  (wrap-response
   ;; TODO: lower all string columns
   (db/create-entity :watch-subreddit (assoc data :active false))))

(defn validate-watch [id email token]
  (log/debug "validate watch" id email token)
  (if (= token (gen-token email))
    (do
      (db/update-entity :watch-subreddit
                        {:id id
                         :active true})
      (wrap-response {:message "ok"}))
    (wrap-response "invalid token" 401 :error)))

(defn get-watch [type qual token]
  (log/debug "getting watch" type qual token)
  (cond
    (= type :id)
    (let [watch (db/get-entity :watch-subreddit [:= :id qual])
          email ((comp :email first) watch)]
      (if (= token (gen-token email))
        (wrap-response watch)
        (wrap-response "invalid token" 401 :error)))
    
    (= type :email)
    (do
      (if (= token (gen-token qual))
        (wrap-response
         (db/get-entity :watch-subreddit [:= :email qual]))
        (wrap-response "invalid token" 401 :error)))))

(defn delete-watch [id email token]
  (log/debug "delete watch" id email token)
  (if (= token (gen-token email))
    (do
      (db/delete-entity :watch-subreddit id)
      (wrap-response {:message "ok"}))
    (wrap-response "invalid token" 401 :error)))

