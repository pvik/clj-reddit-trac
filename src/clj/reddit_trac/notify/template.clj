(ns reddit-trac.notify.template
  (:require [hiccup.core     :refer [html]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [taoensso.timbre :as log])
  (:gen-class))

(defonce ^:private ^:const uri
  (:uri (clojure.edn/read-string (slurp "resources/config.edn"))))

(defn watch-table [watch]
  [:table {:style "border-collapse: collapse; font-family: Arial, Verdana, sans-serif;"}
   [:tr 
    [:th {:style "border:1px solid black"} "Subreddit"]
    [:th {:style "border:1px solid black"} "Keywords"]
    [:th {:style "border:1px solid black"} "Ignore Keywords"]
    [:th {:style "border:1px solid black"} "Ignore Domain"]
    [:th {:style "border:1px solid black"} "Check Flair"]]
   (for [w watch]
     [:tr
      [:td {:style "border:1px solid black"}
       [:a {:href (str "https://www.reddit.com/r/"(:subreddit w))}
        (str "/r/" (:subreddit w))]]
      [:td {:style "border:1px solid black"}
       (:keywords w)]
      [:td {:style "border:1px solid black"}
       (or (:ignore_keywords w) (:ignore-keywords w))]
      [:td {:style "border:1px solid black"}
       (or (:ignore_domain w) (:ignore-domain w))]
      [:td {:style "border:1px solid black"}
       (or (:check_flair w) (:check-flair w))]])])

(defn post-table [posts]
  [:table {:style "border-collapse: collapse; font-family: Arial, Verdana, sans-serif;"}
   (for [p posts]
     [:tr {:style "border:1px solid Gray"}
      [:td {:style "border:1px solid Gray"}
       [:a {:href (str "https://www.reddit.com/r/"(:subreddit p))}
        (str "/r/" (:subreddit p))]]
      [:td {:style "border:1px solid Gray"}
       (if (:link_flair_text p)
         (str "[" (:link_flair_text p) "] "))
       [:a {:href (:url p)} (:title p)]
       [:p {:style "color:Gray; font-size:0.8em"}
        (str " (" (:domain p) ") ")
        (str " posted "
             (t/in-minutes
              (t/interval
               (c/from-long (long (* 1000 (:created_utc p)))) (t/now)))
             " minutes ago")]]
      [:td {:style "border:1px solid Gray"}
       [:a {:href (str "https://www.reddit.com" (:permalink p))} "comments"]]])])

(defn watch-validate [{:keys [token watch]}]
  (html
   [:h4 "Welcome to Reddit-Trac!"]
   [:p  "A new Trac was created online as shown below."]
   [:p  "If you created this, please click "
    [:a
     {:href (str uri "/validate.html?id=" (:id watch)
                 "&email=" (:email watch)
                 "&token=" token)}
     "here"]
    ". If not please ignore/delete this email."]
   (watch-table (vector watch))
   [:br]
   [:p "To know more about Reddit-Trac click "
    [:a {:href (str uri "/faq.html")} "here"] "."]))

(defn watch-success [{:keys [token watch]}]
  (html
   [:h4 "Welcome to Reddit-Trac!"]
   [:p  "Your new Trac is being monitored :)"]
   (watch-table (vector watch))
   [:br]
   [:p "To stop trac'ing this please click "
    [:a {:href (str uri "/delete.html"
                    "?email=" (:email watch)
                    "&token=" token
                    "&id=" (:id watch))}
     "here"] "."]
   [:p "To manage all your Trac's please click "
    [:a {:href (str uri "/manage.html"
                    "?email=" (:email watch)
                    "&token=" token)}
     "here"] "."]))

(defn watch-found [{:keys [watches posts token]}]
  (html
   [:h4 {:style "font-weight:bolder;"} "Posts Trac'ed on Reddit!!"]
   (post-table posts)
   [:p  "Your active Tracs"]
   (watch-table watches)
   [:br]
   [:p "Note: All times above are relative to when the email was sent."]
   [:p "To manage all your Trac's please click "
    [:a {:href (str uri "/manage.html"
                    "?email=" (:email (first watches))
                    "&token=" token)}
     "here"] "."]))

(defn manage-watch [{:keys [token email]}]
  (html
   [:h4 {:style "font-weight:bolder;"} "Manage Trac's"]
   [:p "To manage all your Trac's please click "
    [:a {:href (str uri "/manage.html"
                    "?email=" email
                    "&token=" token)}
     "here"] "."]))

