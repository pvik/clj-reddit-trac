(ns reddit-trac.helper
  (:gen-class))

(defn as-int [int-str]
  (java.lang.Integer/parseInt int-str))

(defn as-float [float-str]
  (java.lang.Float/parseFloat float-str))

(defn as-bool [bool-str]
  (or (#{"true"} bool-str) false))

(defn map-with-keys [map keys-vec]
  (if keys-vec
    (select-keys map keys-vec)
    map))

