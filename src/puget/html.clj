(ns puget.html
  "This namespace defines methods for the :html-inline
  and :html-classes :color-markup options."
  (:require
    [clojure.string :as str]
    [puget.color :as color]))

(def style-attribute
  "Map from keywords usable in a color-scheme value to vectors
  representing css style attributes"
  {:none       nil
   :bold       [:font-weight "bold"]
   :underline  [:text-decoration "underline"]
   :blink      [:text-decoration "blink"]
   :reverse    nil
   :hidden     [:visibility "hidden"]
   :strike     [:text-decoration "line-through"]
   :black      [:color "black"]
   :red        [:color "red"]
   :green      [:color "green"]
   :yellow     [:color "yellow"]
   :blue       [:color "blue"]
   :magenta    [:color "magenta"]
   :cyan       [:color "cyan"]
   :white      [:color "white"]
   :fg-256     nil
   :fg-reset   nil
   :bg-black   [:background-color "black"]
   :bg-red     [:background-color "red"]
   :bg-green   [:background-color "green"]
   :bg-yellow  [:background-color "yellow"]
   :bg-blue    [:background-color "blue"]
   :bg-magenta [:background-color "magenta"]
   :bg-cyan    [:background-color "cyan"]
   :bg-white   [:background-color "white"]
   :bg-256     nil
   :bg-reset   nil})


(defn style
  "Returns a formatted style attribute for a span given a seq of
  keywords usable in a :color-scheme value"
  [codes]
  (let [attributes (filter identity (map style-attribute codes))]
    (str "style=\""
         (str/join ";" (map (fn [[k v]] (str (name k) ":" v)) attributes))
         "\"")))


(defn escape-html
  "Escapes special characters into html entities"
  [text]
  (let [escaped-text (.. ^String text
                         (replace "&"  "&amp;")
                         (replace "<"  "&lt;")
                         (replace ">"  "&gt;")
                         (replace "\"" "&quot;"))
        spans (.split escaped-text "(?=&)")]
    (reduce (fn [acc span]
              (case (first span)
                nil acc
                \& (let [[escaped span] (.split span "(?<=;)" 2)
                         acc (conj acc [:escaped escaped])]
                     (if (seq span)
                       (conj acc span)
                       acc))
                (conj acc span)))
            [:span]
            spans)))


(defmethod color/document :html-inline
  [element text options]
  (if-let [codes (-> options :color-scheme (get element) seq)]
    [:span [:pass "<span " (style codes) ">"]
     (escape-html text)
     [:pass "</span>"]]
    (escape-html text)))


(defmethod color/text :html-inline
  [element text options]
  (if-let [codes (-> options :color-scheme (get element) seq)]
    (str "<span " (style codes) ">" (escape-html text) "</span>")
    (escape-html text)))


(defmethod color/document :html-classes
  [element text options]
  [:span [:pass "<span class=\"" (name element) "\">"]
   (escape-html text)
   [:pass "</span>"]])


(defmethod color/text :html-classes
  [element text options]
  (str "<span class=\"" (name element) "\">" (escape-html text) "</span>"))
