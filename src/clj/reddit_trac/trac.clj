(ns reddit-trac.trac
  (:require [reddit-trac.data.db :as db]
            [reddit-trac.reddit  :as r]))

(def ^:const ^:private post-fields
  [:id :name :author :url :permalink :title :created :link_flair_text])

(defn trac-by-subreddit [subreddit]
  (let [posts (r/get-subreddit-posts subreddit
                                     :new
                                     {:data-keys post-fields})]))

(defn trac []
  (let [subreddits 
        (map :subreddit (db/get-entity-distinct :watch-subreddit
                                                [:= :active true]
                                                [:subreddit]
                                                :no-limit))]
    subreddits))
