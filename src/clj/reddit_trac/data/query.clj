(ns reddit-trac.data.query
  (:require [honeysql.helpers
             :refer [merge-where where sset insert-into values order-by]
             :as helpers]
            [clojure.string :as str]
            [taoensso.timbre   :as log]))

(def ^:const pg-limit 50)

(def ^:const db-schema
  (:schema (:db (clojure.edn/read-string
                 (slurp "resources/config.edn")))))

(defn table-name [tbl]
  (keyword (str db-schema "." (str/replace (name tbl) #"-" "_"))))

(defn set-query-limit [query limit]
  (if (= limit :no-limit)
    query
    (assoc query :limit limit)))

(defn set-query-offset [query offset]
  (if offset
    (assoc query :offset offset)
    query))

(defn select [tbl {:keys [fields limit page order]}]
  (let [fs     (or fields [:*])
        lmt    (or limit pg-limit)
        offset (if page (* (- page 1) lmt) nil)
        ord    (or order :desc)]
    (log/debug "select" fs "from" tbl)
    (-> {:select fs
         :from   [(table-name tbl)]}
        (set-query-limit lmt)
        (set-query-offset offset)
        (order-by [:id :desc]))))

;; Entity

(defn create-entity [entity data]
  (->
   (insert-into (table-name entity))
   (values [data])))

(defn get-entity [entity & [where-clause fs]]
  (-> (select entity fs)
      (merge-where where-clause)))

(defn get-entity-distinct [entity & [where-clause fs]]
  (-> (select entity fs)
      (modifiers :distinct)
      (merge-where where-clause)))

(defn update-entity [entity data]
  (->
   (helpers/update (table-name entity))
   (sset data)
   (where [:= :id (:id data)])))

(defn delete-entity [entity id]
  (->
   (helpers/delete-from (table-name entity))
   (where [:= :id id])))
