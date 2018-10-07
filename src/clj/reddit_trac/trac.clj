(ns reddit-trac.trac
  (:require [clojure.core.async :refer [go-loop <!! <!]]
            [clojure.string :as str]
            [clj-time.core :as t]
            [chime :as c]
            [taoensso.timbre :as log]
            [reddit-trac.data.db :as db]
            [reddit-trac.reddit  :as r]
            [reddit-trac.notify.mail :as notify]
            [reddit-trac.notify.template :as template])
  (:gen-class))

(def ^:const ^:private post-fields
  [:id :name :subreddit :author :url :permalink :domain :title
   :created :link_flair_text])

(defonce ^:const trac-interval
  (:trac-interval (clojure.edn/read-string (slurp "resources/config.edn"))))

(defonce subreddit-before-name
  (atom
   (try
     (clojure.edn/read-string (slurp "resources/cache.edn"))
     (catch Exception e
       ;; use an empty map on exception
       {}))))

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

(defn merge-map-by-email [m [eml ws ps]]
  (let [m-eml (or (eml m) {})
        m-watches (:watches m-eml)
        m-posts (:posts m-eml)]
    (assoc m eml {:watches (apply conj m-watches ws)
                  :posts (apply conj m-posts ps)})))

(defn merge-map [m1 m2]
  (reduce
   #(merge-map-by-email % [%2 (:watches (%2 m2)) (:posts (%2 m2))])
   m1 (keys m2)))

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
                               [:id :keywords, :ignore-keywords,
                                :ignore-domain, :check-flair,
                                :email :subreddit]
                               :no-limit)]
    (if (> (count posts) 0)
      (do
        (swap! subreddit-before-name assoc kw-subreddit first-post-name)
        (reduce
         ;; compress results per email
         merge-map-by-email
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
                                                :no-limit))
        matches        (for [sr subreddits] (trac-by-subreddit sr))
        merged-matches (reduce
                        (fn [m1 m2]
                          (merge-map m1 m2))
                        (first matches) (rest matches))
        _ (clojure.pprint/pprint merged-matches)]    
    (spit "resources/cache.edn" (pr-str @subreddit-before-name))
    (doall
     (map #(if (:posts (second %))
             (do
               (log/debug "FOUND POSTS!")
               (notify/send-mail {:to (name (first %))
                                  :subject "Reddit-Trac: Posts found!"
                                  :body (template/watch-found (second %))})))
          merged-matches))))

(defn trac-sched []
  (trac) ;; initial run
  (let [chimes (c/chime-ch (map #(t/from-now (t/minutes %))
                                (for [r    (range)
                                      intr trac-interval]
                                  (+ (* r 60) intr))))]
    (<!! (go-loop []
           (when-let [chiming-at (<! chimes)]
             (prn "Trac'ing at:" chiming-at)
             (trac)
             (recur))))))
