(ns wmatson.clj-scryer.hiccup
  (:require [hiccup.core :as hccp]
            [orchard.inspect :as ori]))

(defn- concat-line-strings [line]
  (->> line
       (partition-by string?)
       (map-indexed #(if (even? %1)
                       (apply str %2)
                       (apply concat %2)))))

(defn- iline->hiccup [line]
  (->> line
       concat-line-strings
       (reduce (fn [acc [k display idx :as next]]
                 (conj acc (if (#{:value} k)
                             [:span
                              [:span.is-family-monospace display]
                              [:button.button "&gt;"]]
                             [:span.tag next])))
               [:div])))

(defn- irender->hiccup [inspector-rendered]
  (conj [:div.container
         [:div.level [:button "Prev"] [:button "Up"] [:button "Next"]]]
        (->> inspector-rendered
             (partition-by (comp #{:newline} first))
             (partition-all 1 2)
             (apply concat)
             (map iline->hiccup))))

(defn- surround-in-page [hiccup]
  [:html
   [:head
    [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/bulma/0.7.2/css/bulma.min.css"}]]
   [:body hiccup]])

(defn inspect [value]
  (let [hiccup (irender->hiccup (:rendered (ori/start (ori/fresh) value)))
        temp-file (java.io.File/createTempFile "inspect" ".html")]
    (spit temp-file (hccp/html (surround-in-page hiccup)))
    (clojure.java.browse/browse-url (.. temp-file toURI toString))))

(comment
  (inspect (map #(hash-map :num %) (range))))