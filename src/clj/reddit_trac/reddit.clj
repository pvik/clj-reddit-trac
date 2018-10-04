(ns reddit-trac.reddit
  (:require [clj-http.client    :as client]
            [clojure.data.json  :as json]
            [clj-time.core      :as t]
            [clj-time.coerce    :as tc]
            [taoensso.timbre    :as log]
            [reddit-trac.helper :as h]))

(defonce ^:private ^:const props
  (:reddit (clojure.edn/read-string (slurp "resources/config.edn"))))

(defonce ^:private ^:const device-id
  "clj-reddit-trac-v1-webapp")

(defonce ^:private ^:const user-agent
  (str "clojure:org.pvik.reddit-trac:v1 (by /u/" (:user props) ")"))

(defonce ^:private ^:const default-limit 50)

(defonce ^:private token (atom {}))

(defn epoch-now
  "Unix epoch in second"
  []
  (tc/to-long (t/now)))

(defn get-access-token []
  (let [request (client/post
                 "https://www.reddit.com/api/v1/access_token"
                 {:basic-auth [(:client-id props) (:secret props)]
                  :headers     {"User-Agent" user-agent}
                  :form-params {:grant_type "https://oauth.reddit.com/grants/installed_client"
                                :username   (:user props)
                                :password   (:password props)
                                :device_id  device-id}
                  :accept :json})
        body    (json/read-str (:body request) :key-fn keyword)]
    (log/debug "body: " body)
    (if (contains? body :error)
      (throw (ex-info (str "unable to retrieve access token from reddit: "
                           (:error body))
                      {:causes #{:reddit-auth}})) 
      {:access_token (:access_token body)
       :token_type (:token_type body)
       :expires_in (+ (epoch-now) (* (:expires_in body) 1000))})))

(defn access-token []
  (if (and @token (< (epoch-now) (or (:expires_in @token) 0)))
    ;; token atom exists and isn't expired
    (:access_token @token)
    ;; token doesn't exist or is expired
    (let [a-token (get-access-token)]
      (swap! token conj a-token)
      (:access_token a-token))))

(defn get-subreddit-posts [subreddit type &
                           [{:keys [limit data-keys before after]}]]
  (log/debug "get posts" subreddit " - " type "; limit" limit "after:" after "before:" before)
  (let [url (str "https://oauth.reddit.com/r/" subreddit "/" (name type))
        l   (or limit default-limit)
        request (client/get
                 url
                 {:oauth-token (access-token)
                  :headers     {"User-Agent" user-agent}
                  :query-params {"limit" l 
                                 "after" after}
                  :accept :json})
        body    (json/read-str (:body request) :key-fn keyword)]
    (if (contains? body :error)
      (throw (ex-info (str "unable to retrieve posts from reddit: "
                           (:error body) " - " (:message body))
                      {:causes #{:reddit-api}}))
      (let [posts-count (int ((comp :dist :data) body))
            posts  (doall
                    (map #(h/map-with-keys (:data %) data-keys)
                         (filter #(= (:kind %) "t3")
                                 ((comp :children :data) body))))
            cont   (not (when before
                          (some #(= before (:name %)) posts)))
            aft    ((comp :after :data) body)]
        (log/debug "count:" posts-count "after: " aft)
        (clojure.pprint/pprint posts)
        (if (and before cont)
          (apply conj posts
                 (get-subreddit-posts subreddit type
                                      {:limit l
                                       :data-keys data-keys
                                       :before before
                                       :after aft}))
          (if before
            (take-while #(not (= before (:name %))) posts)
            posts))))))

;; (reddit-trac.reddit/get-subreddit-posts "buildapcsales" :new {:limit 3 :data-keys [:id :name :author :url :permalink :title :created :link_flair_text] :before "t3_9l60e9"})
