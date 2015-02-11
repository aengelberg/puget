(ns puget.data
  "Code to handle custom data represented as tagged EDN values."
  (:require
    [clojure.data.codec.base64 :as b64])
  (:import
    (java.net URI)
    (java.util Date TimeZone UUID)))


(defprotocol ExtendedNotation
  "Protocol for types which use extended notation for EDN representation."

  (->edn
    [value]
    "Converts the given value into a tagged value representation for EDN
    serialization. Returns a `TaggedLiteral` record."))


(defrecord TaggedLiteral
  [tag form]

  ExtendedNotation

  (->edn
    [this]
    this)


  Object

  (toString
    [this]
    (str \# tag \space (pr-str form))))


(defmethod print-method TaggedLiteral
  [v ^java.io.Writer w]
  (.write w (str v)))


(defn tagged-literal
  "Creates a generic tagged value record to represent some EDN value. This is
  suitable for use as a default-data-reader function."
  [tag value]
  {:pre [(symbol? tag)]}
  (->TaggedLiteral tag value))


(defn tagged-literal?
  "Returns true if the given value is a tagged-literal form."
  [value]
  (instance? TaggedLiteral value))


(defn edn-str
  "Converts the given value to a tagged EDN string. Falls back to `pr-str` if
  `v` does not use extended notation."
  ^String
  [v]
  (pr-str (if (satisfies? ExtendedNotation v) (->edn v) v)))



;; ## Extension Functions

(defmacro extend-tagged-value
  "Defines an EDN representation for a type `t`. The tag will be the symbol
  given for `tag` and the literal form will be generated by applying `expr` to
  the original value."
  [t tag expr]
  `(let [value-fn# ~expr]
     (extend-type ~t
       ExtendedNotation
       (->edn
         [this#]
         (tagged-literal ~tag (value-fn# this#))))))


(defmacro extend-tagged-str
  "Defines an EDN representation for the given type by converting it to a
  string form."
  [t tag]
  `(extend-tagged-value ~t ~tag str))


(defmacro extend-tagged-map
  "Defines an EDN representation for the given type by converting it to a
  map form."
  [t tag]
  `(extend-tagged-value ~t ~tag
     (comp (partial into {}) seq)))



;; ## Basic EDN Types

(defn- format-utc
  "Produces an ISO-8601 formatted date-time string from the given Date."
  [^Date date]
  (-> "yyyy-MM-dd'T'HH:mm:ss.SSS-00:00"
      java.text.SimpleDateFormat.
      (doto (.setTimeZone (TimeZone/getTimeZone "GMT")))
      (.format date)))


;; `inst` tags a date-time instant represented as an ISO-8601 string.
(extend-tagged-value Date 'inst format-utc)


;; `uuid` tags a universally-unique identifier string.
(extend-tagged-str UUID 'uuid)


;; `puget/bin` tags byte data represented as a base64-encoded string.
(extend-tagged-value
  (class (byte-array 0))
  'puget/bin
  #(->> % b64/encode (map char) (apply str)))


(defn read-bin
  "Reads a base64-encoded string into a byte array. Suitable as a data-reader
  for `puget/bin` literals."
  ^bytes
  [^String bin]
  (b64/decode (.getBytes bin)))


;; `puget/uri` tags a Universal Resource Identifier string.
(extend-tagged-str URI 'puget/uri)


(defn read-uri
  "Constructs a URI from a string value. Suitable as a data-reader for
  `puget/uri` literals."
  ^URI
  [^String uri]
  (URI. uri))