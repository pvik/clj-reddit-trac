(ns reddit-trac.notify.template
  (:require [hiccup.core     :refer [html]]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [taoensso.timbre :as log])
  (:gen-class))

(defonce ^:private ^:const uri
  (:uri (clojure.edn/read-string (slurp "resources/config.edn"))))

(def time-formatter (f/formatter "yyyy-MM-dd HH:mm"))

(defn watch-table [watch]
  [:table
   [:tr
    [:td "Subreddit"]
    [:td "Keywords"]]
   (for [w watch]
     [:tr
      [:td
       [:a {:href (str "https://www.reddit.com/r/"(:subreddit w))}
        (str "/r/" (:subreddit w))]]
      [:td (:keywords w)]])])

(defn post-table [posts]
  [:table
   (for [p posts]
     [:tr
      [:td
       [:a {:href (str "https://www.reddit.com/r/"(:subreddit p))}
        (str "/r/" (:subreddit p))]]
      [:td
       (if (:link_flair_text p)
         (str "[" (:link_flair_text p) "] "))
       [:a {:href (:url p)} (:title p)]
       (str " (" (:domain p) ")")
       [:br]
       (str " posted at "
            (f/unparse time-formatter (c/from-long (long (* 1000 (:created p))))) " GMT")]
      [:td [:a {:href (str "https://www.reddit.com" (:permalink p))} "comments"]]])])

(defn watch-validate [{:keys [token watch]}]
  (html
   [:h4 "Welcome to Reddit-Trac!"]
   [:p  "A new Trac was created online as shown below."]
   [:p  "If you created this, please click "
    [:a
     {:href (str uri "/watch/validate/"
                 {:id watch}
                 "?email=" (:email watch)
                 "&token=" token)}
     "here"]
    ". If not please ignore/delete this email."]
   (watch-table watch)
   [:br]
   [:p "To know more about Reddit-Trac click "
    [:a {:href (str uri "/about")} "here"] "."]))

(defn watch-success [{:keys [token watch]}]
  (html
   [:h4 "Welcome to Reddit-Trac!"]
   [:p  "Your new Trac is being monitored :)"]
   (watch-table watch)
   [:br]
   [:p "To stop trac'ing this please click "
    [:a {:href (str uri "/watch/delete/"
                    {:id watch}
                    "?email=" (:email watch)
                    "&token=" token)}
     "here"] "."]
   [:p "To manage all your Trac's please click "
    [:a {:href (str uri "/watch/manage/"
                    {:id watch}
                    "?email=" (:email watch)
                    "&token=" token)}
     "here"] "."]))

(defn watch-found [{:keys [watches posts]}]
  (html
   [:h4 "Posts Trac'ed on Reddit!!"]
   (post-table posts)
   [:p  "Your active Tracs"]
   (watch-table watches)
   [:br]
   ;; [:p "To manage all your Trac's please click "
   ;;  [:a {:href (str uri "/watch/manage/"
   ;;                  {:id watch}
   ;;                  "?email=" (:email watch)
   ;;                  "&token=" token)}
   ;;   "here"] "."]
   ))

