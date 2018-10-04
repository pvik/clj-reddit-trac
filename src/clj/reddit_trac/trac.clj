(ns reddit-trac.trac
  (:require [clojure.core.async :refer [go]]
            [reddit-trac.data.db :as db]
            [reddit-trac.reddit  :as r]
            [taoensso.timbre :as log]
            [clojure.string :as str]))

(def ^:const ^:private post-fields
  [:id :name :subreddit :author :url :permalink :domain :title
   :created :link_flair_text])

(def subreddit-before-name (atom {}))

(defn watch-matches-post? [watch post]
  (let [keywords (str/split (:keywords watch) #" ")
        ignore   (if (:ignore-keywords watch)
                   (str/split (:ignore-keywords watch) #" ")
                   [])
        search-str (str/lower-case
                    (if (:check-flair watch)
                      (str (:title post) (:link_flair_text post))
                      (:title post)))]
    (and
     (some #(str/includes? search-str %) keywords)
     (not (some #(str/includes? search-str %) ignore))
     (if (:ignore-domain post)
       (str/includes? (:domain post) (:ignore-domain post))
       true))))

(defn trac-by-subreddit [subreddit]
  (let [kw-subreddit (keyword subreddit)
        posts (r/get-subreddit-posts
               subreddit :new
               {:data-keys post-fields
                :before (kw-subreddit @subreddit-before-name)})
        first-post-name (:name (first posts))
        watches (db/get-entity :watch-subreddit
                               [:and [:= :active true]
                                [:= :subreddit subreddit]]
                               [:id :keywords, :ignore-keywords, :ignore-domain, :check-flair, :email]
                               :no-limit)]
    (if (> (count posts) 0)
      (do
        (swap! subreddit-before-name assoc kw-subreddit first-post-name)
        (reduce
         ;; compress results per email
         (fn [m [eml ws ps]]
           (let [m-eml (or (eml m) {})
                 m-watches (:watches m-eml)
                 m-posts (:posts m-eml)]
             (assoc m eml {:watches (apply conj m-watches ws)
                           :posts (apply conj m-posts ps)})))
         {}
         ;; seq of sequences which match posts for each watch
         (for [w watches
               :let [ps (filter #(watch-matches-post? w %) posts)]]
           [(keyword (:email w))
            [w]
            (vec ps)])))
      {})))

(defn trac []
  (let [subreddits 
        (map :subreddit (db/get-entity-distinct :watch-subreddit
                                                [:= :active true]
                                                [:subreddit]
                                                :no-limit))]
    (for [sr subreddits]
      (log/debug "Processing" sr))))
